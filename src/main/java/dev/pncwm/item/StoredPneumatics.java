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
}
