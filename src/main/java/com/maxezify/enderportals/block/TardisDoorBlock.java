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

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
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
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("enderportals.message.locked"), true);
        }
        return InteractionResult.SUCCESS;
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
