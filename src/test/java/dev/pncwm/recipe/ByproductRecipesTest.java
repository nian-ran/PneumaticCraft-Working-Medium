package dev.pncwm.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ByproductRecipesTest {
    @Test
    void byproductSinksAreShipped() throws Exception {
        assertRecipe("neutralize.json", "scrubber_tower", "workingmedium:acid_mist", "workingmedium:caustic", "workingmedium:brine");
        assertRecipe("steam_cyclone.json", "cyclone", "workingmedium:dusty_steam", "minecraft:water", "workingmedium:stone_dust");
        assertRecipe("electrowin.json", "electrolyzer", "workingmedium:byproduct_solution", "minecraft:copper_ingot", "workingmedium:hydrochloric");
        assertRecipe("cycle_hydrogen.json", "cycle_island", "workingmedium:hydrogen", "workingmedium:oxygen", "\"air\": -240");
        assertRecipe("crush_slag.json", "impact_crusher", "workingmedium:slag", "workingmedium:stone_dust", "minecraft:iron_nugget");
        assertRecipe("roast_cake.json", "residue_kiln", "workingmedium:filter_cake", "\"item_counts\": [2]", "workingmedium:metal_ash");
        String substrate = Files.readString(recipe("substrate_plastic.json"));
        assertTrue(substrate.contains("workingmedium:engineering_plastic"), substrate);
        assertTrue(substrate.contains("\"count\": 2"), substrate);
        assertRecipe("inert_sinter.json", "sinter_kiln", "workingmedium:metal_ash", "workingmedium:inerts", "\"count\": 2");
        assertRecipe("reclaim_wafer.json", "sinter_kiln", "workingmedium:waste_chip", "\"item_counts\": [4]", "workingmedium:substrate");
        assertRecipe("combine_acid.json", "electrolyzer", "workingmedium:hydrogen", "workingmedium:chlorine", "workingmedium:hydrochloric");
        assertRecipe("leach_copper.json", "leach_line", "workingmedium:purified_copper", "minecraft:copper_ingot", "\"count\": 3");
        assertRecipe("leach_gold.json", "leach_line", "workingmedium:purified_gold", "minecraft:gold_ingot", "\"count\": 3");
        assertRecipe("wash_ancient.json", "washer_tower", "workingmedium:crushed_ancient", "minecraft:netherite_scrap");
        assertRecipe("quench_cryo.json", "quench_well", "workingmedium:ceramic_alloy", "workingmedium:liquid_co2", "workingmedium:cryo_alloy");
        String critical = Files.readString(recipe("critical_window.json"));
        assertTrue(critical.contains("workingmedium:microfluidic_chip"), critical);
        assertTrue(critical.contains("\"item_counts\""), critical);
    }

    @Test
    void langFilesStayInParityAndCoverNewKeys() throws Exception {
        Set<String> en = keys("en_us.json");
        Set<String> zh = keys("zh_cn.json");
        assertEquals(en, zh);
        for (String status : new String[] {"unformed", "idle", "running", "window", "pressure", "temperature", "clogged",
                "flood", "coking", "frozen", "leak", "no_lube", "no_vent", "no_air", "missing_part", "scrapped", "certified",
                "paused"}) {
            assertTrue(en.contains("hint.workingmedium." + status), status);
            assertTrue(en.contains("status.workingmedium." + status), "status " + status);
        }
        for (int page = 0; page < 16; page++) {
            assertTrue(en.contains("manual.workingmedium.page." + page), "page " + page);
        }
        for (String key : new String[] {"tooltip.workingmedium.certificate", "tooltip.workingmedium.certified",
                "tooltip.workingmedium.asphalt", "tooltip.workingmedium.pump", "tooltip.workingmedium.slide",
                "tooltip.workingmedium.nozzle", "tooltip.workingmedium.cylinder", "tooltip.workingmedium.diaphragm",
                "tooltip.workingmedium.gripper", "tooltip.workingmedium.electrode", "tooltip.workingmedium.cleaned",
                "tooltip.workingmedium.dirty_chip", "tooltip.workingmedium.program_sheet",
                "jei.workingmedium.info.asphalt", "jei.workingmedium.info.pump", "jei.workingmedium.info.slide",
                "jei.workingmedium.info.nozzle", "jei.workingmedium.info.cylinder", "jei.workingmedium.info.diaphragm",
                "jei.workingmedium.info.gripper", "jei.workingmedium.info.electrode", "jei.workingmedium.info.chip",
                "jei.workingmedium.info.program_sheet", "jei.workingmedium.info.hazard_fluid",
                "jei.workingmedium.info.hatch_heat", "jei.workingmedium.info.hatch_gauge",
                "jei.workingmedium.air_produce", "jei.workingmedium.air_cost",
                "message.workingmedium.certified", "message.workingmedium.already_certified",
                "message.workingmedium.certify_unformed"}) {
            assertTrue(en.contains(key), key);
        }
    }

    private static void assertRecipe(String name, String machine, String... needles) throws Exception {
        String json = Files.readString(recipe(name));
        assertTrue(json.contains("\"machine\": \"" + machine + "\""), json);
        for (String needle : needles) {
            assertTrue(json.contains(needle), name + " lacks " + needle);
        }
    }

    private static Set<String> keys(String file) throws Exception {
        String json = Files.readString(Path.of("src/main/resources/assets/workingmedium/lang").resolve(file));
        Matcher matcher = Pattern.compile("(?m)^\\s*\"([^\"]+)\"\\s*:").matcher(json);
        Set<String> keys = new TreeSet<>();
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    private static Path recipe(String name) {
        return Path.of("src/main/resources/data/workingmedium/recipe").resolve(name);
    }
}
