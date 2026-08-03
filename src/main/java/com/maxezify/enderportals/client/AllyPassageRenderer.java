package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.block.AllyPassageBlock;
import com.maxezify.enderportals.block.PassagePhase;
import com.maxezify.enderportals.block.entity.AllyPassageBlockEntity;
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
 * Dessine le Passage des Alliés — le même caisson creux que la Porte de
 * l'Ender, en quartz et or au lieu de l'obsidienne.
 *
 * <p>Le caisson est ici une nécessité technique autant qu'un parti pris. Un
 * modèle de bloc ne peut pas laisser une embrasure vide sans laisser aussi
 * passer la collision, et c'est précisément ce que réclame un portail
 * d'Immersive Portals : <b>rien</b> dans le plan qu'il occupe. L'arche de la
 * 0.15.0, dessinée en modèle de bloc, plaçait un voile de 2 px exactement à cet
 * endroit et faisait courir ses montants de part en part — le portail se voyait
 * découpé par la géométrie qu'il était censé doubler.</p>
 *
 * <p>Comme pour la porte, le fond du caisson est omis dans une passe de rendu de
 * portail (voir {@link ImmPtlRenderCompat}) : c'est lui, et lui seul, que la
 * caméra virtuelle a devant elle.</p>
 */
public class AllyPassageRenderer implements BlockEntityRenderer<AllyPassageBlockEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderPortalsMod.MODID, "textures/entity/ally_passage.png");

    // Régions UV {u0, v0, u1, v1} en pixels (texture 64×64).
    private static final float TEX = 64.0f;
    private static final float[] FRONT = {0, 0, 16, 32};
    private static final float[] BACK = {16, 0, 32, 32};
    private static final float[] EDGE = {32, 0, 36, 32};
    private static final float[] VEIL_OPENING = {36, 0, 52, 16};
    private static final float[] VEIL_OPEN = {36, 16, 52, 32};
    private static final float[] PLATE = {48, 48, 64, 64};

    private static final int AXIS_X = 0;
    private static final int AXIS_Y = 1;
    private static final int AXIS_Z = 2;

    /** Haut du panneau de pseudo, sous la clé de voûte de la façade close. */
    private static final float NAMEPLATE_Y = 1.44f;
    /** Profondeur du panneau : juste devant les montants latéraux (z = 0,47). */
    private static final float NAMEPLATE_Z = 0.478f;
    private static final float NAMEPLATE_MAX_WIDTH = 0.78f;
    private static final float GLYPH_HEIGHT = 9.0f;

    private final Font font;

    public AllyPassageRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    /** Même portée que la porte : les deux machines se repèrent de loin. */
    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(AllyPassageBlockEntity passage, float tickDelta, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState state = passage.getBlockState();
        if (!state.is(ModBlocks.ALLY_PASSAGE.get())
                || state.getValue(AllyPassageBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        boolean insidePortal = ImmPtlRenderCompat.isRenderingThroughPortal(passage.getLevel());
        Direction facing = state.getValue(AllyPassageBlock.FACING);
        PassagePhase phase = state.getValue(AllyPassageBlock.PHASE);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        // Même convention que la porte : +Z local = avant du caisson.
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        PoseStack.Pose entry = poseStack.last();

        // Le quartz est opaque : couche cutout, profondeur nette, zéro tri.
        VertexConsumer shell = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        // La coque : dos, flancs, linteau, seuil — des pavés fins disjoints, aux
        // cotes de la porte. Le fond disparaît dans une passe de portail ; les
        // montants, eux, restent : ce sont eux qui encadrent la vue traversante.
        if (!insidePortal) {
            drawBox(shell, entry, -0.41f, 0.0f, -0.47f, 0.41f, 2.0f, -0.42f, AXIS_Z, BACK, BACK, light, overlay);
        }
        drawBox(shell, entry, -0.47f, 0.0f, -0.47f, -0.42f, 2.0f, 0.47f, AXIS_X, BACK, BACK, light, overlay);
        drawBox(shell, entry, 0.42f, 0.0f, -0.47f, 0.47f, 2.0f, 0.47f, AXIS_X, BACK, BACK, light, overlay);
        drawBox(shell, entry, -0.41f, 1.95f, -0.41f, 0.41f, 1.98f, 0.44f, AXIS_Y, EDGE, EDGE, light, overlay);
        drawBox(shell, entry, -0.41f, 0.02f, -0.41f, 0.41f, 0.05f, 0.44f, AXIS_Y, EDGE, EDGE, light, overlay);

        if (phase == PassagePhase.CLOSED) {
            // Passage clos : la dalle de quartz obture l'embrasure, exactement
            // comme le battant de la porte.
            drawBox(shell, entry, -0.44f, 0.06f, 0.36f, 0.44f, 1.94f, 0.44f, AXIS_Z, FRONT, BACK, light, overlay);
            String owner = passage.getOwnerName();
            if (!owner.isEmpty()) {
                drawNameplate(owner, poseStack, entry, buffer, overlay);
            }
        } else if (phase != PassagePhase.THROUGH) {
            // OPENING et OPEN : le voile doré. THROUGH n'en a aucun — le plan du
            // portail est là, et un voile devant lui le masquerait.
            float pulse = 0.72f + 0.18f * (float) Math.sin(passage.getAge(tickDelta) * 0.12f);
            VertexConsumer veil = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            drawVeil(veil, entry, phase == PassagePhase.OPENING ? VEIL_OPENING : VEIL_OPEN,
                    pulse, LightTexture.FULL_BRIGHT, overlay);
        }

        poseStack.popPose();
    }

    /**
     * Le pseudo du propriétaire sur la façade close. Même patron que la porte :
     * une plaque dessinée à part, puis le texte franchement devant elle — le
     * fond intégré de {@code drawInBatch} s'écrit à la profondeur des glyphes,
     * et le test de profondeur en escamote la moitié selon l'angle de vue.
     */
    private void drawNameplate(String name, PoseStack poseStack, PoseStack.Pose entry,
                               MultiBufferSource buffer, int overlay) {
        int width = font.width(name);
        float scale = 0.01f;
        if (width * scale > NAMEPLATE_MAX_WIDTH) {
            scale = NAMEPLATE_MAX_WIDTH / width;
        }

        float halfWidth = width * scale / 2.0f;
        float pad = 0.025f;
        float top = NAMEPLATE_Y + pad;
        float bottom = NAMEPLATE_Y - GLYPH_HEIGHT * scale - pad * 0.5f;

        VertexConsumer plate = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        region(plate, entry,
                -halfWidth - pad, bottom, NAMEPLATE_Z,
                halfWidth + pad, bottom, NAMEPLATE_Z,
                halfWidth + pad, top, NAMEPLATE_Z,
                -halfWidth - pad, top, NAMEPLATE_Z,
                PLATE, 1.0f, LightTexture.FULL_BRIGHT, overlay, 0, 0, 1);

        poseStack.pushPose();
        poseStack.translate(0.0, NAMEPLATE_Y, NAMEPLATE_Z + 0.008);
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(name, -width / 2.0f, 0.0f, 0xFFFFFFFF, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    /**
     * Pavé plein : 6 faces émises une seule fois chacune, normales sortantes.
     * Les deux faces perpendiculaires à {@code axis} reçoivent {@code uvPos}
     * (face +) et {@code uvNeg} (face −) ; les autres la bande de chant.
     */
    private static void drawBox(VertexConsumer buffer, PoseStack.Pose entry,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                int axis, float[] uvPos, float[] uvNeg, int light, int overlay) {
        float[] pz = axis == AXIS_Z ? uvPos : EDGE;
        float[] nz = axis == AXIS_Z ? uvNeg : EDGE;
        float[] px = axis == AXIS_X ? uvPos : EDGE;
        float[] nx = axis == AXIS_X ? uvNeg : EDGE;
        float[] py = axis == AXIS_Y ? uvPos : EDGE;
        float[] ny = axis == AXIS_Y ? uvNeg : EDGE;

        region(buffer, entry, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, pz, 1.0f, light, overlay, 0, 0, 1);
        region(buffer, entry, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, nz, 1.0f, light, overlay, 0, 0, -1);
        region(buffer, entry, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, px, 1.0f, light, overlay, 1, 0, 0);
        region(buffer, entry, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, nx, 1.0f, light, overlay, -1, 0, 0);
        region(buffer, entry, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, py, 1.0f, light, overlay, 0, 1, 0);
        region(buffer, entry, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, ny, 1.0f, light, overlay, 0, -1, 0);
    }

    /** Voile doré dans le plan de l'embrasure (deux faces). */
    private static void drawVeil(VertexConsumer buffer, PoseStack.Pose entry, float[] uv,
                                 float alpha, int light, int overlay) {
        float x0 = -0.41f, x1 = 0.41f;
        float y0 = 0.06f, y1 = 1.94f;
        float z = 0.0f;
        region(buffer, entry, x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z, uv, alpha, light, overlay, 0, 0, 1);
        region(buffer, entry, x1, y0, z, x0, y0, z, x0, y1, z, x1, y1, z, uv, alpha, light, overlay, 0, 0, -1);
    }

    /** Quadrilatère a→b→c→d texturé avec une région {u0,v0,u1,v1}. */
    private static void region(VertexConsumer buffer, PoseStack.Pose entry,
                               float ax, float ay, float az, float bx, float by, float bz,
                               float cx, float cy, float cz, float dx, float dy, float dz,
                               float[] uv, float alpha, int light, int overlay,
                               float nx, float ny, float nz) {
        vertex(buffer, entry, ax, ay, az, uv[0] / TEX, uv[3] / TEX, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, entry, bx, by, bz, uv[2] / TEX, uv[3] / TEX, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, entry, cx, cy, cz, uv[2] / TEX, uv[1] / TEX, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, entry, dx, dy, dz, uv[0] / TEX, uv[1] / TEX, alpha, light, overlay, nx, ny, nz);
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
