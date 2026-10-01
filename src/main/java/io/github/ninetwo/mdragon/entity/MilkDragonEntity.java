package io.github.ninetwo.mdragon.entity;

import io.github.ninetwo.mdragon.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MilkDragonEntity extends PathfinderMob {

    // ---- 1. 定义同步数据 ----
    // 如果你的奶龙有特殊状态需要同步给客户端（比如是否在飞行），可以在这里定义
    // 例如：private static final EntityDataAccessor<Boolean> IS_FLYING =
    //     SynchedEntityData.defineId(MilkDragonEntity.class, EntityDataSerializers.BOOLEAN);

    // ---- 2. 构造函数 ----
    // 这是主要的构造函数，EntityType 和 Level 由游戏在创建实体时传入
    public MilkDragonEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    // 这是一个便利构造函数，方便你手动创建时使用（可选）
    public MilkDragonEntity(Level level) {
        this(ModEntities.MILK_DRAGON, level);
    }

    // ---- 3. 定义实体属性 ----
    // 这个方法会被 ModEntities 类调用，用来设置奶龙的血量、速度等
    public static AttributeSupplier.Builder createMilkDragonAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)        // 最大生命值：30（15颗心）
                .add(Attributes.MOVEMENT_SPEED, 0.3)      // 移动速度
                .add(Attributes.ATTACK_DAMAGE, 5.0)       // 攻击伤害
                .add(Attributes.ARMOR, 2.0)               // 护甲值
                .add(Attributes.FOLLOW_RANGE, 50.0);      // 追踪范围
    }

    // ---- 4. 注册 AI 目标 ----
    // 这是生物行为的核心，优先级数字越小越先执行
    @Override
    protected void registerGoals() {
        // ---- 优先级 0：基础生存 AI ----
        this.goalSelector.addGoal(0, new FloatGoal(this)); // 在水中上浮，防止溺水

        // ---- 优先级 1：战斗 AI ----
        // 让奶龙主动攻击玩家（你可以改成其他生物，或者去掉这条让它更温和）
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true)); // 近战攻击，速度倍率1.0，追击目标

        // ---- 优先级 2：目标选择 AI ----
        // 当奶龙被攻击时，会反击攻击者
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 主动寻找并攻击玩家
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));

        // ---- 优先级 3：悠闲 AI ----
        // 随机漫步，避免走向危险区域（如水边）
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        // 看向附近的玩家
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // 随机环顾四周
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // ---- 5. 定义声音事件 ----
    // 当实体受到伤害或死亡时播放的声音
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT; // 暂时用铁傀儡的受伤音效
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH; // 暂时用铁傀儡的死亡音效
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.15F, 1.0F); // 脚步声
    }

    // ---- 6. 定义掉落物 ----
    // 掉落物完全由战利品表 JSON 控制，无需覆写任何方法：
    //   src/main/resources/data/mylovelymilkdragon/loot_table/entities/milk_dragon.json
    // （26.3 若要覆写掉落逻辑，父类签名是
    //   dropCustomDeathLoot(ServerLevel, DamageSource, boolean)）

    // ---- 7. 覆盖以下方法让生物更“自然” ----
    // 26.3 起 doHurtTarget 需要传入 ServerLevel
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        // 攻击时造成额外效果（可选），比如击退
        boolean success = super.doHurtTarget(level, target);
        if (success) {
            // 可以在这里添加特殊效果，例如点燃目标等
        }
        return success;
    }
}