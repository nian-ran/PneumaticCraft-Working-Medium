package dev.pncwm.pressure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class WindowBoundsTest {
    @Test
    void criticalKettleUsesTheShippedWindow() throws Exception {
        String json = Files.readString(recipe("critical_window.json"));
        assertTrue(json.contains("\"machine\": \"critical_kettle\""), json);
        assertTrue(json.contains("\"fluid\": \"workingmedium:liquid_co2\""), json);
        assertTrue(json.contains("\"item\": \"workingmedium:critical_core\""), json);
        assertTrue(json.contains("\"void_outside\": true"), json);
        assertTrue(json.contains("workingmedium:microfluidic_chip"), json);
        assertTrue(json.contains("\"item_counts\""), json);

        float pressureMin = field(json, "pressure_min");
        float pressureMax = field(json, "pressure_max");
        int tempMin = (int) field(json, "temp_min");
        int tempMax = (int) field(json, "temp_max");
        assertEquals(70f, pressureMin);
        assertEquals(80f, pressureMax);
        assertEquals(25, tempMin);
        assertEquals(40, tempMax);

        assertTrue(WindowBounds.contains(pressureMin, pressureMax, tempMin, tempMax, 75f, 30, 0));
        assertFalse(WindowBounds.contains(pressureMin, pressureMax, tempMin, tempMax, 69f, 41, 0));

        double expert = WindowBounds.EXPERT_FLOOR;
        float openLow = WindowBounds.edge(pressureMin, pressureMax, true, 0);
        float tightLow = WindowBounds.edge(pressureMin, pressureMax, true, expert);
        assertTrue(tightLow > openLow, "expert tightness must shrink the pressure band");
        float between = (openLow + tightLow) / 2f;
        assertTrue(WindowBounds.contains(pressureMin, pressureMax, tempMin, tempMax, between, 30, 0));
        assertFalse(WindowBounds.contains(pressureMin, pressureMax, tempMin, tempMax, between, 30, expert));
    }

    @Test
    void copperAndGoldWashAndThermolysisHaveASecondOutput() throws Exception {
        assertSecondOutput(recipe("wash_copper.json"), "washer_tower", "purified_copper", "iron_nugget");
        assertSecondOutput(recipe("wash_gold.json"), "washer_tower", "purified_gold", "iron_nugget");
        assertSecondOutput(recipe("thermo_copper.json"), "thermolysis_kettle", "copper_ingot", "slag");
        assertSecondOutput(recipe("thermo_gold.json"), "thermolysis_kettle", "gold_ingot", "slag");
    }

    private static void assertSecondOutput(Path path, String machine, String primary, String byproduct) throws Exception {
        String json = Files.readString(path);
        assertTrue(json.contains("\"machine\": \"" + machine + "\""), path.toString());
        assertTrue(json.contains(primary), path.toString());
        assertTrue(json.contains(byproduct), path.toString());
        assertTrue(json.contains("\"type\": \"workingmedium:window\""), path.toString());
    }

    private static Path recipe(String name) {
        return Path.of("src/main/resources/data/workingmedium/recipe").resolve(name);
    }

    private static float field(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyAt = json.indexOf(marker);
        assertTrue(keyAt >= 0, key);
        int cursor = json.indexOf(':', keyAt) + 1;
        while (cursor < json.length() && "0123456789.-".indexOf(json.charAt(cursor)) < 0) {
            cursor++;
        }
        int end = cursor;
        while (end < json.length() && "0123456789.-".indexOf(json.charAt(end)) >= 0) {
            end++;
        }
        return Float.parseFloat(json.substring(cursor, end));
    }
}
