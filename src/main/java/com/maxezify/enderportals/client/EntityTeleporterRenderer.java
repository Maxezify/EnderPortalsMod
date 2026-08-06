package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.entity.EntityTeleporterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * La coque du Téléporteur d'entité.
 *
 * <p>Dessinée à la main plutôt qu'avec le modèle de bateau de vanilla : celui-ci
 * est fait de rames, de planches et d'une coque bombée dont rien ne convient à
 * une machine.</p>
 *
 * <p><b>Deux faces dans le même plan, c'est du clipping.</b> Quand deux caisses
 * se touchent pile — le rail posé sur la paroi, la paroi posée sur la quille,
 * deux parois qui se rejoignent à l'angle — leurs faces occupent exactement la
 * même profondeur, et la carte graphique n'a aucun moyen de choisir laquelle
 * afficher : le résultat scintille et bave d'une texture à l'autre selon
 * l'angle de vue. La 0.20.1 empilait ses caisses bord à bord et comptait
 * <b>116 paires de faces coplanaires</b> ; c'est ce que montrait la capture.</p>
 *
 * <p>La règle appliquée ici est donc explicite : <b>deux caisses ne se touchent
 * jamais, elles s'ignorent ou elles s'enfoncent l'une dans l'autre</b> d'un
 * demi-centimètre, jamais alignées. Chaque pièce qui en recouvre une autre est
 * un cheveu plus large ou plus haute qu'elle, si bien que la face cachée reste
 * strictement à l'intérieur du volume voisin — invisible, donc muette. Les
 * décrochements font 5 mm, soit un douzième de pixel de texture : indécelables
 * à l'œil, décisifs pour le tampon de profondeur.</p>
 *
 * <p>La géométrie est écrite en toutes lettres dans {@link #PARTS} plutôt que
 * calculée par des boucles, et {@code tools/check_hull.py} relit ce tableau à
 * chaque build pour vérifier la règle. Une symétrie calculée aurait été plus
 * courte à écrire et impossible à vérifier de l'extérieur ; ici la CI refuse la
 * moindre face coplanaire réintroduite.</p>
 *
 * <p><b>L'overlay n'est pas zéro.</b> La 0.20.0 passait {@code 0} à chaque
 * sommet, croyant dire « aucun » : {@code 0} vaut {@code pack(0, 0)}, c'est-à-dire
 * la ligne du <b>flash de dégâts</b> de la planche d'overlay, d'où une coque
 * rouge vif quelle que soit sa texture. La constante est {@link
 * OverlayTexture#NO_OVERLAY}, et elle n'est plus un paramètre : rien ici ne
 * clignote.</p>
 */
public class EntityTeleporterRenderer extends EntityRenderer<EntityTeleporterEntity> {

    private static final ResourceLocation TEXTURE =
            EnderPortalsMod.id("textures/entity/entity_teleporter.png");

    /** Côté de la planche de texture, en pixels. */
    private static final float TEX = 32.0f;

    // Régions {u0, v0, u1, v1} de la planche.
    private static final float[] R_HULL = {0, 0, 16, 8};
    private static final float[] R_DECK = {16, 0, 32, 8};
    private static final float[] R_TRIM = {0, 8, 16, 12};
    private static final float[] R_LAMP_RED = {16, 8, 24, 12};
    private static final float[] R_LAMP_GREEN = {24, 8, 32, 12};
    private static final float[] R_POST = {0, 12, 16, 16};
    private static final float[] R_CRYSTAL = {0, 16, 16, 24};
    private static final float[] R_PAD = {16, 16, 32, 24};
    /** L'anneau de charge : cœur blanc, bords violets, extrémités éteintes. */
    private static final float[] R_GLOW = {0, 24, 16, 32};

    /** Demi-hauteur de l'anneau. Le dégradé de la texture en adoucit les bords. */
    private static final float RING_HALF = 0.13f;
    /** Écartement de l'anneau : juste au-delà des montants (0,585 / 0,725). */
    private static final float RING_X = 0.64f;
    private static final float RING_Z = 0.78f;
    /** Course de l'anneau, du dessous de la quille au-dessus des cristaux. */
    private static final float RING_FROM = -0.12f;
    private static final float RING_TO = 0.86f;
    /** Ticks d'une remontée complète : trois par seconde, c'est un régime. */
    private static final float RING_CYCLE = 20.0f;

    private static final int HULL = 0;
    private static final int DECK = 1;
    private static final int TRIM = 2;
    private static final int POST = 3;
    /** Le témoin : sa région et sa lumière dépendent de l'état, pas du tableau. */
    private static final int LAMP = 4;
    /** Cristaux et plaque : ils brillent de leur propre lumière. */
    private static final int CRYSTAL = 5;
    private static final int PAD = 6;

    private static final float[][] REGIONS =
            {R_HULL, R_DECK, R_TRIM, R_POST, R_LAMP_RED, R_CRYSTAL, R_PAD};

    /**
     * Les caisses de la coque : {@code {x0, y0, z0, x1, y1, z1, région}}.
     *
     * <p>Aucune de ces valeurs n'est décorative : chacune est décalée de 5 mm de
     * sa voisine pour qu'aucune face ne partage un plan avec une autre. Modifier
     * un seul de ces nombres sans repasser {@code tools/check_hull.py} peut
     * réintroduire le scintillement.</p>
     */
    private static final float[][] PARTS = {
            // Quille : légèrement débordante, si bien que les parois s'y
            // enfoncent au lieu de s'y poser.
            {-0.565f, 0.000f, -0.715f, 0.565f, 0.080f, 0.715f, DECK},

            // La plaque de départ, incrustée dans le plancher et débordant de
            // deux centimètres : c'est elle qu'on voit d'en haut, et la seule
            // pièce qui dise à quoi sert la machine.
            {-0.360f, 0.075f, -0.430f, 0.360f, 0.098f, 0.430f, PAD},

            // Les quatre parois. Avant et arrière tiennent toute la largeur ;
            // celles des côtés sont 5 mm plus étroites et plus basses, et
            // mordent dans les premières aux angles.
            {-0.550f, 0.075f, -0.700f, 0.550f, 0.440f, -0.610f, HULL},
            {-0.550f, 0.075f, 0.610f, 0.550f, 0.440f, 0.700f, HULL},
            {-0.545f, 0.070f, -0.665f, -0.460f, 0.435f, 0.665f, HULL},
            {0.460f, 0.070f, -0.665f, 0.545f, 0.435f, 0.665f, HULL},

            // Le rail d'or, en surplomb de 2 cm et plongeant de 1 cm dans les
            // parois : c'est ce surplomb qui donne son ombre à l'arête haute.
            {-0.570f, 0.425f, -0.720f, 0.570f, 0.485f, -0.630f, TRIM},
            {-0.570f, 0.425f, 0.630f, 0.570f, 0.485f, 0.720f, TRIM},
            {-0.565f, 0.430f, -0.680f, -0.475f, 0.480f, 0.680f, TRIM},
            {0.475f, 0.430f, -0.680f, 0.565f, 0.480f, 0.680f, TRIM},

            // Quatre montants d'acier, saillants de tout le reste.
            {-0.585f, 0.050f, -0.725f, -0.490f, 0.500f, -0.635f, POST},
            {-0.585f, 0.050f, 0.635f, -0.490f, 0.500f, 0.725f, POST},
            {0.490f, 0.050f, -0.725f, 0.585f, 0.500f, -0.635f, POST},
            {0.490f, 0.050f, 0.635f, 0.585f, 0.500f, 0.725f, POST},

            // Et leurs cristaux : plus étroits que le montant, enfoncés d'un
            // demi-centimètre dedans. Ce sont eux qui font lire quatre pylônes
            // au lieu de quatre piquets de caisse.
            {-0.575f, 0.495f, -0.715f, -0.500f, 0.590f, -0.645f, CRYSTAL},
            {-0.575f, 0.495f, 0.645f, -0.500f, 0.590f, 0.715f, CRYSTAL},
            {0.500f, 0.495f, -0.715f, 0.575f, 0.590f, -0.645f, CRYSTAL},
            {0.500f, 0.495f, 0.645f, 0.575f, 0.590f, 0.715f, CRYSTAL},

            // Le tableau de proue, posé sur le rail avant. C'est lui qui donne
            // un avant et un arrière à la coque — sans quoi elle se lit comme
            // une caisse, quel que soit le soin mis au reste.
            {-0.220f, 0.430f, -0.735f, 0.220f, 0.560f, -0.640f, POST},

            // Le témoin, à hauteur de regard sur ce tableau.
            {-0.130f, 0.455f, -0.750f, 0.130f, 0.545f, -0.700f, LAMP},
    };

    public EntityTeleporterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTeleporterEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityTeleporterEntity entity, float yRot, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.06f, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yRot));

        PoseStack.Pose entry = poseStack.last();
        VertexConsumer hull = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        boolean ready = entity.isReady();

        for (float[] part : PARTS) {
            int region = (int) part[6];
            // Le témoin dit si le départ est possible, et se voit de nuit comme
            // au fond d'une galerie : il ignore la lumière ambiante.
            float[] uv = region == LAMP ? (ready ? R_LAMP_GREEN : R_LAMP_RED) : REGIONS[region];
            // Trois pièces éclairent d'elles-mêmes : le témoin, les cristaux
            // des pylônes et la plaque de départ. Le reste subit la lumière du
            // lieu, sans quoi la machine flotterait au-dessus de la nuit.
            boolean glowing = region == LAMP || region == CRYSTAL || region == PAD;
            int lit = glowing ? LightTexture.FULL_BRIGHT : light;
            box(hull, entry, part, uv, lit);
        }

        // L'anneau de charge, le temps que les chunks d'arrivée se génèrent.
        // Couche translucide à part : la coque, elle, est dessinée en cutout,
        // qui ne sait pas mélanger.
        if (entity.isCharging()) {
            drawChargeRing(buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)), entry,
                    entity.getChargeAge() + partialTick);
        }

        poseStack.popPose();
        super.render(entity, yRot, partialTick, poseStack, buffer, light);
    }

    /**
     * L'anneau lumineux de la charge : il remonte la machine en boucle, du
     * dessous de la quille au-dessus des cristaux.
     *
     * <p>Il se rallume à chaque tour au lieu de faire un seul passage, parce que
     * la charge n'a pas de durée connue — elle dure ce que dure la génération
     * des chunks d'arrivée. Un balayage unique aurait donc dû se figer quelque
     * part ; un régime, lui, dit « ça travaille » aussi longtemps qu'il faut.
     * Son éclat suit un demi-sinus, nul aux deux bouts de la course : c'est ce
     * qui fait une remontée plutôt que deux coupures.</p>
     */
    private static void drawChargeRing(VertexConsumer buffer, PoseStack.Pose entry, float chargeTime) {
        float t = (chargeTime % RING_CYCLE) / RING_CYCLE;
        float y = RING_FROM + (RING_TO - RING_FROM) * t;
        float y0 = y - RING_HALF;
        float y1 = y + RING_HALF;
        float alpha = (float) Math.sin(Math.PI * t);
        if (alpha <= 0.02f) {
            return;
        }
        float x = RING_X;
        float z = RING_Z;
        int lit = LightTexture.FULL_BRIGHT;
        glowFace(buffer, entry, -x, y0, z, x, y0, z, x, y1, z, -x, y1, z, alpha, lit, 0, 0, 1);
        glowFace(buffer, entry, x, y0, -z, -x, y0, -z, -x, y1, -z, x, y1, -z, alpha, lit, 0, 0, -1);
        glowFace(buffer, entry, x, y0, z, x, y0, -z, x, y1, -z, x, y1, z, alpha, lit, 1, 0, 0);
        glowFace(buffer, entry, -x, y0, -z, -x, y0, z, -x, y1, z, -x, y1, -z, alpha, lit, -1, 0, 0);
    }

    /**
     * Une face de l'anneau. Elle ne passe pas par {@link #region} : celle-ci
     * fixe l'opacité à un, ce qui suffit à toute la coque et à rien d'autre.
     */
    private static void glowFace(VertexConsumer buffer, PoseStack.Pose entry,
                                 float ax, float ay, float az, float bx, float by, float bz,
                                 float cx, float cy, float cz, float dx, float dy, float dz,
                                 float alpha, int light, float nx, float ny, float nz) {
        float u0 = R_GLOW[0] / TEX, v0 = R_GLOW[1] / TEX;
        float u1 = R_GLOW[2] / TEX, v1 = R_GLOW[3] / TEX;
        glowVertex(buffer, entry, ax, ay, az, u0, v1, alpha, light, nx, ny, nz);
        glowVertex(buffer, entry, bx, by, bz, u1, v1, alpha, light, nx, ny, nz);
        glowVertex(buffer, entry, cx, cy, cz, u1, v0, alpha, light, nx, ny, nz);
        glowVertex(buffer, entry, dx, dy, dz, u0, v0, alpha, light, nx, ny, nz);
    }

    private static void glowVertex(VertexConsumer buffer, PoseStack.Pose entry,
                                   float x, float y, float z, float u, float v,
                                   float alpha, int light, float nx, float ny, float nz) {
        buffer.addVertex(entry.pose(), x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, nx, ny, nz);
    }

    /** Une caisse pleine, ses six faces peintes de la même région. */
    private static void box(VertexConsumer buffer, PoseStack.Pose entry, float[] p, float[] uv, int light) {
        float x0 = p[0], y0 = p[1], z0 = p[2], x1 = p[3], y1 = p[4], z1 = p[5];
        region(buffer, entry, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, uv, light, 0, 0, 1);
        region(buffer, entry, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, uv, light, 0, 0, -1);
        region(buffer, entry, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, uv, light, 1, 0, 0);
        region(buffer, entry, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, uv, light, -1, 0, 0);
        region(buffer, entry, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, uv, light, 0, 1, 0);
        region(buffer, entry, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, uv, light, 0, -1, 0);
    }

    /** Quadrilatère a→b→c→d texturé avec une région {u0,v0,u1,v1}. */
    private static void region(VertexConsumer buffer, PoseStack.Pose entry,
                               float ax, float ay, float az, float bx, float by, float bz,
                               float cx, float cy, float cz, float dx, float dy, float dz,
                               float[] uv, int light, float nx, float ny, float nz) {
        vertex(buffer, entry, ax, ay, az, uv[0] / TEX, uv[3] / TEX, light, nx, ny, nz);
        vertex(buffer, entry, bx, by, bz, uv[2] / TEX, uv[3] / TEX, light, nx, ny, nz);
        vertex(buffer, entry, cx, cy, cz, uv[2] / TEX, uv[1] / TEX, light, nx, ny, nz);
        vertex(buffer, entry, dx, dy, dz, uv[0] / TEX, uv[1] / TEX, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose entry,
                               float x, float y, float z, float u, float v,
                               int light, float nx, float ny, float nz) {
        buffer.addVertex(entry.pose(), x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, nx, ny, nz);
    }
}
