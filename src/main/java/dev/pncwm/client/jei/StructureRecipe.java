package dev.pncwm.client.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import dev.pncwm.apparatus.ApparatusKind;
import dev.pncwm.block.PartKind;
import dev.pncwm.multiblock.StructureShapes;
import dev.pncwm.multiblock.StructureShapes.Layout;
import dev.pncwm.multiblock.StructureShapes.Role;
import dev.pncwm.registry.WMBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public record StructureRecipe(ApparatusKind kind, Layout layout) {
    public static StructureRecipe of(ApparatusKind kind) {
        return new StructureRecipe(kind, StructureShapes.minimum(kind.id, kind.family, kind.minSize, kind.maxSize));
    }

    public ItemStack controllerStack() {
        return new ItemStack(WMBlocks.CONTROLLERS.get(kind).get());
    }

    public List<ItemStack> ingredients() {
        Map<Role, Integer> counts = layout.counts();
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(controllerStack());
        counts.entrySet().stream()
                .filter(entry -> entry.getKey() != Role.CONTROLLER && entry.getKey() != Role.AIR)
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .forEach(entry -> {
                    ItemStack stack = stackFor(entry.getKey());
                    stack.setCount(Math.min(64, entry.getValue()));
                    stacks.add(stack);
                });
        return stacks;
    }

    public ItemStack stackFor(Role role) {
        return new ItemStack(stateFor(role).getBlock());
    }

    public BlockState stateFor(Role role) {
        return switch (role) {
            case CONTROLLER -> WMBlocks.CONTROLLERS.get(kind).get().defaultBlockState();
            case CASING -> WMBlocks.CASINGS.get(kind.grade).get().defaultBlockState();
            case TRAY -> WMBlocks.PARTS.get(PartKind.TRAY).get().defaultBlockState();
            case COIL -> WMBlocks.PARTS.get(PartKind.COIL).get().defaultBlockState();
            case STAGE -> WMBlocks.PARTS.get(PartKind.STAGE).get().defaultBlockState();
            case INTERCOOLER -> WMBlocks.PARTS.get(PartKind.INTERCOOLER).get().defaultBlockState();
            case BAY -> WMBlocks.PARTS.get(PartKind.BAY).get().defaultBlockState();
            case BODY -> WMBlocks.PARTS.get(PartKind.BODY).get().defaultBlockState();
            case CAP -> WMBlocks.PARTS.get(PartKind.CAP).get().defaultBlockState();
            case AIR -> net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        };
    }
}
