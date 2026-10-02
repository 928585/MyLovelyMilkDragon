package io.github.ninetwo.mdragon.client.render;

import java.util.Set;

import io.github.ninetwo.mdragon.Mylovelymilkdragon;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;

/**
 * 奶龙的「纸片」模型：一块厚度为 0 的平面，正面一张画、背面一张画。
 *
 * <h2>为什么要拆成两个模型层</h2>
 * 一次绘制只能绑<b>一张</b>贴图。如果正反面放在同一个模型里，它们就必然是
 * 同一张图的两块 UV —— 没法用两个独立的 PNG 文件。
 * 所以这里拆成两块独立的纸片（{@link #LAYER_FRONT} / {@link #LAYER_BACK}）：
 * <ul>
 *   <li><b>正面</b>用 {@link #LAYER_FRONT} 烘焙，由渲染器本体绘制</li>
 *   <li><b>背面</b>用 {@link #LAYER_BACK} 烘焙，由 {@link MilkDragonBackLayer}
 *       这个渲染层绘制</li>
 * </ul>
 * 这样两张贴图各自独立，文件互不影响。
 *
 * <h2>贴图怎么画（每张 32 x 32）</h2>
 * <pre>
 *   +-------------------+
 *   |                   |
 *   |    整张就是一面     |   v=0（上）= 头顶
 *   |                   |   v=32（下）= 脚底
 *   +-------------------+
 * </pre>
 * <ul>
 *   <li><b>正面图</b>画「你正对着奶龙看到的样子」</li>
 *   <li><b>背面图</b>画「你站在奶龙背后看到的样子」</li>
 *   <li>两张都<b>不用镜像</b>，照平常画法画就行（绕序已核对过）</li>
 *   <li><b>1 个贴图像素 = 1 个模型单位</b>，不用做任何换算</li>
 *   <li>贴图是 <b>alpha 阈值（cutout）</b>：alpha &lt; 0.1 的像素直接丢弃，
 *       没有半透明。想镂空（比如背景）就涂成完全透明</li>
 * </ul>
 *
 * <h2>为什么渲染类型要显式指定 entityCutoutCull</h2>
 * 纸片厚度为 0，每个模型只有<b>一个</b>面（正面那个只有 NORTH，背面那个只有 SOUTH）。
 * <p>⚠️ 26.3 里 {@code RenderTypes.entityCutout}（也就是
 * {@link EntityModel#EntityModel(ModelPart)} 的默认值）<b>是「不剔背面」的那个</b>——
 * 看 {@code RenderPipelines.ENTITY_CUTOUT} 的源码，它明确写了 {@code .withCull(false)}；
 * 名字里带 {@code Cull} 的 {@code ENTITY_CUTOUT_CULL} 才剔背面。
 * 这跟旧版命名刚好相反，很容易踩。
 * <p>这里剔除是<b>必须</b>的，不只是优化：
 * <ul>
 *   <li>不剔的话，正面纸片从背后看也会画出来 —— 你会看到<b>左右镜像的正面画</b>
 *       叠在背面画上</li>
 *   <li>剔了之后，任何角度都只有朝着镜头的那一块纸片被画出来，一次 draw call
 *       就够，既不会闪也不会穿帮</li>
 * </ul>
 * 绕序在剔除下是对的：原版 {@code ThrownTridentRenderer} 就是用同样剔背面的
 * {@code entitySolidGlint} 渲染 {@code TridentModel} 这套 {@code ModelPart.Cube} 几何的。
 */
public class MilkDragonModel extends EntityModel<MilkDragonRenderState> {

    /** 正面纸片的模型层。 */
    public static final ModelLayerLocation LAYER_FRONT =
            new ModelLayerLocation(Mylovelymilkdragon.id("milk_dragon"), "front");

    /** 背面纸片的模型层。 */
    public static final ModelLayerLocation LAYER_BACK =
            new ModelLayerLocation(Mylovelymilkdragon.id("milk_dragon"), "back");

    /** 纸片在模型空间里的边长，同时也是贴图的宽高。 */
    private static final int PLANE_SIZE = 32;

    /**
     * 模型空间的「脚底」高度。
     *
     * <p>原版约定脚底在 y = 24、头顶往负数方向长，{@code LivingEntityRenderer}
     * 渲染前那句 {@code translate(0, -1.501, 0)} 就是配合这个约定的
     * （24 / 16 = 1.5 格，减去 1.501 刚好贴地）。所以这里也从 24 起算。
     */
    private static final float FEET_Y = 24.0F;

    /**
     * 背面纸片相对正面往后挪的距离（模型单位）。
     *
     * <p>模型空间的 <b>-z 是实体的正面</b>，所以正面纸片放在 z = 0、
     * 背面放在 z = 1，正好是「背面的画在正面画的后面」。
     * 1 个单位 = 1/16 格，肉眼看不出来，但让两块纸片不共面。
     */
    private static final float BACK_Z = 1.0F;

    public MilkDragonModel(ModelPart root) {
        // 必须用会剔背面的 entityCutoutCull，不能要默认的 entityCutout，
        // 原因见类注释（26.3 这两个的命名跟旧版反着）。
        super(root, RenderTypes::entityCutoutCull);
    }

    public static LayerDefinition createFrontLayer() {
        return createPlaneLayer(Direction.NORTH, 0.0F, 0);
    }

    public static LayerDefinition createBackLayer() {
        return createPlaneLayer(Direction.SOUTH, BACK_Z, -PLANE_SIZE);
    }

    /**
     * 造一块只有一个面的纸片。
     *
     * <p>UV 是这样落位的（公式见 {@code ModelPart.Cube} 构造函数，已对着源码核过）：
     * <pre>
     * depth = 0 时：
     *   NORTH 占 u ∈ [xTexOffs,             xTexOffs + width]
     *   SOUTH 占 u ∈ [xTexOffs + width,     xTexOffs + 2 * width]
     * </pre>
     * ⚠️ 所以只想画<b>一个</b> SOUTH 面时，z 偏移要给 <b>-width</b> 才能把它拉回
     * {@code [0, width]}（也就是贴图的整幅）。NORTH 不用挪，天然就在 {@code [0, width]}。
     *
     * @param face     保留哪个面（{@code NORTH} = 正面，{@code SOUTH} = 背面）
     * @param z        纸片在模型空间的 z 位置
     * @param texOffsX 贴图横向偏移，见上面的说明
     */
    private static LayerDefinition createPlaneLayer(Direction face, float z, int texOffsX) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(texOffsX, 0)
                        // 厚度 depth 传 0 就是一片纸；只留一个面，
                        // 另外五个面要么是零宽度、要么根本不该出现，留着纯属浪费。
                        //
                        // x 取 -16..16 是水平居中；y 取 24-32 = -8..24 是「脚踩在 y=24 上」。
                        .addBox(-PLANE_SIZE / 2.0F, FEET_Y - PLANE_SIZE, z,
                                PLANE_SIZE, PLANE_SIZE, 0.0F,
                                Set.of(face)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, PLANE_SIZE, PLANE_SIZE);
    }
}
