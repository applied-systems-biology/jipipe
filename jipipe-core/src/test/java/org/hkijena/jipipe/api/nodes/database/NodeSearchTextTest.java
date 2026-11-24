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

package org.hkijena.jipipe.api.nodes.database;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the shared search-text helper: the content-word tokenizer (the
 * search's own word split + stopword/length filters) and the non-fuzzy
 * coverage ladder the description gate (MR B) reuses.
 */
class NodeSearchTextTest {

    @Test
    void contentWordsSplitsCamelCaseAndStops() {
        // "to" is a stopword; "2d" is not a pure-digit word and stays; "ROI"
        // has no split point (the pattern only splits at case boundaries)
        assertEquals(List.of("convert", "2d", "roi", "rgb"),
                NodeSearchText.contentWords("Convert 2D ROI to RGB"));
    }

    @Test
    void contentWordsDropsSingleCharsAndPureDigits() {
        assertEquals(List.of("find", "particles"),
                NodeSearchText.contentWords("Find 3 particles a b"));
    }

    @Test
    void contentWordsSplitsSeparatorsAndCamelCase() {
        // "LocalAuto" camel-splits into "local" + "auto"; duplicates are kept
        assertEquals(List.of("auto", "threshold", "local", "auto"),
                NodeSearchText.contentWords("auto_threshold.LocalAuto"));
    }

    @Test
    void wordSetKeepsStopwordsAndIsLowercase() {
        assertEquals(Set.of("the", "to", "rgb"),
                NodeSearchText.wordSet("The TO rgb"));
    }

    @Test
    void coveredByLadder() {
        Set<String> tokens = Set.of("particles", "finds", "segmentation", "running");
        assertTrue(NodeSearchText.coveredBy("particles", tokens));  // exact
        assertTrue(NodeSearchText.coveredBy("particle", tokens));   // token prefix
        assertTrue(NodeSearchText.coveredBy("find", tokens));       // token prefix ("finds")
        assertTrue(NodeSearchText.coveredBy("findings", tokens));   // stem equality ("find")
        assertTrue(NodeSearchText.coveredBy("run", tokens));        // stem equality ("run")
        assertTrue(NodeSearchText.coveredBy("segment", tokens));    // substring ("segmentation".contains)
        assertFalse(NodeSearchText.coveredBy("threshold", tokens));
        assertFalse(NodeSearchText.coveredBy("pattern", tokens));   // not a prefix, stem, or substring of any token
    }
}
