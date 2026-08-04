package com.maxezify.enderportals;

import com.maxezify.enderportals.entity.EntityTeleporterEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EnderPortalsMod.MODID);

    /**
     * Le Téléporteur d'entité. Mêmes dimensions qu'un bateau : la coque doit
     * accueillir les créatures exactement comme lui, et c'est sa boîte de
     * collision qui décide de ce qui peut monter dessus.
     */
    public static final Supplier<EntityType<EntityTeleporterEntity>> ENTITY_TELEPORTER =
            ENTITIES.register("entity_teleporter",
                    () -> EntityType.Builder.<EntityTeleporterEntity>of(EntityTeleporterEntity::new, MobCategory.MISC)
                            .sized(1.375f, 0.5625f)
                            .clientTrackingRange(10)
                            .build("entity_teleporter"));

    private ModEntities() {
    }
}
