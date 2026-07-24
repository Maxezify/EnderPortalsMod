package com.maxezify.enderportals;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(EnderPortalsMod.MODID);

    /** UUID (en texte) du TARDIS auquel une clé est liée. */
    public static final Supplier<DataComponentType<String>> TARDIS_ID = COMPONENTS.registerComponentType(
            "tardis_id",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private ModComponents() {
    }
}
