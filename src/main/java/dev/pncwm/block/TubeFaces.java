package dev.pncwm.block;

import dev.pncwm.apparatus.ApparatusKind;
import dev.pncwm.apparatus.Family;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which neighbour faces a working-medium tube may grow an arm toward.
 * This follows the same faces the air capability actually exposes.
 */
public final class TubeFaces {
    private TubeFaces() {
    }

    /**
     * {@code faceOnNeighbor} is the face of the neighbour that touches the tube
     * (the opposite of the direction the tube is looking).
     */
    public static boolean connectsTo(BlockState neighbor, Direction faceOnNeighbor) {
        Block block = neighbor.getBlock();
        if (block instanceof GradeTubeBlock || block instanceof GaugeBlock || block instanceof HatchBlock) {
            return true;
        }
        if (block instanceof ReliefValveBlock) {
            return isReliefAirFace(neighbor.getValue(HorizontalDirectionalBlock.FACING), faceOnNeighbor);
        }
        if (block instanceof TransferBlock) {
            return isTransferPort(neighbor.getValue(HorizontalDirectionalBlock.FACING), faceOnNeighbor);
        }
        if (block instanceof ApparatusBlock apparatus) {
            return isApparatusAirFace(apparatus.kind, neighbor.getValue(HorizontalDirectionalBlock.FACING), faceOnNeighbor);
        }
        return false;
    }

    /** Every face except the vent (the horizontal facing) can take a tube. */
    public static boolean isReliefAirFace(Direction vent, Direction faceOnValve) {
        return TubeFaceRules.isReliefAirFace(vent.getSerializedName(), faceOnValve.getSerializedName());
    }

    /** Only the front (outlet) and back (inlet) of a reducer or booster take a tube. */
    public static boolean isTransferPort(Direction facing, Direction faceOnTransfer) {
        return TubeFaceRules.isTransferPort(facing.getSerializedName(), facing.getOpposite().getSerializedName(),
                faceOnTransfer.getSerializedName());
    }

    /** Compressor/expander columns and the regulator isolate their back face from the air network. */
    public static boolean isApparatusAirFace(ApparatusKind kind, Direction facing, Direction faceOnApparatus) {
        boolean isolatedBack = kind.family == Family.COLUMN || kind.family == Family.REGULATOR;
        return TubeFaceRules.isApparatusAirFace(isolatedBack, facing.getOpposite().getSerializedName(),
                faceOnApparatus.getSerializedName());
    }
}
