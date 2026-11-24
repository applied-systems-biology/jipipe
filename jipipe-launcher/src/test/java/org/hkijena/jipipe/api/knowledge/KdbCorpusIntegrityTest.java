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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The corpus integrity gate (design spec 5.8): loads the packaged production
 * corpus from {@code jipipe-core} resources
 * ({@code org/hkijena/jipipe/ai/kdb/entries}) and fails the build on any
 * structural defect.
 * <p>
 * Structural defects are: any ERROR severity {@link KdbFinding} (schema
 * conformance, unique keys, usable titles), a NODE_REF that does not resolve
 * against the live node registry, an ENTRY_REF that does not resolve to a
 * loaded entry key, and an entry key that does not match its path-derived
 * shape. WARNING findings never fail this gate, but the full findings report
 * is printed so warnings (an EMPTY_CORPUS including the Files.walk I/O
 * failure case, PROPERTY_SHADOWS_SLOT) stay visible in the build output.
 * <p>
 * Lives in <b>jipipe-launcher</b>: only this module sees every plugin's
 * nodes, so the NODE_REF resolution runs against the same live registry the
 * production app builds (TaxonomyFullValidationTest's bootstrap).
 */
class KdbCorpusIntegrityTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    /**
     * The packaged production corpus root, resolved from the classpath
     * (KdbProductionCorpus supports both the reactor classes directory and the
     * installed jipipe-core jar).
     */
    private static Path productionCorpusRoot() {
        return KdbProductionCorpus.root();
    }

    private static KdbLoadResult loadProductionCorpus() {
        return new KdbEntryLoader().loadAll(productionCorpusRoot());
    }

    /**
     * The NODE_REF values of an entry: the values of every slot whose type is
     * NODE_REF (the loader narrows them to String).
     */
    private static List<String> nodeRefs(KdbEntry entry) {
        List<String> refs = new ArrayList<>();
        for (KdbSlotDef slotDef : entry.getKind().getSlots()) {
            if (slotDef.type() != KdbValueType.NODE_REF) {
                continue;
            }
            for (Object value : entry.getSlotValues().getOrDefault(slotDef.name(), List.of())) {
                refs.add(String.valueOf(value));
            }
        }
        return refs;
    }

    /**
     * The ENTRY_REF values of an entry: the values of every slot whose type
     * is ENTRY_REF (the loader narrows them to String).
     */
    private static List<String> entryRefs(KdbEntry entry) {
        List<String> refs = new ArrayList<>();
        for (KdbSlotDef slotDef : entry.getKind().getSlots()) {
            if (slotDef.type() != KdbValueType.ENTRY_REF) {
                continue;
            }
            for (Object value : entry.getSlotValues().getOrDefault(slotDef.name(), List.of())) {
                refs.add(String.valueOf(value));
            }
        }
        return refs;
    }

    /**
     * The message the node registry itself produces for a node id: for a
     * retired id the registry lookup names the id and lists what it does
     * know, which is the message the gate forwards (design spec 5.8: fail
     * with the registry's own message for a retired id).
     */
    private static String registryMessageFor(String nodeRef) {
        try {
            JIPipe.getNodes().getInfoById(nodeRef);
            return "the node registry resolves '" + nodeRef + "'";
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage() != null) {
                return cause.getMessage();
            }
            return e.toString();
        }
    }

    @Test
    void productionCorpusHasNoStructuralFindings() {
        KdbLoadResult result = loadProductionCorpus();
        // The full report is part of this gate's output: warnings must stay
        // visible (EMPTY_CORPUS is a warning and includes the Files.walk I/O
        // failure case; PROPERTY_SHADOWS_SLOT keeps the property).
        System.out.println("KdbCorpusIntegrityTest: " + result.getEntries().size()
                + " entries, " + result.getFindings().size() + " findings:");
        for (KdbFinding finding : result.getFindings()) {
            System.out.println("  " + finding);
        }
        List<KdbFinding> errors = new ArrayList<>();
        for (KdbFinding finding : result.getFindings()) {
            if (finding.getCode().getSeverity() == KdbFinding.Severity.ERROR) {
                errors.add(finding);
            }
        }
        assertTrue(errors.isEmpty(),
                "The packaged KDB corpus has " + errors.size() + " structural (ERROR) findings:\n  "
                        + String.join("\n  ", errors.stream().map(KdbFinding::toString).toList()));
    }

    @Test
    void everyNodeRefResolvesOrTheEntryIsFlagged() {
        KdbLoadResult result = loadProductionCorpus();
        Map<String, JIPipeNodeInfo> registeredNodes = JIPipe.getNodes().getRegisteredNodeInfos();
        List<String> dangling = new ArrayList<>();
        for (KdbEntry entry : result.getEntries()) {
            for (String nodeRef : nodeRefs(entry)) {
                if (!registeredNodes.containsKey(nodeRef)) {
                    dangling.add(entry.getEntryKey() + " -> '" + nodeRef
                            + "' (registry says: " + registryMessageFor(nodeRef) + ")");
                }
            }
        }
        assertTrue(dangling.isEmpty(),
                dangling.size() + " NODE_REF value(s) do not resolve against the live node registry "
                        + "(UNRESOLVED_NODE_REF):\n  " + String.join("\n  ", dangling));
    }

    @Test
    void everyEntryRefResolves() {
        KdbLoadResult result = loadProductionCorpus();
        Set<String> loadedKeys = new LinkedHashSet<>();
        for (KdbEntry entry : result.getEntries()) {
            loadedKeys.add(entry.getEntryKey());
        }
        List<String> dangling = new ArrayList<>();
        for (KdbEntry entry : result.getEntries()) {
            for (String entryRef : entryRefs(entry)) {
                if (!loadedKeys.contains(entryRef)) {
                    dangling.add(entry.getEntryKey() + " -> '" + entryRef + "'");
                }
            }
        }
        assertTrue(dangling.isEmpty(),
                dangling.size() + " ENTRY_REF value(s) do not resolve to a loaded entry key "
                        + "(UNRESOLVED_ENTRY_REF):\n  " + String.join("\n  ", dangling));
    }

    @Test
    void entryKeysMatchTheirPaths() {
        KdbLoadResult result = loadProductionCorpus();
        for (KdbEntry entry : result.getEntries()) {
            // The entry key is <domain>/<kind-directory>/<name> derived from
            // the file path; the loader must never take it from a YAML field
            // or reorder the segments. Re-deriving from the loaded entry's
            // own domain and kind keeps this contract explicit: both are read
            // from the same path, so a drift in the derivation fails here.
            String[] segments = entry.getEntryKey().split("/", -1);
            assertEquals(3, segments.length,
                    "Entry key '" + entry.getEntryKey() + "' must match <domain>/<kind-directory>/<name>");
            assertEquals(entry.getDomain().getId(), segments[0],
                    "Entry key '" + entry.getEntryKey() + "' does not start with its domain id");
            assertEquals(entry.getKind().getDirectoryName(), segments[1],
                    "Entry key '" + entry.getEntryKey() + "' does not carry its kind directory");
            assertFalse(segments[2].isBlank(),
                    "Entry key '" + entry.getEntryKey() + "' has an empty name segment");
        }
    }
}
