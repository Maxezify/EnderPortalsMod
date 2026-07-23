package com.maxezify.enderportals.block;

import com.maxezify.enderportals.ModBlockEntities;
import com.maxezify.enderportals.ModItems;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.maxezify.enderportals.tardis.TardisData;
import com.maxezify.enderportals.tardis.TardisHelper;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * La porte du TARDIS active. Invisible pour le moteur de rendu classique :
 * c'est le {@code TardisDoorRenderer} qui la dessine, avec son fondu de
 * matérialisation. Le block entity (moitié basse uniquement) porte l'identité
 * du TARDIS.
 */
public class TardisDoorBlock extends Block implements BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = Properties.OPEN;

    public TardisDoorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Le TARDIS occupe un bloc entier d'épaisseur : un vrai caisson.
        return VoxelShapes.fullCube();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(OPEN) ? VoxelShapes.empty() : VoxelShapes.fullCube();
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)
                && !neighborState.isOf(this)) {
            // L'autre moitié a disparu : cette moitié disparaît aussi.
            return net.minecraft.block.Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // La clé est gérée par TardisKeyItem#useOnBlock : on la laisse passer.
        if (player.getMainHandStack().isOf(ModItems.TARDIS_KEY)) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            player.sendMessage(Text.translatable("enderportals.message.locked"), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient || !state.get(OPEN) || !(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (player.hasPortalCooldown() || player.isSpectator()) {
            return;
        }
        BlockPos base = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
        if (!(world.getBlockEntity(base) instanceof TardisDoorBlockEntity door)
                || door.isDematerializing() || door.getTardisId() == null) {
            return;
        }
        MinecraftServer server = world.getServer();
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
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? new TardisDoorBlockEntity(pos, state) : null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.TARDIS_DOOR
                ? (BlockEntityTicker<T>) (BlockEntityTicker<TardisDoorBlockEntity>) TardisDoorBlockEntity::tick
                : null;
    }
}
