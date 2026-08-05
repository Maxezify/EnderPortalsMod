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
 * une machine, et le reprendre aurait imposé de suivre ses couches de modèle
 * d'une version à l'autre.</p>
 *
 * <p><b>L'overlay n'est pas zéro.</b> La 0.20.0 passait {@code 0} à chaque
 * sommet, croyant dire « aucun » : {@code 0} vaut {@code pack(0, 0)}, c'est-à-dire
 * la ligne du <b>flash de dégâts</b> de la planche d'overlay. Toute la coque
 * sortait rouge vif, quelle que soit sa texture — le défaut ne se voyait
 * évidemment pas dans le fichier PNG. La constante est {@link
 * OverlayTexture#NO_OVERLAY}, et elle n'est plus un paramètre ici : rien de ce
 * que dessine cette classe ne clignote, il n'y avait donc aucune raison de
 * laisser le choix ouvert.</p>
 *
 * <p>La silhouette est celle d'une nacelle et non d'une caisse : deux ceintures
 * de bordé, la basse en retrait, quatre montants d'acier aux angles et un rail
 * d'or sur l'arête haute. C'est ce décrochement qui donne une échelle à l'objet
 * — un pavé lisse de cette taille se lit comme un bloc, pas comme une
 * machine.</p>
 */
public class EntityTeleporterRenderer extends EntityRenderer<EntityTeleporterEntity> {

    private static final ResourceLocation TEXTURE =
            EnderPortalsMod.id("textures/entity/entity_teleporter.png");

    /** Côté de la planche de texture, en pixels. */
    private static final float TEX = 32.0f;

    // Régions {u0, v0, u1, v1} de la planche.
    private static final float[] HULL = {0, 0, 16, 8};
    private static final float[] DECK = {16, 0, 32, 8};
    private static final float[] TRIM = {0, 8, 16, 12};
    private static final float[] LAMP_OFF = {16, 8, 24, 12};
    private static final float[] LAMP_ON = {24, 8, 32, 12};
    private static final float[] POST = {0, 12, 16, 16};

    // Deux ceintures : la basse en retrait, la haute au gabarit de la coque.
    private static final float LOW_W = 0.50f, LOW_L = 0.62f;
    private static final float TOP_W = 0.60f, TOP_L = 0.72f;
    private static final float FLOOR = 0.06f;
    private static final float WAIST = 0.21f;
    private static final float RIM = 0.42f;
    /** Épaisseur du bordé. Assez pour se voir, assez fin pour rester creux. */
    private static final float SKIN = 0.08f;

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

        // Le plancher, puis les deux ceintures — creuses, pour que les créatures
        // s'y voient assises comme dans un bateau.
        box(hull, entry, -LOW_W, 0.0f, -LOW_L, LOW_W, FLOOR, LOW_L, DECK, light);
        belt(hull, entry, LOW_W, LOW_L, 0.0f, WAIST, light);
        belt(hull, entry, TOP_W, TOP_L, WAIST, RIM, light);

        // Le rail d'or sur l'arête haute : quatre barres, pas un couvercle.
        rail(hull, entry, TOP_W, TOP_L, RIM, RIM + 0.045f, light);

        // Quatre montants d'acier aux angles, de la quille au rail.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                float x = sx * TOP_W;
                float z = sz * TOP_L;
                box(hull, entry, Math.min(x, x - sx * 0.10f), 0.0f, Math.min(z, z - sz * 0.10f),
                        Math.max(x, x - sx * 0.10f), RIM + 0.045f, Math.max(z, z - sz * 0.10f),
                        POST, light);
            }
        }

        // Le témoin, sur la proue. Allumé, il ignore la lumière ambiante — il
        // doit se voir de nuit comme au fond d'une galerie.
        boolean loaded = entity.isLoaded();
        box(hull, entry, -0.15f, 0.24f, -TOP_L - 0.035f, 0.15f, 0.38f, -TOP_L + 0.02f,
                loaded ? LAMP_ON : LAMP_OFF, loaded ? LightTexture.FULL_BRIGHT : light);

        poseStack.popPose();
        super.render(entity, yRot, partialTick, poseStack, buffer, light);
    }

    /** Une ceinture de bordé : quatre parois, creuse au milieu. */
    private static void belt(VertexConsumer buffer, PoseStack.Pose entry,
                             float halfW, float halfL, float y0, float y1, int light) {
        box(buffer, entry, -halfW, y0, -halfL, -halfW + SKIN, y1, halfL, HULL, light);
        box(buffer, entry, halfW - SKIN, y0, -halfL, halfW, y1, halfL, HULL, light);
        box(buffer, entry, -halfW, y0, -halfL, halfW, y1, -halfL + SKIN, HULL, light);
        box(buffer, entry, -halfW, y0, halfL - SKIN, halfW, y1, halfL, HULL, light);
    }

    /** Le rail d'or : quatre barres suivant l'arête, sans fermer le dessus. */
    private static void rail(VertexConsumer buffer, PoseStack.Pose entry,
                             float halfW, float halfL, float y0, float y1, int light) {
        box(buffer, entry, -halfW, y0, -halfL, -halfW + SKIN, y1, halfL, TRIM, light);
        box(buffer, entry, halfW - SKIN, y0, -halfL, halfW, y1, halfL, TRIM, light);
        box(buffer, entry, -halfW, y0, -halfL, halfW, y1, -halfL + SKIN, TRIM, light);
        box(buffer, entry, -halfW, y0, halfL - SKIN, halfW, y1, halfL, TRIM, light);
    }

    /** Une caisse pleine, ses six faces peintes de la même région. */
    private static void box(VertexConsumer buffer, PoseStack.Pose entry,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float[] uv, int light) {
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
