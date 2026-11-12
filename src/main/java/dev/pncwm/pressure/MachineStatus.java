package dev.pncwm.pressure;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum MachineStatus {
    UNFORMED("unformed", 0xFF888888),
    IDLE("idle", 0xFFAAAAAA),
    RUNNING("running", 0xFF55DD77),
    WINDOW("window", 0xFF3DDC97),
    PRESSURE("pressure", 0xFFE0C040),
    TEMPERATURE("temperature", 0xFFE07030),
    CLOGGED("clogged", 0xFFCC5555),
    FLOOD("flood", 0xFF4488CC),
    COKING("coking", 0xFF664422),
    FROZEN("frozen", 0xFF99EEFF),
    LEAK("leak", 0xFFDD6677),
    NO_LUBE("no_lube", 0xFFCCAA44),
    NO_VENT("no_vent", 0xFFFF6655),
    NO_AIR("no_air", 0xFF8899AA),
    MISSING_PART("missing_part", 0xFFAA88CC),
    SCRAPPED("scrapped", 0xFFAA3333),
    CERTIFIED("certified", 0xFFE8F4FF),
    PAUSED("paused", 0xFFD0A050);

    public final String id;
    public final int color;

    MachineStatus(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public MutableComponent text() {
        return Component.translatable("status.workingmedium." + id);
    }

    public static MachineStatus byOrdinal(int ordinal) {
        MachineStatus[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return IDLE;
        }
        return values[ordinal];
    }
}
