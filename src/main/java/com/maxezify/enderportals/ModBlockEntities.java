package com.maxezify.enderportals;

import com.maxezify.enderportals.block.entity.AllyPassageBlockEntity;
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

    /**
     * Le moteur du Passage des Alliés. Comme la porte, il ne vit que sur la
     * moitié basse : c'est elle qui porte la position mémorisée au registre.
     */
    public static final Supplier<BlockEntityType<AllyPassageBlockEntity>> ALLY_PASSAGE =
            BLOCK_ENTITIES.register("ally_passage",
                    () -> BlockEntityType.Builder.of(AllyPassageBlockEntity::new, ModBlocks.ALLY_PASSAGE.get())
                            .build(null));

    private ModBlockEntities() {
    }
}
