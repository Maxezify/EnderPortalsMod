package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.compat.ImmPtlCompat;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Côté client uniquement : sait dire si Immersive Portals est en train de
 * rendre un monde <em>vu au travers d'un portail</em>, et lequel.
 *
 * <p>Immersive Portals empile un {@code WorldRenderInfo} par passe de rendu de
 * portail ; {@code isRendering()} indique qu'on est dans une telle passe et
 * {@code getTopRenderInfo().world} donne le monde en cours de rendu. C'est
 * l'information qui manquait aux tentatives 0.5.0/0.5.1, qui essayaient de
 * déduire le point de vue de la caméra principale — que le mod déplace
 * justement pendant ces passes.</p>
 *
 * <p>Tout passe par la réflexion, comme {@link ImmPtlCompat} : aucune
 * dépendance de compilation, et un échec se solde par un simple {@code false}
 * (la porte est dessinée comme avant).</p>
 */
public final class ImmPtlRenderCompat {

    private static final String WORLD_RENDER_INFO =
            "qouteall.imm_ptl.core.render.context_management.WorldRenderInfo";

    private static boolean resolved;
    private static boolean broken;
    private static Method isRendering;
    private static Method getTopRenderInfo;
    private static Field worldField;

    /**
     * Le monde donné est-il en ce moment rendu au travers d'un portail ?
     *
     * @param level le monde du bloc que l'on s'apprête à dessiner
     */
    public static boolean isRenderingThroughPortal(Level level) {
        if (level == null || broken || !ImmPtlCompat.isLoaded()) {
            return false;
        }
        if (!resolved) {
            resolve();
            if (broken) {
                return false;
            }
        }
        try {
            // getTopRenderInfo() fait un peek() : sans cette garde, il lève
            // EmptyStackException hors des passes de portail.
            if (!((Boolean) isRendering.invoke(null))) {
                return false;
            }
            Object top = getTopRenderInfo.invoke(null);
            if (top == null) {
                return false;
            }
            return worldField.get(top) instanceof Level rendered
                    && rendered.dimension().equals(level.dimension());
        } catch (Throwable t) {
            broken = true;
            EnderPortalsMod.LOGGER.warn(
                    "Contexte de rendu d'Immersive Portals illisible — la porte sera dessinée dans les portails.", t);
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> renderInfo = Class.forName(WORLD_RENDER_INFO);
            isRendering = renderInfo.getMethod("isRendering");
            getTopRenderInfo = renderInfo.getMethod("getTopRenderInfo");
            worldField = renderInfo.getField("world");
        } catch (Throwable t) {
            broken = true;
            EnderPortalsMod.LOGGER.warn(
                    "Classe {} introuvable — la porte sera dessinée dans les portails.", WORLD_RENDER_INFO, t);
        }
    }

    private ImmPtlRenderCompat() {
    }
}
