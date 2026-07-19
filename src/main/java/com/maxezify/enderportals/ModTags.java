package com.maxezify.enderportals;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public final class ModTags {

    /** Blocs que la pioche de l'Ender casse très vite et fait toujours tomber. */
    public static final TagKey<Block> ENDER_PICKAXE_FAST =
            TagKey.of(RegistryKeys.BLOCK, EnderPortalsMod.id("ender_pickaxe_fast"));

    private ModTags() {
    }
}
