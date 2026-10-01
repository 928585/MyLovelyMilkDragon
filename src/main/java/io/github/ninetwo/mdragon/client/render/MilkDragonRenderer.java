package io.github.ninetwo.mdragon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.ninetwo.mdragon.Mylovelymilkdragon;
import io.github.ninetwo.mdragon.entity.MilkDragonEntity;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * 奶龙的实体渲染器。
 *
 * <p><b>当前是阶段 3a：几何体借用原版猪的模型占位。</b>
 * 之所以能借用，是因为 {@code PigModel} 继承的是
 * {@code QuadrupedModel<LivingEntityRenderState>}（泛型绑在 {@code LivingEntityRenderState} 上，
 * 而不是 {@code PigRenderState}），所以只要我们的 RenderState 继承
 * {@code LivingEntityRenderState} 就能直接塞进去。
 *
 * <p>阶段 3b 会换成真正的奶龙模型：新写一个 {@code MilkDragonModel} + 自定义
 * {@code ModelLayerLocation}，把下面这一行 super(...) 换掉即可，其余不用动。
 */
public class MilkDragonRenderer
        extends MobRenderer<MilkDragonEntity, MilkDragonRenderState, PigModel> {

    // 贴图路径现在就按最终形态定好，PNG 一放进去就能显示，不用改代码。
    // 注意占位模型是猪的，UV 布局是 64x32；等 3b 换成自定义模型后布局会变。
    private static final Identifier TEXTURE =
            Mylovelymilkdragon.id("textures/entity/milk_dragon/milk_dragon.png");
    private static final Identifier TEXTURE_ANGRY =
            Mylovelymilkdragon.id("textures/entity/milk_dragon/milk_dragon_angry.png");

    public MilkDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new PigModel(context.bakeLayer(ModelLayers.PIG)), 0.9F);
    }

    @Override
    public MilkDragonRenderState createRenderState() {
        return new MilkDragonRenderState();
    }

    @Override
    public void extractRenderState(MilkDragonEntity entity, MilkDragonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // 把实体上的同步状态拷进渲染状态，供渲染线程使用
        state.angry = entity.isAngry();
        state.sleeping = entity.isSleeping();
        state.laughing = entity.isLaughing();
    }

    @Override
    protected void scale(MilkDragonRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        // 占位的猪模型比奶龙的碰撞箱（1.0 宽 x 1.5 高）小一圈，先放大凑个比例
        poseStack.scale(1.3F, 1.3F, 1.3F);
    }

    @Override
    public Identifier getTextureLocation(MilkDragonRenderState state) {
        return state.angry ? TEXTURE_ANGRY : TEXTURE;
    }
}
