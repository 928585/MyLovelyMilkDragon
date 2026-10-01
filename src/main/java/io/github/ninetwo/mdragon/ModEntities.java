package io.github.ninetwo.mdragon;

import io.github.ninetwo.mdragon.entity.MilkDragonEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    // 26.3 起 EntityType.Builder.build() 必须传入 ResourceKey，注册表也用同一个 key
    public static final ResourceKey<EntityType<?>> MILK_DRAGON_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Mylovelymilkdragon.id("milk_dragon"));

    // 注册「奶龙」的实体类型。
    //
    // 关于两个容易踩的坑（都已核实，见 CLAUDE.md 第 5.2 节）：
    //   - MONSTER 类别【不会】导致白天自燃。自燃由实体标签 #minecraft:burn_in_daylight 决定，
    //     我们不加那个标签，所以奶龙白天安全。
    //   - MONSTER 类别【不会】导致和平难度消失。那不是由类别决定的，而是由
    //     notInPeaceful() 决定的；我们故意不调用它，奶龙在和平难度下照常存在。
    //   - 选 MONSTER 的实际好处：生成上限是 70，而 CREATURE 只有 10，村庄里才刷得出来。
    public static final EntityType<MilkDragonEntity> MILK_DRAGON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            MILK_DRAGON_KEY,
            EntityType.Builder.<MilkDragonEntity>of(MilkDragonEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.5F) // 碰撞箱：宽 1 格，高 1.5 格
                    .fireImmune()      // 免疫火焰与岩浆
                    .build(MILK_DRAGON_KEY)
    );

    public static void registerModEntities() {
        // 注册实体属性（血量、速度等）
        FabricDefaultAttributeRegistry.register(
                MILK_DRAGON, MilkDragonEntity.createMilkDragonAttributes());
    }
}
