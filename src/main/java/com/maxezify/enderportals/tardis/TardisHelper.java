package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.block.TardisDoorBlock;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.maxezify.enderportals.compat.ImmPtlCompat;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.Nullable;

/**
 * Toute la mécanique TARDIS : activation, salle intérieure, matérialisation,
 * dématérialisation, ouverture et traversées.
 */
public final class TardisHelper {

    private static final int PORTAL_COOLDOWN_TICKS = 60;

    // ------------------------------------------------------------------
    // Activation (coup de masse)
    // ------------------------------------------------------------------

    public static void activate(ServerWorld world, BlockPos base, Direction facing, ServerPlayerEntity player) {
        MinecraftServer server = world.getServer();
        ServerWorld enderWorld = server.getWorld(ModDimensions.ENDER_WORLD);
        if (enderWorld == null) {
            EnderPortalsMod.LOGGER.error("Dimension enderportals:ender_world introuvable !");
            return;
        }

        TardisStateManager manager = TardisStateManager.get(server);
        TardisData data = manager.createTardis();
        buildInteriorRoom(enderWorld, data);

        // Remplace la porte inactive par la porte active, sans réactions de voisins.
        int swapFlags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
        world.setBlockState(base, Blocks.AIR.getDefaultState(), swapFlags);
        world.setBlockState(base.up(), Blocks.AIR.getDefaultState(), swapFlags);
        placeDoor(world, base, facing, false, data, false, false);

        data.deployed = true;
        data.open = false;
        data.exteriorWorld = world.getRegistryKey();
        data.exteriorPos = base;
        data.exteriorFacing = facing;
        manager.markDirty();

        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt != null) {
            bolt.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(base));
            bolt.setCosmetic(true);
            world.spawnEntity(bolt);
        }
        world.playSound(null, base, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5f, 0.6f);
        player.sendMessage(Text.translatable("enderportals.message.activated"), false);
    }

    // ------------------------------------------------------------------
    // Salle intérieure
    // ------------------------------------------------------------------

    /**
     * Creuse la salle de départ dans le monde de l'Ender et y pose la porte
     * intérieure. La porte est dans le mur nord et regarde vers la salle (sud).
     */
    public static void buildInteriorRoom(ServerWorld enderWorld, TardisData data) {
        BlockPos door = data.interiorDoorPos;
        int x0 = door.getX() - 6, x1 = door.getX() + 6;
        int y0 = door.getY() - 1, y1 = door.getY() + 5;
        int z0 = door.getZ(), z1 = door.getZ() + 12;

        BlockState bricks = ModBlocks.ENDER_BRICKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    boolean shell = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    enderWorld.setBlockState(cursor.set(x, y, z), shell ? bricks : air, Block.NOTIFY_LISTENERS);
                }
            }
        }
        // Un peu de lumière aux quatre coins du sol.
        BlockState lantern = Blocks.SEA_LANTERN.getDefaultState();
        enderWorld.setBlockState(cursor.set(x0 + 1, y0, z0 + 2), lantern, Block.NOTIFY_LISTENERS);
        enderWorld.setBlockState(cursor.set(x1 - 1, y0, z0 + 2), lantern, Block.NOTIFY_LISTENERS);
        enderWorld.setBlockState(cursor.set(x0 + 1, y0, z1 - 2), lantern, Block.NOTIFY_LISTENERS);
        enderWorld.setBlockState(cursor.set(x1 - 1, y0, z1 - 2), lantern, Block.NOTIFY_LISTENERS);

        // La porte intérieure, encastrée dans le mur nord (z0), face au sud.
        placeDoor(enderWorld, door, data.interiorFacing, false, data, true, false);
    }

    // ------------------------------------------------------------------
    // Matérialisation / dématérialisation de la porte extérieure
    // ------------------------------------------------------------------

    /**
     * Matérialise la porte extérieure à l'endroit donné (avec fondu). Si elle
     * était déployée ailleurs, elle s'y dématérialise d'abord.
     */
    public static boolean deployExterior(MinecraftServer server, TardisData data, ServerWorld world,
                                         BlockPos base, Direction facing, boolean open,
                                         @Nullable PlayerEntity feedback) {
        if (!world.getBlockState(base).isReplaceable() || !world.getBlockState(base.up()).isReplaceable()) {
            if (feedback != null) {
                feedback.sendMessage(Text.translatable("enderportals.message.no_space"), true);
            }
            return false;
        }
        if (data.deployed) {
            dismissExterior(server, data);
        }

        placeDoor(world, base, facing, open, data, false, true);
        data.deployed = true;
        data.open = open;
        data.exteriorWorld = world.getRegistryKey();
        data.exteriorPos = base;
        data.exteriorFacing = facing;
        setInteriorOpen(server, data, open);
        TardisStateManager.get(server).markDirty();

        world.playSound(null, base, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.2f, 0.5f);
        world.playSound(null, base, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 0.6f);

        if (open) {
            ImmPtlCompat.tryCreatePortals(server, data);
        }
        return true;
    }

    /**
     * Referme les portes et dématérialise la porte extérieure (fondu de
     * disparition, puis les blocs s'effacent).
     */
    public static void dismissExterior(MinecraftServer server, TardisData data) {
        ImmPtlCompat.removePortals(server, data);
        setInteriorOpen(server, data, false);
        data.open = false;

        ServerWorld world = server.getWorld(data.exteriorWorld);
        if (world != null && data.deployed) {
            BlockPos base = data.exteriorPos;
            BlockState state = world.getBlockState(base);
            if (state.isOf(ModBlocks.TARDIS_DOOR)) {
                setOpen(world, base, false);
                if (world.getBlockEntity(base) instanceof TardisDoorBlockEntity door) {
                    door.startDematerialize();
                }
                world.playSound(null, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.2f, 0.5f);
                world.playSound(null, base, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 0.5f);
            }
        }
        data.deployed = false;
        TardisStateManager.get(server).markDirty();
    }

    /**
     * Ouvre ou ferme les deux portes (extérieure et intérieure) d'un coup.
     */
    public static void setDoorsOpen(MinecraftServer server, TardisData data, boolean open) {
        data.open = open;
        if (data.deployed) {
            ServerWorld world = server.getWorld(data.exteriorWorld);
            if (world != null) {
                setOpen(world, data.exteriorPos, open);
                world.playSound(null, data.exteriorPos,
                        open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE,
                        SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
        }
        setInteriorOpen(server, data, open);
        TardisStateManager.get(server).markDirty();

        if (open && data.deployed) {
            ImmPtlCompat.tryCreatePortals(server, data);
        } else {
            ImmPtlCompat.removePortals(server, data);
        }
    }

    private static void setInteriorOpen(MinecraftServer server, TardisData data, boolean open) {
        ServerWorld enderWorld = server.getWorld(ModDimensions.ENDER_WORLD);
        if (enderWorld != null && data.interiorDoorPos != null) {
            setOpen(enderWorld, data.interiorDoorPos, open);
        }
    }

    private static void setOpen(ServerWorld world, BlockPos base, boolean open) {
        BlockState lower = world.getBlockState(base);
        if (lower.isOf(ModBlocks.TARDIS_DOOR)) {
            world.setBlockState(base, lower.with(TardisDoorBlock.OPEN, open), Block.NOTIFY_ALL);
        }
        BlockState upper = world.getBlockState(base.up());
        if (upper.isOf(ModBlocks.TARDIS_DOOR)) {
            world.setBlockState(base.up(), upper.with(TardisDoorBlock.OPEN, open), Block.NOTIFY_ALL);
        }
    }

    /**
     * Pose les deux moitiés d'une porte de TARDIS et initialise son block
     * entity.
     */
    private static void placeDoor(ServerWorld world, BlockPos base, Direction facing, boolean open,
                                  TardisData data, boolean interior, boolean fadeIn) {
        BlockState lower = ModBlocks.TARDIS_DOOR.getDefaultState()
                .with(TardisDoorBlock.FACING, facing)
                .with(TardisDoorBlock.HALF, DoubleBlockHalf.LOWER)
                .with(TardisDoorBlock.OPEN, open);
        world.setBlockState(base, lower, Block.NOTIFY_ALL);
        world.setBlockState(base.up(), lower.with(TardisDoorBlock.HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
        if (world.getBlockEntity(base) instanceof TardisDoorBlockEntity door) {
            door.initialize(data.id, interior, fadeIn);
        }
    }

    // ------------------------------------------------------------------
    // Traversées
    // ------------------------------------------------------------------

    public static void enterTardis(ServerPlayerEntity player, TardisData data) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerWorld enderWorld = server.getWorld(ModDimensions.ENDER_WORLD);
        if (enderWorld == null || data.interiorDoorPos == null) {
            return;
        }
        BlockPos front = data.interiorDoorPos.offset(data.interiorFacing);
        player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        FabricDimensions.teleport(player, new TeleportTarget(enderWorld, Vec3d.ofBottomCenter(front),
                Vec3d.ZERO, data.interiorFacing.asRotation(), 0.0f, TeleportTarget.NO_OP));
        enderWorld.playSound(null, front, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8f, 0.9f);
    }

    public static void exitTardis(ServerPlayerEntity player, TardisData data) {
        MinecraftServer server = player.getServer();
        if (server == null || !data.deployed) {
            return;
        }
        ServerWorld world = server.getWorld(data.exteriorWorld);
        if (world == null) {
            return;
        }
        BlockPos front = data.exteriorPos.offset(data.exteriorFacing);
        player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        FabricDimensions.teleport(player, new TeleportTarget(world, Vec3d.ofBottomCenter(front),
                Vec3d.ZERO, data.exteriorFacing.asRotation(), 0.0f, TeleportTarget.NO_OP));
        world.playSound(null, front, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8f, 0.9f);
    }

    private TardisHelper() {
    }
}
