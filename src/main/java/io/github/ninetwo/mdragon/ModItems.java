package io.github.ninetwo.mdragon;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class ModItems {
    // 1. 牛奶
    public static final Item MILK = register("milk", new Item(new Item.Properties()));
    // 2. 奶龙头颅
    public static final Item MILK_DRAGON_HEAD = register("milk_dragon_head",
            new Item(new Item.Properties()));
    // 3. 奶龙鳞片（用于酿造药水）
    public static final Item MILK_DRAGON_SCALE = register("milk_dragon_scale",
            new Item(new Item.Properties()));
    // 4. 奶龙刷怪蛋
    // 26.3 起 SpawnEggItem 只接收 Item.Properties，实体类型改为通过 spawnEgg() 数据组件指定
    public static final Item MILK_DRAGON_SPAWN_EGG = register("milk_dragon_spawn_egg",
            new SpawnEggItem(new Item.Properties().spawnEgg(ModEntities.MILK_DRAGON)));

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, Mylovelymilkdragon.id(name), item);
    }

    public static void registerModItems() {
        // 空方法，触发类加载
    }
}
