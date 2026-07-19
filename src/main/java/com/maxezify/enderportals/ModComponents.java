package com.maxezify.enderportals;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModComponents {

    /** UUID (en texte) du TARDIS auquel une clé est liée. */
    public static final ComponentType<String> TARDIS_ID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            EnderPortalsMod.id("tardis_id"),
            ComponentType.<String>builder()
                    .codec(Codec.STRING)
                    .packetCodec(PacketCodecs.STRING)
                    .build());

    public static void init() {
    }

    private ModComponents() {
    }
}
