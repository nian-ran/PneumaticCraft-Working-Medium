package dev.pncwm.client;

import dev.pncwm.WorkingMedium;
import dev.pncwm.block.GaugeBlock;
import dev.pncwm.client.model.ConnectedCubeLoader;
import dev.pncwm.registry.WMBlocks;
import dev.pncwm.registry.WMFluids;
import dev.pncwm.registry.WMMenus;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = WorkingMedium.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WMClient {
    private WMClient() {
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(WMMenus.APPARATUS.get(), ApparatusScreen::new);
        event.register(WMMenus.CATALOG.get(), CatalogScreen::new);
    }

    @SubscribeEvent
    public static void modelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "connected_cube"), ConnectedCubeLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (tint != 0) {
                return -1;
            }
            if (state.getBlock() instanceof GaugeBlock) {
                return state.getValue(GaugeBlock.BAND).color;
            }
            return -1;
        }, WMBlocks.GAUGE.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        for (WMFluids.FluidSet fluid : WMFluids.ALL) {
            int tint = fluid.tint;
            event.register((stack, index) -> index == 1 ? tint : -1, fluid.bucket.get());
        }
    }

    @SubscribeEvent
    public static void fluids(RegisterClientExtensionsEvent event) {
        for (WMFluids.FluidSet fluid : WMFluids.ALL) {
            int tint = fluid.tint;
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public int getTintColor() {
                    return tint;
                }

                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "block/fluid_still");
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "block/fluid_flow");
                }

                @Override
                public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                    return new Vector3f(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f);
                }
            }, fluid.type.get());
        }
    }
}
