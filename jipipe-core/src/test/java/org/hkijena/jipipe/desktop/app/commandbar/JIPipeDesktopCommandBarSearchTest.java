/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.commandbar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the fuzzy matching of the command bar
 * ({@link JIPipeDesktopCommandBarSearch}).
 */
class JIPipeDesktopCommandBarSearchTest {

    @Test
    void ladderRanksExactAbovePrefixAboveSubstringAboveFuzzy() {
        // The brief's chain: score("copy") > score("cop") > score("cpy") > 0.
        // "cpy" is not a substring of "copy", so it earns the fuzzy rung
        // (jaroWinkler-based), which the ladder places below substring.
        double exact = JIPipeDesktopCommandBarSearch.score("copy", "Copy");
        double prefix = JIPipeDesktopCommandBarSearch.score("cop", "Copy");
        double substring = JIPipeDesktopCommandBarSearch.score("opy", "Copy");
        double fuzzy = JIPipeDesktopCommandBarSearch.score("cpy", "Copy");
        double none = JIPipeDesktopCommandBarSearch.score("xyz", "Copy");
        assertEquals(1.0, exact, 1e-9);
        assertEquals(0.9, prefix, 1e-9);
        assertEquals(0.8, substring, 1e-9);
        assertEquals(0.0, none, 1e-9);
        assertTrue(exact > prefix, "exact above prefix");
        assertTrue(prefix > substring, "prefix above substring");
        assertTrue(substring > fuzzy, "substring above fuzzy");
        assertTrue(fuzzy > 0.0, "fuzzy above zero (the brief's cpy rank)");
    }

    @Test
    void acronymViaWordPrefixes() {
        // "cb" is not a word prefix of either "command" or "bar"; it matches
        // because its letters are the first letters of the words, in order.
        assertTrue(JIPipeDesktopCommandBarSearch.matches("cb", "Command Bar"));
        assertTrue(JIPipeDesktopCommandBarSearch.matches("CB", "Command Bar"));
        assertFalse(JIPipeDesktopCommandBarSearch.matches("bc", "Command Bar"));
        // Acronyms fall out of the camel-case split, too.
        assertTrue(JIPipeDesktopCommandBarSearch.matches("rp", "RunPipeline"));
        // The acronym rung is exactly 0.7.
        assertEquals(0.7, JIPipeDesktopCommandBarSearch.score("cb", "Command Bar"), 1e-9);
    }

    @Test
    void everyTokenMustMatch() {
        assertEquals(0.0, JIPipeDesktopCommandBarSearch.score("copy delete", "Copy"), 1e-9);
        assertFalse(JIPipeDesktopCommandBarSearch.matches("copy delete", "Copy"));
    }

    @Test
    void caseInsensitive() {
        assertEquals(1.0, JIPipeDesktopCommandBarSearch.score("COPY", "copy"), 1e-9);
        assertEquals(1.0, JIPipeDesktopCommandBarSearch.score("copy", "COPY"), 1e-9);
    }

    @Test
    void emptyQueryScoresZero() {
        assertEquals(0.0, JIPipeDesktopCommandBarSearch.score("", "Copy"), 1e-9);
        assertEquals(0.0, JIPipeDesktopCommandBarSearch.score("   ", "Copy"), 1e-9);
        assertFalse(JIPipeDesktopCommandBarSearch.matches("", "Copy"));
    }

    @Test
    void camelCaseSplitting() {
        // "RunPipeline" splits into the words "run" and "pipeline", so a
        // single word of the name matches exactly.
        assertEquals(1.0, JIPipeDesktopCommandBarSearch.score("pipeline", "RunPipeline"), 1e-9);
        // "ru" is a word prefix of "run".
        assertEquals(0.9, JIPipeDesktopCommandBarSearch.score("ru", "RunPipeline"), 1e-9);
        // "peli" is a substring of "pipeline" (and no exact/prefix match).
        assertEquals(0.8, JIPipeDesktopCommandBarSearch.score("peli", "RunPipeline"), 1e-9);
        // The spaced form splits identically.
        assertEquals(1.0, JIPipeDesktopCommandBarSearch.score("run pipeline", "Run Pipeline"), 1e-9);
    }

    @Test
    void fuzzyRungAboveTheFloor() {
        // "copu" is neither a word prefix nor a substring of "copy", but is
        // close enough (jaroWinkler >= 0.85) to earn the fuzzy rung.
        double score = JIPipeDesktopCommandBarSearch.score("copu", "Copy");
        assertTrue(score > 0.65, "expected a fuzzy match above the rung floor, got " + score);
        assertTrue(score < 0.8, "the fuzzy rung must sit below the substring rung, got " + score);
    }

    @Test
    void multiWordScoreIsTheMean() {
        assertEquals(1.0, JIPipeDesktopCommandBarSearch.score("copy files", "Copy Files"), 1e-9);
        // Exact (1.0) + word prefix (0.9) -> mean 0.95.
        assertEquals(0.95, JIPipeDesktopCommandBarSearch.score("copy file", "Copy Files"), 1e-9);
    }
}
