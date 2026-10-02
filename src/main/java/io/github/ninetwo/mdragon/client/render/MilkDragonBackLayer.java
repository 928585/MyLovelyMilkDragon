package io.github.ninetwo.mdragon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * 画奶龙背面的那一层纸。
 *
 * <p>存在的唯一理由是「一张贴图绑一次绘制」：正面由渲染器本体画，
 * 背面就只能在另一个渲染层里用另一张贴图画。详见 {@link MilkDragonModel} 的类注释。
 *
 * <p>⚠️ 这个层用的是<b>自己烘焙的</b> {@link MilkDragonModel} 实例
 * （{@link MilkDragonModel#LAYER_BACK}），跟渲染器本体那个是两块独立的纸片。
 * 泛型上仍然是 {@code RenderLayer<MilkDragonRenderState, MilkDragonModel>}，
 * 因为 {@code RenderLayer<S, M>} 里的 {@code M} 只约束父渲染器的模型类型，
 * 层自己画什么由 {@link #submit} 决定。
 */
public class MilkDragonBackLayer extends RenderLayer<MilkDragonRenderState, MilkDragonModel> {

    private final MilkDragonModel model;

    public MilkDragonBackLayer(RenderLayerParent<MilkDragonRenderState, MilkDragonModel> parent,
            EntityRendererProvider.Context context) {
        super(parent);
        this.model = new MilkDragonModel(context.bakeLayer(MilkDragonModel.LAYER_BACK));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
            MilkDragonRenderState state, float yRot, float xRot) {
        // 隐形的奶龙不该露出一块背面纸片。
        // ⚠️ 这里只判 isInvisible，跟原版渲染层（RenderLayer.coloredCutoutModelCopyLayerRender）
        // 的做法一致；创造模式玩家看隐形实体时本体会走半透明，背面这块不跟着走，
        // 属于可接受的小差异。
        if (state.isInvisible) {
            return;
        }

        // order(-1) = 排在渲染器本体【之前】画，语义上就是「这块纸在本体的后面」。
        // 两面都是不透明的、走深度测试，所以顺序其实不影响结果，
        // 但跟原版 SulfurCubeInnerLayer 用同样 -1 表示「内层」是一个道理。
        //
        // renderType 走模型自己的（entityCutoutCull，会剔背面），
        // 所以从正面看时这块 SOUTH 面是背面朝向镜头的，直接不画 —— 见 MilkDragonModel 的注释。
        submitNodeCollector.order(-1).submitModel(
                this.model,
                state,
                poseStack,
                MilkDragonRenderer.backTexture(state),
                lightCoords,
                // 原版渲染层统一传 0.0F；受击红闪靠 state.hasRedOverlay，不受这个参数影响
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                state.outlineColor);
    }
}
