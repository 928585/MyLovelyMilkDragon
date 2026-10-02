package io.github.ninetwo.mdragon;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * 奶龙创造模式物品栏。
 *
 * <p>⚠️ <b>26.3 的原版 {@code CreativeModeTab.builder} 变成了 {@code builder(Row, int)}</b>
 * ——要自己指定在第几行第几列，而原版 TOP / BOTTOM 两行的 0~6 列<b>已经排满了</b>。
 * 自己抢列号会跟别的模组撞车，所以这里走 Fabric 的封装：
 * <b>{@code FabricCreativeModeTab.builder()} 是<b>无参</b>的</b>，内部拿 {@code (null, -1)}
 * 去调原版构造器，真正的摆位交给 Fabric 自己在 {@code CreativeModeInventoryScreen}
 * 上加的分页逻辑处理。
 *
 * <p>⚠️ 包名是 <b>{@code net.fabricmc.fabric.api.creativetab.v1}</b>。
 * 旧版 Fabric 叫 {@code api.itemgroup.v1.FabricItemGroup}，照旧教程写会找不到类。
 */
public class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> MILK_DRAGON_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Mylovelymilkdragon.id("milk_dragon"));

    /** 翻译键 {@code itemGroup.<命名空间>.<路径>} 是社区惯例，不是原版强制的。 */
    public static final CreativeModeTab MILK_DRAGON = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            MILK_DRAGON_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mylovelymilkdragon.milk_dragon"))
                    .icon(() -> new ItemStack(ModBlocks.MILK_DRAGON_EGG))
                    .displayItems((parameters, output) -> {
                        // 顺序：先刷怪蛋和战利品，再是衍生品
                        output.accept(ModItems.MILK_DRAGON_SPAWN_EGG);
                        output.accept(ModBlocks.MILK_DRAGON_EGG);
                        output.accept(ModItems.MILK_DRAGON_HEAD);
                        output.accept(ModItems.MILK_DRAGON_MILK);
                        output.accept(ModItems.MILK_DRAGON_SCALE);
                        // 药水得靠 PotionContents 造带组件的水瓶，直接 accept(POTION) 只会得到一瓶水
                        output.accept(PotionContents.createItemStack(
                                Items.POTION, ModPotions.MILK_DRAGON_POTION));
                    })
                    .build());

    public static void registerModCreativeTabs() {
        // ⚠️ 原版「刷怪蛋」页签的内容是【硬编码】的一长串
        // `spawnEggs.accept(Items.XXX_SPAWN_EGG)`（见 CreativeModeTabs 的 SPAWN_EGGS 分支），
        // 模组的刷怪蛋不会自动进去 —— Fabric 也没有自动兜底。
        //
        // 那为什么创造栏里「搜得到」？因为搜索页签扫的是整个物品注册表，
        // 跟页签自己的内容列表是两回事，搜得到 ≠ 页签里有。
        //
        // 这里用 Fabric 的事件把自己插进去。accept 是追加到列表末尾
        // （原版末尾是末影人/末影螨/潜影贝那组，模组刷怪蛋放最后是惯例）。
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(output -> output.accept(ModItems.MILK_DRAGON_SPAWN_EGG));
    }
}
