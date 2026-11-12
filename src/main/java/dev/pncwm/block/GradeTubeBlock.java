package dev.pncwm.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.pncwm.blockentity.GradeTubeBlockEntity;
import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.registry.WMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GradeTubeBlock extends BaseEntityBlock {
    public static final MapCodec<GradeTubeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PressureGrade.CODEC.fieldOf("grade").forGetter(block -> block.grade),
            propertiesCodec()
    ).apply(instance, GradeTubeBlock::new));

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape[] ARMS = {
            Block.box(6, 6, 0, 10, 10, 6),
            Block.box(10, 6, 6, 16, 10, 10),
            Block.box(6, 6, 10, 10, 10, 16),
            Block.box(0, 6, 6, 6, 10, 10),
            Block.box(6, 10, 6, 10, 16, 10),
            Block.box(6, 0, 6, 10, 6, 10)
    };
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = CORE;
            for (int bit = 0; bit < 6; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    shape = Shapes.or(shape, ARMS[bit]);
                }
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public final PressureGrade grade;

    public GradeTubeBlock(PressureGrade grade, Properties properties) {
        super(properties);
        this.grade = grade;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    public static boolean connectsTo(BlockState neighbor) {
        Block block = neighbor.getBlock();
        return block instanceof GradeTubeBlock
                || block instanceof HatchBlock
                || block instanceof ApparatusBlock
                || block instanceof TransferBlock
                || block instanceof ReliefValveBlock
                || block instanceof GaugeBlock;
    }

    @Override
    protected MapCodec<? extends GradeTubeBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(property(direction), connectsTo(neighborState));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        if (state.getValue(NORTH)) {
            mask |= 1;
        }
        if (state.getValue(EAST)) {
            mask |= 2;
        }
        if (state.getValue(SOUTH)) {
            mask |= 4;
        }
        if (state.getValue(WEST)) {
            mask |= 8;
        }
        if (state.getValue(UP)) {
            mask |= 16;
        }
        if (state.getValue(DOWN)) {
            mask |= 32;
        }
        return SHAPES[mask];
    }

    public BlockState connections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), connectsTo(level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            level.setBlock(pos, connections(level, pos, state), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GradeTubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != WMBlockEntities.TUBE.get()) {
            return null;
        }
        return (lvl, pos, blockState, be) -> {
            if (be instanceof GradeTubeBlockEntity tube) {
                tube.tick();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GradeTubeBlockEntity tube) {
            tube.tell(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
