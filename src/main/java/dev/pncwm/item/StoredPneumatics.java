package dev.pncwm.item;

/**
 * Air and temperature written onto a dropped tube, gauge, relief valve, reducer or booster.
 * {@code secondAir} is the outlet side of a reducer or booster; it stays 0 on single-handler blocks.
 */
public record StoredPneumatics(int air, int secondAir, double temperature) {
    public static final double AMBIENT_KELVIN = 300.0;

    public static StoredPneumatics of(int air, double temperature) {
        return new StoredPneumatics(air, 0, temperature);
    }

    public static StoredPneumatics transfer(int inlet, int outlet, double temperature) {
        return new StoredPneumatics(inlet, outlet, temperature);
    }

    public boolean isEmpty() {
        return isEmpty(AMBIENT_KELVIN);
    }

    public boolean isEmpty(double ambientKelvin) {
        return air == 0 && secondAir == 0 && Math.abs(temperature - ambientKelvin) < 0.5;
    }

    /**
     * Tooltip key for air stored on a controller item. {@code null} when there is no air or vacuum
     * to show. A stored vacuum used to be kept on the item but omitted from the tooltip because
     * the text was only added when the amount was positive.
     */
    public static String controllerAirTooltipKey(int air) {
        if (air < 0) {
            return "tooltip.workingmedium.stored_controller_vacuum";
        }
        if (air > 0) {
            return "tooltip.workingmedium.stored_controller_air";
        }
        return null;
    }

    /** Tooltip key for a single air handler (tube, gauge or relief valve). {@code null} when empty. */
    public static String storedAirTooltipKey(int air, boolean hasVolume) {
        if (air < 0) {
            return hasVolume ? "tooltip.workingmedium.stored_vacuum" : "tooltip.workingmedium.stored_vacuum_amount";
        }
        if (air > 0) {
            return hasVolume ? "tooltip.workingmedium.stored_air" : "tooltip.workingmedium.stored_air_amount";
        }
        return null;
    }

    /**
     * Tooltip key for a reducer or booster. Mixed air and vacuum keeps signed amounts, so one
     * side can stay positive while the other is a vacuum.
     */
    public static String transferTooltipKey(int inlet, int outlet) {
        if ((inlet < 0 && outlet <= 0) || (outlet < 0 && inlet <= 0)) {
            return "tooltip.workingmedium.stored_transfer_vacuum";
        }
        if (inlet < 0 || outlet < 0) {
            return "tooltip.workingmedium.stored_transfer_signed";
        }
        return "tooltip.workingmedium.stored_transfer";
    }

    public static String pendingAirTooltipKey(int air) {
        return air < 0 ? "tooltip.workingmedium.pending_vacuum" : "tooltip.workingmedium.pending_air";
    }

    /** Count shown in a tooltip. Vacuum keys use the magnitude, never a leading minus. */
    public static int displayAmount(int air) {
        return Math.abs(air);
    }

    public static String displayBar(int air, int volume) {
        if (volume <= 0) {
            return "0.00";
        }
        return String.format("%.2f", Math.abs(air) / (float) volume);
    }

    public static String signedBar(int air, int volume) {
        if (volume <= 0) {
            return "0.00";
        }
        return String.format("%.2f", air / (float) volume);
    }

    /**
     * Whether a rebuilt air handler should receive this amount. Vacuum is a real stored
     * quantity; a positive-only check used to drop it when the pressure tier changed.
     */
    public static boolean hasCarriedAir(int currentAir) {
        return currentAir != 0;
    }

    /**
     * GUI key for air still waiting on an unformed controller. {@code null} means the
     * ordinary pressure reading should be shown. The manometer already reported held
     * vacuum; the machine screen used to keep showing 0.00 bar.
     */
    public static String pendingGuiTooltipKey(boolean formed, int pendingAir) {
        if (formed || pendingAir == 0) {
            return null;
        }
        return pendingAir < 0
                ? "tooltip.workingmedium.pending_vacuum_short"
                : "tooltip.workingmedium.pending_air_short";
    }
}
