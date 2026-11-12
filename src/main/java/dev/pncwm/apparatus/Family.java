package dev.pncwm.apparatus;

public enum Family {
    /** 5x5x5 hollow shell (cyclone is a taller 5x7x5). */
    CHAMBER,
    /** 5x5 base, 3x3 tray shaft, cap. Height is the tray count. */
    TOWER,
    /** Vertical 3x3 jacketed column of stages and intercoolers. */
    COLUMN,
    /** 3x3 hollow tunnel of bays. Length is the station count. */
    GALLERY,
    /** 5x5 hollow vessel with a body core and a cap. */
    KETTLE,
    /** Hollow room. Interior span is at least 5 (7x7x7 outer). */
    ROOM,
    /** Solid casing bank, at least 5x5x5. Volume is the casing count. */
    STORAGE,
    /** 3x3 base and a tall 1-block shaft that burns light gas. */
    FLARE,
    /** Item line: a 3x3 tunnel of bays. */
    MANIFOLD,
    /** Heat carried along a 3x3 gallery. Resistance grows with length. */
    HEAT_LINE,
    /** 3x3 vertical well. The quench fluid decides the alloy grade. */
    QUENCH,
    /** Charging bay (5x3x5 shell) or a 7x7 drone pad with posts. */
    CHARGE,
    /** Rectangular loop of kettle bodies that returns unused carbon dioxide. */
    LOOP,
    /** 5x5 casing ring. Fuel in, pressure and heat out. */
    CYCLE,
    /** Fixed 5x3x5 solid skid: reducer, buffer, probe, relief. */
    REGULATOR,
    /** 5x5 pad with a backboard. Watches three bound machines. */
    CALIBRATION
}
