package com.maxezify.enderportals.block;

import com.maxezify.enderportals.ModBlockEntities;
import com.maxezify.enderportals.ModItems;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.maxezify.enderportals.tardis.TardisData;
import com.maxezify.enderportals.tardis.TardisHelper;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * La porte du TARDIS active. Invisible pour le moteur de rendu classique :
 * c'est le {@code TardisDoorRenderer} qui la dessine, avec son fondu de
 * matérialisation. Le block entity (moitié basse uniquement) porte l'identité
 * du TARDIS.
 */
public class TardisDoorBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public TardisDoorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Le TARDIS occupe un bloc entier d'épaisseur : un vrai caisson.
        return Shapes.block();
    }

    /** Épaisseur des parois du caisson pour la collision (2 pixels). */
    private static final double WALL = 0.125;

    /** Coque ouverte pré-calculée par orientation : seule la face avant est franchissable. */
    private static final java.util.Map<Direction, VoxelShape> OPEN_SHAPES =
            java.util.Arrays.stream(Direction.values())
                    .filter(d -> d.getAxis().isHorizontal())
                    .collect(java.util.stream.Collectors.toMap(java.util.function.Function.identity(),
                            TardisDoorBlock::buildOpenShape));

    /**
     * Porte ouverte : on ne traverse que par l'avant (le côté {@code FACING}).
     * Les deux flancs et le dos restent pleins — impossible de passer au
     * travers du caisson. Porte fermée : bloc plein.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
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

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)
                && !neighborState.is(this)) {
            // L'autre moitié a disparu : cette moitié disparaît aussi.
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        // La clé est gérée par TardisKeyItem#useOn : on la laisse passer.
        if (player.getMainHandItem().is(ModItems.TARDIS_KEY.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && !operate(state, level, pos, player, false)) {
            player.displayClientMessage(Component.translatable("enderportals.message.locked"), true);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Ce que fait un clic sur une porte, la clé en main ou non.
     *
     * <p>Une porte s'ouvre <b>à la main</b>, comme toutes les portes du jeu, et
     * seulement pour qui elle est. La clé n'a jamais eu à servir de poignée :
     * son affaire, c'est de <b>lier</b> une porte à un joueur, et de la faire
     * <b>apparaître et disparaître</b>. Confondre les deux obligeait à sortir sa
     * clé pour le geste le plus banal — et, quand la clé était restée au lieu
     * d'une mort, murait la base : les affaires tombent là où l'on meurt, et si
     * le lit est dans l'Ender on réapparaît à l'intérieur, sans clé. Reforger
     * en demande une perle de l'Ender, or <b>aucune créature n'apparaît dans
     * l'Ender</b>.</p>
     *
     * <p>La porte <b>intérieure</b> a un cas de plus : si la porte extérieure
     * est rangée, le clic la rappelle au lieu d'ouvrir. Ouvrir seul ne servirait
     * à rien — il n'y aurait rien de l'autre côté à franchir.</p>
     *
     * <p>Le propriétaire, et lui seul. Un allié venu par un Passage repart par
     * où il est entré ; il n'a pas à ouvrir la porte d'autrui, ni à la faire
     * apparaître ailleurs.</p>
     *
     * @param dismiss dématérialiser au lieu d'ouvrir. C'est l'accroupissement,
     *                et il demande la clé : sans elle, personne ne peut faire
     *                disparaître sa propre issue
     * @return faux si ce joueur n'a rien à faire ici — à l'appelant de le dire
     */
    public static boolean operate(BlockState state, Level level, BlockPos pos, Player player, boolean dismiss) {
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!(level.getBlockEntity(base) instanceof TardisDoorBlockEntity door)
                || door.isDematerializing() || door.getTardisId() == null) {
            return false;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        TardisData data = TardisStateManager.get(server).getTardis(door.getTardisId());
        if (data == null || !player.getUUID().equals(data.ownerUuid)) {
            return false;
        }
        if (dismiss) {
            if (data.deployed) {
                TardisHelper.dismissExterior(server, data);
                player.displayClientMessage(
                        Component.translatable("enderportals.message.tardis_dismissed"), true);
            }
            return true;
        }
        if (!data.deployed && door.isInterior()) {
            // Le rappel ne peut pas se contenter d'échouer : voir
            // TardisHelper.recallExterior.
            TardisHelper.recallExterior(server, data, player);
        } else {
            TardisHelper.setDoorsOpen(server, data, !data.open);
        }
        return true;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !state.getValue(OPEN) || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (player.isOnPortalCooldown() || player.isSpectator()) {
            return;
        }
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!(level.getBlockEntity(base) instanceof TardisDoorBlockEntity door)
                || door.isDematerializing() || door.getTardisId() == null) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        TardisData data = TardisStateManager.get(server).getTardis(door.getTardisId());
        if (data == null || data.immptlActive) {
            // immptlActive : Immersive Portals gère lui-même la traversée.
            return;
        }
        if (door.isInterior()) {
            if (data.deployed) {
                TardisHelper.exitTardis(player, data);
            }
        } else {
            TardisHelper.enterTardis(player, data);
        }
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new TardisDoorBlockEntity(pos, state) : null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == ModBlockEntities.TARDIS_DOOR.get()
                ? (BlockEntityTicker<T>) (BlockEntityTicker<TardisDoorBlockEntity>) TardisDoorBlockEntity::tick
                : null;
    }
}
