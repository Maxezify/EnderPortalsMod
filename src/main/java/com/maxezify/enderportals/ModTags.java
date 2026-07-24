package com.maxezify.enderportals;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {

    /** Blocs que la pioche de l'Ender casse très vite et fait toujours tomber. */
    public static final TagKey<Block> ENDER_PICKAXE_FAST =
            TagKey.create(Registries.BLOCK, EnderPortalsMod.id("ender_pickaxe_fast"));

    private ModTags() {
    }
}
