package io.github.ninetwo.mdragon.entity;

import io.github.ninetwo.mdragon.ModEntities;
import io.github.ninetwo.mdragon.Mylovelymilkdragon;
import io.github.ninetwo.mdragon.entity.goal.BreakTorchGoal;
import io.github.ninetwo.mdragon.entity.goal.TrackNearestGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 奶龙实体。
 *
 * <p>设计定稿见 CLAUDE.md 第 11 节。要点：
 * <ul>
 *   <li>只主动攻击<b>村民</b>；对玩家只有跟踪（跟着看）和报复（谁打它它追谁），
 *       绝不会主动攻击玩家</li>
 *   <li>三种同步状态：愤怒 / 睡觉 / 大笑</li>
 *   <li>会拆掉周围 4 格内的火把，拆完冷却 5 秒</li>
 * </ul>
 *
 * <p>当前进度：阶段 2。剩下的（掉落、可佩戴头颅、自然生成、药水）在阶段 4~6 补。
 */
public class MilkDragonEntity extends PathfinderMob {

    // ---- 1. 同步数据 ----
    // 这三个状态必须同步到客户端，否则渲染不出「愤怒换贴图」等表现
    private static final EntityDataAccessor<Boolean> DATA_ANGRY =
            SynchedEntityData.defineId(MilkDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(MilkDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LAUGHING =
            SynchedEntityData.defineId(MilkDragonEntity.class, EntityDataSerializers.BOOLEAN);

    // ---- 2. 行为常量 ----

    /** 挨多少下打会进入愤怒状态。 */
    private static final int HITS_TO_BECOME_ANGRY = 3;

    /** 愤怒持续时长：30 秒，到点自动消气。 */
    private static final int ANGRY_DURATION_TICKS = 20 * 30;

    /** 一次大笑持续多久：约 2 秒。 */
    private static final int LAUGH_DURATION_TICKS = 20 * 2;

    /** 平静状态下，两次大笑之间的随机间隔：20~60 秒。 */
    private static final int LAUGH_INTERVAL_MIN_TICKS = 20 * 20;
    private static final int LAUGH_INTERVAL_MAX_TICKS = 20 * 60;

    /** 两次尝试入睡之间的随机间隔：30 秒 ~ 2 分钟。睡觉不分白天晚上。 */
    private static final int SLEEP_INTERVAL_MIN_TICKS = 20 * 30;
    private static final int SLEEP_INTERVAL_MAX_TICKS = 20 * 120;

    /** 玩家靠到多近会把睡着的奶龙吵醒（格）。 */
    private static final double WAKE_PLAYER_RADIUS = 4.0D;

    /** 愤怒时的移动速度加成：0.25 基础值 + 0.05 = 0.3。 */
    private static final double ANGRY_SPEED_BONUS = 0.05D;

    /** 愤怒加速用的修饰符 id，消气时按这个 id 摘掉。 */
    private static final Identifier ANGRY_SPEED_MODIFIER_ID =
            Mylovelymilkdragon.id("angry_speed_boost");

    // ---- 3. 运行时计时器 ----
    // 只在服务端推进，不进存档：读档后从当前状态重新计时即可
    /** 已连续挨打的次数，愤怒解除时清零。 */
    private int hitsTaken;
    /** 愤怒剩余 tick。 */
    private int angryTicksLeft;
    /** 本次大笑剩余 tick。 */
    private int laughTicksLeft;
    /** 距离下一次大笑还有多少 tick。 */
    private int nextLaughTicks;
    /** 距离下一次尝试入睡还有多少 tick。 */
    private int nextSleepTicks;

    // ---- 4. 构造函数 ----
    public MilkDragonEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.nextLaughTicks = this.rollLaughInterval();
        this.nextSleepTicks = this.rollSleepInterval();
    }

    /** 便利构造函数：手动创建时用，实体类型自动取已注册的奶龙。 */
    public MilkDragonEntity(Level level) {
        this(ModEntities.MILK_DRAGON, level);
    }

    // ---- 5. 实体属性 ----
    // 数值以 CLAUDE.md 第 11 节的表格为准。
    // 这里只放基础值，愤怒时的移速加成走下面的速度修饰符，不动基础属性。
    public static AttributeSupplier.Builder createMilkDragonAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)             // 最大生命值 150
                .add(Attributes.MOVEMENT_SPEED, 0.25)          // 移动速度（愤怒时 +0.05 → 0.3）
                .add(Attributes.ATTACK_DAMAGE, 7.0)            // 攻击伤害
                .add(Attributes.ARMOR, 15.0)                   // 护甲值
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)     // 击退抗性：打不动
                .add(Attributes.FOLLOW_RANGE, 40.0);           // 追踪范围
    }

    // ---- 6. 同步数据的注册与读写 ----
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANGRY, false);
        builder.define(DATA_SLEEPING, false);
        builder.define(DATA_LAUGHING, false);
    }

    public boolean isAngry() {
        return this.entityData.get(DATA_ANGRY);
    }

    /**
     * 设置愤怒状态。愤怒会影响移速修饰符，并强制退出睡觉 / 大笑。
     *
     * <p>这里刻意<b>不</b>播音效：读档时也会走到这里，那样一进世界就会吼一声。
     * 要出声请走 {@link #becomeAngry()}。
     */
    public void setAngry(boolean angry) {
        if (this.entityData.get(DATA_ANGRY) == angry) {
            return;
        }
        this.entityData.set(DATA_ANGRY, angry);
        if (angry) {
            this.angryTicksLeft = ANGRY_DURATION_TICKS;
            this.setSleeping(false);
            this.setLaughing(false);
        } else {
            this.angryTicksLeft = 0;
            this.hitsTaken = 0;
        }
        this.refreshAngrySpeedModifier();
    }

    /** 被激怒：进愤怒状态并吼一声。所有「被惹毛」的入口都走这里。 */
    private void becomeAngry() {
        if (this.isAngry()) {
            return;
        }
        this.setAngry(true);
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 0.9F);
    }

    public boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING);
    }

    /**
     * 设置睡眠状态。睡觉期间整个世界对奶龙按下暂停键：
     * {@code setNoAi(true)} 会直接关掉目标选择器，它就不会一边打呼一边打人了。
     *
     * <p>注意：正因为睡觉时 AI 被关掉了，状态机不能放在
     * {@code customServerAiStep}（那是 AI 的一部分），必须放在 {@link #aiStep()}。
     */
    public void setSleeping(boolean sleeping) {
        if (this.entityData.get(DATA_SLEEPING) == sleeping) {
            return;
        }
        this.entityData.set(DATA_SLEEPING, sleeping);
        if (sleeping) {
            this.setLaughing(false);
            this.getNavigation().stop();
            this.setNoAi(true);
        } else {
            this.setNoAi(false);
        }
        this.nextSleepTicks = this.rollSleepInterval();
    }

    public boolean isLaughing() {
        return this.entityData.get(DATA_LAUGHING);
    }

    /** 设置大笑状态。大笑只持续 2 秒，属于瞬时表现，不进存档。 */
    public void setLaughing(boolean laughing) {
        if (this.entityData.get(DATA_LAUGHING) == laughing) {
            return;
        }
        this.entityData.set(DATA_LAUGHING, laughing);
        if (laughing) {
            this.laughTicksLeft = LAUGH_DURATION_TICKS;
            // 只有服务端 tick 会调到这里（读档不还原大笑），所以直接出声是安全的
            if (this.level() instanceof ServerLevel) {
                this.playSound(SoundEvents.VILLAGER_CELEBRATE, 0.8F, 1.4F);
            }
        } else {
            this.laughTicksLeft = 0;
            this.nextLaughTicks = this.rollLaughInterval();
        }
    }

    /**
     * 按当前愤怒状态挂上 / 摘掉移速修饰符。
     *
     * <p>用瞬时修饰符（transient）而不是永久修饰符：它不该跟着实体存进存档，
     * 否则读档时会叠出一堆重复的加成。
     */
    private void refreshAngrySpeedModifier() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (this.isAngry()) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    ANGRY_SPEED_MODIFIER_ID,
                    ANGRY_SPEED_BONUS,
                    AttributeModifier.Operation.ADD_VALUE));
        } else {
            speed.removeModifier(ANGRY_SPEED_MODIFIER_ID);
        }
    }

    // ---- 7. 状态机 ----
    /**
     * {@code aiStep} 每 tick 双侧都会跑（而且不受 {@code setNoAi} 影响），
     * 是唯一一个「睡觉时也一定会执行」的地方，所以状态机挂这里。
     * 里面再判一次服务端，客户端只负责靠数据同步读结果。
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel) {
            this.tickStates(serverLevel);
        }
    }

    private void tickStates(ServerLevel level) {
        // 1) 愤怒倒计时：到点自动消气
        if (this.isAngry() && --this.angryTicksLeft <= 0) {
            this.setAngry(false);
        }

        // 2) 大笑。它必须每 tick 独立推进：要是被别的分支 return 掉，
        //    已经笑到一半的奶龙会卡在「永远在笑」，直到那个分支结束为止。
        this.tickLaughing();

        // 3) 睡觉。睡着时只剩一件事：有没有人被吵醒。
        if (this.isSleeping()) {
            this.tickSleeping(level);
            return;
        }
        // 生气的时候不睡；正在追人的时候也不睡（否则会追到一半原地趴下，很出戏）。
        // 目标一丢，倒计时接着走，很快就会找个地方躺下——「打累了就睡」。
        if (this.isAngry() || this.getTarget() != null) {
            return;
        }
        if (--this.nextSleepTicks <= 0) {
            this.setSleeping(true);
        }
    }

    /** 睡着时每 tick 检查四周：有玩家凑到 4 格以内就吵醒它，而且直接炸毛。 */
    private void tickSleeping(ServerLevel level) {
        Player waker = level.getNearestPlayer(this, WAKE_PLAYER_RADIUS);
        if (waker == null) {
            return;
        }
        this.setSleeping(false);
        this.becomeAngry();
    }

    private void tickLaughing() {
        // 睡觉或者生气时笑不出来；已经在笑的立刻收住
        if (this.isSleeping() || this.isAngry()) {
            this.setLaughing(false);
            return;
        }
        if (this.isLaughing()) {
            if (--this.laughTicksLeft <= 0) {
                this.setLaughing(false);
            }
            return;
        }
        if (--this.nextLaughTicks <= 0) {
            this.setLaughing(true);
        }
    }

    // ---- 8. 受伤 → 计数 → 愤怒 ----
    /**
     * 26.3 里伤害的服务端入口是 {@code hurtServer(ServerLevel, DamageSource, float)}，
     * 不是旧的 {@code hurt(DamageSource, float)}。
     *
     * <p>这里只管「挨打会生气」；「谁打我我打谁」是 {@code HurtByTargetGoal} 的活。
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (!hurt) {
            return false;
        }
        if (this.isSleeping()) {
            // 睡觉被揍：不数次数，直接炸
            this.setSleeping(false);
            this.becomeAngry();
        } else if (!this.isAngry() && ++this.hitsTaken >= HITS_TO_BECOME_ANGRY) {
            this.becomeAngry();
        }
        return true;
    }

    // ---- 9. 存档读写 ----
    // 26.3 起这两个方法用的是 ValueOutput / ValueInput，不再是 CompoundTag。
    // 只存持久状态；计时器和大笑都是瞬时的，读档后重新计时。
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Angry", this.isAngry());
        output.putBoolean("Sleeping", this.isSleeping());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setAngry(input.getBooleanOr("Angry", false));
        this.setSleeping(input.getBooleanOr("Sleeping", false));
    }

    // ---- 10. AI 目标 ----
    @Override
    protected void registerGoals() {
        // 数字越小优先级越高
        this.goalSelector.addGoal(0, new FloatGoal(this));                   // 防溺水
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true)); // 揍村民 / 反击
        this.goalSelector.addGoal(2, new BreakTorchGoal(this, 1.0D));        // 拆火把
        this.goalSelector.addGoal(3, new TrackNearestGoal(this, 1.0D));      // 跟着玩家或村民看
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 报复：谁打它它追杀谁。不限定类型，所以玩家、村民、别的生物都算。
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 只主动攻击村民。
        // 关键：这里【没有】NearestAttackableTargetGoal<Player>，所以奶龙永远不会主动打玩家。
        this.targetSelector.addGoal(2,
                new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    // ---- 11. 工具方法 ----
    /** 在 [min, max] 闭区间里取一个随机 tick 数。 */
    private int randomBetween(int min, int max) {
        return min + this.random.nextInt(max - min + 1);
    }

    private int rollLaughInterval() {
        return this.randomBetween(LAUGH_INTERVAL_MIN_TICKS, LAUGH_INTERVAL_MAX_TICKS);
    }

    private int rollSleepInterval() {
        return this.randomBetween(SLEEP_INTERVAL_MIN_TICKS, SLEEP_INTERVAL_MAX_TICKS);
    }

    // ---- 12. 声音 ----
    // 阶段 2 先拿原版音效占位（愤怒 = 劫掠兽吼，大笑 = 村民欢呼）；
    // 素材到位后改成自定义 SoundEvent，见 CLAUDE.md 第 9 节。
    // TODO(阶段3b): 换成 mylovelymilkdragon:milk_dragon.roar / .laugh
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.15F, 1.0F);
    }

    // ---- 13. 掉落 ----
    // 完全由战利品表控制，不需要覆写方法：
    //   src/main/resources/data/mylovelymilkdragon/loot_table/entities/milk_dragon.json
    // （阶段 4）
}
