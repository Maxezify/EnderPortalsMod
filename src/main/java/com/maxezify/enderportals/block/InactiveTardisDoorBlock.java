package com.maxezify.enderportals.block;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.tardis.TardisHelper;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * La porte inactive : un caisson dormant de deux blocs de haut et d'un bloc
 * d'épaisseur (même gabarit que la porte éveillée), posable et cassable à la
 * pioche. Pour l'éveiller, il faut reproduire l'attaque écrasante de la
 * Mace : chuter d'au moins {@link EnderPortalsMod#ACTIVATION_FALL_DISTANCE}
 * blocs et la frapper pendant la chute. L'impact absorbe les dégâts de chute
 * du joueur.
 */
public class InactiveTardisDoorBlock extends Block {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public InactiveTardisDoorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        if (pos.getY() < world.getTopY() - 1 && world.getBlockState(pos.up()).canReplace(ctx)) {
            return getDefaultState()
                    .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                    .with(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER),
                Block.NOTIFY_ALL);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            return world.getBlockState(pos.down()).isOf(this);
        }
        return world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        net.minecraft.block.enums.DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)
                && !neighborState.isOf(this)) {
            return Blocks.AIR.getDefaultState();
        }
        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN
                && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    /**
     * Rituel de la Mace. Retourne true si la porte s'est éveillée.
     */
    public static boolean tryActivate(ServerWorld world, BlockPos pos, ServerPlayerEntity player, ItemStack mace) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof InactiveTardisDoorBlock)) {
            return false;
        }
        BlockPos base = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;

        // Pas de porte dans la porte : le monde de l'Ender refuse le rituel.
        if (world.getRegistryKey().equals(ModDimensions.ENDER_WORLD)) {
            player.sendMessage(Text.translatable("enderportals.message.no_create_inside"), true);
            world.playSound(null, base, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.6f, 0.5f);
            return false;
        }
        // Une seule porte par personne.
        if (TardisStateManager.get(world.getServer()).findByOwner(player.getUuid()) != null) {
            player.sendMessage(Text.translatable("enderportals.message.already_owner"), true);
            world.playSound(null, base, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.6f, 0.5f);
            return false;
        }
        // Respecte la spawn protection, le mode aventure et les mods de claim.
        if (!world.canPlayerModifyAt(player, base)) {
            player.sendMessage(Text.translatable("enderportals.message.protected"), true);
            return false;
        }

        float fall = player.fallDistance;
        if (fall < EnderPortalsMod.ACTIVATION_FALL_DISTANCE) {
            player.sendMessage(Text.translatable("enderportals.message.not_falling",
                    (int) EnderPortalsMod.ACTIVATION_FALL_DISTANCE, (int) fall), true);
            world.playSound(null, base, SoundEvents.ITEM_MACE_SMASH_AIR, SoundCategory.PLAYERS, 0.8f, 0.9f);
            return false;
        }

        // L'impact absorbe la chute, comme l'attaque écrasante de la Mace.
        player.fallDistance = 0.0f;

        Vec3d impact = Vec3d.ofBottomCenter(base);
        world.playSound(null, base, SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1.2f, 0.8f);
        world.spawnParticles(ParticleTypes.GUST_EMITTER_LARGE, impact.getX(), impact.getY(), impact.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);

        Direction facing = world.getBlockState(base).get(FACING);
        TardisHelper.activate(world, base, facing, player);
        mace.damage(10, player, EquipmentSlot.MAINHAND);
        return true;
    }
}
