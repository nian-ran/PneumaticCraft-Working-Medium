package dev.pncwm;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.pncwm.drone.DronePieces;
import dev.pncwm.registry.WMBlockEntities;
import dev.pncwm.registry.WMBlocks;
import dev.pncwm.registry.WMData;
import dev.pncwm.registry.WMFluids;
import dev.pncwm.registry.WMItems;
import dev.pncwm.registry.WMMenus;
import dev.pncwm.registry.WMRecipes;
import dev.pncwm.registry.WMSounds;
import dev.pncwm.registry.WMTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@Mod(WorkingMedium.MOD_ID)
public class WorkingMedium {
    public static final String MOD_ID = "workingmedium";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorkingMedium(IEventBus modBus, ModContainer container) {
        WMBlocks.BLOCKS.register(modBus);
        WMItems.ITEMS.register(modBus);
        WMFluids.FLUID_TYPES.register(modBus);
        WMFluids.FLUIDS.register(modBus);
        WMBlockEntities.TYPES.register(modBus);
        WMMenus.MENUS.register(modBus);
        WMRecipes.TYPES.register(modBus);
        WMRecipes.SERIALIZERS.register(modBus);
        WMTabs.TABS.register(modBus);
        WMSounds.SOUNDS.register(modBus);
        WMData.TYPES.register(modBus);

        modBus.addListener(WMBlockEntities::registerCapabilities);
        modBus.addListener(this::commonSetup);
        container.registerConfig(ModConfig.Type.COMMON, WMConfig.SPEC);
        DronePieces.register();

        NeoForge.EVENT_BUS.addListener(CurtainModule::onHurt);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Working Medium grades online: G0-G5");
    }

    /**
     * Held here so the event listener class can stay package-private to the item.
     */
    private static final class CurtainModule {
        private static void onHurt(LivingIncomingDamageEvent event) {
            dev.pncwm.item.CurtainModuleItem.onHurt(event);
        }
    }
}
