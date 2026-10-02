package io.github.ninetwo.mdragon;

import java.util.List;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 奶龙的自然生成（第 11.5 节）。
 *
 * <p><b>村庄高概率、野外低概率，两者都无视光照。</b>
 *
 * <p>做法分两步，缺一不可：
 * <ol>
 *   <li>{@link SpawnPlacements#register} —— 决定「站在哪儿才允许生成」。
 *       这里用 {@link Mob#checkMobSpawnRules}，它<b>完全不查光照</b>，见下方说明</li>
 *   <li>{@link BiomeModifications#addSpawn} —— 把奶龙塞进各个生物群系的刷怪表，
 *       并决定权重（=刷出来的相对频率）与一次刷几只</li>
 * </ol>
 *
 * <p>⚠️ <b>「无视光照」是怎么来的（已读字节码，别再自己写 predicate）：</b>
 * <pre>
 * // net.minecraft.world.entity.Mob —— 只有两行，没有任何光照判断
 * public static boolean checkMobSpawnRules(EntityType&lt;? extends Mob&gt; type,
 *         LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
 *     BlockPos below = pos.below();
 *     return EntitySpawnReason.isSpawner(reason)
 *             || level.getBlockState(below).isValidSpawn(level, below, type);
 * }
 *
 * // net.minecraft.world.entity.monster.Monster —— 光照检查在【这一层】
 * public static boolean checkMonsterSpawnRules(...) {
 *     return (EntitySpawnReason.ignoresLightRequirements(reason)
 *                 || isDarkEnoughToSpawn(level, pos, random))     // ← 就是它
 *             &amp;&amp; checkMobSpawnRules(...);
 * }
 * </pre>
 * 所以<b>不能用</b> {@code Monster.checkMonsterSpawnRules}（会要求黑暗），
 * 也<b>不能用</b> {@code Monster.checkAnyLightMonsterSpawnRules}——它虽然不查光照，
 * 但参数是 {@code EntityType<? extends Monster>}，而 {@code MilkDragonEntity}
 * 继承的是 {@link net.minecraft.world.entity.PathfinderMob}，类型对不上。
 * 直接指向它内部转发的那个 {@code Mob.checkMobSpawnRules} 即可，它是 public。
 *
 * <p>⚠️ <b>「村庄检测」的粒度是生物群系级，不是「附近真的有个村庄」。</b>
 * 用的是原版那五个 {@code #minecraft:has_structure/village_*} 生物群系标签
 * （即「这个生物群系里<b>能</b>生成村庄」）。这是第 11.5 节里那个
 * 「尚未验证可行性」的顾虑的落地方式：真正的「附近有村庄结构」需要按区块查
 * {@code StructureStart}，代价大得多，而且原版刷怪本来也是按生物群系走的。
 *
 * <p>⚠️ 下面几个权重/数量是<b>按常理填的占位值</b>（原设计只写了「高概率」「低概率」），
 * 想调直接改这几个常量。参照：原版 plains 的刷怪总权重约 515，
 * 其中僵尸 95、骷髅 100、苦力怕 100。
 */
public class ModSpawns {
    /**
     * 村庄生物群系里的权重。
     *
     * <p>⚠️ <b>别再往上调回 100。</b>原版 plains 的 MONSTER 总权重是 515
     * （蜘蛛/骷髅/苦力怕/史莱姆各 100、僵尸 90、末影人 10、女巫等各 5），
     * 加 100 就是「16% 的怪物都是奶龙」。
     *
     * <p>真正的杀手是<b>它同时无视光照</b>：白天原版地表怪一只都刷不出来
     * （{@code isDarkEnoughToSpawn} 挡掉了），奶龙却照刷不误，等于独占了白天的刷怪机会。
     * 而它唯一的目标就是村民 —— 结果就是 2026-10-02 那次实测里
     * <b>村庄被屠到几乎没人</b>。
     *
     * <p>15 大约是「12 只原版怪里混 1 只奶龙」，既能在村里碰见，又不至于把村杀空。
     */
    private static final int VILLAGE_WEIGHT = 15;

    /** 野外权重：约等于原版怪物总权重的 1%，≈「低概率」。 */
    private static final int WILD_WEIGHT = 5;

    /** 一次生成几只。奶龙体型大、血厚，别一次刷一群。 */
    private static final int MIN_GROUP_SIZE = 1;
    private static final int MAX_GROUP_SIZE = 2;

    /**
     * 原版的五个村庄生物群系标签。
     *
     * <p>{@code BiomeTags} 里<b>没有</b>这五个的常量（那 86 个常量只覆盖 is_* 之类的），
     * 因为它们属于 {@code has_structure/} 这一类，只能自己按 id 建 TagKey。
     * 内容都很小，比如 {@code village_plains} 就是 plains + meadow。
     */
    private static final List<TagKey<Biome>> VILLAGE_BIOME_TAGS = List.of(
            villageTag("village_plains"),
            villageTag("village_desert"),
            villageTag("village_savanna"),
            villageTag("village_snowy"),
            villageTag("village_taiga"));

    /** 「这个生物群系里能生成村庄」。 */
    private static final Predicate<BiomeSelectionContext> IS_VILLAGE_BIOME =
            context -> VILLAGE_BIOME_TAGS.stream().anyMatch(context::hasTag);

    private static TagKey<Biome> villageTag(String name) {
        // 走原版命名空间，所以这里不能用 Mylovelymilkdragon.id
        return TagKey.create(
                Registries.BIOME, Identifier.withDefaultNamespace("has_structure/" + name));
    }

    public static void registerModSpawns() {
        // ---- 1. 允许生成的地方：站在实心方块上，不看光照 ----
        SpawnPlacements.register(
                ModEntities.MILK_DRAGON,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules);

        // ---- 2. 塞进刷怪表 ----
        // 两个筛选器互斥（野外那个把村庄取反了），所以村庄生物群系只会拿到高权重那一份，
        // 不会变成「高 + 低」两份叠加。
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld().and(IS_VILLAGE_BIOME),
                MobCategory.MONSTER,
                ModEntities.MILK_DRAGON,
                VILLAGE_WEIGHT, MIN_GROUP_SIZE, MAX_GROUP_SIZE);

        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld().and(IS_VILLAGE_BIOME.negate()),
                MobCategory.MONSTER,
                ModEntities.MILK_DRAGON,
                WILD_WEIGHT, MIN_GROUP_SIZE, MAX_GROUP_SIZE);
    }
}
