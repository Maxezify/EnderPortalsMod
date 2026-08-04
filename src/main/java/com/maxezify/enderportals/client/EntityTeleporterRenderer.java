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
import net.minecraft.resources.ResourceLocation;

/**
 * La coque du Téléporteur d'entité.
 *
 * <p>Dessinée à la main plutôt qu'avec le modèle de bateau de vanilla : celui-ci
 * est fait de rames, de planches et d'une coque bombée dont rien ne convient à
 * une machine, et le reprendre aurait imposé de suivre ses couches de modèle
 * d'une version à l'autre. Quelques caisses suffisent, avec la même technique
 * que les deux caissons du mod — un quadrilatère texturé à la fois.</p>
 *
 * <p>Le témoin vert s'allume à pleine lumière : c'est le seul retour visuel
 * indiquant qu'une créature est à bord, et il doit se voir de nuit comme au
 * fond d'une galerie.</p>
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

    /** Demi-largeur, demi-longueur et hauteur de la coque, en blocs. */
    private static final float HALF_W = 0.55f;
    private static final float HALF_L = 0.70f;
    private static final float HEIGHT = 0.45f;

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
        poseStack.translate(0.0f, 0.15f, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yRot));

        PoseStack.Pose entry = poseStack.last();
        VertexConsumer hull = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        // Le fond, les quatre flancs, puis le liseré : une coque creuse, pour
        // que les créatures s'y voient assises comme dans un bateau.
        box(hull, entry, -HALF_W, 0.0f, -HALF_L, HALF_W, 0.09f, HALF_L, DECK, light, 0);
        box(hull, entry, -HALF_W, 0.0f, -HALF_L, -HALF_W + 0.09f, HEIGHT, HALF_L, HULL, light, 0);
        box(hull, entry, HALF_W - 0.09f, 0.0f, -HALF_L, HALF_W, HEIGHT, HALF_L, HULL, light, 0);
        box(hull, entry, -HALF_W, 0.0f, -HALF_L, HALF_W, HEIGHT, -HALF_L + 0.09f, HULL, light, 0);
        box(hull, entry, -HALF_W, 0.0f, HALF_L - 0.09f, HALF_W, HEIGHT, HALF_L, HULL, light, 0);
        box(hull, entry, -HALF_W, HEIGHT, -HALF_L, HALF_W, HEIGHT + 0.05f, HALF_L, TRIM, light, 0);

        // Le témoin, sur la proue. Allumé, il ignore la lumière ambiante.
        boolean loaded = entity.isLoaded();
        box(hull, entry, -0.16f, 0.14f, -HALF_L - 0.03f, 0.16f, 0.32f, -HALF_L + 0.02f,
                loaded ? LAMP_ON : LAMP_OFF, loaded ? LightTexture.FULL_BRIGHT : light, 0);

        poseStack.popPose();
        super.render(entity, yRot, partialTick, poseStack, buffer, light);
    }

    /** Une caisse pleine, ses six faces peintes de la même région. */
    private static void box(VertexConsumer buffer, PoseStack.Pose entry,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float[] uv, int light, int overlay) {
        region(buffer, entry, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, uv, light, overlay, 0, 0, 1);
        region(buffer, entry, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, uv, light, overlay, 0, 0, -1);
        region(buffer, entry, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, uv, light, overlay, 1, 0, 0);
        region(buffer, entry, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, uv, light, overlay, -1, 0, 0);
        region(buffer, entry, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, uv, light, overlay, 0, 1, 0);
        region(buffer, entry, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, uv, light, overlay, 0, -1, 0);
    }

    /** Quadrilatère a→b→c→d texturé avec une région {u0,v0,u1,v1}. */
    private static void region(VertexConsumer buffer, PoseStack.Pose entry,
                               float ax, float ay, float az, float bx, float by, float bz,
                               float cx, float cy, float cz, float dx, float dy, float dz,
                               float[] uv, int light, int overlay, float nx, float ny, float nz) {
        vertex(buffer, entry, ax, ay, az, uv[0] / TEX, uv[3] / TEX, light, overlay, nx, ny, nz);
        vertex(buffer, entry, bx, by, bz, uv[2] / TEX, uv[3] / TEX, light, overlay, nx, ny, nz);
        vertex(buffer, entry, cx, cy, cz, uv[2] / TEX, uv[1] / TEX, light, overlay, nx, ny, nz);
        vertex(buffer, entry, dx, dy, dz, uv[0] / TEX, uv[1] / TEX, light, overlay, nx, ny, nz);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose entry,
                               float x, float y, float z, float u, float v,
                               int light, int overlay, float nx, float ny, float nz) {
        buffer.addVertex(entry.pose(), x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(entry, nx, ny, nz);
    }

}
