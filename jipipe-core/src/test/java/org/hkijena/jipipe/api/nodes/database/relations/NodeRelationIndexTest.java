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

package org.hkijena.jipipe.api.nodes.database.relations;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-logic unit tests for {@link NodeRelationIndex} (the Similar / Previous /
 * Next relation index, spec §10.1). The index is intentionally JIPipe-free so
 * it can be exercised without booting the node registry: edges are added by
 * id and ranked by support (descending) then id.
 * <p>
 * A stale edge (one touching an id that the source did not confirm as known)
 * is dropped, never fatal — the caller resolves and warns.
 */
class NodeRelationIndexTest {

    @Test
    void edgeProducesBothDirections() {
        NodeRelationIndex idx = new NodeRelationIndex();
        idx.addEdge("a", "b");            // b follows a
        assertThat(idx.next("a")).containsExactly("b");
        assertThat(idx.previous("b")).containsExactly("a");
    }

    @Test
    void supportRanksHigherFirst() {
        NodeRelationIndex idx = new NodeRelationIndex();
        idx.addEdge("a", "x");
        idx.addEdge("a", "y");            // x and y both follow a
        idx.addEdge("a", "y");            // y seen again -> higher support
        assertThat(idx.next("a")).isEqualTo(List.of("y", "x"));  // support desc, then id
    }

    @Test
    void sourceNodesGetPrevious() {
        // a template with source node S (no incoming edge) -> previous(S) empty, next(S) = its out-neighbor
        NodeRelationIndex idx = new NodeRelationIndex();
        idx.addEdge("s", "p");
        assertThat(idx.previous("s")).isEmpty();
        assertThat(idx.next("s")).containsExactly("p");
    }

    @Test
    void staleEdgeIsSkippedNotThrown() {
        // a template edge whose node id no longer resolves is dropped, not fatal
        NodeRelationIndex idx = new NodeRelationIndex();
        idx.addEdgesIgnoringUnresolved(Set.of("known-node", "other-known"),   // "ghost-node" is NOT known
                List.of(new Edge("known-node", "ghost-node"), new Edge("known-node", "other-known")));
        assertThat(idx.next("known-node")).isEqualTo(List.of("other-known"));  // ghost edge dropped
    }

    @Test
    void aThirdSourceAttachesWithoutTouchingTheIndex() {
        // §12 "pluggability via a fake source": the index is source-agnostic —
        // it consumes ANY NodeRelationSource via source.contribute(index::addEdge)
        // (exactly the JIPipeNodeDatabase.buildRelationIndex loop). A future
        // source (starter patterns, #1333 repository templates, ...) attaches by
        // adding one entry to that list; no change to the index or to relations().
        // The two real sources (FamilyRelationSource / TemplateRelationSource)
        // are JIPipe-coupled, so this uses fake sources to prove the seam is
        // purely the interface + the loop.
        NodeRelationSource familyLike = sink -> {
            sink.accept(new Edge("f1", "f2"));
            sink.accept(new Edge("f2", "f3"));
        };
        // The fake "third" source — the whole point: a new contributor that
        // requires zero changes to NodeRelationIndex.
        NodeRelationSource futureLike = sink -> {
            sink.accept(new Edge("s1", "f1"));   // links into the familyLike chain
            sink.accept(new Edge("f3", "s2"));
        };

        NodeRelationIndex idx = new NodeRelationIndex();
        List<NodeRelationSource> sources = List.of(familyLike, futureLike);
        for (NodeRelationSource source : sources) {
            source.contribute(idx::addEdge);
        }

        // The fake source's edges are in the same index as the "family" ones,
        // both directions, so a node is reachable across source boundaries.
        assertThat(idx.next("s1")).containsExactly("f1");
        assertThat(idx.previous("f1")).containsExactly("s1");
        assertThat(idx.next("f3")).containsExactly("s2");
        assertThat(idx.previous("s2")).containsExactly("f3");
    }
}
