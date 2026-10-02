package io.github.ninetwo.mdragon.client;

import io.github.ninetwo.mdragon.ModEntities;
import io.github.ninetwo.mdragon.client.render.MilkDragonModel;
import io.github.ninetwo.mdragon.client.render.MilkDragonRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

/**
 * 客户端入口。
 *
 * <p>注册实体渲染器用的是原版 {@link EntityRenderers#register}，
 * <b>不是</b> Fabric 的 {@code EntityRendererRegistry}——后者在 26.3 已标记
 * {@code @Deprecated}，源码注释写着 "Replaced with transitive access wideners"。
 *
 * <p>模型层（模型几何体）用 Fabric 的 {@link ModelLayerRegistry} 注册。
 * ⚠️ 它在 {@code net.fabricmc.fabric.api.client.rendering.v1} 包下，
 * <b>不是</b> {@code fabric-model-loading-api-v1}——后者是方块模型那套，名字很像别搞混。
 */
public class MylovelymilkdragonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册顺序：模型层要在渲染器用到它之前注册好。
        // 渲染器构造函数里的 context.bakeLayer(...) 是【注册实体渲染器时】才调的，
        // 那时模型层已经在了。
        //
        // 两块纸片各注册一层：正面由渲染器本体画，背面由 MilkDragonBackLayer 画，
        // 这样正反面才能各用一张独立的 PNG。详见 MilkDragonModel 的类注释。
        ModelLayerRegistry.registerModelLayer(
                MilkDragonModel.LAYER_FRONT, MilkDragonModel::createFrontLayer);
        ModelLayerRegistry.registerModelLayer(
                MilkDragonModel.LAYER_BACK, MilkDragonModel::createBackLayer);
        EntityRenderers.register(ModEntities.MILK_DRAGON, MilkDragonRenderer::new);
    }
}
