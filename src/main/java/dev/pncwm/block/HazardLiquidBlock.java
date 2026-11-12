package dev.pncwm.block;

import dev.pncwm.pressure.Tuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Chlorine, acid, caustic and flue gas hurt anything that stands in them. Liquid carbon dioxide, nitrogen,
 * argon and oxygen freeze and slow living things. Harmless fluids use this block too, but the tuning rules
 * return zero for those and nothing happens.
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
    }
}
