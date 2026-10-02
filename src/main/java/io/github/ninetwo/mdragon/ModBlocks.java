package io.github.ninetwo.mdragon;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {
    /**
     * 奶蛋。
     *
     * <p>直接复用原版 {@link DragonEggBlock}——它的构造函数是 {@code public} 且只吃一个
     * {@link BlockBehaviour.Properties}，所以「受重力会掉、打它瞬移、右键也瞬移」
     * 这一整套行为一行都不用抄，继承关系（{@code DragonEggBlock extends FallingBlock}）
     * 已经把它们全带过来了。
     *
     * <p>方块属性也照抄原版龙蛋（{@code Blocks.<clinit>} 里 DRAGON_EGG 的那串调用），
     * 只有 {@code mapColor} 换成了白色——原版龙蛋在地图上是黑的，奶蛋跟着奶龙走白色系。
     */
    public static final Block MILK_DRAGON_EGG = registerWithItem(
            "milk_dragon_egg",
            DragonEggBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 1)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    /**
     * 注册一个方块，<b>同时</b>注册它对应的方块物品。
     *
     * <p>26.3 里方块和方块物品是分开注册的两样东西（原版 {@code Blocks} 只注册方块，
     * 方块物品统一在 {@code Items} 里用私有的 {@code registerBlock} 注册），
     * 所以这里把两步合到一起，免得以后漏掉一半。
     *
     * <p>跟 {@link ModItems#register} 一样，{@code setId} <b>必须</b>在构造之前调用：
     * 方块和物品的构造函数都会去读 id 拼翻译键，拿不到就直接抛异常，
     * 而这个错编译期查不出来。不一样的是方块物品多两个属性：
     * <ul>
     *   <li>{@code useBlockDescriptionPrefix()} —— 让翻译键走 {@code block.<命名空间>.<路径>}
     *       而不是 {@code item.<命名空间>.<路径>}。原版龙蛋的键就是
     *       {@code block.minecraft.dragon_egg}，不是 {@code item.*}</li>
     *   <li>{@code requiredFeatures(方块.requiredFeatures())} —— 原版会顺手带过来，
     *       我们自己注册的方块没有功能开关，是空集，省掉不影响</li>
     * </ul>
     *
     * @return 注册好的方块本体（方块物品挂在注册表里，这里不用返回）
     */
    private static Block registerWithItem(String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Mylovelymilkdragon.id(name));
        Block block = Registry.register(
                BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Mylovelymilkdragon.id(name));
        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(block, new Item.Properties()
                        .setId(itemKey)
                        .useBlockDescriptionPrefix()));
        return block;
    }

    public static void registerModBlocks() {
        // 空方法，触发类加载
    }
}
