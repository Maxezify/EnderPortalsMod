package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.block.TardisDoorBlock;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.maxezify.enderportals.compat.ImmPtlCompat;
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

    /**
     * Haut du panneau de pseudo, juste sous le motif étoile. L'étoile occupe
     * les rangées v = 4…6 de la région FRONT ; le battant s'étend de y = 0,06 à
     * y = 1,94 pour v = 32 → 0, donc le bas de l'étoile tombe à y ≈ 1,53.
     */
    private static final float NAMEPLATE_Y = 1.44f;
    /**
     * Profondeur du panneau de pseudo : juste devant les montants latéraux
     * (z = 0,47), qui sinon masquent la moitié du texte en vue de biais.
     */
    private static final float NAMEPLATE_Z = 0.478f;
    /** Largeur utile de la façade pour le panneau de pseudo. */
    private static final float NAMEPLATE_MAX_WIDTH = 0.78f;
    /** Hauteur d'une ligne de texte, en pixels de police. */
    private static final float GLYPH_HEIGHT = 9.0f;

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
        // Quand on regarde la porte extérieure, Immersive Portals rend le monde
        // de l'Ender avec une caméra virtuelle placée DERRIÈRE la porte
        // intérieure, tournée vers elle : c'est le fond de CETTE porte-là, vu
        // par sa face extérieure, qui bouchait la vue traversante. D'où l'échec
        // de toutes les tentatives 0.4.7 → 0.6.0 : l'élimination des faces
        // arrière conserve précisément la face que voit cette caméra, et
        // éloigner le fond ne change rien puisqu'il est devant elle quoi qu'il
        // arrive.
        //
        // On ne dessine donc pas la porte dans les passes de rendu de portail —
        // la solution retenue par NTM Immersive Portals, qui annule de la même
        // façon le rendu de l'extérieur et de la porte intérieure du TARDIS.
        if (ImmPtlRenderCompat.isRenderingThroughPortal(door.getLevel())) {
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
        // Le fond est un pavé plein, opaque, éclairé normalement, présent porte
        // ouverte comme fermée : la vue traversante ne dépend plus de lui, mais
        // du fait que la porte d'en face n'est pas dessinée dans les passes de
        // portail (garde en tête de render).
        drawBox(vertexBuffer, entry, -0.41f, 0.0f, -0.47f, 0.41f, 2.0f, -0.42f, AXIS_Z, BACK, BACK, alpha, lightCoord, overlay);
        drawBox(vertexBuffer, entry, -0.47f, 0.0f, -0.47f, -0.42f, 2.0f, 0.47f, AXIS_X, BACK, BACK, alpha, lightCoord, overlay);
        drawBox(vertexBuffer, entry, 0.42f, 0.0f, -0.47f, 0.47f, 2.0f, 0.47f, AXIS_X, BACK, BACK, alpha, lightCoord, overlay);
        drawBox(vertexBuffer, entry, -0.41f, 1.95f, -0.41f, 0.41f, 1.98f, 0.44f, AXIS_Y, EDGE, EDGE, alpha, lightCoord, overlay);
        drawBox(vertexBuffer, entry, -0.41f, 0.02f, -0.41f, 0.41f, 0.05f, 0.44f, AXIS_Y, EDGE, EDGE, alpha, lightCoord, overlay);

        // Le battant, dans l'embrasure. Porte ouverte il s'efface entièrement :
        // plaqué contre le flanc gauche, il traversait le plan du portail et
        // gênait l'entrée comme la vue traversante.
        if (!open) {
            drawBox(vertexBuffer, entry, -0.44f, 0.06f, 0.36f, 0.44f, 1.94f, 0.44f, AXIS_Z, FRONT, BACK, alpha, lightCoord, overlay);
        }

        // Voile de vide dans l'embrasure ouverte : c'est le repli visuel quand
        // Immersive Portals est absent. Si le mod est installé, on ne le dessine
        // jamais — sinon il recouvre le portail (vue traversante) dès que le
        // drapeau de synchronisation n'est pas encore parvenu au client.
        if (open && !ImmPtlCompat.isLoaded() && !door.isPortalActive()) {
            VertexConsumer veil = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            drawVoidVeil(veil, entry, alpha, lightCoord, overlay);
        }

        // Petit panneau avec le pseudo du propriétaire, sur la façade fermée.
        // Porte ouverte, le battant s'efface : on ne l'affiche pas.
        String owner = door.getOwnerName();
        if (!open && alpha >= 0.6f && !owner.isEmpty()) {
            drawNameplate(owner, poseStack, entry, buffer, overlay);
        }

        poseStack.popPose();
    }

    /**
     * Dessine le pseudo du propriétaire sur un petit panneau sombre plaqué sur
     * l'avant de la porte (repère local : +Z = devant). Même patron que le
     * texte des pancartes vanilla (translation vers la face avant, échelle avec
     * Y inversé), pleine luminosité pour rester lisible dans le noir.
     */
    private void drawNameplate(String name, PoseStack poseStack, PoseStack.Pose entry,
                               MultiBufferSource buffer, int overlay) {
        int width = font.width(name);
        // Le panneau doit tenir dans la largeur de la façade : les pseudos
        // longs sont rétrécis plutôt que de déborder du caisson.
        float scale = 0.01f;
        if (width * scale > NAMEPLATE_MAX_WIDTH) {
            scale = NAMEPLATE_MAX_WIDTH / width;
        }

        float halfWidth = width * scale / 2.0f;
        float pad = 0.025f;
        float top = NAMEPLATE_Y + pad;
        float bottom = NAMEPLATE_Y - GLYPH_HEIGHT * scale - pad * 0.5f;

        // La plaque sombre du panneau, dessinée nous-mêmes avec la zone sombre
        // de la texture. On n'utilise PAS le fond intégré de drawInBatch : il
        // s'écrit exactement à la même profondeur que les glyphes, si bien que
        // le test de profondeur en départageait la moitié selon l'angle de vue
        // — c'est ce qui masquait un côté du pseudo.
        VertexConsumer plate = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        region(plate, entry,
                -halfWidth - pad, bottom, NAMEPLATE_Z,
                halfWidth + pad, bottom, NAMEPLATE_Z,
                halfWidth + pad, top, NAMEPLATE_Z,
                -halfWidth - pad, top, NAMEPLATE_Z,
                VOID_UV, 1.0f, LightTexture.FULL_BRIGHT, overlay, 0, 0, 1);

        // Le pseudo, franchement devant sa plaque : aucune ambiguïté de
        // profondeur, quel que soit l'angle.
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
