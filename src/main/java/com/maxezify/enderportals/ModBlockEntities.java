package com.maxezify.enderportals;

import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnderPortalsMod.MODID);

    public static final Supplier<BlockEntityType<TardisDoorBlockEntity>> TARDIS_DOOR =
            BLOCK_ENTITIES.register("tardis_door",
                    () -> BlockEntityType.Builder.of(TardisDoorBlockEntity::new, ModBlocks.TARDIS_DOOR.get())
                            .build(null));

    private ModBlockEntities() {
    }
}
