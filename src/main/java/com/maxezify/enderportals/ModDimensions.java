package com.maxezify.enderportals;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ModDimensions {

    /** Le monde de l'Ender — la dimension-caverne derrière la porte. */
    public static final ResourceKey<Level> ENDER_WORLD =
            ResourceKey.create(Registries.DIMENSION, EnderPortalsMod.id("ender_world"));

    private ModDimensions() {
    }
}
