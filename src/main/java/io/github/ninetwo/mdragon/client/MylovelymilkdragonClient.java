package io.github.ninetwo.mdragon.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * 客户端入口。
 *
 * <p>目前是空壳，阶段 3（客户端渲染）会在这里注册：
 * 奶龙的实体渲染器、模型层（ModelLayer），以及需要的客户端事件。
 */
public class MylovelymilkdragonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 阶段 3 在这里注册实体渲染器与模型层
    }
}
