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
 */
public class TrackNearestGoal extends Goal {

    /** 搜索 / 保持跟踪的最大距离（格）。 */
    private static final double TRACK_RANGE = 16.0D;

    /** 靠到这么近就停下看着，不再往前挤，免得把玩家顶得走不动路。 */
    private static final double KEEP_DISTANCE_SQR = 3.0D * 3.0D;

    private final MilkDragonEntity dragon;
    private final double speedModifier;

    /** 当前跟着的对象；{@code null} 表示没在跟。 */
    private LivingEntity tracked;

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
    public void tick() {
        LivingEntity target = this.tracked;
        if (target == null) {
            return;
        }
        // 一直扭头看着对方
        this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.dragon.distanceToSqr(target) > KEEP_DISTANCE_SQR) {
            this.dragon.getNavigation().moveTo(target, this.speedModifier);
        } else {
            this.dragon.getNavigation().stop();
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
