package io.github.ninetwo.mdragon.entity;

import io.github.ninetwo.mdragon.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 奶龙实体。
 *
 * <p>设计定稿见 CLAUDE.md 第 11.1 节。要点：
 * <ul>
 *   <li>不主动攻击玩家，只主动攻击村民；被谁打就追杀谁（报复）</li>
 *   <li>三种同步状态：愤怒 / 睡觉 / 大笑</li>
 *   <li>会破坏周围 4 格内的火把（阶段 2 实现）</li>
 * </ul>
 *
 * <p>当前进度：阶段 1（实体骨架）。AI 行为、破坏火把、愤怒改移速等在阶段 2 补。
 */
public class MilkDragonEntity extends PathfinderMob {

    // ---- 1. 同步数据 ----
    // 这三个状态需要同步到客户端，否则客户端渲染不出「愤怒换贴图」等表现
    private static final EntityDataAccessor<Boolean> DATA_ANGRY =
            SynchedEntityData.defineId(MilkDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(MilkDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LAUGHING =
            SynchedEntityData.defineId(MilkDragonEntity.class, EntityDataSerializers.BOOLEAN);

    // ---- 2. 构造函数 ----
    public MilkDragonEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /** 便利构造函数：手动创建时用，实体类型自动取已注册的奶龙。 */
    public MilkDragonEntity(Level level) {
        this(ModEntities.MILK_DRAGON, level);
    }

    // ---- 3. 实体属性 ----
    // 数值以 CLAUDE.md 第 11.1 节的表格为准；愤怒时只改移动速度（0.25 → 0.3），阶段 2 实现
    public static AttributeSupplier.Builder createMilkDragonAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)             // 最大生命值 150
                .add(Attributes.MOVEMENT_SPEED, 0.25)          // 移动速度（愤怒时临时提升到 0.3）
                .add(Attributes.ATTACK_DAMAGE, 7.0)            // 攻击伤害
                .add(Attributes.ARMOR, 15.0)                   // 护甲值
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)     // 击退抗性：打不动
                .add(Attributes.FOLLOW_RANGE, 40.0);           // 追踪范围
    }

    // ---- 4. 同步数据的注册与读写 ----
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

    public void setAngry(boolean angry) {
        this.entityData.set(DATA_ANGRY, angry);
    }

    public boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.entityData.set(DATA_SLEEPING, sleeping);
    }

    public boolean isLaughing() {
        return this.entityData.get(DATA_LAUGHING);
    }

    public void setLaughing(boolean laughing) {
        this.entityData.set(DATA_LAUGHING, laughing);
    }

    // ---- 5. 存档读写 ----
    // 26.3 起这两个方法用的是 ValueOutput / ValueInput，不再是 CompoundTag
    // 大笑只持续 2 秒，属于瞬时状态，不存档
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

    // ---- 6. AI 目标 ----
    // 阶段 2 会大改：加村民攻击目标、跟踪目标、破坏火把、睡觉、大笑。
    // 这里只保留能跑起来的基础部分。
    @Override
    protected void registerGoals() {
        // 优先级 0：基础生存
        this.goalSelector.addGoal(0, new FloatGoal(this)); // 防溺水

        // 优先级 1：近战
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));

        // 目标选择：被谁打就追杀谁（报复）。
        // 注意：奶龙【不】主动攻击玩家，所以这里没有 NearestAttackableTargetGoal<Player>。
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        // 悠闲 AI
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // ---- 7. 声音 ----
    // 阶段 2/3 用原版音效占位；素材到位后改为自定义 SoundEvent（见 CLAUDE.md 第 9 节）
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

    // ---- 8. 掉落 ----
    // 完全由战利品表控制，不需要覆写方法：
    //   src/main/resources/data/mylovelymilkdragon/loot_table/entities/milk_dragon.json
    // （阶段 4）
}
