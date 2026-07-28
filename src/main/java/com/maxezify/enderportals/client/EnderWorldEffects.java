package com.maxezify.enderportals.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Effets visuels propres au monde de l'Ender, côté client.
 *
 * <p>La dimension empruntait jusqu'ici ceux du Nether. Ils lui allaient à peu
 * près — pas de ciel, brouillard dense — mais deux de leurs choix la
 * desservaient, et le troisième est ce sur quoi les shaders s'appuient.</p>
 *
 * <p><b>La teinte du brouillard n'est plus délavée.</b>
 * {@link #getBrightnessDependentFogColor} rend la couleur du biome telle
 * quelle, sans la moduler par la lumière du jour comme le fait l'Overworld.
 * C'est cette valeur qui alimente l'uniforme {@code fogColor} lu par les
 * shaders : Complementary Reimagined en tire son {@code netherColor} par
 * {@code fogColor * 0.6 + 0.2 * normalize(fogColor)}. La couleur du brouillard
 * du monde de l'Ender est donc pilotée, sous shader comme sans, par le seul
 * {@code fog_color} de son biome.</p>
 *
 * <p><b>L'éclairage redevient directionnel.</b> Le Nether force
 * {@code constantAmbientLight}, qui éclaire toutes les faces d'un bloc à
 * l'identique — commode pour un monde de lave, désastreux pour une caverne
 * où l'on veut lire le relief. En le désactivant, les faces retrouvent leur
 * ombrage vanilla : les galeries se creusent, les nuées de reliques prennent
 * du volume, et les filons lumineux projettent un contraste franc.</p>
 */
public class EnderWorldEffects extends DimensionSpecialEffects {

    public EnderWorldEffects() {
        super(
                // Pas de nuages : le monde est clos.
                Float.NaN,
                // Pas de sol : aucun plan sombre sous le monde.
                false,
                // Pas de ciel du tout — ni soleil, ni lune, ni étoiles.
                SkyType.NONE,
                // Pas de lightmap forcée en clair : l'obscurité doit rester
                // franche là où aucun filon n'éclaire.
                false,
                // Éclairage directionnel, contrairement au Nether.
                false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
        return biomeFogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        // Brouillard dense partout, comme le Nether : la masse translucide ne
        // doit jamais se lire jusqu'à l'horizon.
        return true;
    }
}
