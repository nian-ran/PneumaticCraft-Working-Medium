package dev.pncwm.client.model;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class ConnectedCubeModel implements IDynamicBakedModel {
    public static final ModelProperty<int[]> FACE_MASKS = new ModelProperty<>();

    private static final FaceBakery BAKERY = new FaceBakery();
    private static final Vector3f FROM = new Vector3f(0f, 0f, 0f);
    private static final Vector3f TO = new Vector3f(16f, 16f, 16f);

    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private final boolean ambientOcclusion;
    private final boolean blockLight;
    private final boolean gui3d;
    private final BakedQuad[] isolated;
    private final BakedQuad[][] connected;

    public ConnectedCubeModel(TextureAtlasSprite base, TextureAtlasSprite ctm, TextureAtlasSprite particle,
                              ItemTransforms transforms, boolean ambientOcclusion, boolean blockLight, boolean gui3d,
                              ModelState modelState) {
        this.particle = particle;
        this.transforms = transforms;
        this.ambientOcclusion = ambientOcclusion;
        this.blockLight = blockLight;
        this.gui3d = gui3d;
        this.isolated = new BakedQuad[6];
        this.connected = new BakedQuad[16][6];
        for (Direction face : Direction.values()) {
            isolated[face.ordinal()] = bake(base, face, 0, 0, 16, 16, modelState);
            for (int mask = 0; mask < 16; mask++) {
                float u0 = CtmIndex.u0(mask);
                float v0 = CtmIndex.v0(mask);
                connected[mask][face.ordinal()] = bake(ctm, face, u0, v0, u0 + 4, v0 + 4, modelState);
            }
        }
    }

    private static BakedQuad bake(TextureAtlasSprite sprite, Direction face, float u0, float v0, float u1, float v1, ModelState modelState) {
        BlockFaceUV uv = new BlockFaceUV(new float[] {u0, v0, u1, v1}, 0);
        BlockElementFace element = new BlockElementFace(face, -1, "#all", uv);
        return BAKERY.bakeQuad(FROM, TO, element, sprite, face, modelState, null, true);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        int[] masks = extraData.get(FACE_MASKS);
        List<BakedQuad> quads = new ArrayList<>(side == null ? 6 : 1);
        if (side == null) {
            for (Direction face : Direction.values()) {
                quads.add(quad(face, masks));
            }
        } else {
            quads.add(quad(side, masks));
        }
        return quads;
    }

    private BakedQuad quad(Direction face, @Nullable int[] masks) {
        if (masks == null) {
            return isolated[face.ordinal()];
        }
        return connected[masks[face.ordinal()] & 15][face.ordinal()];
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        Block self = state.getBlock();
        int[] masks = new int[6];
        for (Direction face : Direction.values()) {
            Direction[] edges = CtmFaces.edges(face);
            masks[face.ordinal()] = CtmIndex.pack(
                    same(level, pos, self, edges[0]),
                    same(level, pos, self, edges[1]),
                    same(level, pos, self, edges[2]),
                    same(level, pos, self, edges[3])
            );
        }
        return modelData.derive().with(FACE_MASKS, masks).build();
    }

    private static boolean same(BlockAndTintGetter level, BlockPos pos, Block self, Direction direction) {
        return level.getBlockState(pos.relative(direction)).getBlock() == self;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    @Override
    public boolean usesBlockLight() {
        return blockLight;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }
}
