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
        assertRecipe("cycle_kerosene.json", "cycle_island", "pneumaticcraft:kerosene", "\"air\": -220");
        assertRecipe("cycle_gasoline.json", "cycle_island", "pneumaticcraft:gasoline", "\"air\": -240");
        assertRecipe("cycle_lpg.json", "cycle_island", "pneumaticcraft:lpg", "\"air\": -260");
        assertRecipe("cycle_diesel_oxygen.json", "cycle_island", "pneumaticcraft:diesel", "workingmedium:oxygen", "\"air\": -280");
        assertRecipe("crush_cobble.json", "impact_crusher", "minecraft:cobblestone", "workingmedium:stone_dust");
        assertRecipe("crush_stone.json", "impact_crusher", "minecraft:stone", "workingmedium:stone_dust");
        assertRecipe("crush_gravel.json", "impact_crusher", "minecraft:gravel", "workingmedium:stone_dust");
        assertRecipe("clay_dust.json", "lube_mixer", "workingmedium:stone_dust", "\"item_counts\": [4]", "minecraft:clay_ball");
        String cobble = Files.readString(recipe("stone_dust_block.json"));
        assertTrue(cobble.contains("workingmedium:stone_dust"), cobble);
        assertTrue(cobble.contains("minecraft:cobblestone"), cobble);
        assertRecipe("electro_water.json", "electrolyzer", "minecraft:water", "workingmedium:hydrogen", "workingmedium:oxygen");
        assertRecipe("cycle_kerosene_oxygen.json", "cycle_island", "pneumaticcraft:kerosene", "workingmedium:oxygen", "\"air\": -300");
        assertRecipe("cycle_gasoline_oxygen.json", "cycle_island", "pneumaticcraft:gasoline", "workingmedium:oxygen", "\"air\": -320");
        assertRecipe("cycle_lpg_oxygen.json", "cycle_island", "pneumaticcraft:lpg", "workingmedium:oxygen", "\"air\": -340");
        assertRecipe("brine_dried.json", "lube_mixer", "minecraft:dried_kelp", "workingmedium:brine");
        assertRecipe("crush_netherrack.json", "impact_crusher", "minecraft:netherrack", "workingmedium:stone_dust", "minecraft:gold_nugget");
        assertRecipe("crush_deepslate.json", "impact_crusher", "minecraft:deepslate", "workingmedium:stone_dust");
        assertRecipe("crush_cobbled_deepslate.json", "impact_crusher", "minecraft:cobbled_deepslate", "workingmedium:stone_dust");
        assertRecipe("crush_blackstone.json", "impact_crusher", "minecraft:blackstone", "workingmedium:stone_dust");
        assertRecipe("crush_andesite.json", "impact_crusher", "minecraft:andesite", "workingmedium:stone_dust");
        assertRecipe("crush_diorite.json", "impact_crusher", "minecraft:diorite", "workingmedium:stone_dust");
        assertRecipe("crush_granite.json", "impact_crusher", "minecraft:granite", "workingmedium:stone_dust");
        assertRecipe("crush_tuff.json", "impact_crusher", "minecraft:tuff", "workingmedium:stone_dust");
        assertRecipe("crush_basalt.json", "impact_crusher", "minecraft:basalt", "workingmedium:stone_dust");
        assertRecipe("crush_calcite.json", "impact_crusher", "minecraft:calcite", "workingmedium:stone_dust");
        assertRecipe("crush_end_stone.json", "impact_crusher", "minecraft:end_stone", "workingmedium:stone_dust");
        assertRecipe("crush_iron_ore.json", "impact_crusher", "minecraft:iron_ore", "workingmedium:crushed_iron", "\"count\": 2");
        assertRecipe("crush_deepslate_iron_ore.json", "impact_crusher", "minecraft:deepslate_iron_ore", "workingmedium:crushed_iron");
        assertRecipe("crush_copper_ore.json", "impact_crusher", "minecraft:copper_ore", "workingmedium:crushed_copper");
        assertRecipe("crush_gold_ore.json", "impact_crusher", "minecraft:gold_ore", "workingmedium:crushed_gold");
        assertRecipe("crush_nether_gold_ore.json", "impact_crusher", "minecraft:nether_gold_ore", "workingmedium:crushed_gold", "minecraft:gold_nugget");
        assertRecipe("kiln_glass.json", "residue_kiln", "minecraft:sand", "minecraft:glass", "\"count\": 3");
        assertRecipe("quench_ice.json", "quench_well", "minecraft:water", "workingmedium:liquid_co2", "minecraft:packed_ice");
        assertRecipe("quench_blue_ice.json", "quench_well", "minecraft:packed_ice", "workingmedium:liquid_co2", "minecraft:blue_ice");
        assertRecipe("cycle_oil.json", "cycle_island", "pneumaticcraft:oil", "\"air\": -160");
        assertRecipe("cycle_oil_oxygen.json", "cycle_island", "pneumaticcraft:oil", "workingmedium:oxygen", "\"air\": -220");
        assertRecipe("neutralize_acid.json", "scrubber_tower", "workingmedium:hydrochloric", "workingmedium:caustic", "workingmedium:brine");
        String asphalt = Files.readString(recipe("asphalt_mix.json"));
        org.junit.jupiter.api.Assertions.assertTrue(asphalt.contains("workingmedium:stone_dust"), asphalt);
        org.junit.jupiter.api.Assertions.assertTrue(asphalt.contains("workingmedium:heavy_wax"), asphalt);
        org.junit.jupiter.api.Assertions.assertTrue(asphalt.contains("workingmedium:asphalt"), asphalt);
        assertRecipe("crush_coal_ore.json", "impact_crusher", "minecraft:coal_ore", "minecraft:coal", "\"count\": 3");
        assertRecipe("crush_deepslate_coal_ore.json", "impact_crusher", "minecraft:deepslate_coal_ore", "minecraft:coal");
        assertRecipe("crush_redstone_ore.json", "impact_crusher", "minecraft:redstone_ore", "minecraft:redstone", "\"count\": 6");
        assertRecipe("crush_deepslate_redstone_ore.json", "impact_crusher", "minecraft:deepslate_redstone_ore", "minecraft:redstone");
        assertRecipe("crush_lapis_ore.json", "impact_crusher", "minecraft:lapis_ore", "minecraft:lapis_lazuli", "\"count\": 8");
        assertRecipe("crush_deepslate_lapis_ore.json", "impact_crusher", "minecraft:deepslate_lapis_ore", "minecraft:lapis_lazuli");
        assertRecipe("crush_diamond_ore.json", "impact_crusher", "minecraft:diamond_ore", "minecraft:diamond", "\"count\": 2");
        assertRecipe("crush_deepslate_diamond_ore.json", "impact_crusher", "minecraft:deepslate_diamond_ore", "minecraft:diamond");
        assertRecipe("crush_emerald_ore.json", "impact_crusher", "minecraft:emerald_ore", "minecraft:emerald");
        assertRecipe("crush_deepslate_emerald_ore.json", "impact_crusher", "minecraft:deepslate_emerald_ore", "minecraft:emerald");
        assertRecipe("crush_nether_quartz_ore.json", "impact_crusher", "minecraft:nether_quartz_ore", "minecraft:quartz", "\"count\": 3");
        assertRecipe("crush_glowstone.json", "impact_crusher", "minecraft:glowstone", "minecraft:glowstone_dust", "\"count\": 4");
        assertRecipe("crush_amethyst_block.json", "impact_crusher", "minecraft:amethyst_block", "minecraft:amethyst_shard");
        assertRecipe("crush_quartz_block.json", "impact_crusher", "minecraft:quartz_block", "minecraft:quartz");
        assertRecipe("kiln_red_sand.json", "residue_kiln", "minecraft:red_sand", "minecraft:glass", "\"count\": 3");
        assertRecipe("kiln_brick.json", "residue_kiln", "minecraft:clay_ball", "minecraft:brick", "\"count\": 3");
        assertRecipe("kiln_nether_brick.json", "residue_kiln", "minecraft:netherrack", "minecraft:nether_brick");
        assertRecipe("quench_obsidian.json", "quench_well", "minecraft:lava", "workingmedium:liquid_co2", "minecraft:obsidian");
        assertRecipe("wash_gravel.json", "washer_tower", "minecraft:gravel", "minecraft:flint", "workingmedium:stone_dust");
        assertRecipe("cycle_monomer.json", "cycle_island", "workingmedium:monomer", "\"air\": -140");
        assertRecipe("cycle_plasticizer.json", "cycle_island", "workingmedium:plasticizer", "\"air\": -120");
        assertRecipe("crush_sandstone.json", "impact_crusher", "minecraft:sandstone", "minecraft:sand", "\"count\": 4");
        assertRecipe("crush_red_sandstone.json", "impact_crusher", "minecraft:red_sandstone", "minecraft:red_sand");
        assertRecipe("crush_bone_block.json", "impact_crusher", "minecraft:bone_block", "minecraft:bone_meal", "\"count\": 9");
        assertRecipe("wash_sand.json", "washer_tower", "minecraft:sand", "minecraft:clay_ball");
        assertRecipe("wash_red_sand.json", "washer_tower", "minecraft:red_sand", "minecraft:clay_ball");
        assertRecipe("wash_concrete_white.json", "washer_tower", "minecraft:white_concrete_powder", "minecraft:white_concrete");
        assertRecipe("wash_concrete_black.json", "washer_tower", "minecraft:black_concrete_powder", "minecraft:black_concrete");
        assertRecipe("kiln_stone.json", "residue_kiln", "minecraft:cobblestone", "minecraft:stone", "\"count\": 3");
        assertRecipe("kiln_sponge.json", "residue_kiln", "minecraft:wet_sponge", "minecraft:sponge");
        assertRecipe("kiln_terracotta.json", "residue_kiln", "minecraft:clay", "minecraft:terracotta");
        assertRecipe("quench_obsidian_water.json", "quench_well", "minecraft:lava", "minecraft:water", "minecraft:obsidian", "\"count\": 1");
        assertRecipe("cycle_wax.json", "cycle_island", "workingmedium:heavy_wax", "workingmedium:flue_gas", "\"air\": -150");
        assertRecipe("neutralize_feed.json", "scrubber_tower", "workingmedium:acid_feed", "workingmedium:caustic", "workingmedium:brine");
    }

    @Test
    void langFilesStayInParityAndCoverNewKeys() throws Exception {
        Set<String> en = keys("en_us.json");
        Set<String> zh = keys("zh_cn.json");
        assertEquals(en, zh);
        for (String status : new String[] {"unformed", "idle", "running", "window", "pressure", "temperature", "clogged",
                "flood", "coking", "frozen", "leak", "no_lube", "no_vent", "no_air", "missing_part", "scrapped", "certified",
                "paused", "no_drain"}) {
            assertTrue(en.contains("hint.workingmedium." + status), status);
            assertTrue(en.contains("status.workingmedium." + status), "status " + status);
        }
        for (int page = 0; page < 21; page++) {
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
                "message.workingmedium.certify_unformed", "message.workingmedium.cannot_certify",
                "jei.workingmedium.info.cryo_fluid", "jei.workingmedium.info.purge", "jei.workingmedium.info.flare",
                "jei.workingmedium.info.hatch_drain", "jei.workingmedium.info.hatch_io",
                "jei.workingmedium.info.redstone_pause", "jei.workingmedium.info.stone_dust",
                "jei.workingmedium.info.scald_fluid", "jei.workingmedium.info.flammable_fluid",
                "jei.workingmedium.info.oxidizer_fluid", "jei.workingmedium.info.probe",
                "tooltip.workingmedium.probe", "message.workingmedium.rotate_formed",
                "message.workingmedium.probe_unbound"}) {
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
