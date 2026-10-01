package io.github.ninetwo.mdragon;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class ModItems {
    // 1. 牛奶
    public static final Item MILK = register("milk", Item::new);
    // 2. 奶龙头颅
    public static final Item MILK_DRAGON_HEAD = register("milk_dragon_head", Item::new);
    // 3. 奶龙鳞片（用于酿造药水）
    public static final Item MILK_DRAGON_SCALE = register("milk_dragon_scale", Item::new);
    // 4. 奶龙刷怪蛋
    // 26.3 起 SpawnEggItem 只接收 Item.Properties，实体类型改为通过 spawnEgg() 数据组件指定
    public static final Item MILK_DRAGON_SPAWN_EGG = register("milk_dragon_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.MILK_DRAGON)));

    /**
     * 注册一个物品。
     *
     * <p>⚠️ 26.3 的关键约束：<b>物品 id 必须在构造 {@link Item} 之前就写进
     * {@link Item.Properties}</b>。{@code Item} 的构造函数会调用
     * {@code Properties.itemIdOrThrow()} 来拼翻译键（{@code descriptionId}），
     * 拿不到就直接抛 {@code NullPointerException: Item id not set}。
     *
     * <p>这个坑<b>编译期完全看不出来</b>——只在启动时炸。本项目就是在
     * {@code ModItems.<clinit>} 里被它崩掉过一次。
     *
     * <p>所以顺序只能是：先建 key → {@code new Item.Properties().setId(key)}
     * → 再 new 物品 → 最后 {@code Registry.register(注册表, key, 物品)}。
     * 原版 {@code Items} 也是这个写法。
     */
    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Mylovelymilkdragon.id(name));
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                factory.apply(new Item.Properties().setId(key)));
    }

    public static void registerModItems() {
        // 空方法，触发类加载
    }
}
