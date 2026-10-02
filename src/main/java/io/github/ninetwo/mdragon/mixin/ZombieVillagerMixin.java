package io.github.ninetwo.mdragon.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.ninetwo.mdragon.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让「奶龙的奶」可以代替金苹果治愈僵尸村民（CLAUDE.md 11.3 第 4 条）。
 *
 * <p>为什么要 Mixin：{@code ZombieVillager.mobInteract} 里的判断是<b>硬编码的字面量</b>，
 * 不是标签、没有任何数据驱动的口子：
 *
 * <pre>{@code
 * ItemStack itemStack = player.getItemInHand(hand);
 * if (itemStack.is(Items.GOLDEN_APPLE)) {          // ← 就这一句
 *     if (this.hasEffect(MobEffects.WEAKNESS)) { ... startConverting(...); }
 *     else { return InteractionResult.CONSUME; }
 * } else { return super.mobInteract(player, hand); }
 * }</pre>
 *
 * <h2>为什么插在 {@code is(...)} 这一句上，而不是 {@code @Inject} 到方法开头</h2>
 *
 * <p>把判断<b>放宽</b>成「金苹果<b>或</b>奶龙的奶」之后，后面的流程原样复用原版代码：
 * 一样要求虚弱效果、一样随机 3600~6000 tick、一样扣物品、一样返回同样的
 * {@code InteractionResult}、一样播音效和那些粒子。<b>一行都不用重写，
 * 也不需要用 {@code @Invoker} 去撬私有的 {@code startConverting}。</b>
 *
 * <h2>两个必须写对的细节</h2>
 *
 * <p><b>1. 目标方法描述符是 {@code is(Ljava/lang/Object;)Z}，不是 {@code is(Predicate)}。</b>
 * {@code ItemStack} 上有<b>两个</b>重载：
 * <ul>
 *   <li>{@code ItemStack.is(Predicate<Holder<Item>>)} —— ItemStack 自己声明的</li>
 *   <li>{@code is(T)} —— 从 {@code ItemInstance → TypedInstance<Item>} 继承来的泛型默认方法，
 *       擦除后签名就是 {@code is(Object)}</li>
 * </ul>
 * {@code Items.GOLDEN_APPLE} 是 {@code Item}，绑的是第二个（已对着字节码常量池核实：
 * {@code Methodref ItemStack.is:(Ljava/lang/Object;)Z}）。写错了匹配不上，
 * 启动时会直接报注入失败。
 *
 * <p><b>2. 用 MixinExtras 的 {@code @ModifyExpressionValue} 而不是 {@code @Redirect}。</b>
 * 它只改「这一句表达式的返回值」，而且我们只会把 false 改成 true、绝不会改回 false，
 * 所以跟别的模组改同一句时不会互相覆盖（{@code @Redirect} 是独占的，只能有一个）。
 * MixinExtras 由 Fabric Loader 自带，不需要额外声明依赖。
 */
@Mixin(ZombieVillager.class)
public class ZombieVillagerMixin {

    @ModifyExpressionValue(
            method = "mobInteract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
    private boolean mdragon$milkDragonMilkCures(boolean original, Player player,
            InteractionHand hand) {
        if (original) {
            return true;
        }
        return player.getItemInHand(hand).is(ModItems.MILK_DRAGON_MILK);
    }
}
