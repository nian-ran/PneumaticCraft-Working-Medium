package dev.pncwm.apparatus;

import com.mojang.serialization.Codec;

import dev.pncwm.pressure.PressureGrade;
import net.minecraft.util.StringRepresentable;

public enum ApparatusKind implements StringRepresentable {
    IMPACT_CRUSHER("impact_crusher", PressureGrade.G0, Family.CHAMBER, 1, 1, true),
    GAS_BANK("gas_bank", PressureGrade.G0, Family.STORAGE, 5, 7, false),

    RESIDUE_KILN("residue_kiln", PressureGrade.G1, Family.KETTLE, 3, 5, true),
    LUBE_MIXER("lube_mixer", PressureGrade.G1, Family.KETTLE, 3, 6, true),
    FLARE_STACK("flare_stack", PressureGrade.G1, Family.FLARE, 8, 16, false),

    WASHER_TOWER("washer_tower", PressureGrade.G2, Family.TOWER, 6, 10, true),
    THERMOLYSIS_KETTLE("thermolysis_kettle", PressureGrade.G2, Family.KETTLE, 3, 5, true),
    COMPRESSOR_COLUMN("compressor_column", PressureGrade.G2, Family.COLUMN, 4, 8, true),
    REGULATOR_SKID("regulator_skid", PressureGrade.G2, Family.REGULATOR, 1, 1, true),
    CYCLONE("cyclone", PressureGrade.G2, Family.CHAMBER, 1, 1, false),
    ITEM_GALLERY("item_gallery", PressureGrade.G2, Family.MANIFOLD, 6, 10, false),

    FRACTION_TOWER("fraction_tower", PressureGrade.G3, Family.TOWER, 8, 12, true),
    CRACKER("cracker", PressureGrade.G3, Family.GALLERY, 6, 10, true),
    ELECTROLYZER("electrolyzer", PressureGrade.G3, Family.KETTLE, 3, 5, true),
    POLYMERIZER("polymerizer", PressureGrade.G3, Family.KETTLE, 3, 6, true),
    LEACH_LINE("leach_line", PressureGrade.G3, Family.GALLERY, 6, 8, true),
    ALLOY_KETTLE("alloy_kettle", PressureGrade.G3, Family.KETTLE, 3, 6, true),
    SINTER_KILN("sinter_kiln", PressureGrade.G3, Family.KETTLE, 3, 4, false),
    QUENCH_WELL("quench_well", PressureGrade.G3, Family.QUENCH, 4, 8, false),
    HEAT_GALLERY("heat_gallery", PressureGrade.G3, Family.HEAT_LINE, 8, 12, false),

    SCRUBBER_TOWER("scrubber_tower", PressureGrade.G4, Family.TOWER, 6, 10, true),
    EXPANDER_COLUMN("expander_column", PressureGrade.G4, Family.COLUMN, 4, 8, true),
    CRYO_TOWER("cryo_tower", PressureGrade.G4, Family.TOWER, 8, 12, true),
    LAMINATION_GALLERY("lamination_gallery", PressureGrade.G4, Family.GALLERY, 6, 10, true),
    CLEAN_ROOM("clean_room", PressureGrade.G4, Family.ROOM, 5, 7, true),
    CYCLE_ISLAND("cycle_island", PressureGrade.G4, Family.CYCLE, 1, 1, false),
    CHARGING_STATION("charging_station", PressureGrade.G4, Family.CHARGE, 1, 4, false),
    DRONE_NEST("drone_nest", PressureGrade.G4, Family.CHARGE, 1, 6, false),

    CRITICAL_KETTLE("critical_kettle", PressureGrade.G5, Family.KETTLE, 3, 5, true),
    CALIBRATION_BENCH("calibration_bench", PressureGrade.G5, Family.CALIBRATION, 1, 1, true),
    FLUID_LOOP("fluid_loop", PressureGrade.G5, Family.LOOP, 5, 7, false);

    public final String id;
    public final PressureGrade grade;
    public final Family family;
    public final int minSize;
    public final int maxSize;
    public static final Codec<ApparatusKind> CODEC = StringRepresentable.fromEnum(ApparatusKind::values);

    public final boolean required;

    ApparatusKind(String id, PressureGrade grade, Family family, int minSize, int maxSize, boolean required) {
        this.id = id;
        this.grade = grade;
        this.family = family;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.required = required;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public boolean usesRecipes() {
        return switch (family) {
            case CHAMBER, TOWER, GALLERY, KETTLE, ROOM, QUENCH, CYCLE -> true;
            default -> false;
        };
    }

    public int minimumSize() {
        if (family == Family.TOWER) {
            return minSize + dev.pncwm.WMConfig.towerExtra();
        }
        return minSize;
    }
}
