package io.github.ninetwo.mdragon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.ninetwo.mdragon.Mylovelymilkdragon;
import io.github.ninetwo.mdragon.entity.MilkDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * 奶龙的实体渲染器。
 *
 * <p><b>几何体是自定义的纸片模型 {@link MilkDragonModel}</b>（厚度为 0 的平面）。
 * 正面由本渲染器绘制，背面由 {@link MilkDragonBackLayer} 绘制 —— 拆开是为了让
 * 正面和背面能用<b>两个独立的 PNG</b>。
 *
 * <p><b>一共 6 张贴图</b>：平静 / 愤怒 / 睡觉 三套状态，每套分正、背两面，都在
 * {@code textures/entity/milk_dragon/} 下，每张 32 x 32：
 *
 * <pre>
 *   milk_dragon_front.png            milk_dragon_back.png
 *   milk_dragon_angry_front.png      milk_dragon_angry_back.png
 *   milk_dragon_sleeping_front.png   milk_dragon_sleeping_back.png
 * </pre>
 *
 * 画法见 {@link MilkDragonModel} 的类注释。
 */
public class MilkDragonRenderer
        extends MobRenderer<MilkDragonEntity, MilkDragonRenderState, MilkDragonModel> {

    /** 贴图所在目录。 */
    private static final String TEXTURE_DIR = "textures/entity/milk_dragon/";

    /**
     * 纸片在模型空间里是 32 x 32 单位（= 2 x 2 格），缩一半变成 1 x 1 格。
     *
     * <p>⚠️ 这是<b>唯一</b>决定奶龙世界大小的数，改它就够。
     * 注意碰撞箱是 1.0 宽 x 1.5 高（第 11.1 节定死的），而纸片贴图画布是正方形，
     * 所以 1 x 1 格时「宽度对得上、比碰撞箱矮半格」。
     * 想跟碰撞箱一样高就把画布改成 32 x 48，模型高度跟着改成 48，
     * 缩放保持 0.5 —— 但那会把画拉伸，得重新画。
     */
    private static final float MODEL_SCALE = 0.5F;

    public MilkDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new MilkDragonModel(context.bakeLayer(MilkDragonModel.LAYER_FRONT)), 0.5F);
        // 背面那块纸走渲染层，因为一次绘制只能绑一张贴图
        this.addLayer(new MilkDragonBackLayer(this, context));
    }

    @Override
    public MilkDragonRenderState createRenderState() {
        return new MilkDragonRenderState();
    }

    @Override
    public void extractRenderState(MilkDragonEntity entity, MilkDragonRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // 把实体上的同步状态拷进渲染状态，供渲染线程使用
        state.angry = entity.isAngry();
        state.sleeping = entity.isSleeping();
        state.laughing = entity.isLaughing();
    }

    @Override
    protected void scale(MilkDragonRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    /** 渲染器本体画的是正面。 */
    @Override
    public Identifier getTextureLocation(MilkDragonRenderState state) {
        return frontTexture(state);
    }

    public static Identifier frontTexture(MilkDragonRenderState state) {
        return texture(variantOf(state), "front");
    }

    public static Identifier backTexture(MilkDragonRenderState state) {
        return texture(variantOf(state), "back");
    }

    /**
     * 按优先级挑状态贴图。
     *
     * <p>愤怒排最前：第 11.1 节里愤怒会强制踢出睡觉，两者本来就是互斥的，
     * 这里只是防御性地定个优先级。
     *
     * <p>「大笑」没有单独的贴图 —— 它只持续 2 秒，跟平静共用一套。
     */
    private static String variantOf(MilkDragonRenderState state) {
        if (state.angry) {
            return "angry";
        }
        return state.sleeping ? "sleeping" : "";
    }

    private static Identifier texture(String variant, String face) {
        String name = variant.isEmpty()
                ? "milk_dragon_" + face
                : "milk_dragon_" + variant + "_" + face;
        return Mylovelymilkdragon.id(TEXTURE_DIR + name + ".png");
    }
}
