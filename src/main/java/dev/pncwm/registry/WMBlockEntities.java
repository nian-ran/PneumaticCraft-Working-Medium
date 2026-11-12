package dev.pncwm.registry;

import dev.pncwm.WorkingMedium;
import dev.pncwm.block.TransferBlock;
import dev.pncwm.blockentity.ApparatusBlockEntity;
import dev.pncwm.blockentity.GaugeBlockEntity;
import dev.pncwm.blockentity.GradeTubeBlockEntity;
import dev.pncwm.blockentity.HatchBlockEntity;
import dev.pncwm.blockentity.ReliefValveBlockEntity;
import dev.pncwm.blockentity.TransferBlockEntity;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WMBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WorkingMedium.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApparatusBlockEntity>> APPARATUS = TYPES.register("apparatus", () ->
            BlockEntityType.Builder.of(ApparatusBlockEntity::new, WMBlocks.CONTROLLERS.values().stream().map(block -> (Block) block.get()).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HatchBlockEntity>> HATCH = TYPES.register("hatch", () ->
            BlockEntityType.Builder.of(HatchBlockEntity::new, WMBlocks.HATCHES.values().stream().map(block -> (Block) block.get()).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GradeTubeBlockEntity>> TUBE = TYPES.register("tube", () ->
            BlockEntityType.Builder.of(GradeTubeBlockEntity::new, WMBlocks.INDUSTRIAL_TUBE.get(), WMBlocks.CRYO_TUBE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GaugeBlockEntity>> GAUGE = TYPES.register("gauge", () ->
            BlockEntityType.Builder.of(GaugeBlockEntity::new, WMBlocks.GAUGE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReliefValveBlockEntity>> RELIEF = TYPES.register("relief", () ->
            BlockEntityType.Builder.of(ReliefValveBlockEntity::new, WMBlocks.RELIEF.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransferBlockEntity>> TRANSFER = TYPES.register("transfer", () ->
            BlockEntityType.Builder.of((pos, state) -> new TransferBlockEntity(pos, state, state.getBlock() instanceof TransferBlock transfer && transfer.booster),
                    WMBlocks.REDUCER.get(), WMBlocks.BOOSTER.get()).build(null));

    private WMBlockEntities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PNCCapabilities.AIR_HANDLER_MACHINE, TUBE.get(), (be, side) -> be.air());
        event.registerBlockEntity(PNCCapabilities.HEAT_EXCHANGER_BLOCK, TUBE.get(), (be, side) -> be.heat());
        event.registerBlockEntity(PNCCapabilities.AIR_HANDLER_MACHINE, GAUGE.get(), (be, side) -> be.air());
        event.registerBlockEntity(PNCCapabilities.HEAT_EXCHANGER_BLOCK, GAUGE.get(), (be, side) -> be.heat());
        event.registerBlockEntity(PNCCapabilities.AIR_HANDLER_MACHINE, RELIEF.get(), (be, side) -> be.air());
        event.registerBlockEntity(PNCCapabilities.HEAT_EXCHANGER_BLOCK, RELIEF.get(), (be, side) -> be.heat());
        event.registerBlockEntity(PNCCapabilities.AIR_HANDLER_MACHINE, TRANSFER.get(), TransferBlockEntity::air);
        event.registerBlockEntity(PNCCapabilities.HEAT_EXCHANGER_BLOCK, TRANSFER.get(), (be, side) -> be.heat());
        event.registerBlockEntity(PNCCapabilities.AIR_HANDLER_MACHINE, APPARATUS.get(), ApparatusBlockEntity::airOn);
        event.registerBlockEntity(PNCCapabilities.HEAT_EXCHANGER_BLOCK, APPARATUS.get(), (be, side) -> be.heat());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, APPARATUS.get(), (be, side) -> be.directItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, APPARATUS.get(), (be, side) -> be.directFluids());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HATCH.get(), HatchBlockEntity::items);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, HATCH.get(), HatchBlockEntity::fluids);
        event.registerBlockEntity(PNCCapabilities.HEAT_EXCHANGER_BLOCK, HATCH.get(), HatchBlockEntity::heat);
    }
}
