package io.github.ninetwo.mdragon.entity.goal;

import java.util.EnumSet;

import io.github.ninetwo.mdragon.entity.MilkDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 拆火把：奶龙看到周围 4 格内（上下 2 格）有火把，就晃过去把它拆掉，拆完冷却 5 秒。
 *
 * <p>破坏用的是 {@link Level#destroyBlock(BlockPos, boolean)} 且第二个参数传 {@code true}，
 * 也就是走原版正常破坏流程，所以会照常掉火把、出粒子和音效。
 *
 * <p>「火把」的口径见 {@link #isTorch(BlockState)}——包含普通 / 灵魂 / 铜火把的直立与墙面版本，
 * <b>不含</b>红石火把。
 */
public class BreakTorchGoal extends Goal {

    /** 水平搜索半径（格）。设计定稿是 4。 */
    private static final int SEARCH_RADIUS = 4;

    /** 垂直只上下找 2 格，免得盯着地底或者树梢上的火把来回绕圈。 */
    private static final int SEARCH_RADIUS_Y = 2;

    /** 拆掉一个火把之后的冷却，5 秒。 */
    private static final int COOLDOWN_TICKS = 20 * 5;

    /**
     * 走到多近才动手（格）。
     * 必须明显大于奶龙碰撞箱的半宽（0.5），否则会被自己身体卡住永远走不到原点。
     */
    private static final double REACH_DISTANCE = 2.5D;

    /** 重算路径的间隔（tick）。每 tick 重寻路太浪费，而且会打断走路动画。 */
    private static final int REPATH_INTERVAL = 10;

    private final MilkDragonEntity dragon;
    private final double speedModifier;

    /** 当前盯上的火把；{@code null} 表示没在拆。 */
    private BlockPos targetTorch;

    /** 冷却剩余 tick，大于 0 时这个 Goal 不起用。 */
    private int cooldown;

    public BreakTorchGoal(MilkDragonEntity dragon, double speedModifier) {
        this.dragon = dragon;
        this.speedModifier = speedModifier;
        // 走过去 + 盯着看，所以占住 MOVE 和 LOOK 两个 Flag
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        if (this.dragon.isSleeping()) {
            return false;
        }
        this.targetTorch = this.findNearbyTorch();
        return this.targetTorch != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetTorch != null
                && !this.dragon.isSleeping()
                && isTorch(this.dragon.level().getBlockState(this.targetTorch));
    }

    @Override
    public void start() {
        this.moveToTorch();
    }

    @Override
    public void tick() {
        BlockPos torch = this.targetTorch;
        if (torch == null) {
            return;
        }

        double x = torch.getX() + 0.5D;
        double y = torch.getY() + 0.5D;
        double z = torch.getZ() + 0.5D;
        this.dragon.getLookControl().setLookAt(x, y, z);

        if (this.dragon.distanceToSqr(x, y, z) > REACH_DISTANCE * REACH_DISTANCE) {
            if (this.dragon.tickCount % REPATH_INTERVAL == 0) {
                this.moveToTorch();
            }
            return;
        }

        // 够得着了：正常破坏（true = 产生掉落物），然后进冷却
        this.dragon.level().destroyBlock(torch, true);
        this.targetTorch = null;
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public void stop() {
        this.targetTorch = null;
    }

    private void moveToTorch() {
        BlockPos torch = this.targetTorch;
        if (torch != null) {
            this.dragon.getNavigation().moveTo(
                    torch.getX() + 0.5D, torch.getY(), torch.getZ() + 0.5D, this.speedModifier);
        }
    }

    /** 在自身周围找最近的一个火把，找不到返回 {@code null}。 */
    private BlockPos findNearbyTorch() {
        Level level = this.dragon.level();
        BlockPos origin = this.dragon.blockPosition();
        BlockPos min = origin.offset(-SEARCH_RADIUS, -SEARCH_RADIUS_Y, -SEARCH_RADIUS);
        BlockPos max = origin.offset(SEARCH_RADIUS, SEARCH_RADIUS_Y, SEARCH_RADIUS);

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!isTorch(level.getBlockState(pos))) {
                continue;
            }
            double distance = this.dragon.distanceToSqr(
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distance < bestDistance) {
                bestDistance = distance;
                // betweenClosed 交出来的是会被复用的可变对象，必须拷一份
                best = pos.immutable();
            }
        }
        return best;
    }

    /**
     * 判断方块是不是火把。
     *
     * <p>26.3 里火把只有两个实现类，都挂在 {@link TorchBlock} 这条线上：
     * <ul>
     *   <li>{@code TorchBlock}——原版把 TORCH / SOUL_TORCH / COPPER_TORCH 全注册成它</li>
     *   <li>{@code WallTorchBlock}（继承自 {@code TorchBlock}）——WALL_TORCH /
     *       SOUL_WALL_TORCH / COPPER_WALL_TORCH</li>
     * </ul>
     * 红石火把走的是另一条线（{@code RedstoneTorchBlock} 直接继承 {@code BaseTorchBlock}），
     * 不会命中这里——它是红石元件，不在「拆火把」的范围内。
     */
    private static boolean isTorch(BlockState state) {
        return state.getBlock() instanceof TorchBlock;
    }
}
