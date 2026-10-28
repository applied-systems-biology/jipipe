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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Per-block merge of two {@link SettingsDocument}s: the decision half of
 * multi-instance settings sync. When another JIPipe process writes a
 * settings file, the owner loads the file into a temporary <em>foreign</em>
 * document and calls {@link #merge} to decide, block by block, what to
 * adopt into the in-memory <em>local</em> document.
 * <p>
 * <b>Rules.</b> For every block present in either document (blocks that
 * are structurally equal in both, or absent from both, are untouched and
 * not reported):
 * <ul>
 * <li><b>Adopted</b> — the block was changed only externally: the foreign
 * value is written into the local document, including whole-block adds
 * (foreign present, local absent) and removals (foreign absent, local
 * value identical to the last-synced value — the other side deleted the
 * block and this side has not touched it since the sync point).</li>
 * <li><b>Kept</b> — the block was changed only locally: the local value
 * stays, and the caller must persist it again so the file converges (this
 * process is newer). Concretely: a locally-changed block whose foreign
 * value equals the last-synced value (the other side never changed it
 * since the sync point), or a block present only locally whose foreign
 * value is unknown (the other instance may simply not have written it
 * yet).</li>
 * <li><b>Conflicted</b> — a true same-block collision: the block was
 * changed locally <em>and</em> the foreign value differs from both the
 * local value and the last-synced value (or the other side deleted it
 * while this side changed it). The <b>foreign block wins</b> (it is the
 * most recent on-disk truth); the local unsaved edit of that block is
 * discarded, and the block is reported in
 * {@link MergeResult#conflictedBlocks()} so the caller can surface it to
 * the user (e.g. a "Settings were updated by another JIPipe instance"
 * banner).</li>
 * </ul>
 * Pass-through blocks (unknown kind or unregistered id) are merged by the
 * same rules as registered blocks. Block comparison is structural
 * ({@link JsonNode#equals}); reserved {@code $}-prefixed fields are never
 * merged.
 * <p>
 * <b>The last-synced baseline is what makes the three outcomes distinct.</b>
 * "Changed locally" alone does not say whether the foreign side also
 * changed the block, and "absent in the foreign document" alone does not
 * say whether that is a foreign deletion or an instance that simply never
 * wrote the block. The deciding input is the block's value at the last
 * point where both sides were in sync — this process's last save, the
 * value adopted by a previous merge, or (at document load) the content
 * that was on disk. The caller tracks this baseline and supplies it via
 * {@link #merge(SettingsDocument, SettingsDocument, Set, Map)} and
 * {@link #baselineOf(SettingsDocument)}.
 * <p>
 * When no baseline is known for a block (e.g. the three-argument form of
 * this class's contract, or a block that did not exist at the last sync),
 * the decision is conservative in the non-data-losing direction: a
 * locally-changed block that the foreign document still holds is treated
 * as a collision (the foreign value wins — the on-disk truth), while a
 * locally-changed or untracked block that the foreign document does not
 * hold is kept (never destroy a local block the other side may simply not
 * have written yet).
 * <p>
 * This class performs no IO and no event handling: it mutates the local
 * document's blocks in place and reports the outcome. Firing
 * {@code SettingsChangedEvent}s and scheduling re-saves is the caller's
 * concern (see {@code JIPipeSettingsServiceComponent}).
 */
public final class SettingsDocumentMerger {
    private SettingsDocumentMerger() {
    }

    /**
     * The outcome of a per-block merge, partitioning the affected blocks
     * into three disjoint sets of canonical block paths.
     *
     * @param adoptedBlocks blocks copied from the foreign document into the
     *                      local one (adds, updates, and foreign deletions
     *                      of blocks this side has not touched since the
     *                      last sync)
     * @param keptBlocks blocks that stay at their local value and must be
     *                   persisted by the caller so the file converges (this
     *                   process is newer)
     * @param conflictedBlocks blocks with a true same-block collision, where
     *                        the foreign value won and the local edit was
     *                        discarded
     */
    public record MergeResult(Set<String> adoptedBlocks, Set<String> keptBlocks, Set<String> conflictedBlocks) {
        /**
         * @return whether any block conflicted, i.e. a local edit was
         * discarded in favor of the foreign value
         */
        public boolean hasConflicts() {
            return !conflictedBlocks.isEmpty();
        }

        /**
         * @return whether the caller should persist the local document to
         * converge the file: kept blocks are newer than the foreign file,
         * and a conflicted merge must write the winning foreign values
         * back so both processes converge on the same content
         */
        public boolean requiresSave() {
            return !keptBlocks.isEmpty() || hasConflicts();
        }
    }

    /**
     * Merges the foreign document into the local document without a
     * last-synced baseline (see the class Javadoc for the conservative
     * decisions that implies): a locally-changed block the foreign
     * document still holds is a conflict won by the foreign value; a
     * block present only locally is kept.
     *
     * @param local the in-memory document to merge into; mutated in place
     * @param foreign the document loaded from the other process's write;
     *                never mutated
     * @param locallyChangedBlocks the block paths changed locally since the
     *                             last sync (a save or a previous merge);
     *                             they are never adopted as-is
     * @return which blocks were adopted, kept, or conflicted
     */
    public static MergeResult merge(SettingsDocument local, SettingsDocument foreign, Set<String> locallyChangedBlocks) {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(foreign, "foreign");
        Objects.requireNonNull(locallyChangedBlocks, "locallyChangedBlocks");
        return merge(local, foreign, locallyChangedBlocks, Map.of());
    }

    /**
     * Merges the foreign document into the local document, using the given
     * last-synced values to distinguish "changed only locally", "changed
     * only externally", and a true same-block collision (see the class
     * Javadoc for the full rule set).
     * <p>
     * The local document is mutated in place for every adopted and
     * conflicted block; the foreign document is only read. {@code lastSynced}
     * maps canonical block paths to the block's structural value at the
     * last point where both sides were in sync (this process's last save,
     * the value adopted by a previous merge, or the document's content at
     * load); blocks not present in the map have no known baseline.
     *
     * @param local the in-memory document to merge into; mutated in place
     * @param foreign the document loaded from the other process's write;
     *                never mutated
     * @param locallyChangedBlocks the block paths changed locally since the
     *                             last sync; they are never adopted as-is
     * @param lastSynced the block values of the last sync point, keyed by
     *                   canonical block path
     * @return which blocks were adopted, kept, or conflicted
     */
    public static MergeResult merge(SettingsDocument local, SettingsDocument foreign,
                                    Set<String> locallyChangedBlocks, Map<String, JsonNode> lastSynced) {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(foreign, "foreign");
        Objects.requireNonNull(locallyChangedBlocks, "locallyChangedBlocks");
        Objects.requireNonNull(lastSynced, "lastSynced");
        Set<String> affected = new LinkedHashSet<>(local.blockPaths());
        affected.addAll(foreign.blockPaths());
        Set<String> adoptedBlocks = new LinkedHashSet<>();
        Set<String> keptBlocks = new LinkedHashSet<>();
        Set<String> conflictedBlocks = new LinkedHashSet<>();
        for (String blockPath : affected) {
            JsonNode l = local.getBlock(blockPath);
            JsonNode f = foreign.getBlock(blockPath);
            if (l.equals(f)) {
                continue; // in sync: both absent, or structurally equal
            }
            JsonNode baseline = lastSynced.get(blockPath);
            boolean locallyChanged = locallyChangedBlocks.contains(blockPath);
            if (!foreignEqualsBaseline(f, baseline)) {
                // The foreign side changed this block since the last sync
                // (different value, or a deletion of a baseline value).
                applyForeign(local, blockPath, f);
                if (locallyChanged) {
                    // True same-block collision: the foreign value is the
                    // most recent on-disk truth and wins; the local edit is
                    // discarded and the conflict reported.
                    conflictedBlocks.add(blockPath);
                } else {
                    // Changed only externally: adopt the foreign value.
                    adoptedBlocks.add(blockPath);
                }
            } else {
                // The foreign side has not changed this block since the
                // last sync (its value is exactly the baseline, or it is
                // absent from the file this process last wrote). Whatever
                // the local side did — or did not track — ours stays:
                // another instance may simply not have written the block
                // yet, and destroying a local value over an unchanged
                // foreign one would be pure data loss.
                keptBlocks.add(blockPath);
            }
        }
        return new MergeResult(adoptedBlocks, keptBlocks, conflictedBlocks);
    }

    /**
     * Builds the last-synced baseline map for a merge: the block values of
     * the given document, keyed by canonical block path, deep-copied so
     * later mutation of either document cannot corrupt the baseline. A
     * document that was persisted is a valid baseline for what the file
     * held from this process's point of view; a document whose content was
     * just read from disk (or adopted from a foreign merge) is a valid
     * baseline for what this process now considers in sync.
     *
     * @param doc the document whose blocks become the baseline
     * @return an immutable map of block path to structural value
     */
    public static Map<String, JsonNode> baselineOf(SettingsDocument doc) {
        Objects.requireNonNull(doc, "doc");
        Map<String, JsonNode> baseline = new LinkedHashMap<>();
        for (String blockPath : doc.blockPaths()) {
            baseline.put(blockPath, doc.getBlock(blockPath).deepCopy());
        }
        return Map.copyOf(baseline);
    }

    /**
     * @param f the foreign block value, or the JSON missing node when the
     *         foreign document does not carry the block
     * @param baseline the last-synced value, or {@code null} when no
     *                 baseline is known
     * @return whether the foreign value is exactly the last-synced value,
     *         i.e. the foreign side did not change the block since the
     *         sync point. A missing foreign block matches a missing
     *         baseline (both sides simply never wrote the block).
     */
    private static boolean foreignEqualsBaseline(JsonNode f, JsonNode baseline) {
        if (f.isMissingNode()) {
            return baseline == null;
        }
        return baseline != null && f.equals(baseline);
    }

    /**
     * Writes the foreign value into the local document, whole-block, or
     * removes the block when the foreign document does not carry it.
     */
    private static void applyForeign(SettingsDocument local, String blockPath, JsonNode f) {
        if (f.isMissingNode()) {
            local.removeBlock(blockPath);
        } else {
            local.setBlock(blockPath, f.deepCopy());
        }
    }
}
