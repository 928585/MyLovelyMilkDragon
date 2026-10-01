# 奶龙模组（mylovelymilkdragon）项目上下文

> 这份文件是给 AI 助手看的项目说明。**每次开新对话，先读完这份文件再动手。**
> 里面标了「已验证」的内容都是直接对着真实 jar / 编译器核实过的，可以信；
> 标了「待核实」的必须先查再写。
>
> **第 11 节的玩法设计已定稿，是唯一的权威需求来源。不要自由发挥。**

---

## 0. 最重要的一条：禁止凭记忆写 API

Minecraft **26.3** 是很新的版本，你的训练数据里几乎肯定没有它的 API。
凡是涉及 Minecraft / Fabric 的调用，**先核实，再写代码**。核实方法见第 4 节。

本项目历史上出现的 4 个编译错误，全部都是「凭记忆写旧版 API」导致的。
26.3 相对旧版本的改动很大（`EntityType.Builder`、`SpawnEggItem`、`Potion`、
`Mob.doHurtTarget`、`Mob.dropCustomDeathLoot` 全都变过签名），**不能靠印象**。

---

## 1. 项目基本信息

| 项 | 值 |
|---|---|
| 模组 id | `mylovelymilkdragon` |
| Java 包 | `io.github.ninetwo.mdragon` |
| 主入口 | `io.github.ninetwo.mdragon.Mylovelymilkdragon`（`ModInitializer`） |
| 客户端入口 | `io.github.ninetwo.mdragon.client.MylovelymilkdragonClient`（`ClientModInitializer`） |
| 版本 / group | `1.0.0` / `io.github.ninetwo.mdragon` |
| 许可 | CC0-1.0 |
| 工作区 | `E:\Java\mcmods\mylovelymilkdragon-template-26.3` |
| 版本控制 | **git 仓库**，默认分支 `main`（2026-10-01 初始化） |
| GitHub | https://github.com/928585/MyLovelyMilkDragon |
| 作者 | `928585`（`fabric.mod.json` 的 `authors` 是自由文本，写什么都行） |

**网络**：本机走 Windows 系统代理 `127.0.0.1:7897`（Clash），但 Git Bash 不继承该设置，
所以 git 推送需要单独配代理（已配在本仓库的 local config，只影响本仓库）：

```bash
git config --local http.proxy http://127.0.0.1:7897
git config --local https.proxy http://127.0.0.1:7897
# 若想让所有仓库都用，把 --local 换成 --global
```

凭据由系统级的 Git Credential Manager 处理，首次 `git push` 会弹窗要求登录 GitHub。

## 2. 版本矩阵与环境（已验证）

版本号一律以 [gradle.properties](gradle.properties) 为准，不要凭记忆填。

| 组件 | 版本 |
|---|---|
| Minecraft | **26.3** |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Fabric Loom | 1.18-SNAPSHOT（实际解析为 1.18.2） |
| Gradle | 9.7.1（wrapper） |
| JDK | 25（`C:\Program Files\Java\jdk-25`，JDK 25.0.1） |
| Java release | 25 |
| Mixin 兼容级别 | `JAVA_25` |

环境是通的：`compileJava` 与 `runClient` 都成功跑过（`run/logs/latest.log` 里
有 "Loading Minecraft 26.3 with Fabric Loader 0.19.5"）。

## 3. 映射：Mojang 官方名（mojmap），不是 Yarn（已验证）

**这是最容易搞错的一点。** 本项目用的是 Mojang 官方映射，依据：

- [build.gradle](build.gradle) 里**没有** `mappings` 声明
- 缓存 jar 里的类名已经是 `net.minecraft.core.BlockPos`、`net.minecraft.resources.Identifier`
- 运行日志里有 `(FabricLoader/Mappings) Mappings not present!`

所以代码里的类名长这样（以下都是本项目代码里实际出现、且编译通过的名字）：

```
net.minecraft.resources.Identifier          ← 不是 Yarn 的 ResourceLocation
net.minecraft.core.Registry
net.minecraft.core.registries.BuiltInRegistries
net.minecraft.core.registries.Registries
net.minecraft.world.entity.Mob
net.minecraft.world.entity.PathfinderMob
net.minecraft.world.level.Level
net.minecraft.server.level.ServerLevel
```

外面网上（教程、Wiki、其他模组）绝大多数用的是 Yarn 名，
**直接抄过来会编译不过**。类名拿不准时一律用第 4 节的办法查，不要照抄 Yarn 教程。

## 4. 怎么核实 API（照抄这些命令）

Shell 是 Git Bash（`bash`）。下面命令都实测可用。

```bash
# 0) 先定义两个路径变量
JAR=$(ls .gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/*/minecraft-merged-*.jar)
JAVAP="/c/Program Files/Java/jdk-25/bin/javap"   # javap 不在 PATH 上，必须用全路径

# 1) 查某个类的全部方法/字段签名 —— 这是最权威的来源
"$JAVAP" -cp "$JAR" net.minecraft.world.entity.EntityType
"$JAVAP" -cp "$JAR" 'net.minecraft.world.entity.EntityType$Builder'   # 内部类用 $，并加引号

# 2) 看某个方法的字节码实现（想知道原版怎么写的时候用）
"$JAVAP" -c -p -cp "$JAR" net.minecraft.world.item.PotionItem
"$JAVAP" -c -p -cp "$JAR" net.minecraft.world.item.Items | grep -A25 "registerSpawnEgg"

# 3) 列原版资源目录结构 / 直接读原版 JSON（写资源文件前必查）
unzip -l "$JAR" | awk '{print $4}' | grep -E "^data/minecraft/"
unzip -p "$JAR" assets/minecraft/lang/en_us.json
unzip -p "$JAR" assets/minecraft/items/diamond.json

# 4) 读 Fabric API 的源码（gradle 缓存里已经有 sources jar）
find ~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api -name "*sources.jar"
```

另外：`./gradlew genSources` **还没跑过**。跑一次会生成反编译源码，
以后在 IDE 里能直接跳转阅读原版实现，比看字节码舒服得多，建议尽早执行。

## 5. 已核实的 26.3 API 变更（踩过的坑，别再踩）

这些签名全部用 `javap` 对着 jar 核实过，可直接依赖。

### 5.1 实体注册

`build()` 现在必须传 `ResourceKey`，且注册表要用同一个 key：

```java
ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, id);
EntityType<MilkDragonEntity> TYPE = Registry.register(
        BuiltInRegistries.ENTITY_TYPE, KEY,
        EntityType.Builder.<MilkDragonEntity>of(MilkDragonEntity::new, MobCategory.MONSTER)
                .sized(1.0F, 1.5F).fireImmune().build(KEY));
```

- 工厂方法是 `EntityType.Builder.of(...)`，**不是** `create(...)`
- 链式调用里 JDT 会把泛型推成 `Entity`，**必须写 `<MilkDragonEntity>of`** 显式类型见证
- 属性注册：`FabricDefaultAttributeRegistry.register(TYPE, Supplier<AttributeSupplier.Builder>)`

### 5.2 自燃与和平难度：都与 MobCategory 无关（重要）

读了 `Mob.aiStep()` 的字节码，真实逻辑是：

```java
if (this.is(EntityTypeTags.BURN_IN_DAYLIGHT)) {   // ← 由实体标签决定
    this.burnUndead();
}
```

- **白天自燃**由实体标签 `#minecraft:burn_in_daylight` 控制，**跟 MobCategory 无关**。
  奶龙用 `MONSTER` 类别但**不加**这个标签 → **白天不会烧**
- **和平难度消失**由 `EntityType.Builder.notInPeaceful()` 控制（内部是
  `EntityType.isAllowedInPeaceful()`），**也跟 MobCategory 无关**。
  奶龙**不加** `notInPeaceful()` → **和平难度下不消失**
- `MobCategory` 生成上限常量：**MONSTER = 70，CREATURE = 10**
  （这是选 MONSTER 的实际好处：村庄里刷得出来）

### 5.3 物品注册：id 必须在 new 之前写进 Properties（**编译期看不出来，一启动就崩**）

26.3 里 `Item` 的构造函数会调 `Properties.itemIdOrThrow()` 去拼翻译键
（`descriptionId`），**拿不到就直接抛异常**：

```
java.lang.NullPointerException: Item id not set
    at net.minecraft.world.item.Item$Properties.itemIdOrThrow(Item.java:459)
    at net.minecraft.world.item.Item.<init>(Item.java:163)
```

本项目 2026-10-01 就是这么崩的（`ModItems.<clinit>`）。
**这个坑 `compileJava` 完全查不出来，只有真启动游戏才会炸。**

正确顺序：先建 key → `setId(key)` → 再 new 物品 → 最后注册。原版 `Items` 也这么写：

```java
private static Item register(String name, Function<Item.Properties, Item> factory) {
    ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Mylovelymilkdragon.id(name));
    return Registry.register(
            BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
}
```

刷怪蛋同理——`SpawnEggItem` 构造函数只剩 `SpawnEggItem(Item.Properties)`，
实体类型走数据组件 `spawnEgg(...)`，但**一样要先 `setId`**：

```java
register("milk_dragon_spawn_egg",
        properties -> new SpawnEggItem(properties.spawnEgg(ModEntities.MILK_DRAGON)));
```

⚠️ **教训：能编译 ≠ 能跑。** 注册类这种只在启动时执行一次的代码，
改完一定要 `./gradlew runClient` 实际启动一次验证，别只看 `compileJava` 绿了就汇报。

### 5.4 药水与状态效果

`Potion` 第一个参数是基础名（String），且原版用 `Holder<Potion>`：

```java
new Potion("milk_dragon", new MobEffectInstance(effect, 3600, 0))
```

`MobEffect` 不是抽象类，构造函数是 `protected` → 用匿名子类。

⚠️ 空壳效果**什么都不会发生**，要真正生效必须覆写
`applyEffectTick(ServerLevel, LivingEntity, int)` /
`shouldApplyEffectTickThisTick(int, int)`，或加属性修饰符。

**加属性修饰符**（已核实签名，用于「+5 护甲」）：

```java
public MobEffect addAttributeModifier(
        Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation)

// 例：Attributes.ARMOR 的类型确认是 Holder<Attribute>
new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFFFFF) {}
        .addAttributeModifier(Attributes.ARMOR, Mylovelymilkdragon.id("milk_dragon_milk"),
                5.0, AttributeModifier.Operation.ADD_VALUE)
```

### 5.5 运行时改属性（愤怒加移速用）

```java
// AttributeModifier 是 record，签名：AttributeModifier(Identifier id, double amount, Operation)
AttributeInstance inst = this.getAttribute(Attributes.MOVEMENT_SPEED);
inst.addOrUpdateTransientModifier(new AttributeModifier(id, 0.05, Operation.ADD_VALUE)); // 进愤怒
inst.removeModifier(id);                                                               // 平息
```

### 5.6 消耗品 / 奶桶（「奶龙的奶」直接照抄这段）

从 `Items` 静态初始化字节码挖出来的原版奶桶真身：

```java
new Item.Properties()
        .craftRemainder(Items.BUCKET)                                    // 喝完留空桶
        .component(DataComponents.CONSUMABLE, Consumables.MILK_BUCKET)    // 清效果
        .usingConvertsTo(Items.BUCKET)
        .stacksTo(1)
```

- `net.minecraft.world.item.component.Consumables` 里有一排现成预设：
  `MILK_BUCKET` / `GOLDEN_APPLE` / `ENCHANTED_GOLDEN_APPLE` / `HONEY_BOTTLE` /
  `DEFAULT_FOOD` / `DEFAULT_DRINK` / …
- 自定义用 `Consumable.builder()`：`consumeSeconds(float)` / `animation(ItemUseAnimation)` /
  `sound(Holder<SoundEvent>)` / `soundAfterConsume(...)` / `hasConsumeParticles(boolean)` /
  `onConsume(ConsumeEffect)` / `build()`
- 消耗效果类都在 `net.minecraft.world.item.consume_effects`：
  `ClearAllStatusEffectsConsumeEffect.INSTANCE`、
  `ApplyStatusEffectsConsumeEffect(MobEffectInstance...)`、
  `RemoveStatusEffectsConsumeEffect`、`PlaySoundConsumeEffect`、`TeleportRandomlyConsumeEffect`

### 5.7 治愈僵尸村民：硬编码，只能 Mixin

`ZombieVillager.mobInteract` 的字节码（`net.minecraft.world.entity.monster.zombie.ZombieVillager`）：

```java
ItemStack stack = player.getItemInHand(hand);
if (stack.is(Items.GOLDEN_APPLE)) {          // ← 硬编码字面量，不是标签
    if (this.hasEffect(MobEffects.WEAKNESS)) {
        stack.consume(1, player);
        if (!level().isClientSide) {
            this.startConverting(player.getUUID(), random.nextInt(2401) + 3600);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
    return InteractionResult.CONSUME;
}
return super.mobInteract(player, hand);
```

**没有数据驱动（标签）的绕法，必须 Mixin `mobInteract`。**

### 5.8 头颅：可佩戴 ≠ 方块（重要，别走弯路）

- **戴在头上不需要任何头颅机制**：`CustomHeadLayer` 的字节码直接读
  `LivingEntityRenderState.headItem`（一个 `ItemStackRenderState`）然后 `submit(...)`，
  也就是**把物品自己的普通模型画到头上**。给物品写个普通模型 + `equippableUnswappable(HEAD)` 就够
- **做成方块是泥潭，本项目已放弃**：`SkullBlockRenderer.createModel` 是
  `if (type instanceof SkullBlock.Types) { switch (enum 的 ordinal) ... }`
  否则 `throw new MatchException`；`SKIN_BY_TYPE` 还是 `private static final Map`
  里硬编码的 6 条原版路径。**原版凋零骷髅头 / 猪灵头 / 苦力怕头走的是完全同一段代码，
  换哪个参考对象都省不掉工作量**

### 5.9 视野染色：无现成扩展点，本项目已放弃

- 原版有 `MobEffectFogEnvironment`（抽象类，`getMobEffect()`）+ `FogEnvironment`
  （`setupFog` / `getBaseColor`），但实现类全是原版写死的
  （Blindness / Darkness / Lava / Water / PowderedSnow），**没有面向模组的注册表**
- `fabric-rendering-v1` 全部 60+ 个类里**没有任何雾 / 画面染色事件**
  （`ColorResolverRegistry` 是生物群系着色，不是屏幕染色）

### 5.10 Mob 方法签名多了 ServerLevel

- `public boolean doHurtTarget(ServerLevel level, Entity target)`
- `protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit)`
- 掉落物正常由战利品表控制，**不需要**覆写任何方法

### 5.11 生物转化（村民同化用）

`Mob.convertTo(EntityType<T>, ConversionParams, EntitySpawnReason, AfterConversion<T>)`
比「先删再生成」更好，同化村民时用它。

### 5.12 破坏方块

`LevelWriter.destroyBlock(BlockPos, boolean dropBlock)` —— 第二参传 `true`
就会正常产生掉落物 + 破坏音效粒子。破坏火把用它。

### 5.13 火把的类谱系（26.3 已大改，别照抄旧版）

26.3 里**没有 `SoulTorchBlock` 这个类了**。整个火把家族只剩两级：

| 类 | 谁在用 |
|---|---|
| `BaseTorchBlock`（抽象） | 只被下面两个继承 |
| └ `TorchBlock` | TORCH / SOUL_TORCH / **COPPER_TORCH**（铜火把是新增的） |
| &nbsp;&nbsp;&nbsp;└ `WallTorchBlock` | WALL_TORCH / SOUL_WALL_TORCH / COPPER_WALL_TORCH |
| └ `RedstoneTorchBlock` | REDSTONE_TORCH |
| &nbsp;&nbsp;&nbsp;└ `RedstoneWallTorchBlock` | REDSTONE_WALL_TORCH |

- 所以 `block instanceof TorchBlock` = **所有发光火把（含灵魂、铜、墙面版），不含红石火把**
- 想连红石火把一起管，就改用 `instanceof BaseTorchBlock`
- 用 `Blocks.TORCH` 之类的字段**判断不出类型**——javap 里它们的声明类型全是 `Block`，
  要知道具体实现类得去看 `Blocks.<clinit>` 的 `new` 指令

### 5.14 状态机该挂在 aiStep，不是 customServerAiStep（重要）

- `Mob.serverAiStep()` 是 **`protected final`**，覆写不了；它内部按顺序调用
  `targetSelector.tick()` → `goalSelector.tick()` → `navigation.tick()` → `customServerAiStep(ServerLevel)`
- `customServerAiStep` 是常规的「每 tick 服务端 AI」扩展点，**但它属于 AI 的一部分**：
  一旦用 `setNoAi(true)` 关掉 AI（睡觉就靠这个），它就不跑了
- 所以自己维护的状态机（愤怒倒计时、睡醒判定）**要放在 `aiStep()`** ——
  它是 `public`，双侧每 tick 必跑，且不受 `noAi` 影响。里面再判一次
  `if (this.level() instanceof ServerLevel)` 隔离客户端
- `setNoAi(true)` 会一并存进存档（`Mob.addAdditionalSaveData` 写的 `NoAI`），重启后仍生效

### 5.15 运行时属性修饰符（愤怒加移速）

```java
AttributeModifier(Identifier, double, AttributeModifier.Operation)   // 是个 record
instance.addOrUpdateTransientModifier(modifier);   // 瞬时：不进存档、不叠层
instance.removeModifier(Identifier);
```

改移速**必须**用修饰符，不能去改 `Attributes.MOVEMENT_SPEED` 的基础值。
用 transient 而不是 permanent，否则读档时会一层层叠上去。

### 5.16 其它零碎

- `Villager` 现在在 **`net.minecraft.world.entity.npc.villager.Villager`**
  （多了中间的 `.villager` 包，`AbstractVillager` 也在那）
- `NearestAttackableTargetGoal(Mob, Class<T>, boolean)`
- `HurtByTargetGoal(PathfinderMob, Class<?>...)` —— 报复用，谁打它追谁
- `Goal.Flag` 只有 `MOVE / LOOK / JUMP / TARGET` 四个
- `BlockPos.betweenClosed(BlockPos, BlockPos)` 迭代出来的对象**会被复用**，
  要留住必须 `.immutable()` 拷一份
- 伤害的服务端入口是 `public boolean hurtServer(ServerLevel, DamageSource, float)`

## 6. 资源文件路径（已验证）

```
src/main/resources/
├── fabric.mod.json
├── mylovelymilkdragon.mixins.json
├── assets/mylovelymilkdragon/
│   ├── lang/en_us.json          # 英文（必做，游戏内回退全靠它）
│   ├── lang/zh_cn.json          # 中文
│   ├── icon.png                 # 已有
│   ├── items/<物品>.json         # 物品「模型定义」，1.21.4+ 的新结构
│   ├── models/item/<物品>.json   # 真正的模型
│   ├── textures/item/<物品>.png  # 贴图
│   ├── sounds.json              # 音效（后期补，见第 9 节）
│   └── textures/entity/<实体>/   # 实体贴图
└── data/mylovelymilkdragon/
    ├── loot_table/entities/<实体>.json   # 注意是单数 loot_table
    ├── recipe/<配方>.json               # 单数 recipe
    ├── advancement/<进度>.json          # 单数 advancement
    └── tags/...
```

物品 JSON 的实际格式（原版 `diamond` 的真实内容）：

```json
// assets/mylovelymilkdragon/items/milk.json
{ "model": { "type": "minecraft:model", "model": "mylovelymilkdragon:item/milk" } }

// assets/mylovelymilkdragon/models/item/milk.json
{ "parent": "minecraft:item/generated",
  "textures": { "layer0": "mylovelymilkdragon:item/milk" } }
```

### 翻译键格式（已验证，从原版 en_us.json 与字节码确认）

| 类型 | 键格式 | 例子 |
|---|---|---|
| 物品 | `item.<命名空间>.<路径>` | `item.mylovelymilkdragon.milk` |
| 实体 | `entity.<命名空间>.<路径>` | `entity.mylovelymilkdragon.milk_dragon` |
| 状态效果 | `effect.<命名空间>.<路径>` | `effect.mylovelymilkdragon.dragon_breath` |
| **药水** | **`item.minecraft.potion.effect.<Potion的名字>`** | `item.minecraft.potion.effect.dragon_breath` |

⚠️ 药水的翻译键走的是**原版命名空间**（由 `PotionItem` 的 `descriptionId + ".effect."`
拼出来），所以 `Potion` 的 String 名字要选个不和原版冲突的。

## 7. 代码规范

- 注释用中文，与现有代码保持一致
- 所有 `Identifier` 统一走 `Mylovelymilkdragon.id("xxx")`，不要写字面量命名空间
- 每个内容类别一个注册类（`ModItems` / `ModEntities` / `ModPotions` / `ModSounds` / …），
  资源路径与注册名保持一致
- 新注册类要在 `Mylovelymilkdragon.onInitialize()` 里调用它的 `registerXxx()`

排版：VS Code 里装了 Checkstyle，会按 **4 空格缩进 / 行宽 100 / 禁用 tab** 报警告。
新写的代码按 4 空格写。`ExampleMixin.java` 还是模板的 tab 版，会一直报
`FileTabCharacterCheck`，**这只是警告，不影响编译**（该文件目前仍是模板遗留物）。

## 8. 常用命令

```bash
./gradlew compileJava    # 只编译 Java（改完代码的快速自检）
./gradlew build          # 完整构建，产物在 build/libs/
./gradlew runClient      # 启动客户端（游戏数据在 run/）
./gradlew genSources     # 生成反编译源码（还没跑过，建议跑）
```

**收尾规矩：每轮对话改完代码，必须跑一次 `./gradlew compileJava` 并确认
`BUILD SUCCESSFUL`，再向用户汇报。** 不要只说「改好了」而不编译。

## 9. 占位策略（贴图 / 音效）

美术与音频素材**由用户后期补**，代码这边先占位。两类的做法不同：

| 资源 | 占位方式 | 素材到位后 |
|---|---|---|
| **贴图** | 资源 JSON 里直接写**最终路径**。PNG 不存在时 MC 显示紫黑格，不会崩 | 把 PNG 丢进对应文件夹即可，**不用改任何代码** |
| **音效** | 阶段 2/3 **直接用原版音效常量**（如 `SoundEvents.IRON_GOLEM_HURT`），保证有声可测 | 新增 `ModSounds` 类 + `sounds.json`，把调用点换成 `ModSounds.XXX`（局部改动） |

**不要**为了占位而创建 `sounds.json` 指向不存在的 `.ogg`——那样会静音并刷日志警告。

## 10. 当前状态

### 已经能用的

- 构建环境完整可用，`compileJava` 通过；git 仓库已建立
- **`runClient` 实机验证通过**（2026-10-01 22:36）：模组加载无报错，
  日志有 `[奶龙] 模组加载完成`，实体与物品注册均未抛异常
- 注册骨架：`ModItems`、`ModEntities`、`ModPotions`（内容见第 11 节，部分待重做）
- `onInitialize()` 已接线，三个 `registerXxx()` 都会调用；client 入口已挂上
- `MilkDragonEntity`：属性、三状态机、只打村民、报复、跟踪、拆火把（阶段 2 完成）
- 渲染管线：`MilkDragonRenderer` + `MilkDragonRenderState`（阶段 3a，几何体借原版猪模型占位）

### 还没做的（重要）

- [ ] **实体贴图一张都没有** —— 渲染器引用的两个 PNG 路径还不存在，游戏里会是紫黑格
- [ ] 模型还是原版猪（`PigModel`），待阶段 3b 换成奶龙自己的
- [ ] 无战利品表、无配方、无自然生成、无 `sounds.json`
- [ ] `ExampleMixin` 是模板遗留物，注入 `MinecraftServer.loadLevel`，目前没用
- [ ] 药水内部效果仍叫「龙息」，**命名待定**（见 11.7），不阻塞开发
- [ ] `README.md` 还是模板内容

## 11. 功能设计（定稿）

> **这是唯一的权威需求来源。** 与代码里的旧雏形冲突时，以本节为准。

### 11.1 奶龙实体

| 项 | 值 |
|---|---|
| 注册 id | `milk_dragon` |
| 类别 | `MobCategory.MONSTER` |
| 和平难度 | **不消失**（不加 `.notInPeaceful()`） |
| 白天自燃 | **不会**（不加 `#minecraft:burn_in_daylight` 标签） |
| 碰撞箱 | 宽 1.0 × 高 1.5 |
| 火焰免疫 | `fireImmune()` |

**属性**（愤怒时只改移动速度）：

| 属性 | 平时 | 愤怒时 |
|---|---|---|
| `MAX_HEALTH` | **150** | 150 |
| `MOVEMENT_SPEED` | **0.25** | **0.3** |
| `ATTACK_DAMAGE` | 7 | 7 |
| `ARMOR` | 15 | 15 |
| `KNOCKBACK_RESISTANCE` | 1.0 | 1.0 |
| `FOLLOW_RANGE` | 40 | 40 |

**三种同步状态**（`SynchedEntityData`，需同步到客户端）：

| 状态 | 触发 | 表现 |
|---|---|---|
| `isAngry` | 挨打累计 3 下，或睡觉时被打断 | 换贴图 + 换音效；移动速度 0.25 → 0.3；30 秒后自动平息 |
| `isSleeping` | **随机发生，白天晚上都会** | 静止不动（`setNoAi(true)`）；被打或有玩家靠近则醒（醒 → 愤怒） |
| `isLaughing` | 随机 | 持续约 2 秒 |

**三者的互斥关系**：愤怒会强制踢出睡觉和大笑；睡觉时笑不出来；睡觉/愤怒期间不推进入睡倒计时。
愤怒期间和追击期间**都不会入睡**（否则会追到一半原地趴下），目标一丢倒计时继续走。

**具体时长与半径**（都写在 `MilkDragonEntity` 顶部的常量区，改数值去那里）：

| 常量 | 值 |
|---|---|
| `HITS_TO_BECOME_ANGRY` | 3 |
| `ANGRY_DURATION_TICKS` | 30 秒 |
| `LAUGH_DURATION_TICKS` | 2 秒 |
| 两次大笑间隔 | 随机 20~60 秒 |
| 两次入睡间隔 | 随机 30 秒 ~ 2 分钟 |
| `WAKE_PLAYER_RADIUS` | 4 格 |
| `ANGRY_SPEED_BONUS` | +0.05（0.25 → 0.3） |

**行为**：

- **跟踪**玩家和村民（凑近到 3 格内就停下注视），但**只主动攻击村民**（`TrackNearestGoal`）
- **不主动攻击玩家**：`targetSelector` 里根本没有 `NearestAttackableTargetGoal<Player>`
- **被攻击时报复**：`HurtByTargetGoal` —— **谁打的他就追杀谁**，不限定生物类型；
  不是无差别攻击（不会去屠村里的牲畜）
- **破坏火把**：范围**周围 4 格**（上下 2 格）、冷却 **5 秒**、走正常破坏流程
  **产生掉落物**（`level.destroyBlock(pos, true)`），见 `BreakTorchGoal`
  - 「火把」= `instanceof TorchBlock`：普通 / 灵魂 / **铜火把**的立式与壁挂版，
    **不含红石火把**（那是红石元件，走 `RedstoneTorchBlock` 另一条线）。
    想连红石火把一起拆就改成 `BaseTorchBlock`，见 5.13
- 防溺水上浮、随机漫步
- 音效：阶段 2/3 用原版占位，见第 9 节

### 11.2 物品

| 注册 id | 显示名 | 说明 |
|---|---|---|
| `milk` | 牛奶 | 已有占位 |
| `milk_dragon_head` | 奶龙头 | **只做可佩戴物品，不做方块**（见 5.8）。`equippableUnswappable(HEAD)`；戴上有绑定诅咒；每 5 秒大笑；右键村民/灾厄村民/女巫 → 戴上 → 30 秒后**同化成奶龙** |
| `milk_dragon_scale` | 奶龙鳞片 | 酿造原料 |
| `milk_dragon_spawn_egg` | 奶龙刷怪蛋 | |
| `milk_dragon_milk` | **奶龙的奶** | 详见 11.3 |
| （原版 `minecraft:sulfur`） | 硫磺 | **直接用原版物品**，不自己注册 |

### 11.3 奶龙的奶（`milk_dragon_milk`）

**饮用效果**（4 条，全部要做）：

1. **原版牛奶效果** —— 清除所有状态效果
2. **+5 护甲** —— 通过一个自定义 `MobEffect` 加属性修饰符（见 5.4）
3. **幻听** —— 喝下后定时给玩家播放随机音效（自定义 `MobEffect` 覆写 `applyEffectTick`）
4. **治愈僵尸村民** —— 可代替金苹果（见 5.7，**必须 Mixin**）

**其他用途**：

- **喂给奶龙** → 使其摆脱愤怒状态（覆写 `MobEntity.mobInteract`）
- 喝下后返还空桶（`craftRemainder`）

**视觉**：视野变黄 —— **已放弃**（见 5.9，无扩展点，用户同意不做）

### 11.4 掉落（战利品表）

固定掉落：**奶龙的奶**、**硫磺**、**奶蛋** ｜ **15%** 掉附魔金苹果 ｜
兼容抢夺附魔 ｜ **爆炸死亡时额外掉奶龙头**

### 11.5 自然生成

- **村庄**：高概率，**无视光照**
- **野外**：低概率（权重视情况调高），**无视光照**
- ⚠️ 「检测附近村庄」这套逻辑**尚未验证可行性**，最坏退化成「野外也生成」

### 11.6 药水

**奶龙药水**，一种新的特殊药水（不是「龙息药水」）。

| 项 | 值 |
|---|---|
| 药水注册 id | `mylovelymilkdragon:milk_dragon` |
| 药水翻译键 | `item.minecraft.potion.effect.milk_dragon` |
| 显示名 | 奶龙药水 / Potion of Milk Dragon |
| 酿造配方 | 粗制的药水 + 奶龙鳞片 → 奶龙药水（阶段 6a 实现） |
| 内部效果 | 暂沿用旧的 `effect.mylovelymilkdragon.dragon_breath`（「龙息」），**命名待定**，见 11.7 |

药水**本身**的名字已定稿（奶龙药水），只是它内部那个**状态效果**的名字还没想好，
先按旧名跑着，不阻塞任何开发。

### 11.7 待定 / 已放弃 / 预留

> **待定**项：用户还没想好，先不定。**不要为了填坑而自由发挥**——
> 按现有旧名跑着即可，等用户明确后再改。

| 项 | 状态 |
|---|---|
| 药水内部**效果**的 id 与显示名 | 🟡 **待定**（暂沿用 `dragon_breath`／「龙息」） |
| 召唤事件（指令 / 奶蛋触发，可飞行的奶龙） | 🟡 **待定**，暂不做 |
| 奶龙祭坛（结构） | 🟡 **待定**，暂不做 |
| 头颅做成方块 | ❌ **放弃**（见 5.8，泥潭；原版三种头颅走同一段硬编码） |
| 视野变黄 | ❌ **放弃**（见 5.9，无扩展点；用户同意不做） |

## 12. 阶段划分（一次对话做一块，按依赖顺序）

| 阶段 | 内容 | 状态 |
|---|---|---|
| **0** | 接线 + 资源骨架：`onInitialize()`、`fabric.mod.json`、client 入口、lang | ✅ 已完成 |
| **1** | 实体骨架：属性表、MONSTER 类别、`fireImmune`、同步状态定义 | ✅ 已完成 |
| **2** | AI 与行为：三状态、跟踪/攻击目标、报复、打火把、随机睡觉、愤怒改移速 | ✅ 已完成 |
| **3a** | 渲染管线：RenderState + Renderer 注册（几何体借原版猪模型占位） | ✅ 已完成 |
| **3b** | 自定义奶龙模型 + 平时/愤怒两套贴图 | ⬜ |
| **4** | 掉落 + 奶龙头（可佩戴物品）：战利品表、头物品、`convertTo` 同化村民 | ⬜ |
| **5** | 自然生成：村庄高概率 + 野外低概率，均无视光照 | ⬜ |
| **6a** | 奶龙药水 + 酿造配方 | ⬜ |
| **6b** | 奶龙的奶的饮用效果：+5 护甲、幻听、喂奶龙消怒、Mixin 治愈僵尸村民 | ⬜ |
| **7** | 预留：召唤事件 + 奶龙祭坛 | ⬜ |

每阶段收尾都要跑 `./gradlew compileJava` 确认 `BUILD SUCCESSFUL`。
