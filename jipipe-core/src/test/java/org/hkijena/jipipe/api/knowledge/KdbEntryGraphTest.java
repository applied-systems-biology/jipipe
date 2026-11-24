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
 * Tests for the relation lane: the one-hop, labeled ENTRY_REF graph over the
 * corpus (design spec 5.4).
 */
class KdbEntryGraphTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(KdbEntryGraphTest.class.getResource("/kdb-fixtures/lanes/" + name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KdbEntryGraph relationGraph() {
        return new KdbEntryGraph(new KdbEntryLoader().loadAll(fixture("relation")).getEntries());
    }

    @Test
    void oneHopTraversalIsCycleGuardedAndLabeled() {
        // alpha elaborates beta, and beta relatedConcepts alpha: the pair
        // forms a 2-cycle. One hop from alpha sees beta exactly once (the
        // entry's own declaration wins over the incoming edge) and delta.
        KdbEntryGraph graph = relationGraph();
        assertEquals(
                List.of(
                        new RelatedEntry("bioimage-analysis/concepts/beta", "elaborates",
                                RelatedEntry.Direction.OUTGOING),
                        new RelatedEntry("bioimage-analysis/concepts/delta", "elaborates",
                                RelatedEntry.Direction.OUTGOING)),
                graph.neighbors("bioimage-analysis/patterns/alpha"),
                "The cycle must collapse to one labeled neighbor per target");
    }

    @Test
    void outgoingDeclarationWinsOverTheIncomingEdge() {
        // From beta, its own relatedConcepts declaration toward alpha is the
        // primary evidence; the incoming elaborates edge of alpha collapses
        // into it.
        assertEquals(
                List.of(new RelatedEntry("bioimage-analysis/patterns/alpha", "relatedConcepts",
                        RelatedEntry.Direction.OUTGOING)),
                relationGraph().neighbors("bioimage-analysis/concepts/beta"));
    }

    @Test
    void incomingNeighborsCarryTheInverseRelationAndDirection() {
        // delta declares nothing; alpha elaborates delta. Seen from delta the
        // edge is incoming and labeled as the inverse of the declared
        // relation.
        assertEquals(
                List.of(new RelatedEntry("bioimage-analysis/patterns/alpha", "elaborated-by",
                        RelatedEntry.Direction.INCOMING)),
                relationGraph().neighbors("bioimage-analysis/concepts/delta"));
    }

    @Test
    void selfReferencesAreSkipped() {
        // epsilon elaborates itself: the visited set, seeded with the queried
        // entry key, absorbs the trivial cycle.
        assertTrue(relationGraph().neighbors("bioimage-analysis/patterns/epsilon").isEmpty());
    }

    @Test
    void danglingAndUnknownKeysYieldNoNeighbors() {
        // zeta crossRefs an entry key that is not in the corpus: traversal
        // cannot resolve it and yields nothing instead of a broken neighbor.
        // An unknown entry key has no neighbors by definition.
        KdbEntryGraph graph = relationGraph();
        assertTrue(graph.neighbors("bioimage-analysis/patterns/zeta").isEmpty());
        assertTrue(graph.neighbors("bioimage-analysis/patterns/missing").isEmpty());
    }
}
