package dev.pncwm.registry;

import dev.pncwm.WorkingMedium;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WMSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, WorkingMedium.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_RUN = register("machine_run");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_DONE = register("machine_done");
    public static final DeferredHolder<SoundEvent, SoundEvent> VENT = register("vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLICK = register("ui_click");

    private WMSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, name)));
    }

    public static void play(Level level, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        if (level == null || sound == null) {
            return;
        }
        level.playSound(null, pos, sound, source, volume, pitch);
    }
}
