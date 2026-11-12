package dev.pncwm.blockentity;

import dev.pncwm.block.HatchBlock;
import dev.pncwm.block.HatchType;
import dev.pncwm.registry.WMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class HatchBlockEntity extends BlockEntity {
    private BlockPos controller = BlockPos.ZERO;
    private boolean bound;

    public HatchBlockEntity(BlockPos pos, BlockState state) {
        super(WMBlockEntities.HATCH.get(), pos, state);
    }

    public void bind(BlockPos controller) {
        this.controller = controller.immutable();
        this.bound = true;
        setChanged();
    }

    public void unbind() {
        if (bound) {
            bound = false;
            setChanged();
        }
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
        if (level.getBlockEntity(controller) instanceof ApparatusBlockEntity apparatus) {
            return apparatus;
        }
        bound = false;
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
