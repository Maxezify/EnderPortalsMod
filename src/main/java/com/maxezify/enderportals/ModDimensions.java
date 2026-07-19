package com.maxezify.enderportals;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public final class ModDimensions {

    /** Le monde de l'Ender — la dimension-caverne derrière la porte. */
    public static final RegistryKey<World> ENDER_WORLD =
            RegistryKey.of(RegistryKeys.WORLD, EnderPortalsMod.id("ender_world"));

    private ModDimensions() {
    }
}
