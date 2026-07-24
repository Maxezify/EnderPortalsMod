package com.maxezify.enderportals.block;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.tardis.TardisHelper;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

/**
 * La porte inactive : un caisson dormant de deux blocs de haut et d'un bloc
 * d'épaisseur, posable et cassable à la pioche. Frappée à la Mace en pleine
 * chute (≥ {@link EnderPortalsMod#ACTIVATION_FALL_DISTANCE} blocs), elle
 * s'éveille.
 */
public class InactiveTardisDoorBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public InactiveTardisDoorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return level.getBlockState(pos.below()).is(this);
        }
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
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
        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Rituel de la Mace. Retourne true si la porte s'est éveillée.
     */
    public static boolean tryActivate(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack mace) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof InactiveTardisDoorBlock)) {
            return false;
        }
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;

        if (level.dimension().equals(ModDimensions.ENDER_WORLD)) {
            player.displayClientMessage(Component.translatable("enderportals.message.no_create_inside"), true);
            level.playSound(null, base, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 0.5f);
            return false;
        }
        if (TardisStateManager.get(level.getServer()).findByOwner(player.getUUID()) != null) {
            player.displayClientMessage(Component.translatable("enderportals.message.already_owner"), true);
            level.playSound(null, base, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 0.5f);
            return false;
        }
        if (!level.mayInteract(player, base)) {
            player.displayClientMessage(Component.translatable("enderportals.message.protected"), true);
            return false;
        }

        float fall = player.fallDistance;
        if (fall < EnderPortalsMod.ACTIVATION_FALL_DISTANCE) {
            player.displayClientMessage(Component.translatable("enderportals.message.not_falling",
                    (int) EnderPortalsMod.ACTIVATION_FALL_DISTANCE, (int) fall), true);
            level.playSound(null, base, SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 0.8f, 0.9f);
            return false;
        }

        player.fallDistance = 0.0f;
        Vec3 impact = Vec3.atBottomCenterOf(base);
        level.playSound(null, base, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2f, 0.8f);
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, impact.x, impact.y, impact.z, 1, 0.0, 0.0, 0.0, 0.0);

        Direction facing = level.getBlockState(base).getValue(FACING);
        TardisHelper.activate(level, base, facing, player);
        mace.hurtAndBreak(10, player, EquipmentSlot.MAINHAND);
        return true;
    }
}
