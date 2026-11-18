package dev.pncwm.blockentity;

import dev.pncwm.block.HatchBlock;
import dev.pncwm.block.HatchType;
import dev.pncwm.registry.WMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import me.desht.pneumaticcraft.api.tileentity.IManoMeasurable;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class HatchBlockEntity extends BlockEntity implements IManoMeasurable {
    private BlockPos controller = BlockPos.ZERO;
    private boolean bound;

    public HatchBlockEntity(BlockPos pos, BlockState state) {
        super(WMBlockEntities.HATCH.get(), pos, state);
    }

    public void bind(BlockPos controller) {
        boolean changed = !bound || !this.controller.equals(controller);
        this.controller = controller.immutable();
        this.bound = true;
        setChanged();
        if (changed) {
            refreshCapabilities();
        }
    }

    public void unbind() {
        if (bound) {
            bound = false;
            setChanged();
            refreshCapabilities();
        }
    }

    /**
     * Pipes, hoppers and heat pipes cache what a hatch exposes. Without this they kept the "nothing here" they saw
     * before the machine formed (or the old handlers after it broke) until the chunk was reloaded.
     */
    private void refreshCapabilities() {
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    public boolean isBound() {
        return bound;
    }

    /** Saved controller position, or null when the hatch is not bound. */
    public BlockPos boundControllerPos() {
        return bound ? controller.immutable() : null;
    }

    public HatchType type() {
        if (getBlockState().getBlock() instanceof HatchBlock hatch) {
            return hatch.type;
        }
        return HatchType.ITEM_IN;
    }

    public ApparatusBlockEntity controller() {
        if (!bound || level == null) {
            return null;
        }
        // The controller may sit in another chunk. Treating an unloaded block entity as "gone"
        // used to clear the binding, and a later save would forget the link.
        if (!level.isLoaded(controller)) {
            return null;
        }
        if (level.getBlockEntity(controller) instanceof ApparatusBlockEntity apparatus) {
            return apparatus;
        }
        bound = false;
        setChanged();
        refreshCapabilities();
        return null;
    }

    public IItemHandler items(Direction side) {
        ApparatusBlockEntity apparatus = controller();
        if (apparatus == null) {
            return null;
        }
        return apparatus.hatchItems(type());
    }

    public IFluidHandler fluids(Direction side) {
        ApparatusBlockEntity apparatus = controller();
        if (apparatus == null) {
            return null;
        }
        return apparatus.hatchFluids(type());
    }

    /** Heat pipes connect through a heat hatch to the bound controller. Other hatch types expose nothing. */
    public IHeatExchangerLogic heat(Direction side) {
        if (type() != HatchType.HEAT) {
            return null;
        }
        ApparatusBlockEntity apparatus = controller();
        return apparatus == null ? null : apparatus.heat();
    }

    /**
     * Official PneumaticCraft manometers look at the block entity under the cursor.
     * A bound hatch now answers with the controller's shell pressure and temperature.
     */
    @Override
    public void printManometerMessage(Player player, List<Component> curInfo) {
        ApparatusBlockEntity apparatus = controller();
        if (apparatus != null) {
            apparatus.printManometerMessage(player, curInfo);
            return;
        }
        if (bound && level != null && !level.isLoaded(controller)) {
            return;
        }
        curInfo.add(Component.translatable("message.workingmedium.probe_unbound"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (bound) {
            tag.putLong("Controller", controller.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Controller")) {
            controller = BlockPos.of(tag.getLong("Controller"));
            bound = true;
        }
    }
}
