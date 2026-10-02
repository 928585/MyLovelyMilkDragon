package io.github.ninetwo.mdragon.item;

import io.github.ninetwo.mdragon.ModDataComponents;
import io.github.ninetwo.mdragon.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 奶龙头：既能自己戴，也能给村民戴上把对方变成奶龙。
 *
 * <p><b>「戴上」这一步完全交给原版，我们没有插手。</b>
 * 物品身上带的是 {@code DataComponents.EQUIPPABLE}，由
 * {@code Equippable.builder(HEAD).setEquipOnInteract(true)} 打开「右键生物即可佩戴」。
 * 原版 {@code Mob.checkAndHandleImportantInteractions} 会在打开村民交易界面<b>之前</b>
 * 先走这条路径，所以右键村民是先戴帽子、而不是先弹出交易界面。
 *
 * <p>于是这个类只需要管戴上<b>之后</b>的事，全在 {@link #inventoryTick} 里：
 * <ul>
 *   <li>每 5 秒笑一声（谁戴都笑，玩家也笑）</li>
 *   <li>戴在村民 / 灾厄村民 / 女巫头上满 30 秒 → 同化成奶龙</li>
 * </ul>
 *
 * <p>为什么挂在 {@code inventoryTick} 上：26.3 里 {@code LivingEntity.tick()} 会调用
 * {@code EntityEquipment.tick(this)}，后者对<b>每一个装备槽</b>调用
 * {@code ItemStack.inventoryTick(...)}，最终转发到 {@link Item#inventoryTick} 且<b>只在服务端</b>。
 * 所以任何生物（玩家、村民、灾厄村民、女巫）只要头上戴着它，每 tick 都会执行到这里，
 * 不需要 Mixin，也不需要全局 tick 事件或 attachment。
 */
public class MilkDragonHeadItem extends Item {

    /** 戴上之后多久同化：30 秒。 */
    public static final int CONVERSION_TICKS = 20 * 30;

    /** 戴上之后每隔多久笑一声：5 秒。 */
    public static final int LAUGH_INTERVAL_TICKS = 20 * 5;

    /**
     * 谁能戴上这顶帽子。
     *
     * <p>⚠️ 这里必须<b>连 {@code PLAYER} 一起写上</b>。原版
     * {@code LivingEntity.isEquippableInSlot} 会调 {@code Equippable.canBeEquippedBy(typeHolder())}，
     * 玩家自己往头上戴也走这个检查——只写村民的话，玩家反而戴不上。
     * 没写进这个名单的生物（牛、僵尸……）右键时不会被戴上，会落到普通交互流程。
     *
     * <p><b>改动这里时务必同步改 {@link #isConvertible}</b>：名单里能戴的村民类生物
     * 就会在 30 秒后被同化，两边不一致会出现「戴得上但不变」或「变得了但戴不上」。
     */
    public static final EntityType<?>[] ALLOWED_WEARERS = {
            EntityTypes.PLAYER,
            EntityTypes.VILLAGER,
            EntityTypes.PILLAGER,
            EntityTypes.VINDICATOR,
            EntityTypes.EVOKER,
            EntityTypes.ILLUSIONER,
            EntityTypes.WITCH,
    };

    public MilkDragonHeadItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * 戴着的时候每 tick 走一次，只在服务端。
     *
     * <p>{@code slot} 一定是 {@code HEAD}（只有戴在头上才算数，拿在手里不算），
     * 但保险起见还是判一下——背包里乱放时也会调到这个方法。
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (slot != EquipmentSlot.HEAD) {
            return;
        }
        long now = level.getLevelData().getGameTime();

        this.tickLaughing(stack, entity, now);

        // 玩家不是 Mob，所以这一句天然把玩家排除在「被同化」之外
        if (entity instanceof Mob mob && isConvertible(mob)) {
            this.tickConversion(stack, level, mob, now);
        }
    }

    /** 每 5 秒笑一声。笑声沿用原版村民欢呼占位，等音效素材到位再换（见 CLAUDE.md 第 9 节）。 */
    private void tickLaughing(ItemStack stack, Entity entity, long now) {
        Long next = stack.get(ModDataComponents.NEXT_LAUGH_TICK);
        if (next == null) {
            stack.set(ModDataComponents.NEXT_LAUGH_TICK, now + LAUGH_INTERVAL_TICKS);
            return;
        }
        if (now < next) {
            return;
        }
        entity.playSound(SoundEvents.VILLAGER_CELEBRATE, 0.8F, 1.4F);
        stack.set(ModDataComponents.NEXT_LAUGH_TICK, now + LAUGH_INTERVAL_TICKS);
    }

    /**
     * 倒计时；到点就把佩戴者同化成奶龙。
     *
     * <p>第一次执行时只记下「截止时刻」，之后每 tick 只读不写，避免每 tick 同步物品数据。
     */
    private void tickConversion(ItemStack stack, ServerLevel level, Mob mob, long now) {
        Long deadline = stack.get(ModDataComponents.CONVERSION_DEADLINE);
        if (deadline == null) {
            stack.set(ModDataComponents.CONVERSION_DEADLINE, now + CONVERSION_TICKS);
            return;
        }
        if (now < deadline) {
            return;
        }

        // 头是仪式的消耗品：先摘掉再变身，否则它会作为「装备」被转换流程处理，结果不可控。
        // 想改成「变完把头吐出来」的话，把这一行换成往 level 里 addFreshEntity 一个掉落物即可。
        mob.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        // 参数含义：keepEquipment=false（不保留原装备）、preserveCanPickUpLoot=false
        mob.convertTo(
                ModEntities.MILK_DRAGON,
                ConversionParams.single(mob, false, false),
                EntitySpawnReason.CONVERSION,
                dragon -> { });
    }

    /**
     * 这个生物戴满 30 秒后该不该变成奶龙。
     *
     * <p>口径以 CLAUDE.md 11.2 为准：<b>村民 / 灾厄村民 / 女巫</b>。
     * 26.3 里灾厄村民的公共父类是 {@code AbstractIllager}（Pillager / Vindicator / Evoker /
     * Illusioner 都挂在它下面），女巫单独在 {@code monster.Witch}。
     * 流浪商人（{@code WanderingTrader}）<b>不算</b>村民，不在此列。
     */
    private static boolean isConvertible(LivingEntity entity) {
        return entity instanceof Villager
                || entity instanceof AbstractIllager
                || entity instanceof Witch;
    }
}
