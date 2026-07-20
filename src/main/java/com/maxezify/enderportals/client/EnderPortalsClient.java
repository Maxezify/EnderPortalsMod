package com.maxezify.enderportals.client;

import com.maxezify.enderportals.ModBlockEntities;
import com.maxezify.enderportals.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

@Environment(EnvType.CLIENT)
public class EnderPortalsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ENDER_BLOCK, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INACTIVE_TARDIS_DOOR, RenderLayer.getCutout());
        // La surcouche de cristaux du minerai est transparente (base end stone vanilla).
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ENDER_ORE, RenderLayer.getCutoutMipped());

        BlockEntityRendererFactories.register(ModBlockEntities.TARDIS_DOOR, TardisDoorRenderer::new);
    }
}
