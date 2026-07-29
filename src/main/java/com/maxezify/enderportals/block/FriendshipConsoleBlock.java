package com.maxezify.enderportals.block;

import com.maxezify.enderportals.network.ConsoleServerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Le Contrôle de l'amitié : le pavé numérique par lequel tout se décide.
 *
 * <p>Le bloc ne retient rien. Pas de block entity, pas d'inventaire : le carnet
 * d'amis appartient au joueur, pas au panneau, et vit dans la sauvegarde
 * globale. Un panneau cassé et reposé retrouve donc exactement la même liste —
 * et deux panneaux dans la même base montrent la même chose.</p>
 */
public class FriendshipConsoleBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public FriendshipConsoleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ConsoleServerLogic.open(serverPlayer, pos);
        }
        // SUCCESS des deux côtés : le client ne doit pas ouvrir l'écran de son
        // propre chef, il attend l'état que le serveur lui enverra.
        return InteractionResult.SUCCESS;
    }
}
