package io.github.ninetwo.mdragon.client;

import io.github.ninetwo.mdragon.ModEntities;
import io.github.ninetwo.mdragon.client.render.MilkDragonRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

/**
 * 客户端入口。
 *
 * <p>注册实体渲染器用的是原版 {@link EntityRenderers#register}，
 * <b>不是</b> Fabric 的 {@code EntityRendererRegistry}——后者在 26.3 已标记
 * {@code @Deprecated}，源码注释写着 "Replaced with transitive access wideners"。
 */
public class MylovelymilkdragonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRenderers.register(ModEntities.MILK_DRAGON, MilkDragonRenderer::new);
    }
}
