package dev.pncwm.client.model;

import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

public final class ConnectedCubeLoader implements IGeometryLoader<ConnectedCubeLoader.Geometry> {
    public static final ConnectedCubeLoader INSTANCE = new ConnectedCubeLoader();

    private ConnectedCubeLoader() {
    }

    @Override
    public Geometry read(JsonObject json, JsonDeserializationContext context) {
        return new Geometry();
    }

    public static final class Geometry implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides) {
            TextureAtlasSprite base = spriteGetter.apply(context.getMaterial("all"));
            TextureAtlasSprite ctm = context.hasMaterial("ctm") ? spriteGetter.apply(context.getMaterial("ctm")) : base;
            TextureAtlasSprite particle = context.hasMaterial("particle") ? spriteGetter.apply(context.getMaterial("particle")) : base;
            return new ConnectedCubeModel(base, ctm, particle, context.getTransforms(), context.useAmbientOcclusion(),
                    context.useBlockLight(), context.isGui3d(), modelState);
        }
    }
}
