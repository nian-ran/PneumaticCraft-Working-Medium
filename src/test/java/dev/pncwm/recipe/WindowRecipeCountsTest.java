package dev.pncwm.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class WindowRecipeCountsTest {
    @Test
    void listedZeroMeansHeldNotConsumed() {
        List<Integer> counts = List.of(0, 1);
        assertEquals(1, RecipeCounts.present(counts, 0));
        assertEquals(0, RecipeCounts.consume(counts, 0));
        assertEquals(1, RecipeCounts.present(counts, 1));
        assertEquals(1, RecipeCounts.consume(counts, 1));
        assertEquals(1, RecipeCounts.present(counts, 2));
        assertEquals(1, RecipeCounts.consume(counts, 2));
    }

    @Test
    void polymerRecipesAndNewChainsAreShipped() throws Exception {
        String polymer = Files.readString(recipe("polymer.json"));
        assertTrue(polymer.contains("\"item_counts\": [0]"), polymer);
        assertTrue(polymer.contains("workingmedium:catalyst"), polymer);

        String boosted = Files.readString(recipe("polymer_plasticized.json"));
        assertTrue(boosted.contains("\"item_counts\": [0, 1]"), boosted);
        assertTrue(boosted.contains("workingmedium:plasticizer"), boosted);
        assertTrue(boosted.contains("\"count\": 3"), boosted);

        String plasticize = Files.readString(recipe("plasticize.json"));
        assertTrue(plasticize.contains("\"machine\": \"lube_mixer\""), plasticize);
        assertTrue(plasticize.contains("workingmedium:plasticizer"), plasticize);

        String crush = Files.readString(recipe("crush_ancient.json"));
        assertTrue(crush.contains("minecraft:ancient_debris"), crush);
        assertTrue(crush.contains("workingmedium:crushed_ancient"), crush);

        String thermo = Files.readString(recipe("thermo_ancient.json"));
        assertTrue(thermo.contains("workingmedium:crushed_ancient"), thermo);
        assertTrue(thermo.contains("minecraft:netherite_scrap"), thermo);
    }

    private static Path recipe(String name) {
        return Path.of("src/main/resources/data/workingmedium/recipe").resolve(name);
    }
}
