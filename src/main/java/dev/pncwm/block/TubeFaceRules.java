package dev.pncwm.block;

/**
 * Direction-name rules for tube arms. Kept free of Minecraft types so unit tests can cover them.
 */
public final class TubeFaceRules {
    private TubeFaceRules() {
    }

    /** Every face except the vent can take a tube. */
    public static boolean isReliefAirFace(String vent, String faceOnValve) {
        return !vent.equals(faceOnValve);
    }

    /** Only the front (outlet) and back (inlet) of a reducer or booster take a tube. */
    public static boolean isTransferPort(String facing, String opposite, String faceOnTransfer) {
        return faceOnTransfer.equals(facing) || faceOnTransfer.equals(opposite);
    }

    /** Compressor/expander columns and the regulator isolate their back face from the air network. */
    public static boolean isApparatusAirFace(boolean isolatedBack, String backFace, String faceOnApparatus) {
        return !isolatedBack || !faceOnApparatus.equals(backFace);
    }
}
