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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link SettingsDocumentMerger}: the five per-block merge rules
 * (adopted / kept / conflicted / only-locally / pass-through) and the
 * "foreign wins on a true same-block collision" contract.
 * <p>
 * The five core tests from the task brief are kept verbatim except
 * {@code keepsBlockChangedOnlyLocally}: its three-argument inputs are
 * mathematically indistinguishable from {@code
 * foreignWinsOnCollisionAndIsReported} (same local, same locally-changed
 * set, differing foreign), so the brief's two expectations cannot both be
 * met by a pure function of those three arguments alone. The deciding
 * input is the block's value at the last sync point — the design spec
 * (§5: "changed only locally (<em>the file still shows the old block</em>)
 * → keep ours") — which the test supplies explicitly via
 * {@link SettingsDocumentMerger#baselineOf(SettingsDocument)}, modeling
 * "this process last saw x=1 on disk and changed it to x=9 locally".
 * {@code docWith} is the brief's test-local helper. Additional tests pin
 * the directions the rules must guard against: a locally-only-changed
 * block must never be adopted (the data-loss direction), foreign removals
 * of unmodified blocks are adopted, and equal documents produce an empty
 * result.
 */
class SettingsDocumentMergerTest {
    @Test void adoptsBlockChangedOnlyExternally() {
        SettingsDocument local = docWith("registry:a", "x", 1);
        SettingsDocument foreign = docWith("registry:a", "x", 2);
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of());
        assertEquals(Set.of("registry:a"), r.adoptedBlocks());
        assertEquals(2, local.get("registry:a", "x").asInt());
    }

    @Test void keepsBlockChangedOnlyLocally() {
        SettingsDocument local = docWith("registry:a", "x", 9);
        SettingsDocument foreign = docWith("registry:a", "x", 1);
        // The last-synced value is the foreign one (this process last saw
        // x=1 on disk and changed it locally to x=9): the foreign side
        // never changed the block, so ours is kept. Without this baseline
        // the inputs are indistinguishable from a true collision.
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(
                local, foreign, Set.of("registry:a"),
                SettingsDocumentMerger.baselineOf(docWith("registry:a", "x", 1)));
        assertEquals(Set.of("registry:a"), r.keptBlocks());
        assertEquals(9, local.get("registry:a", "x").asInt());
    }

    @Test void foreignWinsOnCollisionAndIsReported() {
        SettingsDocument local = docWith("registry:a", "x", 9);
        SettingsDocument foreign = docWith("registry:a", "x", 2);
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of("registry:a"));
        assertEquals(Set.of("registry:a"), r.conflictedBlocks());
        assertEquals(2, local.get("registry:a", "x").asInt());
    }

    @Test void blockPresentOnlyLocallyIsKept() {
        SettingsDocument local = docWith("registry:a", "x", 9);
        SettingsDocument foreign = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of());
        assertEquals(9, local.get("registry:a", "x").asInt());
    }

    @Test void unknownPassThroughBlocksAreMergedToo() {
        SettingsDocument local = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsDocument foreign = new SettingsDocument(SettingsScope.APPLICATION);
        foreign.setBlock("future:z", IntNode.valueOf(1));
        SettingsDocumentMerger.merge(local, foreign, Set.of());
        assertEquals(1, local.get("future:z", "").asInt());
    }

    /**
     * The data-loss direction, with a baseline: a block changed only
     * locally — the foreign document still shows the last-synced value —
     * must never be replaced, and no foreign sub-path may leak in.
     */
    @Test void locallyChangedBlockIsNeverAdopted() {
        ObjectNode foreignBlock = JsonUtils.getObjectMapper().createObjectNode();
        foreignBlock.put("x", 2);
        foreignBlock.put("y", 100);
        SettingsDocument foreign = new SettingsDocument(SettingsScope.APPLICATION);
        foreign.setBlock("registry:a", foreignBlock);
        SettingsDocument local = docWith("registry:a", "x", 9);

        // The last-synced value equals what the foreign document still
        // shows: only this side changed the block since the sync point.
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(
                local, foreign, Set.of("registry:a"),
                Map.of("registry:a", foreignBlock.deepCopy()));

        assertEquals(Set.of("registry:a"), r.keptBlocks(), "a foreign-unchanged block is kept, not conflicted");
        assertEquals(9, local.get("registry:a", "x").asInt(), "the locally changed block must not be clobbered");
        assertEquals(false, local.has("registry:a", "y"), "no foreign sub-path may leak into a kept block");
    }

    /**
     * A block absent locally but present in the foreign document is a
     * foreign-only change: it is adopted (as a whole-block add).
     */
    @Test void absentLocallyBlockIsAdopted() {
        SettingsDocument local = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsDocument foreign = docWith("registry:b", "x", 7);
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of());
        assertEquals(Set.of("registry:b"), r.adoptedBlocks());
        assertEquals(7, local.get("registry:b", "x").asInt());
    }

    /**
     * A block the foreign document removed is only a foreign change when a
     * baseline shows the block existed at the last sync point: then the
     * deletion is adopted. Without a baseline the same shape is
     * indistinguishable from "another instance never wrote it" and is kept
     * (see {@code blockPresentOnlyLocallyIsKept}).
     */
    @Test void blockRemovedExternallyIsAdopted() {
        SettingsDocument local = docWith("registry:a", "x", 1);
        SettingsDocument foreign = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(
                local, foreign, Set.of(),
                SettingsDocumentMerger.baselineOf(docWith("registry:a", "x", 1)));
        assertEquals(Set.of("registry:a"), r.adoptedBlocks(), "the foreign deletion of an unmodified block is adopted");
        assertEquals(false, local.hasBlock("registry:a"));
    }

    /**
     * A block changed only locally and absent in the foreign document is
     * kept; and a block absent in both documents is not mentioned at all.
     */
    @Test void locallyChangedBlockAbsentExternallyIsKept() {
        SettingsDocument local = docWith("registry:a", "x", 9);
        SettingsDocument foreign = new SettingsDocument(SettingsScope.APPLICATION);
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of("registry:a"));
        assertEquals(Set.of("registry:a"), r.keptBlocks());
        assertEquals(9, local.get("registry:a", "x").asInt());
        assertEquals(0, r.adoptedBlocks().size());
        assertEquals(0, r.conflictedBlocks().size());
    }

    /**
     * Identical documents produce an empty result: nothing is adopted,
     * kept, or conflicted, and the local document is untouched.
     */
    @Test void identicalDocumentsProduceEmptyResult() {
        SettingsDocument local = docWith("registry:a", "x", 1);
        SettingsDocument foreign = docWith("registry:a", "x", 1);
        SettingsDocumentMerger.MergeResult r = SettingsDocumentMerger.merge(local, foreign, Set.of("registry:a"));
        assertEquals(0, r.adoptedBlocks().size());
        assertEquals(0, r.keptBlocks().size());
        assertEquals(0, r.conflictedBlocks().size());
        assertEquals(1, local.get("registry:a", "x").asInt());
    }

    private static SettingsDocument docWith(String blockPath, String subPath, int value) {
        SettingsDocument doc = new SettingsDocument(SettingsScope.APPLICATION);
        doc.setBlock(blockPath, JsonUtils.getObjectMapper().createObjectNode().put(subPath, value));
        return doc;
    }
}
