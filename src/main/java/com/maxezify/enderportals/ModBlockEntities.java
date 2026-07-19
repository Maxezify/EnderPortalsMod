package com.maxezify.enderportals;

import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {

    public static final BlockEntityType<TardisDoorBlockEntity> TARDIS_DOOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            EnderPortalsMod.id("tardis_door"),
            BlockEntityType.Builder.create(TardisDoorBlockEntity::new, ModBlocks.TARDIS_DOOR).build(null));

    public static void init() {
    }

    private ModBlockEntities() {
    }
}
