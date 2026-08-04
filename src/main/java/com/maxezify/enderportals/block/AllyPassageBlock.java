package com.maxezify.enderportals.block;

import com.maxezify.enderportals.ModBlockEntities;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.block.entity.AllyPassageBlockEntity;
import com.maxezify.enderportals.tardis.AllyPassageHelper;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le Passage des Alliés : un caisson clair, de deux blocs de haut, qui relie la
 * parcelle de son propriétaire à celle d'un allié.
 *
 * <p><b>Même structure que la Porte de l'Ender</b>, et pas par goût de la
 * symétrie : c'est la seule qui fonctionne avec Immersive Portals. Trois traits
 * y sont indissociables.</p>
 * <ol>
 *   <li><b>{@link RenderShape#INVISIBLE} + un renderer dédié.</b> L'arche de la
 *       0.15.0 était un modèle de bloc ordinaire dont le voile — un pavé de
 *       2 px — occupait très exactement le plan du portail, et dont les
 *       montants traversaient ce plan de part en part. Un portail découpé par
 *       la géométrie qu'il double se voit déformé. Le caisson, lui, est creux :
 *       son embrasure ne contient rien.</li>
 *   <li><b>Une seule face franchissable.</b> L'arche laissait passer par
 *       l'avant <i>et</i> par l'arrière. Un portail d'Immersive Portals ne
 *       téléporte qu'au franchissement par sa normale : entré par le dos, on
 *       traversait l'arche de part en part sans rien déclencher.</li>
 *   <li><b>Le plan du portail au centre du bloc du haut, 0,8 × 1,9.</b> Les
 *       cotes de l'embrasure de la Porte, validées en jeu.</li>
 * </ol>
 *
 * <p>Le passage ne s'ouvre jamais de lui-même. Toute la décision est prise au
 * {@link FriendshipConsoleBlock Contrôle de l'amitié} accolé, et ce bloc-ci
 * n'est que la conséquence visible d'un lien noté côté serveur — c'est pour ça
 * qu'il ne porte aucune identité : cassé et reposé, il retrouve son état depuis
 * le registre.</p>
 */
public class AllyPassageBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<PassagePhase> PHASE = EnumProperty.create("phase", PassagePhase.class);

    /** Épaisseur des parois du caisson pour la collision (2 pixels). */
    private static final double WALL = 0.125;

    /**
     * Coque franchissable pré-calculée par orientation : seule la face avant
     * (le côté {@code FACING}) laisse passer. Les deux flancs et le dos restent
     * pleins.
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
        manager.addPassage(player.getUUID(), pos, state.getValue(FACING));
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
     * Casser un passage rompt le lien qu'il portait, des deux côtés, et le
     * retire du registre. Les autres arches du joueur ne sont pas touchées :
     * chacune porte son propre lien.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER && !newState.is(this)) {
            MinecraftServer server = level.getServer();
            if (server != null) {
                AllyPassageHelper.demolish(server, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ------------------------------------------------------------------
    // Formes et rendu
    // ------------------------------------------------------------------

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Dessiné par AllyPassageRenderer : le caisson doit être creux, et un
        // modèle de bloc ne sait pas laisser son embrasure vide sans laisser
        // aussi passer la collision.
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Le caisson occupe un bloc entier : c'est lui qu'on vise à la pioche.
        return Shapes.block();
    }

    /**
     * Passage franchissable : on ne traverse que par l'avant. Passage clos ou
     * en cours d'ouverture : bloc plein — et c'est essentiel, car aucun portail
     * ne double l'arche tant qu'elle n'a pas fini de s'ouvrir.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        if (!isCrossable(state)) {
            return Shapes.block();
        }
        return OPEN_SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    private static VoxelShape buildOpenShape(Direction facing) {
        VoxelShape shape = Shapes.empty();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side != facing) {
                shape = Shapes.or(shape, wall(side));
            }
        }
        return shape;
    }

    /** Paroi verticale plaquée contre la face donnée du bloc. */
    private static VoxelShape wall(Direction side) {
        return switch (side) {
            case NORTH -> Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, WALL);
            case SOUTH -> Shapes.box(0.0, 0.0, 1.0 - WALL, 1.0, 1.0, 1.0);
            case WEST -> Shapes.box(0.0, 0.0, 0.0, WALL, 1.0, 1.0);
            case EAST -> Shapes.box(1.0 - WALL, 0.0, 0.0, 1.0, 1.0, 1.0);
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
     * L'animation d'ouverture et le halo du passage ouvert. Purement client :
     * la phase est déjà synchronisée, chacun l'anime chez lui.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        PassagePhase phase = state.getValue(PHASE);
        if (phase == PassagePhase.CLOSED) {
            return;
        }
        Direction facing = state.getValue(FACING);
        // Les particules naissent devant l'embrasure, pas dans les parois : le
        // caisson est creux, et ses flancs sont opaques.
        double front = 0.30;
        int count = phase == PassagePhase.OPENING ? 6 : 2;
        for (int i = 0; i < count; i++) {
            double across = (random.nextDouble() - 0.5) * 0.7;
            double x = pos.getX() + 0.5 + facing.getStepX() * front - facing.getStepZ() * across;
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + 0.5 + facing.getStepZ() * front + facing.getStepX() * across;
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

    // ------------------------------------------------------------------
    // Block entity
    // ------------------------------------------------------------------

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new AllyPassageBlockEntity(pos, state) : null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        return type == ModBlockEntities.ALLY_PASSAGE.get()
                ? (BlockEntityTicker<T>) (BlockEntityTicker<AllyPassageBlockEntity>) AllyPassageBlockEntity::tick
                : null;
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
