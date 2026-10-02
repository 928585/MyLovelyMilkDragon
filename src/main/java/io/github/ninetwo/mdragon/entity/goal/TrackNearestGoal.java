package io.github.ninetwo.mdragon.entity.goal;

import java.util.EnumSet;
import java.util.List;

import io.github.ninetwo.mdragon.entity.MilkDragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 跟踪：奶龙会凑到附近的玩家或村民跟前，盯着对方看，但不主动动手。
 *
 * <p>这是「跟踪行为」而不是「攻击行为」——主动攻击村民是
 * {@code NearestAttackableTargetGoal} 的活，对玩家则永远只是跟着看
 * （报复走 {@code HurtByTargetGoal}，谁打它它才追谁）。
 *
 * <p>愤怒、睡觉、或者已经有攻击目标时，这个 Goal 主动让位。
 *
 * <h2>⚠️ 为什么要有重算路径的节流</h2>
 * 一开始这里是「每 tick 调一次 {@code getNavigation().moveTo(target, ...)}」，
 * 结果奶龙在上台阶时会原地转圈。原因是 26.3 的
 * {@code PathNavigation.moveTo(...)} 内部是 {@code createPath(...)}——<b>完整的 A* 寻路</b>，
 * 不是「设个目的地」，每 tick 调一次的后果是：
 * <ul>
 *   <li>路径对象每 tick 换一个新的，<b>节点进度从 0 重新开始</b>；
 *       而 {@code MoveControl} 是拿「当前节点」算朝向的
 *       （{@code atan2(zd, xd) - 90°}，每 tick 最多转 90°），
 *       节点在身子左边还是右边来回跳 → 朝向来回甩。上台阶正是
 *       「我站在哪一格」最模糊的时刻，所以症状在那时候最明显</li>
 *   <li>每次调用都会把卡住检测的基准重置（{@code lastStuckCheck = this.tick}），
 *       而 {@code doStuckDetection} 要 {@code tick - lastStuckCheck > 100} 才触发 ——
 *       于是<b>永远触发不了</b>，奶龙卡住了也不会自救</li>
 *   <li>顺带白烧 CPU：周围 16 格内有玩家或村民时，每 tick 一次 A*</li>
 * </ul>
 *
 * <p>下面照抄原版 {@code MeleeAttackGoal.tick()}（它做的是同一件事：追一个活的目标）：
 * 冷却 4~10 tick，并且要求<b>目标至少挪动了 1 格</b>才值得重算
 * （留 5% 的随机兜底，免得卡在死路上）；寻路失败再额外多等一会儿。
 */
public class TrackNearestGoal extends Goal {

    /** 搜索 / 保持跟踪的最大距离（格）。 */
    private static final double TRACK_RANGE = 16.0D;

    /** 靠到这么近就停下看着，不再往前挤，免得把玩家顶得走不动路。 */
    private static final double KEEP_DISTANCE_SQR = 3.0D * 3.0D;

    /** 两次重算路径之间至少隔的 tick 数（原版 MeleeAttackGoal 用的是 4 + rand(7)）。 */
    private static final int REPATH_INTERVAL_BASE = 4;
    private static final int REPATH_INTERVAL_RANDOM = 7;

    /** 寻路失败（够不着）时额外多等多少 tick，别在原地死磕。 */
    private static final int REPATH_FAIL_BONUS = 15;

    /** 目标挪动超过多远才值得重算一次路径（平方值）。 */
    private static final double REPATH_TARGET_MOVED_SQR = 1.0D;

    /** 目标原地不动时，仍有这么小的概率重算一次。 */
    private static final float REPATH_REROLL_CHANCE = 0.05F;

    private final MilkDragonEntity dragon;
    private final double speedModifier;

    /** 当前跟着的对象；{@code null} 表示没在跟。 */
    private LivingEntity tracked;

    /** 距离下次可以重算路径还有多少 tick。 */
    private int ticksUntilNextPathRecalculation;

    /**
     * 上一次寻路时目标所在的位置。
     * 三个都是 0.0 表示「还没算过」——这是原版 {@code MeleeAttackGoal} 的写法。
     */
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;

    public TrackNearestGoal(MilkDragonEntity dragon, double speedModifier) {
        this.dragon = dragon;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.tracked = this.findNearestTarget();
        return this.tracked != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.tracked == null || !this.tracked.isAlive()) {
            return false;
        }
        if (this.isBusy()) {
            return false;
        }
        return this.dragon.distanceToSqr(this.tracked) <= TRACK_RANGE * TRACK_RANGE;
    }

    @Override
    public void start() {
        // 刚开始追就立刻算一次路径，别先干等一个冷却
        this.ticksUntilNextPathRecalculation = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.tracked;
        if (target == null) {
            return;
        }
        // 一直扭头看着对方
        this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 已经够近了：停下看着，不再往前挤
        if (this.dragon.distanceToSqr(target) <= KEEP_DISTANCE_SQR) {
            this.dragon.getNavigation().stop();
            // 目标一跑开就要立刻跟上，所以这里把冷却清零
            this.ticksUntilNextPathRecalculation = 0;
            return;
        }

        this.ticksUntilNextPathRecalculation =
                Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if (this.ticksUntilNextPathRecalculation > 0) {
            return;
        }

        // 目标没挪窝就不值得重算；0.0 那一串是「还从没算过」的哨兵值
        if (this.pathedTargetX != 0.0D || this.pathedTargetY != 0.0D
                || this.pathedTargetZ != 0.0D) {
            boolean targetMoved = target.distanceToSqr(
                    this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ)
                    >= REPATH_TARGET_MOVED_SQR;
            if (!targetMoved
                    && this.dragon.getRandom().nextFloat() >= REPATH_REROLL_CHANCE) {
                return;
            }
        }

        this.pathedTargetX = target.getX();
        this.pathedTargetY = target.getY();
        this.pathedTargetZ = target.getZ();
        this.ticksUntilNextPathRecalculation =
                REPATH_INTERVAL_BASE + this.dragon.getRandom().nextInt(REPATH_INTERVAL_RANDOM);
        if (!this.dragon.getNavigation().moveTo(target, this.speedModifier)) {
            // 这次够不着，多等一会儿再来
            this.ticksUntilNextPathRecalculation += REPATH_FAIL_BONUS;
        }
    }

    @Override
    public void stop() {
        this.tracked = null;
        this.dragon.getNavigation().stop();
    }

    /** 愤怒 / 睡觉 / 已有攻击目标时不该分心去跟踪。 */
    private boolean isBusy() {
        return this.dragon.isAngry() || this.dragon.isSleeping() || this.dragon.getTarget() != null;
    }

    /** 找 {@value #TRACK_RANGE} 格内最近的玩家或村民，两个都没有就返回 {@code null}。 */
    private LivingEntity findNearestTarget() {
        if (this.isBusy()) {
            return null;
        }
        AABB box = this.dragon.getBoundingBox().inflate(TRACK_RANGE);
        List<LivingEntity> candidates = this.dragon.level().getEntitiesOfClass(
                LivingEntity.class, box,
                entity -> entity.isAlive() && (entity instanceof Player || entity instanceof Villager));

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distance = this.dragon.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
