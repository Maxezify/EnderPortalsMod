package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.block.TardisDoorBlock;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/**
 * Dessine la porte du TARDIS (panneau bleu sur deux blocs) avec le fondu de
 * matérialisation, la pulsation pendant les transitions, l'ouverture sur
 * charnière et un voile de vortex sombre dans l'embrasure ouverte (quand
 * Immersive Portals n'affiche pas déjà l'autre côté).
 */
@Environment(EnvType.CLIENT)
public class TardisDoorRenderer implements BlockEntityRenderer<TardisDoorBlockEntity> {

    private static final Identifier TEXTURE = Identifier.of(EnderPortalsMod.MOD_ID, "textures/entity/tardis_door.png");
    private static final boolean IMMPTL_LOADED = FabricLoader.getInstance().isModLoaded("imm_ptl_core")
            || FabricLoader.getInstance().isModLoaded("immersive_portals");

    // Régions UV (texture 64×64).
    private static final float TEX = 64.0f;
    private static final float FRONT_U0 = 0, FRONT_U1 = 16, FRONT_V0 = 0, FRONT_V1 = 32;
    private static final float BACK_U0 = 16, BACK_U1 = 32;
    private static final float EDGE_U0 = 32, EDGE_U1 = 35, EDGE_V0 = 0, EDGE_V1 = 32;
    private static final float VOID_U0 = 48, VOID_U1 = 64, VOID_V0 = 48, VOID_V1 = 64;

    public TardisDoorRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public boolean rendersOutsideBoundingBox(TardisDoorBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 96;
    }

    @Override
    public void render(TardisDoorBlockEntity door, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState state = door.getCachedState();
        if (!state.isOf(ModBlocks.TARDIS_DOOR) || state.get(TardisDoorBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        float alpha = door.getAlpha(tickDelta);
        if (alpha <= 0.02f) {
            return;
        }
        Direction facing = state.get(TardisDoorBlock.FACING);
        boolean open = state.get(TardisDoorBlock.OPEN);
        // Pendant les fondus, la porte irradie légèrement.
        int lightCoord = alpha < 1.0f ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        // Après cette rotation, +Z local pointe vers `facing` (l'avant de la porte).
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));

        // Voile de vortex dans l'embrasure ouverte.
        if (open && !IMMPTL_LOADED) {
            drawVoidVeil(matrices, buffer, alpha, lightCoord, overlay);
        }

        // Panneau de porte, sur charnière (bord gauche) quand elle est ouverte.
        matrices.push();
        if (open) {
            matrices.translate(-0.5, 0.0, 0.375);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-105.0f));
            matrices.translate(0.5, 0.0, -0.375);
        }
        drawPanel(matrices, buffer, alpha, lightCoord, overlay);
        matrices.pop();

        matrices.pop();
    }

    /** Panneau 16×32×2 px, plaqué vers l'avant du bloc (z ≈ +0.375). */
    private static void drawPanel(MatrixStack matrices, VertexConsumer buffer, float alpha, int light, int overlay) {
        float x0 = -0.5f, x1 = 0.5f;
        float y0 = 0.0f, y1 = 2.0f;
        float z0 = 0.3125f, z1 = 0.4375f;
        MatrixStack.Entry entry = matrices.peek();

        // Face avant (+Z) et arrière (−Z).
        quad(buffer, entry, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,
                FRONT_U0, FRONT_V1, FRONT_U1, FRONT_V0, alpha, light, overlay, 0, 0, 1);
        quad(buffer, entry, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,
                BACK_U0, FRONT_V1, BACK_U1, FRONT_V0, alpha, light, overlay, 0, 0, -1);
        // Chants gauche/droit.
        quad(buffer, entry, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0,
                EDGE_U0, EDGE_V1, EDGE_U1, EDGE_V0, alpha, light, overlay, -1, 0, 0);
        quad(buffer, entry, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1,
                EDGE_U0, EDGE_V1, EDGE_U1, EDGE_V0, alpha, light, overlay, 1, 0, 0);
        // Dessus / dessous.
        quad(buffer, entry, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0,
                EDGE_U0, EDGE_V1, EDGE_U1, EDGE_V0, alpha, light, overlay, 0, 1, 0);
        quad(buffer, entry, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1,
                EDGE_U0, EDGE_V1, EDGE_U1, EDGE_V0, alpha, light, overlay, 0, -1, 0);
    }

    /** Voile sombre du vortex, dans le plan de l'embrasure. */
    private static void drawVoidVeil(MatrixStack matrices, VertexConsumer buffer, float alpha, int light, int overlay) {
        float x0 = -0.44f, x1 = 0.44f;
        float y0 = 0.03f, y1 = 1.97f;
        float z = 0.38f;
        float veilAlpha = Math.min(1.0f, alpha) * 0.9f;
        MatrixStack.Entry entry = matrices.peek();
        quad(buffer, entry, x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z,
                VOID_U0, VOID_V1, VOID_U1, VOID_V0, veilAlpha, light, overlay, 0, 0, 1);
        quad(buffer, entry, x1, y0, z, x0, y0, z, x0, y1, z, x1, y1, z,
                VOID_U0, VOID_V1, VOID_U1, VOID_V0, veilAlpha, light, overlay, 0, 0, -1);
    }

    /**
     * Quadrilatère a→b→c→d ; (u0,v0) correspond au coin a, (u1,v1) au coin c.
     * Coordonnées UV en pixels de la texture 64×64.
     */
    private static void quad(VertexConsumer buffer, MatrixStack.Entry entry,
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

    private static void vertex(VertexConsumer buffer, MatrixStack.Entry entry,
                               float x, float y, float z, float u, float v,
                               float alpha, int light, int overlay,
                               float nx, float ny, float nz) {
        buffer.vertex(entry.getPositionMatrix(), x, y, z)
                .color(1.0f, 1.0f, 1.0f, alpha)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(entry, nx, ny, nz);
    }
}
