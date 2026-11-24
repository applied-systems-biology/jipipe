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

package org.hkijena.jipipe.api.knowledge;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the lexical retrieval lane: the inverted index with BM25 over
 * the corpus fields (design spec 5.5, lane 1).
 * <p>
 * The critical contract is verbatim matching: node ids and error strings are
 * single tokens (splitting happens on whitespace and punctuation except
 * hyphen, colon, and underscore), so a query must carry them exactly.
 */
class KdbLexicalIndexTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(KdbLexicalIndexTest.class.getResource("/kdb-fixtures/lanes/" + name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KdbLexicalIndex lexicalIndex() {
        return KdbLexicalIndex.build(new KdbEntryLoader().loadAll(fixture("lexical")).getEntries());
    }

    @Test
    void matchesAnExactNodeIdVerbatim() {
        // The pattern's nodeChain slot carries the node id as one token.
        List<KdbLexicalIndex.ScoredEntry> results = lexicalIndex().search("ij3-cellpose-import", 5);
        assertTrue(results.stream().anyMatch(scored ->
                        scored.entryKey().equals("bioimage-analysis/patterns/cellpose-segmentation")),
                "The exact node id must match the pattern entry, got: " + results);
    }

    @Test
    void neverSplitsANodeIdOnHyphens() {
        // "cellpose-import" is a fragment of the node id ij3-cellpose-import.
        // The tokenizer must keep node ids verbatim: the fragment exists as a
        // token nowhere in the corpus, so it must match nothing.
        assertTrue(lexicalIndex().search("cellpose-import", 5).isEmpty(),
                "A hyphen fragment of a node id must not match");
    }

    @Test
    void matchesAnErrorStringVerbatim() {
        // Error strings tokenize as units: colon and hyphens stay inside the
        // token, so the full string matches and a fragment does not.
        List<KdbLexicalIndex.ScoredEntry> results = lexicalIndex().search("error:ij3-cellpose-segment-42", 5);
        assertEquals(List.of("bioimage-analysis/mistakes/cellpose-pixel-type"),
                results.stream().map(KdbLexicalIndex.ScoredEntry::entryKey).toList());
        assertTrue(lexicalIndex().search("cellpose-segment-42", 5).isEmpty(),
                "A fragment of an error string must not match");
    }

    @Test
    void matchesAnAliasAndIsCaseInsensitive() {
        KdbLexicalIndex index = lexicalIndex();
        // The alias "GroupStats" exists nowhere else in the corpus: the
        // lowercase query reaches the entry through the alias surface only.
        assertTrue(index.search("groupstats", 5).stream().anyMatch(scored ->
                        scored.entryKey().equals("tables/patterns/aggregate-columns")),
                "A lowercase query must match the indexed alias, got: "
                        + index.search("groupstats", 5));
        // Case folds on the query side: "CELLPOSE" answers through the alias
        // "Cellpose" of the pattern entry.
        assertTrue(index.search("CELLPOSE", 5).stream().anyMatch(scored ->
                        scored.entryKey().equals("bioimage-analysis/patterns/cellpose-segmentation")),
                "An uppercase query must match through case folding");
    }

    @Test
    void doesNotMatchUnrelatedQueries() {
        assertTrue(lexicalIndex().search("quantum-astrobiology", 5).isEmpty(),
                "A query with no corpus token must produce an empty result");
    }

    @Test
    void ignoresBlankQueries() {
        KdbLexicalIndex index = lexicalIndex();
        assertTrue(index.search(null, 5).isEmpty());
        assertTrue(index.search("", 5).isEmpty());
        assertTrue(index.search("   ", 5).isEmpty());
    }

    @Test
    void respectsTheTopKCap() {
        KdbLexicalIndex index = lexicalIndex();
        // Both corpus entries answer "cellpose"; the cap bounds the result.
        assertEquals(2, index.search("cellpose", 10).size());
        assertEquals(1, index.search("cellpose", 1).size());
        assertEquals(0, index.search("cellpose", 0).size());
    }
}
