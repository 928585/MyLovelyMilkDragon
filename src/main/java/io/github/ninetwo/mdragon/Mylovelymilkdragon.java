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
        // 注册顺序有讲究：ModItems 里有刷怪蛋引用了 ModEntities.MILK_DRAGON 这个静态字段，
        // 所以必须先让 ModEntities 完成类加载与注册，再加载 ModItems。
        ModEntities.registerModEntities();
        ModItems.registerModItems();
        ModPotions.registerModPotions();

        LOGGER.info("[奶龙] 模组加载完成");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
