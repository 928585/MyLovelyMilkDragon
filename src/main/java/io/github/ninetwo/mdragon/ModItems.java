package io.github.ninetwo.mdragon;

import java.util.function.Function;

import io.github.ninetwo.mdragon.item.MilkDragonHeadItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

public class ModItems {
    // 1. 奶龙头（可佩戴，也能给村民戴上把对方同化成奶龙，见 MilkDragonHeadItem）
    public static final Item MILK_DRAGON_HEAD = register("milk_dragon_head",
            properties -> new MilkDragonHeadItem(properties
                    .component(DataComponents.EQUIPPABLE, headEquippable())
                    .delayedComponent(DataComponents.ENCHANTMENTS, ModItems::bindingCurse)));
    // 2. 奶龙鳞片（用于酿造药水）
    public static final Item MILK_DRAGON_SCALE = register("milk_dragon_scale", Item::new);
    // 3. 奶龙刷怪蛋
    // 26.3 起 SpawnEggItem 只接收 Item.Properties，实体类型改为通过 spawnEgg() 数据组件指定
    public static final Item MILK_DRAGON_SPAWN_EGG = register("milk_dragon_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.MILK_DRAGON)));

    // 4. 奶龙的奶
    //
    // 属性直接照抄原版奶桶（从 Items 静态初始化字节码里挖出来的），所以「清空所有状态效果 +
    // 喝完返还空桶」这两条现在就已经生效了。
    // 11.3 里剩下的两条（+5 护甲、幻听）属于阶段 6b，要另外注册自定义 MobEffect 再挂上来。
    public static final Item MILK_DRAGON_MILK = register("milk_dragon_milk",
            properties -> new Item(properties
                    .craftRemainder(Items.BUCKET)                                 // 喝完留空桶
                    .component(DataComponents.CONSUMABLE, Consumables.MILK_BUCKET) // 清除所有状态效果
                    .usingConvertsTo(Items.BUCKET)
                    .stacksTo(1)));

    /**
     * 奶龙头的佩戴信息。
     *
     * <p>跟原版 {@code equippableUnswappable(HEAD)} 的差别只有两点，其余完全一致：
     * <ul>
     *   <li>{@code setEquipOnInteract(true)} —— 打开「右键生物就能给它戴上」的原版流程。
     *       这一步不需要我们写代码：{@code ItemStack.interactLivingEntity} 会先看
     *       {@code EQUIPPABLE} 组件，成立就直接调 {@code Equippable.equipOnTarget} 把帽子扣上去</li>
     *   <li>{@code setAllowedEntities(...)} —— 限定只有玩家和那几种村民类生物戴得上，
     *       否则右键牛、僵尸也会给它们扣帽子</li>
     * </ul>
     *
     * <p>{@code setSwappable(false)} 就是 {@code equippableUnswappable} 的全部内容。
     * {@code setDispensable(false)} 只是不让发射器乱戴，属于顺手收紧。
     */
    private static Equippable headEquippable() {
        return Equippable.builder(EquipmentSlot.HEAD)
                .setSwappable(false)
                .setDispensable(false)
                .setEquipOnInteract(true)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                .setAllowedEntities(MilkDragonHeadItem.ALLOWED_WEARERS)
                .build();
    }

    /**
     * 绑定诅咒：戴上就摘不下来（生存模式下）。
     *
     * <p><b>这里必须用 {@code delayedComponent}，不能直接 {@code component(...)}。</b>
     * 附魔是<b>动态注册表</b>，要凑出 {@code ItemEnchantments} 得先有一个
     * {@code Holder<Enchantment>}，而物品是在类静态初始化里注册的，那时注册表还没加载完。
     * {@code delayedComponent} 收的是一个 {@code create(HolderLookup.Provider)}，
     * 原版会在加载期把它跑一遍——那时候注册表已经齐了，才拿得到 Holder。
     *
     * <p>另外 {@code ItemEnchantments} 本身只有 {@code EMPTY} 和几个只读方法，
     * 想造一个出来只能借道 {@code Mutable}。
     */
    private static ItemEnchantments bindingCurse(HolderLookup.Provider registries) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(
                registries.lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.BINDING_CURSE),
                1);
        return mutable.toImmutable();
    }

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
