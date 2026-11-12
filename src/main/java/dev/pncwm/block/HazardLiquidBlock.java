package dev.pncwm.block;

import dev.pncwm.pressure.Tuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Chlorine, acid, caustic and flue gas hurt anything that stands in them. Liquid carbon dioxide, nitrogen,
 * argon and oxygen freeze and slow living things. Dusty steam scalds. Hydrogen ignites next to fire and
 * the spilled pool itself burns off; oxygen feeds a fire that is already burning. Harmless fluids use
 * this block too, but the tuning rules return zero for those and nothing happens.
 */
public class HazardLiquidBlock extends LiquidBlock {
    public HazardLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        String path = BuiltInRegistries.FLUID.getKey(state.getFluidState().getType()).getPath();
        float harm = Tuning.fluidHarm(path);
        if (harm > 0f) {
            living.hurt(living.damageSources().magic(), harm);
        }
        int poison = Tuning.fluidPoisonTicks(path);
        if (poison > 0) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, poison, 0));
        }
        float freeze = Tuning.fluidFreeze(path);
        if (freeze > 0f) {
            living.hurt(living.damageSources().freeze(), freeze);
            living.setTicksFrozen(Math.min(living.getTicksRequiredToFreeze() + 40, living.getTicksFrozen() + 40));
        }
        int slow = Tuning.fluidSlowTicks(path);
        if (slow > 0) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 0));
        }
        float scald = Tuning.fluidScald(path);
        if (scald > 0f) {
            living.hurt(living.damageSources().hotFloor(), scald);
        }
        if (Tuning.isFlammableGas(path) && (living.isOnFire() || nearFlame(level, pos))) {
            living.hurt(living.damageSources().onFire(), 2f);
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 80));
            if (nearFlame(level, pos)) {
                ignitePool(level, pos);
            }
        }
        if (Tuning.isOxidizer(path) && living.isOnFire()) {
            living.hurt(living.damageSources().onFire(), 2f);
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 160));
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        tryIgnite(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        tryIgnite(level, pos, state);
    }

    private static void tryIgnite(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        String path = BuiltInRegistries.FLUID.getKey(state.getFluidState().getType()).getPath();
        if (Tuning.poolIgnites(path, nearFlame(level, pos))) {
            ignitePool(level, pos);
        }
    }

    private static void ignitePool(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        boolean support = floor.isFaceSturdy(level, below, Direction.UP);
        level.setBlock(pos, support ? Blocks.FIRE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.55f, 1.15f);
    }

    private static boolean nearFlame(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(Blocks.FIRE) || neighbor.is(Blocks.SOUL_FIRE) || neighbor.is(Blocks.LAVA)
                    || neighbor.getFluidState().is(Fluids.LAVA) || neighbor.getFluidState().is(Fluids.FLOWING_LAVA)
                    || neighbor.is(Blocks.MAGMA_BLOCK)) {
                return true;
            }
        }
        return false;
    }
}
