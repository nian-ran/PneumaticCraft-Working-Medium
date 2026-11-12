package dev.pncwm.block;

import net.minecraft.world.level.block.Block;

public class PartBlock extends Block {
    public final PartKind kind;

    public PartBlock(PartKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }
}
