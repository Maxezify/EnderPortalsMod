package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.block.TardisDoorBlock;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Dessine la Porte de l'Ender : un caisson d'un bloc d'épaisseur composé de
 * pavés fins (aucune face coplanaire — pas de scintillement), avec un
 * panneau qui pivote instantanément contre le flanc gauche à l'ouverture,
 * façon porte vanilla. Hors fondu de matérialisation, tout est rendu sur
 * une couche opaque (z-buffer propre) ; le voile de vide n'apparaît que si
 * aucun portail Immersive Portals ne couvre l'embrasure.
 */
public class TardisDoorRenderer implements BlockEntityRenderer<TardisDoorBlockEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderPortalsMod.MODID, "textures/entity/tardis_door.png");

    // Régions UV {u0, v0, u1, v1} en pixels (texture 64×64).
    private static final float TEX = 64.0f;
    private static final float[] FRONT = {0, 0, 16, 32};
    private static final float[] BACK = {16, 0, 32, 32};
    private static final float[] EDGE = {32, 0, 35, 32};
    private static final float[] VOID_UV = {48, 48, 64, 64};

    private static final int AXIS_X = 0;
    private static final int AXIS_Y = 1;
    private static final int AXIS_Z = 2;

    private final Font font;

    public TardisDoorRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public boolean shouldRenderOffScreen(TardisDoorBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(TardisDoorBlockEntity door, float tickDelta, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState state = door.getBlockState();
        if (!state.is(ModBlocks.TARDIS_DOOR.get()) || state.getValue(TardisDoorBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        float alpha = door.getAlpha(tickDelta);
        if (alpha <= 0.02f) {
            return;
        }
        Direction facing = state.getValue(TardisDoorBlock.FACING);
        boolean open = state.getValue(TardisDoorBlock.OPEN);
        // Pendant les fondus, la porte irradie légèrement.
        int lightCoord = alpha < 1.0f ? LightTexture.FULL_BRIGHT : light;
        boolean stable = alpha >= 0.999f;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        // Orientation validée en jeu (v4) : +Z local = avant de la porte.
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        PoseStack.Pose entry = poseStack.last();

        // Hors fondu : couche opaque (cutout), profondeur nette, zéro tri translucide.
        VertexConsumer vertexBuffer = buffer.getBuffer(stable
                ? RenderType.entityCutoutNoCull(TEXTURE)
                : RenderType.entityTranslucent(TEXTURE));

        // La coque : dos, flancs, plafond, plancher — des pavés fins disjoints.
        drawBox(vertexBuffer, entry, -0.41f, 0.0f, -0.47f, 0.41f, 2.0f, -0.42f, AXIS_Z, BACK, BACK, alpha, lightCoord, overlay);
        drawBox(vertexBuffer, entry, -0.47f, 0.0f, -0.47f, -0.42f, 2.0f, 0.47f, AXIS_X, BACK, BACK, alpha, lightCoord, overlay);
        drawBox(vertexBuffer, entry, 0.42f, 0.0f, -0.47f, 0.47f, 2.0f, 0.47f, AXIS_X, BACK, BACK, alpha, lightCoord, overlay);
        drawBox(vertexBuffer, entry, -0.41f, 1.95f, -0.41f, 0.41f, 1.98f, 0.44f, AXIS_Y, EDGE, EDGE, alpha, lightCoord, overlay);
        drawBox(vertexBuffer, entry, -0.41f, 0.02f, -0.41f, 0.41f, 0.05f, 0.44f, AXIS_Y, EDGE, EDGE, alpha, lightCoord, overlay);

        // Le panneau : fermé dans l'embrasure, ouvert plaqué contre le flanc
        // gauche (pivot instantané, comme les portes vanilla).
        if (open) {
            drawBox(vertexBuffer, entry, -0.41f, 0.07f, -0.40f, -0.33f, 1.93f, 0.36f, AXIS_X, FRONT, BACK, alpha, lightCoord, overlay);
        } else {
            drawBox(vertexBuffer, entry, -0.44f, 0.06f, 0.36f, 0.44f, 1.94f, 0.44f, AXIS_Z, FRONT, BACK, alpha, lightCoord, overlay);
        }

        // Voile de vide dans l'embrasure ouverte, seulement si aucun portail
        // Immersive Portals ne l'occupe déjà.
        if (open && !door.isPortalActive()) {
            VertexConsumer veil = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            drawVoidVeil(veil, entry, alpha, lightCoord, overlay);
        }

        // Petit panneau avec le pseudo du propriétaire, sur le devant (+Z local).
        String owner = door.getOwnerName();
        if (alpha >= 0.6f && !owner.isEmpty()) {
            drawNameplate(owner, poseStack, buffer);
        }

        poseStack.popPose();
    }

    /**
     * Dessine le pseudo du propriétaire sur un petit panneau sombre plaqué sur
     * l'avant de la porte (repère local : +Z = devant). Même patron que le
     * texte des pancartes vanilla (translation vers la face avant, échelle avec
     * Y inversé), pleine luminosité pour rester lisible dans le noir.
     */
    private void drawNameplate(String name, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        // Plaqué juste devant l'avant du caisson (le montant le plus avancé est
        // à +0,47) : le texte affleure la façade sans jamais la traverser.
        // Hauteur : sous le motif étoile du panneau.
        poseStack.translate(0.0, 1.42, 0.481);
        poseStack.scale(0.01f, -0.01f, 0.01f);
        float x = -font.width(name) / 2.0f;
        int background = 0xAA000000;
        font.drawInBatch(name, x, 0.0f, 0xFFFFFFFF, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, background,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    /**
     * Pavé plein : 6 faces émises une seule fois chacune, normales sortantes.
     * Les deux faces perpendiculaires à {@code axis} reçoivent {@code uvPos}
     * (face +) et {@code uvNeg} (face −) ; les autres la bande obsidienne.
     */
    private static void drawBox(VertexConsumer buffer, PoseStack.Pose entry,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                int axis, float[] uvPos, float[] uvNeg,
                                float alpha, int light, int overlay) {
        float[] pz = axis == AXIS_Z ? uvPos : EDGE;
        float[] nz = axis == AXIS_Z ? uvNeg : EDGE;
        float[] px = axis == AXIS_X ? uvPos : EDGE;
        float[] nx = axis == AXIS_X ? uvNeg : EDGE;
        float[] py = axis == AXIS_Y ? uvPos : EDGE;
        float[] ny = axis == AXIS_Y ? uvNeg : EDGE;

        region(buffer, entry, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, pz, alpha, light, overlay, 0, 0, 1);
        region(buffer, entry, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, nz, alpha, light, overlay, 0, 0, -1);
        region(buffer, entry, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, px, alpha, light, overlay, 1, 0, 0);
        region(buffer, entry, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, nx, alpha, light, overlay, -1, 0, 0);
        region(buffer, entry, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, py, alpha, light, overlay, 0, 1, 0);
        region(buffer, entry, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, ny, alpha, light, overlay, 0, -1, 0);
    }

    /** Voile sombre du vortex, dans le plan de l'embrasure (deux faces). */
    private static void drawVoidVeil(VertexConsumer buffer, PoseStack.Pose entry,
                                     float alpha, int light, int overlay) {
        float x0 = -0.41f, x1 = 0.41f;
        float y0 = 0.06f, y1 = 1.94f;
        float z = 0.40f;
        float veilAlpha = Math.min(1.0f, alpha) * 0.9f;
        region(buffer, entry, x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z,
                VOID_UV, veilAlpha, light, overlay, 0, 0, 1);
        region(buffer, entry, x1, y0, z, x0, y0, z, x0, y1, z, x1, y1, z,
                VOID_UV, veilAlpha, light, overlay, 0, 0, -1);
    }

    /** Quadrilatère a→b→c→d texturé avec une région {u0,v0,u1,v1} (bas de région en bas). */
    private static void region(VertexConsumer buffer, PoseStack.Pose entry,
                               float ax, float ay, float az, float bx, float by, float bz,
                               float cx, float cy, float cz, float dx, float dy, float dz,
                               float[] uv, float alpha, int light, int overlay,
                               float nx, float ny, float nz) {
        quad(buffer, entry, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz,
                uv[0], uv[3], uv[2], uv[1], alpha, light, overlay, nx, ny, nz);
    }

    /**
     * Quadrilatère a→b→c→d ; (u0,v0) correspond au coin a, (u1,v1) au coin c.
     * Coordonnées UV en pixels de la texture 64×64.
     */
    private static void quad(VertexConsumer buffer, PoseStack.Pose entry,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1,
                             float alpha, int light, int overlay,
                             float nx, float ny, float nz) {
        vertex(buffer, entry, ax, ay, az, u0 / TEX, v0 / TEX, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, entry, bx, by, bz, u1 / TEX, v0 / TEX, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, entry, cx, cy, cz, u1 / TEX, v1 / TEX, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, entry, dx, dy, dz, u0 / TEX, v1 / TEX, alpha, light, overlay, nx, ny, nz);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose entry,
                               float x, float y, float z, float u, float v,
                               float alpha, int light, int overlay,
                               float nx, float ny, float nz) {
        buffer.addVertex(entry.pose(), x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(entry, nx, ny, nz);
    }
}
