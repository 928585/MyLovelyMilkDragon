package io.github.ninetwo.mdragon;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mylovelymilkdragon implements ModInitializer {
    public static final String MOD_ID = "mylovelymilkdragon";

    // 用模组 id 当作 logger 名，方便在日志里分辨输出来自哪个模组
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 注册顺序有讲究：
        //   1. ModDataComponents 自定义数据组件，必须走在用它的物品前面
        //   2. ModEntities 紧随其后：后面的刷怪蛋和自然生成都要引用它的静态字段
        //   3. ModSpawns 依赖 ModEntities.MILK_DRAGON
        //   4. ModEffects 的两个状态效果被 ModItems 的「奶龙的奶」引用，要排在它前面
        //   5. ModItems 里有刷怪蛋引用了 ModEntities.MILK_DRAGON 这个静态字段
        //   6. ModCreativeTabs 的物品栏内容引用了 ModItems / ModBlocks / ModPotions
        //      三家，必须排在最后
        ModDataComponents.registerModDataComponents();
        ModEntities.registerModEntities();
        ModSpawns.registerModSpawns();
        ModBlocks.registerModBlocks();
        ModEffects.registerModEffects();
        ModItems.registerModItems();
        ModPotions.registerModPotions();
        ModCreativeTabs.registerModCreativeTabs();

        LOGGER.info("[奶龙] 模组加载完成");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
