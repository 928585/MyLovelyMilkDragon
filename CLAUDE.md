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

另外：`./gradlew genSources` **已经跑过了**，反编译源码在
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.3/*-sources.jar`，
解压出来约 7300 个 `.java`（本项目解在 `build/mcsrc`）。
想读原版某个方法的真实实现，**优先读源码而不是 javap 的字节码**，快得多
（5.28 那几个坑全是这么查出来的）。5.31 里讲 `javap` 的地方，只适用于
「要确认某个符号在字节码里的**准确描述符**」这种场景。

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

### 5.17 Goal 里不要用「递减计数器」做冷却（会卡住）

`GoalSelector.tick()` 里 `canUse()` **不保证每 tick 都被调用**（Flag 可能被别的 Goal 占着）。
所以把计时器写成「在 `canUse()` 里 `if (cooldown > 0) cooldown--;`」是有隐患的。

正确做法：存**绝对 tick**，比较 `entity.tickCount`：

```java
private int nextAllowedTick;                 // 字段

if (entity.tickCount < this.nextAllowedTick) return false;   // canUse()
this.nextAllowedTick = entity.tickCount + COOLDOWN;          // 用掉之后
```

这样即使 `canUse()` 隔了几秒才被调一次，时间该过去的也早过去了。

### 5.18 测试用命令（已对着字节码核实）

**`/damage` 的命令树** —— `<damageType>` 不是可有可无的装饰，
**想用 `by` 就必须先把它填上**：

```
damage <目标> <伤害>
  └ damageType                        ← 必须填，否则 by 会被当成它的值
      ├ at <坐标>
      └ by <实体> [from <原因>]
```

```bash
# 让奶龙认出「是你打的」（触发报复）——注意中间那个伤害类型不能省
/damage @e[type=mylovelymilkdragon,limit=1,sort=nearest] 5 minecraft:player_attack by @s
```

常用伤害类型：`player_attack` / `mob_attack` / `generic` / `magic` /
`explosion` / `lava` / `fall`（完整列表见 jar 里的 `data/minecraft/damage_type/`）。

**查奶龙的同步状态**（`Angry` / `Sleeping` 写在 `addAdditionalSaveData` 里，所以能读）：

```bash
/data get entity @e[type=mylovelymilkdragon,limit=1,sort=nearest] Angry
/data get entity @e[type=mylovelymilkdragon,limit=1,sort=nearest] Sleeping
# 返回 1b = 生效中，0b = 没有
```

### 5.19 实体类型常量在 `EntityTypes`（复数），不在 `EntityType`（**最容易踩**）

26.3 里 `net.minecraft.world.entity.EntityType` **只剩类本身**，整个类里只有 3 个
`public static final` 字段。`PLAYER` / `VILLAGER` / `ZOMBIE` 这些常量统统搬到了：

```java
net.minecraft.world.entity.EntityTypes.PLAYER      // ← 复数！跟 Items / Blocks 一个风格
net.minecraft.world.entity.EntityTypes.VILLAGER
net.minecraft.world.entity.EntityTypes.WITCH
```

写 `EntityType.VILLAGER` 会直接编译不过，而且这个错误在照着旧教程抄的时候特别隐蔽。

顺带核实过：**`SoundEvents` / `Items` / `Blocks` / `MobEffects` / `Enchantments` 都没搬家**，
常量数量分别是 1904 / 1253 / 911 / 40 / 43，只有 `EntityType` 被拆了。

### 5.20 装备栏里的物品每 tick 都会被 tick（不用 Mixin、不用全局事件）

想「某生物戴着某物品时每 tick 做点什么」，26.3 有现成的钩子，别去写 Mixin：

```
LivingEntity.tick()
  └─ EntityEquipment.tick(this)          // 遍历全部装备槽
       └─ ItemStack.inventoryTick(level, entity, slot)
            └─ Item.inventoryTick(stack, serverLevel, entity, slot)   // ← 你的覆写点
```

- **任何 `LivingEntity`（含全部 `Mob`）** 只要装备槽里有东西，每 tick 都会走到
- 转发到 `Item.inventoryTick` 时**已经判过 `instanceof ServerLevel`**，所以那里只会被服务端调用
- ⚠️ 26.3 签名是 **`inventoryTick(ItemStack, ServerLevel, Entity, EquipmentSlot)`**，
  不是旧版的 `(ItemStack, Level, Entity, int, boolean)`
- 拿「现在是第几刻」用 `level.getLevelData().getGameTime()`（`Level.getLevelData()` 返回
  `LevelData`，上面有 `getGameTime()`）。**不要用 `entity.tickCount`**——它不进存档，读档归零

**推论：倒计时别每 tick 写组件。** 写组件会同步给客户端，20 包/秒纯属浪费。
存「截止的游戏刻」而不是「还剩多少」，戴上时写一次，之后只读不写。

### 5.21 装备组件：原版自带「右键给生物戴上」

`ItemStack.interactLivingEntity` 的逻辑（已读字节码）：

```java
Equippable equippable = get(DataComponents.EQUIPPABLE);
if (equippable != null && equippable.equipOnInteract()) {
    InteractionResult r = equippable.equipOnTarget(player, target, this);
    if (r != InteractionResult.PASS) return r;   // 成功就不再往下走
}
return getItem().interactLivingEntity(this, player, target, hand);
```

调用链是 `Mob.interact` → `Mob.checkAndHandleImportantInteractions` → 上面这段，
**在 `mobInteract` 之前**。所以「右键村民 → 给村民戴帽子」不需要自己写代码，
也不用 Mixin 村民的 `mobInteract`（跟 5.7 的治愈僵尸村民是两回事）。

```java
Equippable.builder(EquipmentSlot.HEAD)
        .setSwappable(false)          // 等价于 equippableUnswappable(HEAD)
        .setEquipOnInteract(true)     // 打开右键佩戴
        .setAllowedEntities(EntityTypes.PLAYER, EntityTypes.VILLAGER, ...)
        .build()
```

⚠️ **`allowedEntities` 里必须写 `EntityTypes.PLAYER`。** 玩家自己往头上戴走的是
`Equippable.swapWithEquipmentSlot` → `canBeEquippedBy(player.typeHolder())`，
限制名单时不写玩家，玩家反而戴不上。而 `equipOnTarget` 走的是
`LivingEntity.isEquippableInSlot` → 同样会 `canBeEquippedBy`，所以这张名单
**同时**是「谁能戴」和「能给谁戴」的名单。

- `Equippable.Builder` 不设 `equipSound` 时默认是 `ARMOR_EQUIP_GENERIC`
- `equipOnTarget` 内部已经处理了客户端/服务端（`level().isClientSide()` 判断）
  和对 `Mob` 的 `setGuaranteedDrop(slot)`
  - 副作用值得利用：戴着自定义头的怪**死了会把头掉出来**，不会凭空蒸发

⚠️ **包名是 `net.minecraft.world.item.equipment.Equippable`**，不是
`net.minecraft.world.entity.Equippable`。它是数据组件（是个 `record`），
放在 `world/item/equipment/` 下；`entity/` 包下没有这个类。

`equipOnTarget` 的准入条件只有三条（已读字节码），**它自己不查 `allowedEntities`**：

```java
if (target.isEquippableInSlot(stack, this.slot)   // ← 名单是在这里头查的
        && !target.hasItemInSlot(this.slot)        // 槽位必须是空的
        && target.isAlive()) {
    ...  target.setItemSlot(this.slot, stack.split(1));   // 只取 1 个
}
return InteractionResult.PASS;
```

`isEquippableInSlot` 内部：

```java
return slot == equippable.slot()
        && canUseSlot(equippable.slot())
        && equippable.canBeEquippedBy(this.typeHolder());   // ← allowedEntities 真正生效处
```

因为 `equipOnTarget` 第一步就调 `isEquippableInSlot`，名单**间接**照样管用；
只是别去 `equipOnTarget` 里找 `allowedEntities`，会以为没生效。

### 5.22 给物品挂附魔：必须用 `delayedComponent`

附魔是**动态注册表**，凑一个 `ItemEnchantments` 需要 `Holder<Enchantment>`，
而物品是在类静态初始化里注册的，那时候注册表还没加载完——**拿不到 Holder**。

解法是 `Item.Properties.delayedComponent`：

```java
public <T> Item.Properties delayedComponent(DataComponentType<T> type,
        DataComponentInitializers.SingleComponentInitializer<T> initializer)
// SingleComponentInitializer<C> { C create(HolderLookup.Provider registries); }
```

原版会在**加载期**带着 `HolderLookup.Provider` 跑一遍这个 initializer，那时注册表已经齐了：

```java
.delayedComponent(DataComponents.ENCHANTMENTS, registries -> {
    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    mutable.set(registries.lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.BINDING_CURSE), 1);
    return mutable.toImmutable();
})
```

- `ItemEnchantments` 是**不可变**的，没有公开构造函数，只有 `EMPTY` 和几个只读方法
  → 想造一个出来只能借道 `ItemEnchantments.Mutable(ItemEnchantments)` + `set(...)` + `toImmutable()`
- `Enchantments.BINDING_CURSE` 的类型是 `ResourceKey<Enchantment>`，**不是** `Holder<Enchantment>`
- `HolderLookup.Provider.lookupOrThrow(ResourceKey)` → `RegistryLookup<T>`（继承 `HolderGetter<T>`）
  → `getOrThrow(ResourceKey<T>)` 返回 `Holder.Reference<T>`
- **绑定诅咒是靠附魔效果组件 `EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE` 生效的**，
  原版 `swapWithEquipmentSlot` 里就是查这个（创造模式除外）

### 5.23 战利品表 JSON（26.3 格式，字段名变了）

放 `data/<ns>/loot_table/entities/<实体名>.json`。

**实体怎么找到这张表：** `EntityType.Builder` 里有个 `lootTable` 字段，类型是 `DependantName`
——**从实体 id 自动派生**，不用手动指定。只有调了 `noLootTable()` 才会没有。
我们的奶龙没调，所以只要文件放对路径就自动生效。

26.3 的字段名跟旧教程不一样，**单数**：

| 旧写法 | 26.3 |
|---|---|
| `"functions": [...]` | **`"modifier": [...]`** |
| `"conditions": [...]` | **`"condition": {...}`**（单个对象，不是数组） |

`LootTable` 本体只有 4 个字段：`type` / `pools` / `random_sequence` / `modifier`。
原版 1447 张表**全都**写了 `random_sequence`，照写别省。

常用片段（都从原版表里抄的，可直接用）：

```jsonc
// 定数量
{ "type": "minecraft:set_count",
  "count": { "type": "minecraft:uniform", "min": 1, "max": 2 } }

// 抢夺每级 +0~1 个
{ "type": "minecraft:enchanted_count_increase",
  "count": { "type": "minecraft:uniform", "min": 0.0, "max": 1.0 },
  "enchantment": "minecraft:looting" }

// 概率掉落 + 抢夺加成（base 是「1 级时的概率」，不是 0 级）
{ "type": "minecraft:random_chance_with_enchanted_bonus",
  "enchantment": "minecraft:looting",
  "unenchanted_chance": 0.15,
  "enchanted_chance": { "type": "minecraft:linear", "base": 0.16, "per_level_above_first": 0.01 } }

// 判断死因（爆炸 / 火焰 / 摔落……）
// ⚠️ "id" 必须带 # 前缀，否则会被当成【单个伤害类型 id】去查注册表，然后解析失败
// 字段名是 id + expected（expected: false 表示「不属于这个标签」）
{ "type": "minecraft:damage_source_properties",
  "predicate": { "tags": [ { "expected": true, "id": "#minecraft:is_explosion" } ] } }
```

- `condition` 可以放在 **pool 级**（跟 `entries`/`rolls` 平级）也可以放在 **entry 级**
- 伤害来源标签写在 `data/minecraft/tags/damage_type/` 下，
  比如 `is_explosion` = fireworks / explosion / player_explosion / bad_respawn_point
- 别的常用标签：`is_fire` / `is_projectile` / `is_fall` / `is_drowning` / `bypasses_armor`
- 找原版真实写法：`data/minecraft/enchantment/` 下**每个保护类附魔**都用了这个条件，
  最标准的参照是 `blast_protection.json`（爆炸保护，正好就是 `#minecraft:is_explosion`）
- 确认实体掉落上下文里有 `DAMAGE_SOURCE`：`LivingEntity.dropFromLootTable` 写进去的参数是
  `THIS_ENTITY` / `ORIGIN` / `DAMAGE_SOURCE` / `ATTACKING_ENTITY` /
  `DIRECT_ATTACKING_ENTITY` / `LAST_DAMAGE_PLAYER`

> ⚠️ **战利品表写错会连累整个世界的加载。** 本项目 2026-10-01 踩过一次：
> `id` 少了 `#`，日志报
> `Failed to parse mylovelymilkdragon:entities/milk_dragon` +
> `Failed to load level data or datapacks, can't proceed with server load`，
> **世界直接进不去**（不是「少掉一件东西」那么轻）。
> 所以改完战利品表**必须真的进一次世界**看日志，光编译过、光 JSON 语法合法都没用。

#### 文件路径是怎么派生的（已逐字节核实，两边前缀不一样）

实体和方块的掉落表**都不用手动指定**，各自从自己的 id 自动拼出来，
但**前缀一个是 `entities/`、一个是 `blocks/`**：

```java
// EntityType.Builder 里
ResourceKey.create(Registries.LOOT_TABLE, id.identifier().withPrefix("entities/"))

// BlockBehaviour.Properties 里（字段 drops，是个 DependantName）
ResourceKey.create(Registries.LOOT_TABLE, id.identifier().withPrefix("blocks/"))
```

| 对象 | 注册 id | 自动派生的键 | 文件放哪 |
|---|---|---|---|
| 实体 | `mylovelymilkdragon:milk_dragon` | `mylovelymilkdragon:entities/milk_dragon` | `data/mylovelymilkdragon/loot_table/entities/milk_dragon.json` |
| 方块 | `mylovelymilkdragon:milk_dragon_egg` | `mylovelymilkdragon:blocks/milk_dragon_egg` | `data/mylovelymilkdragon/loot_table/blocks/milk_dragon_egg.json` |

- 实体那边靠 `EntityType.Builder` 的 `lootTable` 字段，**调了 `noLootTable()` 才会没有**
- 方块那边靠 `BlockBehaviour.Properties.drops`，同理是 `noLootTable()` / `overrideLootTable(...)`
- 两边都读了「没设 id 就抛异常」的分支（方块那边报的是 **`Block id not set`**），
  又一次说明 `setId` 是硬性要求

### 5.24 自定义数据组件

```java
public static final DataComponentType<Long> XXX = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Mylovelymilkdragon.id("xxx"),
        DataComponentType.<Long>builder().persistent(Codec.LONG).build());
```

- `Registry.register(Registry<V>, Identifier, T extends V)` —— 泛型够宽，直接返回 `DataComponentType<T>`
- **`.persistent(Codec)` 不能省**，不然组件进不了存档
- 想要客户端也能读到就再加 `.networkSynchronized(StreamCodec)`

### 5.25 方块与方块物品：26.3 是**两步注册**，而且翻译键前缀不一样

**方块和它的方块物品是两样东西，要分别注册。** 原版 `Blocks` 只注册方块本体，
方块物品统一丢在 `Items` 里用一个私有的 `registerBlock` 注册：

```java
// Blocks 里的（public，可以直接调用）
public static Block register(ResourceKey<Block> key, Function<Properties, Block> factory, Properties props) {
    Block block = factory.apply(props.setId(key));      // ← setId 是它帮你调的
    return Registry.register(BuiltInRegistries.BLOCK, key, block);
}

// Items 里的（private，抄它的写法）
private static Item registerBlock(BlockItemId id, Block block, BiFunction<Block, Properties, Item> factory,
        Properties props) {
    return registerItem(id.item(), p -> factory.apply(block, p),
            props.useBlockDescriptionPrefix()               // ← 关键差异
                 .requiredFeatures(block.requiredFeatures()));
}
```

自己写的时候合到一起最省事：

```java
ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Mylovelymilkdragon.id(name));
Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
        factory.apply(properties.setId(blockKey)));

ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Mylovelymilkdragon.id(name));
Registry.register(BuiltInRegistries.ITEM, itemKey,
        new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
```

⚠️ **三个都核实过、都会踩的点：**

- `BlockBehaviour.Properties.setId(ResourceKey<Block>)` 是 **public**，且跟 5.3 的
  `Item.Properties.setId` 一样**必须在构造方块之前调用**，否则构造函数拼不出翻译键
- `Item.Properties.useBlockDescriptionPrefix()` 让方块物品的翻译键走
  **`block.<命名空间>.<路径>`**，不是 `item.*`。原版龙蛋的键就是
  `block.minecraft.dragon_egg`（读 `assets/minecraft/lang/en_us.json` 确认过）。
  **漏了这个，方块物品在游戏里会显示成原始键名**
- `BlockItem` 的构造函数还是老样子：`BlockItem(Block, Item.Properties)`

**能直接复用的原版方块：** 想要「原版行为」时先看目标方块类的构造函数是不是 `public`。
比如 `DragonEggBlock(BlockBehaviour.Properties)` 就是 public，直接 `new` 即可拿到
「受重力掉落 + 被打瞬移」全套行为，一行逻辑都不用抄。

### 5.26 创造模式物品栏：原版要坐标，Fabric 有免坐标封装

⚠️ **26.3 的原版 builder 变成了 `CreativeModeTab.builder(Row, int)`** —— 要自己指定
第几行第几列：

```java
public static CreativeModeTab.Builder builder(CreativeModeTab.Row row, int column)
// Row 是枚举，只有 TOP / BOTTOM 两个值
```

而原版 **TOP 和 BOTTOM 两行的 0~6 列已经排满了**（TOP：建筑/颜色/自然/功能/红石/快捷栏/搜索；
BOTTOM：战斗/工具/食物/材料/刷怪蛋/管理员/生存物品栏）。要去抢第 7 列就会跟别的模组撞车。

**正确做法是用 Fabric 的封装，它是无参的：**

```java
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        TAB_KEY,
        FabricCreativeModeTab.builder()                    // ← 无参！
                .title(Component.translatable("itemGroup.<命名空间>.<路径>"))
                .icon(() -> new ItemStack(ModBlocks.MILK_DRAGON_EGG))
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.MILK_DRAGON_SCALE);
                    output.accept(ModBlocks.MILK_DRAGON_EGG);   // 方块物品直接传方块
                })
                .build());
```

- 包名是 **`net.fabricmc.fabric.api.creativetab.v1`**。旧版 Fabric 叫
  `api.itemgroup.v1.FabricItemGroup`，**照旧教程写会找不到类**
- 内部实现是 `super(null, -1)`，真正的摆位由 Fabric 打在
  `CreativeModeInventoryScreen` 上的分页逻辑（`FabricCreativeGuiComponents`）处理
- `FabricCreativeModeTabBuilderImpl.build()` 会检查有没有设过 `title`，
  没设直接抛 `IllegalStateException`
- `displayItems` 的参数是 `(ItemDisplayParameters, CreativeModeTab.Output)`，
  `Output.accept(ItemLike)` 就行

**药水不能直接 `output.accept(Items.POTION)`** —— 那只会得到一瓶普通水瓶。
必须带上药水组件：

```java
output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.MILK_DRAGON_POTION));
// public static ItemStack createItemStack(Item, Holder<Potion>)
```

#### ⚠️ 模组刷怪蛋**不会**自动进原版「刷怪蛋」页签

**「搜索找得到」≠「页签里有」**，这俩是两回事，别被搜索骗了：

- 搜索页签扫的是**整个物品注册表**，所以任何注册过的物品都搜得到
- 而「刷怪蛋」页签的内容是**硬编码**的一长串
  `spawnEggs.accept(Items.XXX_SPAWN_EGG)`（`CreativeModeTabs` 的 `SPAWN_EGGS` 分支，
  26.3 里从 `Items.SPAWNER` 一路写到 `Items.SHULKER_SPAWN_EGG`）
- **Fabric 也没有自动兜底**（翻遍 fabric 源码，只有 `CreativeModeTabsMixin`
  在做分页和重名检测，没有碰刷怪蛋列表）

正确做法是用 Fabric 的事件往那个页签里插（`ModCreativeTabs` 里就有现成的例子）：

```java
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
        .register(output -> output.accept(ModItems.MILK_DRAGON_SPAWN_EGG));
```

- `CreativeModeTabs.SPAWN_EGGS` 是 `ResourceKey<CreativeModeTab>`，**public**，直接用
- 事件在 `CreativeModeTab.buildContents` 的 `@TAIL` 触发，
  **对原版页签一样生效**（`alignedRight` 的那几个特殊页签会被跳过，
  但 `SPAWN_EGGS` 不是，它没调 `alignedRight()`）
- `FabricCreativeModeTabOutput` 除了 `accept`（追加到末尾），还有
  `prepend` / `insertAfter(anchor, ...)` / `insertBefore(anchor, ...)`，
  想排在某个原版刷怪蛋旁边就用 `insertAfter(Items.VILLAGER_SPAWN_EGG, ...)`
- 同一个事件对所有页签都有：`CreativeModeTabEvents.MODIFY_OUTPUT_ALL`

### 5.27 自然生成：两步走，「无视光照」用 `Mob.checkMobSpawnRules`

**注册自然生成要同时做两件事，缺一不可**（见 `ModSpawns`）：

```java
// 1) 决定「站在哪儿才允许生成」
public static <T extends Mob> void register(EntityType<T>, SpawnPlacementType,
        Heightmap.Types, SpawnPlacements.SpawnPredicate<T>)

// 2) 塞进各生物群系的刷怪表（Fabric API）
public static void addSpawn(Predicate<BiomeSelectionContext> biomeSelector, MobCategory category,
        EntityType<?> entityType, int weight, int minGroupSize, int maxGroupSize)
```

**⚠️ 「无视光照」的正确做法（已读字节码）** —— 光照检查在**谓词**里，不在
`SpawnPlacementType` 里。原版两层：

```java
// net.minecraft.world.entity.Mob —— public，只有两行，完全不查光照
public static boolean checkMobSpawnRules(EntityType<? extends Mob> type, LevelAccessor level,
        EntitySpawnReason reason, BlockPos pos, RandomSource random) {
    BlockPos below = pos.below();
    return EntitySpawnReason.isSpawner(reason)
            || level.getBlockState(below).isValidSpawn(level, below, type);
}

// net.minecraft.world.entity.monster.Monster —— 光照检查在这儿
public static boolean checkMonsterSpawnRules(...) {
    return (EntitySpawnReason.ignoresLightRequirements(reason)
                || isDarkEnoughToSpawn(level, pos, random))     // ← 就是它
            && checkMobSpawnRules(...);
}
```

所以**想不查光照就直接指向 `Mob.checkMobSpawnRules`**（它是 public）。
两个走不通的岔路，别再试：

- `Monster.checkMonsterSpawnRules` —— 会要求黑暗，奶龙白天就不刷了
- `Monster.checkAnyLightMonsterSpawnRules` —— 它本身确实不查光照（只是转发给
  `checkMobSpawnRules`），但参数类型是 `EntityType<? extends Monster>`，
  而 **`MilkDragonEntity` 继承的是 `PathfinderMob`**，类型对不上，编译不过

`SpawnPlacementTypes` 只有 4 个常量：`NO_RESTRICTIONS` / `IN_WATER` / `IN_LAVA` /
`ON_GROUND`。奶龙用 `ON_GROUND` + `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`。

**⚠️「村庄检测」只能做到生物群系级。** 原版 5 个标签
（都在 `data/minecraft/tags/worldgen/biome/has_structure/` 下）：

| 标签 | 包含的生物群系 |
|---|---|
| `village_plains` | plains、meadow |
| `village_desert` | desert |
| `village_savanna` | savanna |
| `village_snowy` | snowy_plains |
| `village_taiga` | taiga |

`BiomeTags` 里**没有**这 5 个的常量（那 86 个常量只覆盖 `is_*` 之类），
必须自己 `TagKey.create(Registries.BIOME, Identifier.withDefaultNamespace("has_structure/village_plains"))`。
语义是「这个生物群系里**能**生成村庄」，不是「附近真的有个村庄」——
真正的后者要按区块查 `StructureStart`，代价大得多，而且原版刷怪本来也按生物群系走。

**Fabric `addSpawn` 的两个前置断言**（读源码确认，违反直接抛异常）：

- `entityType.getCategory() != MobCategory.MISC`，否则报
  `Cannot add spawns for entities with category=MISC since they'd be replaced by pigs.`
- 实体类型必须已注册

**村庄和野外用互斥的筛选器**，否则同一个生物群系会拿到两份权重叠加：

```java
Predicate<BiomeSelectionContext> village = BiomeSelectors.foundInOverworld().and(IS_VILLAGE_BIOME);
Predicate<BiomeSelectionContext> wild    = BiomeSelectors.foundInOverworld().and(IS_VILLAGE_BIOME.negate());
```

`BiomeSelectionContext` 上可用的判断：`getBiomeKey()` / `getBiome()` / `getBiomeHolder()` /
`hasTag(TagKey<Biome>)` / `validForStructure(ResourceKey<Structure>)` /
`canGenerateIn(ResourceKey<LevelStem>)` / `hasFeature(...)` / `hasPlacedFeature(...)`。
`BiomeSelectors` 另有现成的 `foundInOverworld()` / `foundInTheNether()` /
`foundInTheEnd()` / `tag(...)` / `includeByKey(...)` / `excludeByKey(...)` / `spawnsOneOf(...)`。

### 5.28 纸片模型（厚度 0 的平面）与「剔除」渲染类型的命名陷阱

> 以下全部对着 `genSources` 生成的反编译源码核实过（源码 jar 在
> `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.3/*-sources.jar`，
> 解压到 `build/mcsrc` 就能直接读）。

#### 一次绘制只能绑一张贴图 ⇒ 正反面想用独立 PNG 就必须拆成两个渲染层

这是「正反面要不要各自一个文件」这个设计问题的硬约束：

- 同一个 `ModelPart` 树在一次 `submitModel` 里画完，**只绑一张贴图**
- 所以「正面一个 PNG、背面另一个 PNG」在同一个模型里**做不到**
- 解法：拆成两块独立的纸片，各自烘焙成独立的模型层
  （`ModelLayerLocation` 的第二个参数是**层名字符串**，同一个 id 可以注册多层）：

```java
public static final ModelLayerLocation LAYER_FRONT =
        new ModelLayerLocation(Mylovelymilkdragon.id("milk_dragon"), "front");
public static final ModelLayerLocation LAYER_BACK =
        new ModelLayerLocation(Mylovelymilkdragon.id("milk_dragon"), "back");
```

正面归渲染器本体（`context.bakeLayer(LAYER_FRONT)` 塞进 `super(...)`），
背面挂一个渲染层（见下面「自定义渲染层」）。

#### UV 是怎么落的（`ModelPart.Cube` 构造函数，第 290-322 行）

```java
float u0 = xTexOffs;
float u1 = xTexOffs + depth;
float u2 = xTexOffs + depth + width;
float u3 = xTexOffs + depth + width + depth;
float u4 = xTexOffs + depth + width + depth + width;
float v1 = yTexOffs + depth;
float v2 = yTexOffs + depth + height;
// NORTH 用 (u1,v1)-(u2,v2)，SOUTH 用 (u3,v1)-(u4,v2)
```

**`depth = 0` 时**：`u1 = xTexOffs`、`u2 = xTexOffs + width`、
`u3 = xTexOffs + width`、`u4 = xTexOffs + 2*width`。也就是——

| 面 | 占贴图哪一块（`depth = 0`） |
|---|---|
| `NORTH` | `u ∈ [xTexOffs, xTexOffs + width]` |
| `SOUTH` | `u ∈ [xTexOffs + width, xTexOffs + 2*width]` |

而 `v ∈ [yTexOffs, yTexOffs + height]`，**v=0 在上（是头顶），没有垂直翻转**。

⚠️ **想让某一个面独占整张贴图，SOUTH 必须把 texOffs 往左推 `width`。**
因为 NORTH 天然落在 `[x, x+w]`，而 SOUTH 永远从 `x+w` 起算——
只要 `width = 贴图宽`、`texOffs = -width`，SOUTH 就被拉回 `[0, width]` 正好铺满：

```java
createPlaneLayer(Direction.NORTH, z, 0);           // 正面：texOffs(0, 0)
createPlaneLayer(Direction.SOUTH, z, -PLANE_SIZE); // 背面：texOffs(-32, 0)，贴图也是 32 宽
```

负数 texOffs 完全合法（源码里只是除法算 UV，没有任何校验），别被吓到。

⚠️ **`depth` 一旦不为 0，`u3`/`u4` 会多偏移一个 `depth`**，上面这套算法就不成立了。
想要厚度就拆成两个 cube，各只留一个面。本项目要的就是「零厚度的纸」，所以 `depth = 0`。

⚠️ 想让「1 贴图像素 = 1 模型单位」，就把 `width` 设成模型边长、
`LayerDefinition.create(mesh, width, height)`，这样完全不用做换算。

#### 正反两面都**不镜像**（已按顶点绕序算过）

- 模型空间里实体**正面 = `-z`（NORTH 方向）**（原版猪、村民的头都在负 z）
- 渲染器里 `rotateDegrees(YP, 180 - bodyRot)` 配合 `scale(-1,-1,1)`，
  净效果是模型 `(x,y,z) → 世界 (x,-y,-z)`
- 于是：看正面的人，右侧 = `+x` = 模型 `maxX` = 贴图 `u` **较大**的一侧；
  看背面的人，右侧 = `-x` = 模型 `minX` = 贴图 `u` **较大**的一侧
- **⇒ 正反两张图都是「照平常画法画就行」，不用手动镜像**
- 两块纸片在 z 上错开 1 个模型单位（正面 z=0、背面 z=1）：
  背面是 SOUTH 面，方向跟正面相反，所以从任何角度都只有一块纸片
  「正面朝着镜头」——另一块被剔除掉，压根不画

#### ⚠️ 26.3 的 `entityCutout` 是**不剔背面**的那个，跟旧版命名相反

```java
// RenderPipelines.java
public static final RenderPipeline ENTITY_CUTOUT_CULL = register(...);   // ← 默认，剔背面
public static final RenderPipeline ENTITY_CUTOUT = register(
        ...
        .withCull(false)                                                 // ← 明确关掉剔除！
        .build());
```

- `RenderTypes.entityCutout(Identifier)` → `ENTITY_CUTOUT` → **不剔背面**
- `RenderTypes.entityCutoutCull(Identifier)` → `ENTITY_CUTOUT_CULL` → **剔背面**
- 而 `EntityModel(ModelPart)` 的默认渲染类型就是 `RenderTypes::entityCutout`，
  也就是说**原版实体模型默认是不剔背面的**（以前叫 `entityCutoutNoCull` 的那个）

**⇒ 纸片（`depth = 0`）的正反两面共面，如果沿用默认渲染类型，两面都会画 → 转视角会闪。**
解法是显式指定：

```java
public MilkDragonModel(ModelPart root) {
    super(root, RenderTypes::entityCutoutCull);   // ← EntityModel(ModelPart, Function<Identifier, RenderType>)
}
```

只用「朝着镜头的那一面」还有个附带好处：填充率减半。

**绕序在剔除下确实是对的**（这点验证过，不是推测）：原版
`ThrownTridentRenderer` 就是用同样剔背面的 `RenderTypes.entitySolidGlint(...)` 渲染
`TridentModel` 这套 `ModelPart.Cube` 几何，显示正常。
反过来说，**别拿 `FishingHookRenderer` 当参照**——那个用的是 `EntityRenderer` 手搓四边形，
不是 `ModelPart.Cube`。

#### 模型层的注册（Fabric）

```java
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

ModelLayerRegistry.registerModelLayer(MyModel.LAYER, MyModel::createBodyLayer);

// LAYER = new ModelLayerLocation(Mylovelymilkdragon.id("..."), "main");
// registerModelLayer(ModelLayerLocation, TexturedLayerDefinitionProvider)
//   TexturedLayerDefinitionProvider 是函数式接口，方法 createLayerDefinition() 返回 LayerDefinition
```

⚠️ **包名是 `fabric.api.client.rendering.v1`（`fabric-rendering-v1`）**，
不是名字很像的 `fabric-model-loading-api-v1`——后者是**方块**模型那套，别搞混。

模型骨架的构造链（都核实过）：

```java
MeshDefinition mesh = new MeshDefinition();
PartDefinition root = mesh.getRoot();
root.addOrReplaceChild("body",
        CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(x0, y0, z0, w, h, d, Set<Direction> visibleSides),   // ← 有带 Set 的重载
        PartPose.ZERO);
return LayerDefinition.create(mesh, xTexSize, yTexSize);
```

- `PartPose.ZERO` 是现成常量
- `Model.setupAnim(S)` 的默认实现**就是** `this.resetPose()`，静态模型不用覆写
- 模型空间的坐标约定：**脚底在 `y = 24`**、头顶往负数方向长。
  `LivingEntityRenderer` 渲染前那句 `translate(0, -1.501, 0)` 就是配合它的
  （`24 / 16 = 1.5` 格，减 `1.501` 刚好贴地）

#### 自定义渲染层（`RenderLayer`，26.3 新渲染系统）

想「同一个实体、第二张贴图」就得用它。签名全部核实过：

```java
// net.minecraft.client.renderer.entity.layers.RenderLayer
public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
    public RenderLayer(RenderLayerParent<S, M> renderer) { ... }
    public M getParentModel() { ... }
    public abstract void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            int lightCoords, S state, float yRot, float xRot);
}
```

挂上去用 `LivingEntityRenderer.addLayer(...)`（`protected final`，在渲染器构造函数里调）：

```java
public MyRenderer(EntityRendererProvider.Context context) {
    super(context, new MyModel(context.bakeLayer(MyModel.LAYER_MAIN)), 0.5F);
    this.addLayer(new MyExtraLayer(this, context));   // ← this 还没构造完，但这是原版标准写法
}
```

**关键点：`M` 只约束「父渲染器的模型类型」，层自己画什么由 `submit` 决定。**
所以层可以 `context.bakeLayer(...)` 一个**完全不同**的 `MyModel` 实例（本项目就这么干：
背面纸片用的是 `LAYER_BACK` 烘出来的另一个 `MilkDragonModel`）。

提交几何用的是 26.3 的新渲染系统（**不是**旧的 `VertexConsumer`）：

```java
// OrderedSubmitNodeCollector —— 从 submitNodeCollector.order(n) 拿
<S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
        int lightCoords, int overlayCoords, int tintedColor, @Nullable UvMapping uvMapping, int outlineColor);

// 便捷重载（直接传贴图，内部走 model.renderType(texture)）
default <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, Identifier texture,
        int lightCoords, int overlayCoords, int outlineColor);
```

```java
submitNodeCollector.order(-1).submitModel(this.model, state, poseStack,
        texture, lightCoords,
        LivingEntityRenderer.getOverlayCoords(state, 0.0F),   // public static
        state.outlineColor);
```

- `order(n)` 是绘制顺序：原版大量用 `1`（盖在本体上），
  `SulfurCubeInnerLayer` 用 **`-1`** 表示「画在本体后面」——不透明几何其实无所谓，
  但语义上对齐它更清楚
- **受击红闪不用自己处理**：`LivingEntityRenderer.getOverlayCoords(state, 0.0F)`
  里的 `state.hasRedOverlay` 就是它，原版渲染层统一传 `0.0F`
- **隐形要自己判**：原版 `RenderLayer.coloredCutoutModelCopyLayerRender` 里就是
  `if (!state.isInvisible) { ... }`，照抄即可
- 渲染器里有个现成的静态辅助 `renderColoredCutoutModel(model, texture, ...)`，
  ⚠️ 但它内部写死了 `RenderTypes.entityCutout(texture)`（**不剔背面**的那个），
  纸片模型不能用，得自己调 `submitModel` 换渲染类型

### 5.29 Goal 里追活动目标：路径**不能**每 tick 重算（会原地转圈）

跟 5.17 是一家人（都是 Goal 写法），但这次踩的是 `getNavigation()`。

**症状**：奶龙上台阶时会原地转一圈。用户 2026-10-02 报的。

**根因**：`PathNavigation.moveTo(...)` 内部是 `createPath(...)` —— **完整的 A\* 寻路**，
不是「设个目的地」。每 tick 调一次会同时坏三件事：

```java
// PathNavigation.moveTo(Path, double) 里，每次调用都会：
if (!newPath.sameAs(this.path)) { this.path = newPath; }   // ← 路径换新对象，节点进度从 0 开始
...
this.trimPath();
this.lastStuckCheck = this.tick;                            // ← 卡住检测基准被重置
this.lastStuckCheckPos = mobPos;
```

1. **朝向来回甩**：路径每 tick 重建，当前节点在身子左边还是右边会来回跳；而
   `MoveControl.tick()` 是拿当前节点算朝向的 ——
   `float yRotD = atan2(zd, xd) * 180/PI - 90; this.mob.setYRot(rotlerp(getYRot(), yRotD, 90.0F));`
   （每 tick 最多转 90°，且 `rotlerp` 走最短路径，本身不会转满一圈 —— **甩来自节点跳变**）。
   上台阶正是「我站在哪一格」最模糊的时刻，所以症状在那时候最明显
2. **卡住检测永远不触发**：`doStuckDetection` 要 `tick - lastStuckCheck > 100`，
   每 tick 重置就永远到不了，奶龙卡住了也不会自救
3. **白烧 CPU**：周围 16 格内有玩家/村民时，每 tick 一次 A\*

**正确写法：照抄原版 `MeleeAttackGoal.tick()`**（它做的是同一件事）。它有 4 道节流：

```java
this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
if (... && this.ticksUntilNextPathRecalculation <= 0
        && (this.pathedTargetX == 0.0 && this.pathedTargetY == 0.0 && this.pathedTargetZ == 0.0
            || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0
            || this.mob.getRandom().nextFloat() < 0.05F)) {     // ← 4 道：没算过 / 目标动了 1 格 / 5% 兜底
    this.pathedTargetX = target.getX();                         // ← 记下本次寻路时目标在哪
    ...
    this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
    if (!this.mob.getNavigation().moveTo(target, 0, this.speedModifier)) {
        this.ticksUntilNextPathRecalculation += 15;             // ← 够不着就退避
    }
}
```

- **注意它没写 `if (cooldown > 0) return;` 就走了**，而是「冷却没到就跳过重算，
  但本体照常跑」—— 跳过重算不代表 Goal 停了，`PathNavigation.tick()`
  仍然会带着旧路径走。这两件事别混
- 原版还会 `adjustedTickDelay(...)` 把 4~10 砍成 2~5（`Goal.reducedTickDelay` 是除以 2），
  我们直接留 4~10，节流更狠一点，没有副作用
- 本项目的落地见 `TrackNearestGoal`（`REPATH_INTERVAL_BASE` /
  `REPATH_TARGET_MOVED_SQR` / `REPATH_REROLL_CHANCE` / `REPATH_FAIL_BONUS`）。
  同一个包里的 `BreakTorchGoal` 从一开始就有 `REPATH_INTERVAL = 10`，
  反而是 `TrackNearestGoal` 漏了 —— **新写追目标的 Goal 时记得照抄**

**已核实**：`Entity.distanceToSqr(double, double, double)` 与
`PathNavigation.moveTo(Entity, double)`（返回 `boolean`）/ `stop()` / `isDone()`。

### 5.30 酿造配方：26.3 是**数据包 JSON**，没有 `BrewingRecipeRegistry` 了

**这是本项目里唯一一个「整块功能零 Java 代码」的落地方式**（阶段 6a）。
旧教程里的 `BrewingRecipeRegistry.registerPotionRecipe(...)` 静态注册**在 26.3 里已经不存在**，
Fabric 也没提供对应的封装（`fabric-content-registries-v0` 里只有个 datagen 用的
`FabricBrewingProvider`，跟运行时无关）。取而代之的是一整个 `Recipe` 类型：

```
net.minecraft.world.item.crafting.BrewingRecipe   implements Recipe<BrewingInput>
RecipeType.BREWING                                （常量，不用自己注册）
```

原版 300+ 条酿造配方**全部**是 `data/minecraft/recipe/brewing/*.json`，
照抄一条、改两个 id 就行。

#### JSON 格式（字段名/单复数都别写错）

```jsonc
{
  "type": "minecraft:brewing",
  "input": {                       // ← PotionIngredient：一个 Ingredient + 可选的药水谓词
    "item": "minecraft:potion",    //   物品（也可写 tag）
    "potion_contents": {
      "potions": "minecraft:awkward"    // ⚠️ 复数 potions，类型是 HolderSet<Potion>
    }                                   //    所以单个 id / 列表 / #tag 都能写
  },
  "output": {
    "components": {
      "minecraft:potion_contents": {
        "potion": "mylovelymilkdragon:milk_dragon"   // ⚠️ 这里【单数】potion，是个 ResourceKey
      }
    },
    "id": "minecraft:potion"        // 输出物品：potion / splash_potion / lingering_potion
  },
  "reagent": {                      // 酿造台上格放的材料
    "item": "mylovelymilkdragon:milk_dragon_scale"
  }
}
```

- 单复数已对着 codec 字节码核实：`PotionsPredicate.CODEC` 用 `optionalFieldOf("potions")`
  （值是 `HolderSet<Potion>`），`PotionContents.FULL_CODEC` 用 `optionalFieldOf("potion")`
- 没有 `group` / `category` / `random_sequence` 这些字段，原版表里一个都没写
- 原版还有 `data/minecraft/tags/item/brewing_potion_inputs.json`
  （= potion / splash_potion / lingering_potion / glass_bottle），那是给别处用的，
  **写酿造配方时不需要碰它**

#### ⚠️ 为什么不用自己注册「鳞片能不能当酿造材料」

`BrewingStandBlockEntity` 在**看配方之前**会先查 `RecipePropertySet.BREWING_REAGENTS`：

```java
// canPlaceItem(slot == 3, ...)
recipeAccess.propertySet(RecipePropertySet.BREWING_REAGENTS).test(itemStack)
// isBrewable(...) 里也是同一句
```

不在这个集合里，鳞片**连放进酿造台上格都不行**。但**不用管它**——
`RecipeManager.finalizeRecipeLoading()` 从一个写死的映射表里把每个
`BrewingRecipe` 的 reagent 抽出来自动拼成这个集合：

```java
RecipePropertySet.BREWING_REAGENTS,
recipe -> recipe instanceof BrewingRecipe r ? Optional.of(r.getReagent().ingredient()) : Optional.empty(),
// 同理 BREWING_INPUTS 来自 r.getInput().ingredient()
```

**⇒ 写一条配方 JSON 就自动让鳞片合法了，零额外登记。**

#### ⚠️ 喷溅型 / 滞留型不会自动跟着走

原版是**给每一种药水单独列一条**的（`potion_water_gunpowder`、`potion_awkward_gunpowder`、
`potion_mundane_gunpowder`……一路枚举完），并没有「任意药水 + 火药」这种通配配方。
所以新药水只写一条的话，**做不出喷溅型**。要三形态齐全就得写三条：

| 配方 | input | reagent | output.id |
|---|---|---|---|
| 普通 | `potion` + 粗制 | 奶龙鳞片 | `potion` |
| 喷溅 | `potion` + 奶龙药水 | 火药 | `splash_potion` |
| 滞留 | `splash_potion` + 奶龙药水 | 龙息 | `lingering_potion` |

（滞留的前置必须是**喷溅型**，`input.item` 要跟着改。）

本项目的三条落地在 `data/mylovelymilkdragon/recipe/brewing/`。

#### 药水翻译键（已核实）

`PotionItem.getName(ItemStack)` 的字节码是
`PotionContents.getName(descriptionId + ".effect.")`，
而 `PotionContents.getName(prefix)` 是 `Component.translatable(prefix + potion.name())` ——
`Items.POTION` 的 `descriptionId` 是 `item.minecraft.potion`，
所以键就是 **`item.minecraft.potion.effect.<Potion 构造函数的第一个参数>`**，
**走原版命名空间**，名字不能跟原版药水重名。

### 5.31 状态效果 / 消耗效果 / Mixin 注入（阶段 6b 核实）

#### `Consumable.onConsume` 是**有序列表**，谁先谁后决定成败

```java
// Consumable.Builder：攒进一个 List
private final List<ConsumeEffect> onConsumeEffects = new ArrayList<>();
public Consumable.Builder onConsume(ConsumeEffect effect) { this.onConsumeEffects.add(effect); ... }

// Consumable.onConsume(...) 真正执行时：按加入顺序逐个跑
this.onConsumeEffects.forEach(action -> action.apply(level, stack, user));
```

原版奶桶的真身就一句：

```java
public static final Consumable MILK_BUCKET =
        defaultDrink().onConsume(ClearAllStatusEffectsConsumeEffect.INSTANCE).build();
// defaultDrink() = consumeSeconds(1.6F).animation(DRINK).sound(GENERIC_DRINK).hasConsumeParticles(false)
```

⚠️ **「清空所有效果」必须排在「挂上新效果」前面**，反过来的话刚加上的会被自己清掉。

`ApplyStatusEffectsConsumeEffect` 三个构造函数（不带 probability 的默认 1.0）：

```java
new ApplyStatusEffectsConsumeEffect(MobEffectInstance effect)
new ApplyStatusEffectsConsumeEffect(MobEffectInstance effect, float probability)
new ApplyStatusEffectsConsumeEffect(List<MobEffectInstance> effects)
```

- 它内部会**复制**一遍再 `addEffect(new MobEffectInstance(effect))`，不是直接用你传进去的对象

#### ⚠️ `ItemStack` 上有**两个** `is`，字节码描述符不一样（写 Mixin 必须知道）

| 声明处 | 源码形态 | 擦除后的描述符 |
|---|---|---|
| `ItemStack` 自己 | `is(Predicate<Holder<Item>>)` | `is(Ljava/util/function/Predicate;)Z` |
| `ItemInstance → TypedInstance<Item>` | `is(T rawType)` 泛型默认方法 | **`is(Ljava/lang/Object;)Z`** |

原版 `ZombieVillager.mobInteract` 里的 `itemStack.is(Items.GOLDEN_APPLE)` 走的是**第二个**——
常量池里核实过是 `Methodref ItemStack.is:(Ljava/lang/Object;)Z`。
**`javap ItemStack` 只会列出它自己声明的那个，继承来的看不见**，
所以光看 javap 会以为只有一个 `is`，然后 Mixin 的 target 写错。

`TypedInstance<T>` 一共 5 个重载：`is(TagKey)` / `is(HolderSet)` / `is(T)` / `is(Holder<T>)` / `is(ResourceKey<T>)`。

#### MixinExtras 可以直接用，不用加依赖

- 编译类路径上就有 `io.github.llamalad7:mixinextras-fabric:0.5.5`
  （`./gradlew dependencies --configuration compileClasspath` 能看到）
- 运行期由 Fabric Loader 自带，日志里会打
  `Initializing MixinExtras via ...MixinExtrasServiceImpl(version=0.5.5)`
- 注解包名 `com.llamalad7.mixinextras.injector.ModifyExpressionValue`

**改「某个表达式的返回值」优先用它而不是 `@Redirect`**：它只改这一句，
可以只把 false 改成 true、不会把别人的 true 改回 false，跟别的模组改同一句时不会互相顶掉
（`@Redirect` 是独占的，同一个注入点只允许一个）。

```java
@ModifyExpressionValue(method = "mobInteract",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
private boolean handler(boolean original, Player player, InteractionHand hand) { ... }
```

#### 怎么确认「Mixin 真的注入了」——不是「没报错」

启动时加 `-Dmixin.debug.export=true`，Mixin 会把**改写后**的 class 导到
`run/.mixin.out/class/...`，直接 javap 看：

```bash
JAVA_TOOL_OPTIONS="-Dmixin.debug.export=true" ./gradlew runClient
JAVAP="/c/Program Files/Java/jdk-25/bin/javap"
"$JAVAP" -c -p run/.mixin.out/class/net/minecraft/world/entity/monster/zombie/ZombieVillager.class
```

看到自己的 handler（本项目是
`modifyExpressionValue$zgc000$mylovelymilkdragon$mdragon$milkDragonMilkCures`）
被插在原位置才算数。

⚠️ **`latest.log` 里没报错 ≠ 注入成功**：目标类可能是惰性加载的，
那次启动压根没碰过它。2026-10-02 实测 `ZombieVillager` 在启动阶段就会被加载
（导出了改写后的 class），但这不是所有类都成立。

#### `shouldApplyEffectTickThisTick` 的参数是**剩余时长**；`applyEffectTick` 返回 false 会摘掉效果

```java
// MobEffectInstance.tickServer
int tickCount = this.isInfiniteDuration() ? target.tickCount : this.duration;   // ← 剩余时长
if (this.effect.value().shouldApplyEffectTickThisTick(tickCount, this.amplifier)
        && !this.effect.value().applyEffectTick(serverLevel, target, this.amplifier)) {
    return false;    // ← applyEffectTick 返回 false = 效果被移除
}
```

- 「每 5 秒一次」写成 `return tickCount % 100 == 0;`（从末尾往前数，效果仍是等间隔）
- `applyEffectTick` **必须 return true**，否则等于自己把效果删了
- `MobEffect` 的默认 `shouldApplyEffectTickThisTick` 返回 `false` ——
  **不覆写就永远不 tick**，这正是 5.4 说的「空壳效果什么都不会发生」

#### `Level.playSound(Entity except, ...)` 的第一个参数是「**除了**谁」

想在服务端**只放给某一个人**听，不能用它——把目标传进去恰好相反（全场只有他听不见）。
原版是直接发封包（`Raid` / `PlaySoundCommand` 都这么写）：

```java
player.connection.send(new ClientboundSoundPacket(
        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent),   // SoundEvent → Holder<SoundEvent>
        SoundSource.AMBIENT, x, y, z, volume, pitch, seed));
// 构造：(Holder<SoundEvent>, SoundSource, double x, double y, double z, float, float, long)
```

⚠️ `SoundEvents` 里各常量的**声明类型并不一致**：`AMBIENT_CAVE` 是
`Holder.Reference<SoundEvent>`，`CREEPER_PRIMED` 是 `SoundEvent`，
`TRIDENT_THUNDER` 是 `Holder<SoundEvent>`。混着用就先统一成 `SoundEvent`
（Holder 的取 `.value()`），发封包时再 `wrapAsHolder` 转回去。

#### `MobEffectCategory` 只影响 HUD 排序和提示文字颜色

全代码库只有两处读它：`Hud`（`isBeneficial()` 决定图标谁排前面）和
`PotionContents`（`getTooltipFormatting()` 决定那行字的颜色）。**没有任何功能后果。**

#### 让「生气」的生物真正消气，光改状态位是不够的

`HurtByTargetGoal` 盯的是 `getTarget()`，跟自定义的愤怒标志位**没有任何关系**。
只把自己的 `isAngry` 置回 false，会出现「贴图换回来了、移速也降了，但还在追着你打」
的怪状态。要么认了，要么真消气时一并清：

```java
this.setTarget(null);
this.setLastHurtByMob(null);      // public
this.getNavigation().stop();
```

#### 喂东西给生物：用 `Mob.usePlayerItem`，别用 `stack.consume(1, player)`

```java
protected void usePlayerItem(Player player, InteractionHand hand, ItemStack itemStack)
// 内部会读 USE_REMAINDER 组件（= Item.Properties.usingConvertsTo(...) 登记的东西），
// 把「喝完留下的空桶」这类剩余物塞回玩家手里
```

直接 `consume` 就只是少一个，空桶没了。

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
│   ├── models/item/<物品>.json   # 物品的模型
│   ├── blockstates/<方块>.json    # 方块的模型定义（变体表）
│   ├── models/block/<方块>.json   # 方块的模型
│   ├── textures/item/<物品>.png  # 物品贴图
│   ├── textures/block/<方块>.png  # 方块贴图
│   ├── sounds.json              # 音效（后期补，见第 9 节）
│   └── textures/entity/<实体>/   # 实体贴图
└── data/mylovelymilkdragon/
    ├── loot_table/entities/<实体>.json   # 注意是单数 loot_table
    ├── loot_table/blocks/<方块>.json     # 方块自己被打掉时的掉落
    ├── recipe/<配方>.json               # 单数 recipe
    ├── recipe/brewing/<酿造配方>.json    # 酿造也是 recipe，子目录只是原版的分类习惯
    ├── advancement/<进度>.json          # 单数 advancement
    └── tags/...
```

**方块要四个文件**（少任何一个方块都会变紫黑格或掉不出来）：
`blockstates/<方块>.json` + `models/block/<方块>.json` + `items/<方块>.json`
+ `data/<ns>/loot_table/blocks/<方块>.json`。
其中 `items/<方块>.json` 里 `"model"` 指向的是 **`block/` 下的那个模型**，不是 `item/`：

```json
{ "model": { "type": "minecraft:model", "model": "mylovelymilkdragon:block/milk_dragon_egg" } }
```

方块战利品表比实体的简单，原版龙蛋的可以照抄：

```json
{ "type": "minecraft:block",
  "pools": [ { "rolls": 1, "entries": [ { "type": "minecraft:item", "name": "<命名空间>:<方块>" } ] } ],
  "random_sequence": "<命名空间>:blocks/<方块>" }
```

**每个物品也要一对文件**（原版 `diamond` 的真实内容，26.3 已核实）：

```json
// assets/mylovelymilkdragon/items/milk_dragon_scale.json
{ "model": { "type": "minecraft:model", "model": "mylovelymilkdragon:item/milk_dragon_scale" } }

// assets/mylovelymilkdragon/models/item/milk_dragon_scale.json
{ "parent": "minecraft:item/generated",
  "textures": { "layer0": "mylovelymilkdragon:item/milk_dragon_scale" } }
```

⚠️ **少了这对 JSON，光把 PNG 丢进去是没有用的** —— 物品找不到模型，游戏里是紫黑格，
日志会报 `Missing model`。本项目 2026-10-01 发现四个物品全都缺这对文件
（当时只有奶蛋有），补齐后日志变成 `Missing textures in model ...`，
那才是「只差贴图」的正确状态。

⚠️ **刷怪蛋在 26.3 也是普通 `item/generated` + 单层 `layer0`**，不再是旧版的
`item/template_spawn_egg` 双层染色模板 —— 原版 `pig_spawn_egg.png` 只有 193 字节，
颜色直接画在贴图里。所以奶龙刷怪蛋不用写任何 tint。

### 翻译键格式（已验证，从原版 en_us.json 与字节码确认）

| 类型 | 键格式 | 例子 |
|---|---|---|
| 物品 | `item.<命名空间>.<路径>` | `item.mylovelymilkdragon.milk_dragon_scale` |
| 方块 | `block.<命名空间>.<路径>` | `block.mylovelymilkdragon.milk_dragon_egg`（靠 `useBlockDescriptionPrefix()` 自动切换前缀） |
| 物品栏 | `itemGroup.<命名空间>.<路径>` | `itemGroup.mylovelymilkdragon.milk_dragon`（社区惯例，非原版强制） |
| 实体 | `entity.<命名空间>.<路径>` | `entity.mylovelymilkdragon.milk_dragon` |
| 状态效果 | `effect.<命名空间>.<路径>` | `effect.mylovelymilkdragon.milk_dragon_qi` |
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
新写的代码按 4 空格写。模板遗留的 `ExampleMixin` 已于 2026-10-02 删除，
现在 `mixin` 包下只有 `ZombieVillagerMixin`，不再有 tab 报警告。

## 8. 常用命令

```bash
./gradlew compileJava    # 只编译 Java（改完代码的快速自检）
./gradlew build          # 完整构建，产物在 build/libs/
./gradlew runClient      # 启动客户端（游戏数据在 run/）
./gradlew genSources     # 生成反编译源码（已跑过，源码在 build/mcsrc）
```

### ⚠️ 关掉 `runClient` 时，杀 gradle wrapper 不会连带杀掉游戏进程

客户端是 wrapper **另起**的 `java.exe`，把 wrapper 停掉之后它会继续活着 ——
窗口留在用户屏幕上，而且会占着 `run/` 不放，下一次 `runClient` 可能起不来。

关干净的办法（Git Bash）：

```bash
# 找出跑着 Fabric 的那个 java 进程
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" \
  | Where-Object { \$_.CommandLine -like '*net.fabricmc*' } | Select-Object -ExpandProperty ProcessId"
powershell -NoProfile -Command "Stop-Process -Id <PID> -Force"
```

**关完一定要复查一次**（同一条查询命令，返回空才算干净），2026-10-02 踩过。

**收尾规矩：每轮对话改完代码，必须跑一次 `./gradlew compileJava` 并确认
`BUILD SUCCESSFUL`，再向用户汇报。** 不要只说「改好了」而不编译。

### ⚠️ `--quickPlaySingleplayer` 在本环境**不生效**（2026-10-01 实测）

```
./gradlew runClient --args='--quickPlaySingleplayer "New World (1)"'      # 无效
./gradlew runClient --args='--quickPlaySingleplayer="New World (1)"'     # 也无效
```

两种写法都确认参数**真的进了 java 命令行**（`Get-CimInstance Win32_Process` 查过），
但客户端仍然停在标题界面，日志里连一条 quickPlay 记录都没有。
所以**不要指望它自动进世界**，测试时让用户手点「单人游戏 → 存档」。

排查到什么程度（下次接着查的起点）：

- `net.minecraft.client.main.Main` 里 `quickPlaySingleplayer` 声明是
  **`withOptionalArg()`**，而 `quickPlayPath` / `quickPlayMultiplayer` / `quickPlayRealms`
  都是 `withRequiredArg()` —— 只有它一个特殊
- 执行链：`Gui.buildInitialScreens(GameLoadCookie)` 的 lambda 里判断
  `cookie.quickPlayData().isEnabled()`，成立才调 `QuickPlay.connect(...)`，
  否则 `setScreen(new TitleScreen(...))`。我们每次都走了 else 分支
- `QuickPlayData.isEnabled()` 只是转发给 `variant.isEnabled()`，
  而 `variant` 由 `Main.getQuickPlayVariant` 决定；它对三个 spec 数 `optionSet.has()` 的个数，
  为 0 就返回 `QuickPlayVariant.DISABLED`
- ✅ **`net.fabricmc.devlaunchinjector.Main` 已排除**（2026-10-02 读字节码核实）：
  它把 `fabric.dli.config` 里的启动参数和原始 `args` 用 `System.arraycopy`
  拼成一个新数组再 `astore_0` 传下去，**参数确实转发给了 MC 的 `Main`**。
  所以要么是 `KnotClient` 环节，要么是参数本身的问题，**尚未查完**

同一环境里**「能编译 ≠ 能跑」的验证靠的是**：启动客户端 → 看 `run/logs/latest.log` 里
`[奶龙] 模组加载完成` + 无异常 + 让用户手点进世界看效果。

### ⚠️ 启动期间**把窗口最小化会让客户端卡死**（2026-10-02 实测）

**现象**：看着像「世界进不去」，实际是客户端整个冻住了。

`run/logs/latest.log` 里的铁证（那天真发生过一次 21 分钟的卡顿）：

```
[17:35:50] Reloading ResourceManager              ← 启动的资源/数据包加载开始
[17:35:58] SurfaceException: Cannot acquire minimized window   ← 窗口被最小化
      （中间 21 分钟一行输出都没有）
[17:57:13] Loaded 1866 advancements               ← 恢复窗口后这才跑完
[17:57:17] Stopping!                              ← 用户等不及关了
```

- `SurfaceException: Cannot acquire minimized window` 这个 WARN 就是信号，
  出在 `com.mojang.renderpearl.backend.opengl.GlSurface.acquireNextTexture`
- 启动阶段的资源加载压在 **Render thread** 上，窗口最小化时这条线程拿不到
  GL surface → 整个启动流程停摆，**看起来就是卡在加载界面 / 世界进不去**
- **没有异常、没有崩溃报告、`level.dat` 也完好**，光看日志很容易误判成数据包写错

**规矩**：让用户测试时**明确提醒不要最小化窗口**。
真出现「卡住」时，先按这个清单排查，别急着怀疑自己的代码：

| 检查 | 命令/位置 |
|---|---|
| 有没有那 21 分钟的输出空档 | `debug.log` 里找时间戳断层 |
| 是不是真的进过世界 | `run/logs/latest.log` 找 `Starting integrated minecraft server`（**这行才是铁证**） |
| 存档有没有被写 | `run/saves/<世界>/session.lock` 的 mtime |
| 有没有崩溃报告 | `run/crash-reports/` |

> 顺带纠正一个曾经的误解：`Loaded 1866 advancements` **不是**「进世界成功」的标志，
> 它在**客户端启动**时也会打一次（对比 `-5`/`-6` 两份日志，结尾都是它）。

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
- `ModDataComponents`：`conversion_deadline` / `next_laugh_tick` 两个持久化组件
- `MilkDragonHeadItem`：可佩戴、绑定诅咒、每 5 秒笑、戴上满 30 秒把村民同化成奶龙
- 战利品表：`data/mylovelymilkdragon/loot_table/entities/milk_dragon.json`
- `milk_dragon_milk`（奶龙的奶）已注册，带原版奶桶的「清效果 + 还空桶」行为
- `ModBlocks`：`milk_dragon_egg`（奶蛋）方块，直接复用原版 `DragonEggBlock`，
  行为与原版龙蛋一致，方块物品一并注册
- `ModCreativeTabs`：创造模式物品栏「奶龙」，装全部 6 件自定义物品 + 奶龙药水
- **奶龙刷怪蛋已进原版「刷怪蛋」页签**（2026-10-02 用户反馈后补的，见 5.26 末尾）：
  原版那个页签是硬编码的，Fabric 也不自动加，得用
  `CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)` 自己插
- 实机验证（2026-10-01 23:39）：`ModBlocks` / `ModCreativeTabs` 上线后启动无异常，
  世界正常加载，1866 个进度加载成功（战利品表解析通过）
- **物品模型文件已补齐**（2026-10-01，见第 6 节）：`milk_dragon_head` /
  `milk_dragon_scale` / `milk_dragon_spawn_egg` / `milk_dragon_milk` 各有
  `items/<名>.json` + `models/item/<名>.json` 一对；奶蛋另有 4 个文件。
  实机日志已从 `Missing model` 变成 `Missing textures in model ...`（= 只差 PNG）
- **旧的 `milk`（牛奶）已删除**（2026-10-01，用户要求）：这是阶段 0 的模板残留，
  不在第 11 节的任何一张表里。`ModItems` 的 `MILK` 字段、创造栏里的那一项、
  以及 `lang` 里的 `item.mylovelymilkdragon.milk` 都已移除
- **自然生成已上线**（2026-10-02，阶段 5）：`ModSpawns` 把奶龙塞进刷怪表 ——
  村庄生物群系权重 **15**、野外权重 5，两者互补且都走 `Mob::checkMobSpawnRules`
  （**不看光照**）。实机验证：进世界时 Fabric 打出
  `Applied 56 biome modifications to 56 of 67 new biomes in 5.479 ms`，
  全程零异常、`Cannot add spawns` 断言触发 0 次。写法细节见 5.27
- **村庄权重从 100 降到 15**（2026-10-02，用户报告「村庄里几乎没人了」之后）：
  100 时奶龙占村庄怪物生成的 16%，叠加「无视光照」（白天独占地表刷怪）
  就把村庄屠空了。因果链与数值表见 11.5
- **`TrackNearestGoal` 加了重算路径节流**（2026-10-02，修「上台阶原地转圈」）：
  之前每 tick 一次 A* 寻路，见 5.29
- **纸片模型已上线**（2026-10-02，阶段 3b）：`MilkDragonModel` —— 两块厚度为 0 的
  平面（正反面**各一块独立的纸片**，每块只留一个面，不再用 `PigModel`），
  两个模型层用 Fabric 的 `ModelLayerRegistry` 注册在 `MylovelymilkdragonClient`。
  **正面**由 `MilkDragonRenderer` 本体画，**背面**由新增的渲染层
  `MilkDragonBackLayer` 画 —— 拆开是因为一次绘制只能绑一张贴图。
  贴图因此是**一状态一对、共 6 张独立的 PNG**（见下方待办；渲染层 API 见 5.28，
  里面对纸片模型的 UV 落位、`texOffs` 取负、「26.3 的 `entityCutout` 其实不剔背面」
  这几个坑都有说明）。渲染器里 `MODEL_SCALE = 0.5F` 是唯一决定世界大小的数
- **奶龙药水的酿造配方已上线**（2026-10-02，阶段 6a）：三条纯数据包 JSON 在
  `data/mylovelymilkdragon/recipe/brewing/`，**零 Java 改动**（26.3 的酿造已数据驱动化，
  见 5.30）。实机验证：用户建创造世界、摆酿造台，
  **成功酿出奶龙药水**；日志全程无配方解析报错
  （19:23:49 集成服务端启动 → 19:33:24 正常退出，`Failed to parse` 出现 0 次）
- **奶龙的奶四条饮用效果全部落地**（2026-10-02，阶段 6b，见 5.31）：
  - 新增 `ModEffects`（注册类）+ `effect/HallucinationEffect`（幻听）
  - `+5 护甲` 走 `MobEffect.addAttributeModifier(...ADD_VALUE)`，
    `幻听` 每 5 秒发 `ClientboundSoundPacket` **只给喝的人**听
  - 饮用效果从 `Consumables.MILK_BUCKET` 换成自定义的 `milkConsumable()`：
    **先清空、后挂效果**（顺序反了会被自己清掉，见 5.31）
  - `MilkDragonEntity.mobInteract`：喂奶龙的奶消怒
  - `ZombieVillagerMixin`：用 MixinExtras 的 `@ModifyExpressionValue`
    把金苹果判定放宽成「金苹果 **或** 奶龙的奶」，原版治愈流程一行没改
  - 启动验证：`[奶龙] 模组加载完成` 无异常，且用
    `-Dmixin.debug.export=true` 导出改写后的 `ZombieVillager.class`，
    javap 确认 handler 已插在 `ItemStack.is(Object)` 那一条上（判断注入是否
    成功的方法见 5.31）
- **阶段 6b 已由用户实机逐个测过**（2026-10-02）：四条饮用效果、喂奶消怒、
  治愈僵尸村民全部确认可用，无问题
- **药水内部效果定名「奶龙之气」**（2026-10-02，用户拍板）：注册 id 从阶段 0/1 的
  旧占位 `dragon_breath` 改为 **`milk_dragon_qi`**（旧 id 还跟原版物品
  `minecraft:dragon_breath` 撞名），Java 字段 → `MILK_DRAGON_QI_EFFECT`，
  两个 lang 文件同步。⚠️ 它目前仍是**空壳效果** —— 只显示一个图标，
  喝了没有任何实际作用，行为还没定，见 11.6
- **模板遗留物已清掉 + README 重写**（2026-10-02）：删除了 `ExampleMixin`
  （注入 `MinecraftServer.loadLevel`，从未启用过）以及 `mylovelymilkdragon.mixins.json`
  里对应的那一项；`README.md` 从 Fabric 模板的「Setup / License」两段换成了模组本身的
  说明（需求、版本要求、内容一览、构建、开发状态）。见第 7 节

### 还没做的（重要）

- [ ] **贴图一张都没有**（实体 6 张 + 物品 4 张 + 奶蛋 1 张）。日志里对应的
      `Missing textures in model <路径>` 就是它们（**占位状态，不是错误**，见第 9 节）。
      模型 JSON 全部就位，用户把 PNG 丢进 `textures/item/` 和
      `textures/block/`、`textures/entity/milk_dragon/` 即可，不用改任何代码。
      各物品需要的 PNG 路径见第 6 节那对 JSON 里的 `layer0`
- [ ] **实体贴图共 6 张，每张 32 x 32**（阶段 3b 定稿，2026-10-02 用户拍板）：

      | 状态 | 正面 | 背面 |
      |---|---|---|
      | 平静 | `milk_dragon_front.png` | `milk_dragon_back.png` |
      | 愤怒 | `milk_dragon_angry_front.png` | `milk_dragon_angry_back.png` |
      | 睡觉 | `milk_dragon_sleeping_front.png` | `milk_dragon_sleeping_back.png` |

      全部放在 `textures/entity/milk_dragon/` 下。要点：
      - **每张都是一整幅画，不再有「左半右半」**（旧版 64x32 方案已作废）
      - 一张只画**一个**方向：`_front` 画「正对着奶龙看到的样子」，
        `_back` 画「站在奶龙背后看到的样子」
      - **两张都不用镜像**，照平常画法画即可（绕序已核对）
      - 上方是头（v=0）、下方是脚（v=32）
      - alpha 阈值：alpha &lt; 0.1 的像素直接丢弃，**没有半透明**；
        想镂空就涂成完全透明
      - **大笑没有独立贴图**，跟平静共用（只持续 2 秒，不值得多一对图）
      - **睡觉目前只是换图，姿势不变** —— 纸片模型没有可动的骨架，
        想做出「躺下」的感觉只能直接画进 `_sleeping` 那张图里
      - 缺哪张只影响哪一张，游戏会显示紫黑格，不会崩
- [ ] 音效仍是原版占位（用户决定往后放）
- [ ] **无合成配方**（`recipe/` 下目前**只有酿造的 3 条**）、无 `sounds.json`
- [ ] **奶龙之气目前是空壳效果**（只显示图标，没有任何实际作用）。11.6 只定了名字、
      没写它该做什么，所以没有自行发挥 —— 行为等用户明确后再加
- [ ] 幻听的音效池（8 个原版音效）、间隔（5 秒）、两个效果的时长（护甲 3 分钟 / 幻听 1 分钟）
      都是**占位值**，原设计没写数，都在 `HallucinationEffect` 和 `ModItems.milkConsumable()`
      顶部改。幻听用的是原版音效占位，等自定义音效到位后按第 9 节的流程换

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
| `isSleeping` | **随机发生，白天晚上都会** | 静止不动（`setNoAi(true)`）；睡够了自己醒，被打或被玩家靠近则醒（后两种醒 → 愤怒） |
| `isLaughing` | 随机 | 持续约 2 秒 |

⚠️ **睡觉必须有「自然醒」**。因为睡觉走的是 `setNoAi(true)`，AI 被整个关掉——
如果只有「玩家靠近才醒」这一个条件，奶龙在没人的地方睡着就**再也醒不过来**，
不拆火把、不走路、什么都不做。本项目踩过这个坑（2026-10-01），
所以 `SLEEP_DURATION_*` 那两个常量不能删。

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
| 一次睡觉时长 | 随机 10 ~ 30 秒（到点自然醒） |
| 拆火把冷却 | 5 秒（用绝对 tick 算，见 5.17） |
| 够不着时的放弃时限 | 10 秒，之后把那根火把拉黑 30 秒 |
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
| `milk_dragon_head` | 奶龙头 | **只做可佩戴物品，不做方块**（见 5.8）。`equippableUnswappable(HEAD)`；戴上有绑定诅咒；每 5 秒大笑；右键村民/灾厄村民/女巫 → 戴上 → 30 秒后**同化成奶龙**（实现见下方备注） |
| `milk_dragon_scale` | 奶龙鳞片 | 酿造原料 |
| `milk_dragon_spawn_egg` | 奶龙刷怪蛋 | |
| `milk_dragon_milk` | **奶龙的奶** | 详见 11.3 |
| `milk_dragon_egg` | 奶蛋 | **是方块，不是普通物品**，见 11.4。注册在 `ModBlocks` 里 |
| （原版 `minecraft:sulfur`） | 硫磺 | **直接用原版物品**，不自己注册 |

> ~~`milk`（牛奶）~~ **已于 2026-10-01 删除**。它是阶段 0 搭骨架时的遗留占位：
> 一个空壳 `Item`，非食物、不能喝、没有任何行为，全代码库无一处引用，
> 需求文档里也只有「已有占位」四个字。**别把它和 `milk_dragon_milk`（奶龙的奶）搞混**——
> 后者是真正有设计的物品（见 11.3）。

**奶龙头的实现备注**（`io.github.ninetwo.mdragon.item.MilkDragonHeadItem`）：

- **不需要 Mixin，也不需要碰村民的 `mobInteract`。** 「右键给生物戴帽子」是原版行为，
  由 `Equippable.setEquipOnInteract(true)` 打开，而且它在打开村民交易界面**之前**执行，
  详见 5.21。这跟 5.7 的「治愈僵尸村民必须 Mixin」是两回事，别混
- 「戴上之后」的两件事（30 秒同化、每 5 秒笑）全挂在 `Item.inventoryTick` 上，
  因为任何生物戴着它都会被每 tick 调到，详见 5.20
- 能戴 = 会被同化的名单写在 `MilkDragonHeadItem.ALLOWED_WEARERS`，
  **必须包含 `EntityTypes.PLAYER`**（否则玩家自己戴不上），详见 5.21。
  另一半 `isConvertible()` 是 `Villager` / `AbstractIllager` / `Witch`，
  **改一边就要同步改另一边**
- **流浪商人（`WanderingTrader`）不在名单里** —— 11.2 写的是「村民」，按字面取 `Villager`
- **头是同化的消耗品**，变身时会被删掉（`MilkDragonHeadItem` 里摘掉 HEAD 槽那行）。
  想改成「变完把头吐出来」的话，把那行换成往 `level` 里 `addFreshEntity` 一个掉落物
- 同化用 `Mob.convertTo`（`ConversionParams.single(mob, false, false)` +
  `EntitySpawnReason.CONVERSION`），见 5.11

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

**已落地**（`data/mylovelymilkdragon/loot_table/entities/milk_dragon.json`）：

| 掉落 | 数量 | 抢夺加成 | 备注 |
|---|---|---|---|
| 奶龙的奶 | 1 | +0~1 | |
| 硫磺 `minecraft:sulfur` | 1~2 | +0~1 | 用原版物品，不自己注册 |
| 奶龙鳞片 | 1 | +0~1 | **用户 2026-10-01 补的**：原稿漏了它，而 11.6 的酿造配方要用 |
| **奶蛋** | 1 | 无 | **每次击杀都掉**（用户 2026-10-01 拍板）。是**方块**，见下方 |
| 附魔金苹果 | 1 | 15% → 每级 +1% | 单独的池 |
| 奶龙头 | 1 | 无 | **只在爆炸死亡时掉**，条件是伤害来源带 `minecraft:is_explosion` 标签 |

**上述数量/概率是 AI 按常理填的占位值**（原设计只写了「固定掉落」「15%」，没写几个），
想调直接改那个 JSON。表里的条件/修饰符写法见 5.23。

#### 奶蛋（`milk_dragon_egg`）

**用户 2026-10-01 定稿：定位是「像原版龙蛋那样的战利品」，但每次击杀都掉。**

这两个特征原本是矛盾的（原版龙蛋全存档只掉一个），用户明确选了「每次击杀都掉」，
所以**不需要**做存档级的状态标记。后续模组 2.0 可能把它加进合成配方。

- 是**方块**，行为照抄原版龙蛋：能放地上、受重力会掉、被打会瞬移。
  实现方式是直接复用原版 `DragonEggBlock`（构造函数是 public），一行逻辑都没抄
- 注册在 `ModBlocks`（**不是 `ModItems`**），方块物品由 `ModBlocks.registerWithItem` 一并注册
- 方块属性照抄原版龙蛋，只有 `mapColor` 换成 `MapColor.SNOW`（白色，配奶龙主题；
  原版龙蛋是黑的）。**这一处是 AI 自作主张的，用户没指定**
- 方块自己被打掉时的掉落表是 `data/mylovelymilkdragon/loot_table/blocks/milk_dragon_egg.json`

### 11.5 自然生成

- **村庄**：高概率，**无视光照**
- **野外**：低概率（权重视情况调高），**无视光照**
- ⚠️ 「检测附近村庄」这套逻辑**尚未验证可行性**，最坏退化成「野外也生成」

**权重的实际取值（2026-10-02 据实机反馈定稿）：**

| | 权重 | 说明 |
|---|---|---|
| 村庄 | **15** | 原版 plains 的 MONSTER 总权重是 515（蜘蛛/骷髅/苦力怕/史莱姆各 100、僵尸 90…），15 ≈「12 只原版怪里混 1 只奶龙」 |
| 野外 | **5** | 约 1% |

> ⚠️ **村庄权重曾试过 100，被实测否掉了。** 100 = 16% 的怪物都是奶龙，
> 再叠上「无视光照」（白天原版地表怪一只都刷不出来，奶龙照刷），
> 等于**独占了白天的刷怪机会**；而它唯一的目标就是村民，150 血 / 护甲 15 /
> 击退抗性 1.0 又让铁傀儡很难翻盘 —— 结果就是**村庄被屠到几乎没人**。
> 想再调高请先想清楚这一串因果。
>
> 数值都是按常理填的占位值（原设计只写了「高概率」「低概率」），
> 改直接改 `ModSpawns` 顶部那两个常量。

### 11.6 药水

**奶龙药水**，一种新的特殊药水（不是「龙息药水」）。

| 项 | 值 |
|---|---|
| 药水注册 id | `mylovelymilkdragon:milk_dragon` |
| 药水翻译键 | `item.minecraft.potion.effect.milk_dragon` |
| 显示名 | 奶龙药水 / Potion of Milk Dragon |
| 酿造配方 | 粗制的药水 + 奶龙鳞片 → 奶龙药水（阶段 6a 实现） |
| 内部效果 | **奶龙之气**（注册 id `mylovelymilkdragon:milk_dragon_qi`），2026-10-02 定稿 |

药水和它内部效果的名字**都**已定稿（奶龙药水 / 奶龙之气）。

⚠️ **但「奶龙之气」该做什么，原设计里没写。** 当前实现是个**空壳效果**：
`ModPotions` 里只有 `new MobEffect(...) {}`，喝下去只会多一个图标，
没有任何实际作用（这正是 5.4 警告的那种情况）。**没有自行发挥去补行为** ——
想让它有实际效果（加属性、定时做点什么……），先说清楚要什么。

**配方已落地**（阶段 6a，2026-10-02）—— 三条纯数据包 JSON，在
`data/mylovelymilkdragon/recipe/brewing/`，**一行 Java 都没加**（原因见 5.30）：

| 形态 | 基础 | 材料 | 文件 |
|---|---|---|---|
| 普通（可饮用） | 粗制的药水 | 奶龙鳞片 | `potion_awkward_milk_dragon_scale.json` |
| 喷溅型 | 奶龙药水 | 火药 | `potion_milk_dragon_gunpowder.json` |
| 滞留型 | **喷溅型**奶龙药水 | 龙息 | `splash_potion_milk_dragon_dragon_breath.json` |

> 后两条是 **2026-10-02 用户拍板加的**：11.6 原文只写了第一条，而原版是**每种药水
> 单独列一条**、没有通配配方，不写就没有喷溅/滞留形态。用户选了「跟原版药水行为对齐」。

### 11.7 待定 / 已放弃 / 预留

> **待定**项：用户还没想好，先不定。**不要为了填坑而自由发挥**——
> 按现有旧名跑着即可，等用户明确后再改。

| 项 | 状态 |
|---|---|
| ~~药水内部**效果**的 id 与显示名~~ | ✅ **已定稿**（2026-10-02）：**奶龙之气**，id `milk_dragon_qi`。见 11.6 |
| **奶龙之气**具体做什么 | 🟡 **待定**，目前是空壳效果，见 11.6 |
| ~~**奶蛋**是什么~~ | ✅ **已定稿**（2026-10-01）：像原版龙蛋那样的方块战利品，每次击杀都掉。见 11.4 |
| 召唤事件（指令 / 奶蛋触发，可飞行的奶龙） | 🟡 **待定**，暂不做。奶蛋现已落地，将来做事件时可以直接拿它当触发物 |
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
| **3b** | 自定义奶龙模型 + 平时/愤怒/睡觉 三套贴图 | ✅ 已完成（2026-10-02，纸片模型，正背各一张共 6 图，见 5.28） |
| **4** | 掉落 + 奶龙头（可佩戴物品）：战利品表、头物品、`convertTo` 同化村民 | ✅ 已完成 |
| **5** | 自然生成：村庄高概率 + 野外低概率，均无视光照 | ✅ 已完成（2026-10-02，见 5.27） |
| **6a** | 奶龙药水 + 酿造配方 | ✅ 已完成（2026-10-02，纯数据包 JSON，零 Java 改动，见 5.30） |
| **6b** | 奶龙的奶的饮用效果：+5 护甲、幻听、喂奶龙消怒、Mixin 治愈僵尸村民 | ✅ 已完成（2026-10-02，见 5.31） |
| **7** | 预留：召唤事件 + 奶龙祭坛 | ⬜ |

每阶段收尾都要跑 `./gradlew compileJava` 确认 `BUILD SUCCESSFUL`。
