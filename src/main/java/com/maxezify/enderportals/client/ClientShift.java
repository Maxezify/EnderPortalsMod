package com.maxezify.enderportals.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * L'état de la touche Maj, et rien d'autre.
 *
 * <p>Cette classe existe pour une seule raison : {@link Screen} n'existe que du
 * côté client, et une infobulle se construit dans du code commun. Isoler
 * l'appel ici permet à {@code EnderTooltip} de le garder derrière un test de
 * {@code Dist}, si bien que la classe n'est jamais chargée sur un serveur
 * dédié — même patron que {@code ModNetwork} pour l'écran du Contrôle de
 * l'amitié.</p>
 */
public final class ClientShift {

    /** Maj est-elle enfoncée ? */
    public static boolean down() {
        return Screen.hasShiftDown();
    }

    private ClientShift() {
    }
}
