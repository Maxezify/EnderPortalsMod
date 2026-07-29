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

    /**
     * Code d'ami à huit chiffres, recopié sur la clé au moment de la liaison.
     *
     * <p>Il est dupliqué là volontairement : l'infobulle se dessine côté client,
     * qui n'a aucun accès au registre des portes. Sans cette copie, le joueur
     * devrait aller lire son code sur un panneau pour pouvoir le dicter.</p>
     */
    public static final Supplier<DataComponentType<Integer>> FRIEND_CODE = COMPONENTS.registerComponentType(
            "friend_code",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModComponents() {
    }
}
