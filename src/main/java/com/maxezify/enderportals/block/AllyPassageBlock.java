package com.maxezify.enderportals.block;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.tardis.AllyPassageHelper;
import com.maxezify.enderportals.tardis.TardisData;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le Passage des Alliés : une arche claire, de deux blocs de haut, qui relie la
 * parcelle de son propriétaire à celle d'un allié.
 *
 * <p>Il ne s'ouvre jamais de lui-même. Toute la décision est prise au
 * {@link FriendshipConsoleBlock Contrôle de l'amitié} accolé, et ce bloc-ci
 * n'est que la conséquence visible d'un lien noté côté serveur — c'est pour ça
 * qu'il ne porte aucune identité : cassé et reposé, il retrouve son état depuis
 * le registre.</p>
 */
public class AllyPassageBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<PassagePhase> PHASE = EnumProperty.create("phase", PassagePhase.class);

    /** Épaisseur des montants de l'arche, pour la collision. */
    private static final double JAMB = 0.1875;

    /**
     * Coque franchissable, par orientation : seuls les deux montants latéraux
     * restent solides. On entre par l'avant et on sort par l'arrière — un
     * passage, pas un caisson.
     */
    private static final Map<Direction, VoxelShape> OPEN_SHAPES =
            Arrays.stream(Direction.values())
                    .filter(direction -> direction.getAxis().isHorizontal())
                    .collect(Collectors.toMap(Function.identity(), AllyPassageBlock::buildOpenShape));

    public AllyPassageBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(PHASE, PassagePhase.CLOSED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, PHASE);
    }

    // ------------------------------------------------------------------
    // Pose et retrait des deux moitiés
    // ------------------------------------------------------------------

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1
                || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER),
                Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        if (level.isClientSide || !(placer instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        if (manager.findByOwner(player.getUUID()) == null) {
            player.displayClientMessage(Component.translatable("enderportals.message.passage_no_door"), true);
            return;
        }
        manager.setPassage(player.getUUID(), pos, state.getValue(FACING));
        player.displayClientMessage(Component.translatable("enderportals.message.passage_placed"), true);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)
                && !neighborState.is(this)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER || level.getBlockState(pos.above()).is(this)
                || level.getBlockState(pos.above()).canBeReplaced();
    }

    /**
     * Casser un passage rompt le lien qu'il portait, des deux côtés. Sans cela
     * l'allié garderait un passage ouvert vers un néant.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER && !newState.is(this)) {
            MinecraftServer server = level.getServer();
            if (server != null) {
                TardisStateManager manager = TardisStateManager.get(server);
                TardisData data = manager.findByPassage(pos);
                if (data != null && data.ownerUuid != null) {
                    UUID other = manager.allies().closeLink(data.ownerUuid);
                    AllyPassageHelper.closeBoth(server, data.ownerUuid, other);
                    data.passagePos = null;
                    manager.setDirty();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ------------------------------------------------------------------
    // Formes
    // ------------------------------------------------------------------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        if (!isCrossable(state)) {
            return Shapes.block();
        }
        return OPEN_SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    private static VoxelShape buildOpenShape(Direction facing) {
        Direction left = facing.getCounterClockWise();
        return Shapes.or(jamb(left), jamb(left.getOpposite()));
    }

    private static VoxelShape jamb(Direction side) {
        return switch (side) {
            case NORTH -> Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, JAMB);
            case SOUTH -> Shapes.box(0.0, 0.0, 1.0 - JAMB, 1.0, 1.0, 1.0);
            case WEST -> Shapes.box(0.0, 0.0, 0.0, JAMB, 1.0, 1.0);
            case EAST -> Shapes.box(1.0 - JAMB, 0.0, 0.0, 1.0, 1.0, 1.0);
            default -> Shapes.empty();
        };
    }

    // ------------------------------------------------------------------
    // Interaction, traversée, animation
    // ------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            PassagePhase phase = state.getValue(PHASE);
            player.displayClientMessage(Component.translatable(isCrossable(state)
                    ? "enderportals.message.passage_open_hint"
                    : "enderportals.message.passage_closed_hint"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // THROUGH est exclu volontairement : Immersive Portals y assure la
        // traversée, et les deux mécanismes ensemble se marcheraient dessus.
        if (level.isClientSide || state.getValue(PHASE) != PassagePhase.OPEN) {
            return;
        }
        if (!(entity instanceof ServerPlayer player) || player.isOnPortalCooldown() || player.isSpectator()) {
            return;
        }
        AllyPassageHelper.cross(player, pos, state.getValue(HALF));
    }

    /**
     * L'animation d'ouverture et le voile du passage ouvert. Purement client :
     * la phase est déjà synchronisée, chacun l'anime chez lui.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        PassagePhase phase = state.getValue(PHASE);
        if (phase == PassagePhase.CLOSED) {
            return;
        }
        int count = phase == PassagePhase.OPENING ? 6 : 2;
        Direction facing = state.getValue(FACING);
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            if (phase == PassagePhase.OPENING) {
                // Les étincelles convergent vers l'axe du passage : on voit le
                // portail se nouer avant de s'ouvrir.
                level.addParticle(ParticleTypes.END_ROD, x, y, z,
                        -facing.getStepX() * 0.05, 0.02, -facing.getStepZ() * 0.05);
            } else {
                level.addParticle(ParticleTypes.GLOW, x, y, z, 0.0, 0.01, 0.0);
            }
        }
    }

    /**
     * Fin de l'animation : le passage devient franchissable.
     *
     * <p>Un tick programmé plutôt qu'un block entity — l'attente est unique,
     * connue d'avance et sans état à conserver. Un block entity ne servirait
     * qu'à compter, et il faudrait le synchroniser.</p>
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PHASE) != PassagePhase.OPENING) {
            return;
        }
        BlockPos base = baseOf(state, pos);
        // C'est le serveur qui décide de la phase d'arrivée — OPEN ou THROUGH
        // selon qu'Immersive Portals ait pris la main — et il la pose des deux
        // côtés à la fois. Voir AllyPassageHelper.finishOpening.
        AllyPassageHelper.finishOpening(level.getServer(), base);
        level.playSound(null, base, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.5f, 1.8f);
    }

    /** Le passage est-il franchissable ? Les deux phases ouvertes le sont. */
    public static boolean isCrossable(BlockState state) {
        PassagePhase phase = state.getValue(PHASE);
        return phase == PassagePhase.OPEN || phase == PassagePhase.THROUGH;
    }

    /** Le bloc de base d'un passage, quelle que soit la moitié désignée. */
    public static BlockPos baseOf(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    /** Ce bloc est-il un Passage des Alliés ? Raccourci pour le panneau. */
    public static boolean isPassage(BlockState state) {
        return state.is(ModBlocks.ALLY_PASSAGE.get());
    }
}
