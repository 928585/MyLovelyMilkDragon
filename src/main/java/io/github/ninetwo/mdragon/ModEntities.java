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

    // 注册“奶龙”的实体类型
    public static final EntityType<MilkDragonEntity> MILK_DRAGON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            MILK_DRAGON_KEY,
            EntityType.Builder.<MilkDragonEntity>of(MilkDragonEntity::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.5F) // 碰撞箱：宽1格，高1.5格
                    .build(MILK_DRAGON_KEY)
    );

    public static void registerModEntities() {
        // 注册实体属性（血量、速度等）
        FabricDefaultAttributeRegistry.register(
                MILK_DRAGON, MilkDragonEntity.createMilkDragonAttributes());
    }
}
