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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the headless KDB entry loader.
 * <p>
 * The loader must parse the corpus into {@link KdbEntry} values and report
 * malformed content as {@link KdbFinding} values. It must never throw for
 * malformed corpus content: conflicts are findings, not exceptions.
 */
class KdbEntryLoaderTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(KdbEntryLoaderTest.class.getResource("/kdb-fixtures/" + name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void loadsAWellFormedEntryWithDerivedDomainAndKey() {
        KdbLoadResult result = new KdbEntryLoader().loadAll(fixture("good"));
        assertEquals(1, result.getEntries().size());
        assertTrue(result.getFindings().isEmpty(),
                "A well formed corpus must produce no findings, got: " + result.getFindings());
        KdbEntry entry = result.getEntries().get(0);
        assertEquals("bioimage-analysis/patterns/pilot", entry.getEntryKey());
        assertEquals(KdbDomain.BIOIMAGE_ANALYSIS, entry.getDomain());
        assertEquals(KdbEntryKind.PATTERN, entry.getKind());
        assertNotNull(entry.getId());
        assertEquals("Pilot pattern: Cellpose segmentation of 2D fluorescence data", entry.getTitle());
        assertEquals(List.of("ij3-cellpose-import", "ij3-cellpose-segment", "ij2-roi-to-rgb"),
                entry.getSlotValues().get("nodeChain"));
        assertEquals(List.of("bioimage-analysis/concepts/segmentation"),
                entry.getSlotValues().get("elaborates"));
        assertEquals("Segment nuclei or cells in 2D fluorescence images",
                entry.getSlotValue("problem"));
        assertTrue(entry.getSlotValue("problem") instanceof String,
                "A TEXT slot must narrow to String");
        assertEquals(List.of("cellpose", "segment nuclei with cellpose"), entry.getAliases());
        assertTrue(entry.getBody().contains("60-workflow analysis"));
        assertEquals(List.of("docs/knowledge/sources/bioimage-analysis/patterns/pilot.md",
                "workflow-pattern-analysis.md#section-7.2"), entry.getSourceRefs());
        assertTrue(entry.getProperties().isEmpty());
    }

    @Test
    void rejectsAnUnknownSlotAsAStructuralFindingNotAnException() {
        KdbLoadResult result = assertDoesNotThrow(() -> new KdbEntryLoader().loadAll(fixture("unknown-slot")));
        assertTrue(result.getFindings().stream().anyMatch(finding ->
                        finding.getCode() == KdbFindingCode.UNKNOWN_SLOT
                                && finding.getMessage().contains("flavor")
                                && finding.getMessage().contains("bioimage-analysis/patterns/pilot")),
                "Expected an UNKNOWN_SLOT finding naming the slot and the entry key, got: "
                        + result.getFindings());
        // The known slots still load: one defect does not discard the entry.
        assertEquals(1, result.getEntries().size());
        assertTrue(result.getEntries().get(0).getSlotValues().containsKey("problem"));
        assertFalse(result.getEntries().get(0).getSlotValues().containsKey("flavor"));
    }

    @Test
    void rejectsAKindDirectoryMismatch() {
        KdbLoadResult result = assertDoesNotThrow(() -> new KdbEntryLoader().loadAll(fixture("kind-mismatch")));
        assertTrue(result.getFindings().stream().anyMatch(finding ->
                        finding.getCode() == KdbFindingCode.KIND_PATH_MISMATCH
                                && finding.getMessage().contains("bioimage-analysis/concepts/pilot")),
                "Expected a KIND_PATH_MISMATCH finding for the pattern file under concepts/, got: "
                        + result.getFindings());
        // The classification is contradictory: the file is skipped.
        assertTrue(result.getEntries().isEmpty());
    }

    @Test
    void reportsACardinalityViolation() {
        KdbLoadResult result = assertDoesNotThrow(() -> new KdbEntryLoader().loadAll(fixture("cardinality-violation")));
        assertTrue(result.getFindings().stream().anyMatch(finding ->
                        finding.getCode() == KdbFindingCode.CARDINALITY_VIOLATION
                                && finding.getMessage().contains("bioimage-analysis/patterns/pilot")),
                "Expected a CARDINALITY_VIOLATION finding carrying the entry key, got: "
                        + result.getFindings());
    }

    @Test
    void loadsAnEmptyCorpusAsAWarningFinding() {
        KdbLoadResult result = assertDoesNotThrow(() -> new KdbEntryLoader().loadAll(fixture("empty")));
        assertTrue(result.getEntries().isEmpty());
        assertTrue(result.getFindings().stream().anyMatch(finding ->
                        finding.getCode() == KdbFindingCode.EMPTY_CORPUS),
                "An empty corpus must be reported as an EMPTY_CORPUS warning");
    }

    @Test
    void malformedYamlProducesAFindingNotAnException() {
        // A syntactically broken file must surface as a finding that names the
        // file (via its path-derived entry key), never as a thrown exception:
        // the gate decides what fails the build, the loader only reports.
        KdbLoadResult result = assertDoesNotThrow(() -> new KdbEntryLoader().loadAll(fixture("malformed-yaml")));
        assertTrue(result.getEntries().isEmpty());
        assertTrue(result.getFindings().stream().anyMatch(finding ->
                        (finding.getCode() == KdbFindingCode.TYPE_MISMATCH
                                || finding.getCode() == KdbFindingCode.UNKNOWN_KIND)
                                && finding.getMessage().contains("bioimage-analysis/patterns/broken")),
                "Expected a TYPE_MISMATCH or UNKNOWN_KIND finding naming the broken file, got: "
                        + result.getFindings());
    }

    @Test
    void entryKeyInTheFileIsIgnoredInFavourOfThePath() {
        // The entry key is derived from the path, never from a YAML field. The
        // fixture's real.yaml claims the key of the second file; if the loader
        // honored the claim, both files would collide on
        // bioimage-analysis/concepts/pretend and the collision would have to
        // be a DUPLICATE_ENTRY_KEY. Claims are ignored, so both entries load
        // with their own path-derived keys and no duplicate fires.
        KdbLoadResult result = assertDoesNotThrow(() -> new KdbEntryLoader().loadAll(fixture("key-claim")));
        assertEquals(2, result.getEntries().size(),
                "Both files must load, got findings: " + result.getFindings());
        Set<String> keys = result.getEntries().stream().map(KdbEntry::getEntryKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(Set.of("bioimage-analysis/patterns/real", "bioimage-analysis/concepts/pretend"),
                keys,
                "The path-derived key must win over the entryKey field claimed in the body");
        assertTrue(result.getFindings(KdbFindingCode.DUPLICATE_ENTRY_KEY).isEmpty(),
                "Ignoring the claimed key must not produce a duplicate, got: "
                        + result.getFindings(KdbFindingCode.DUPLICATE_ENTRY_KEY));
        // Classification still comes from the path segments.
        KdbEntry real = result.getEntries().stream()
                .filter(entry -> entry.getEntryKey().equals("bioimage-analysis/patterns/real"))
                .findFirst().orElseThrow();
        assertEquals(KdbEntryKind.PATTERN, real.getKind());
        assertEquals(KdbDomain.BIOIMAGE_ANALYSIS, real.getDomain());
        KdbEntry pretend = result.getEntries().stream()
                .filter(entry -> entry.getEntryKey().equals("bioimage-analysis/concepts/pretend"))
                .findFirst().orElseThrow();
        assertEquals(KdbEntryKind.CONCEPT, pretend.getKind());
    }

    @Test
    void narrowsValueTypesStrictly() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        assertEquals("hello", KdbValueType.TEXT.narrow(mapper.readTree("hello")).orElse(null));
        assertFalse(KdbValueType.TEXT.narrow(mapper.readTree("5")).isPresent(),
                "A TEXT slot takes a string: a number cannot narrow");
        assertEquals("ij3-cellpose-import",
                KdbValueType.NODE_REF.narrow(mapper.readTree("ij3-cellpose-import")).orElse(null));
        assertEquals("bioimage-analysis/concepts/segmentation",
                KdbValueType.ENTRY_REF.narrow(mapper.readTree("bioimage-analysis/concepts/segmentation")).orElse(null));
        assertFalse(KdbValueType.ENTRY_REF.narrow(mapper.readTree("5")).isPresent());
        assertEquals(5L, KdbValueType.NUMBER.narrow(mapper.readTree("5")).orElseThrow());
        assertEquals(2.5, (Double) KdbValueType.NUMBER.narrow(mapper.readTree("2.5")).orElseThrow(), 1e-12);
        assertFalse(KdbValueType.NUMBER.narrow(mapper.readTree("banana")).isPresent());
        assertEquals("6.0.0", KdbValueType.VERSION.narrow(mapper.readTree("6.0.0")).orElse(null));
        assertFalse(KdbValueType.VERSION.narrow(mapper.readTree("banana")).isPresent(),
                "VERSION is a string shaped like a version");
    }

    @Test
    void kindSchemasMatchTheSpec() {
        assertEquals("concepts", KdbEntryKind.CONCEPT.getDirectoryName());
        assertEquals(List.of(
                        new KdbSlotDef("prerequisites", KdbValueType.ENTRY_REF, KdbCardinality.MULTI),
                        new KdbSlotDef("relatedConcepts", KdbValueType.ENTRY_REF, KdbCardinality.MULTI),
                        new KdbSlotDef("nodes", KdbValueType.NODE_REF, KdbCardinality.MULTI)),
                KdbEntryKind.CONCEPT.getSlots());
        assertEquals("patterns", KdbEntryKind.PATTERN.getDirectoryName());
        assertEquals(List.of(
                        new KdbSlotDef("problem", KdbValueType.TEXT, KdbCardinality.SINGLE),
                        new KdbSlotDef("nodeChain", KdbValueType.NODE_REF, KdbCardinality.MULTI),
                        new KdbSlotDef("parameterEvidence", KdbValueType.TEXT, KdbCardinality.MULTI),
                        new KdbSlotDef("elaborates", KdbValueType.ENTRY_REF, KdbCardinality.MULTI),
                        new KdbSlotDef("crossRefs", KdbValueType.ENTRY_REF, KdbCardinality.MULTI)),
                KdbEntryKind.PATTERN.getSlots());
        assertEquals("mistakes", KdbEntryKind.MISTAKE.getDirectoryName());
        assertEquals(List.of(
                        new KdbSlotDef("symptom", KdbValueType.TEXT, KdbCardinality.SINGLE),
                        new KdbSlotDef("cause", KdbValueType.TEXT, KdbCardinality.SINGLE),
                        new KdbSlotDef("fix", KdbValueType.TEXT, KdbCardinality.SINGLE),
                        new KdbSlotDef("nodes", KdbValueType.NODE_REF, KdbCardinality.MULTI),
                        new KdbSlotDef("crossRefs", KdbValueType.ENTRY_REF, KdbCardinality.MULTI)),
                KdbEntryKind.MISTAKE.getSlots());
        assertEquals("decision-trees", KdbEntryKind.DECISION_TREE.getDirectoryName());
        assertEquals(List.of(
                        new KdbSlotDef("question", KdbValueType.TEXT, KdbCardinality.SINGLE),
                        new KdbSlotDef("options", KdbValueType.ENTRY_REF, KdbCardinality.MULTI),
                        new KdbSlotDef("nodes", KdbValueType.NODE_REF, KdbCardinality.MULTI)),
                KdbEntryKind.DECISION_TREE.getSlots());
        assertEquals("node-notes", KdbEntryKind.NODE_NOTE.getDirectoryName());
        assertEquals(List.of(
                        new KdbSlotDef("nodes", KdbValueType.NODE_REF, KdbCardinality.MULTI),
                        new KdbSlotDef("caveat", KdbValueType.TEXT, KdbCardinality.SINGLE)),
                KdbEntryKind.NODE_NOTE.getSlots());
    }

    @Test
    void domainIdsMatchTheSpec() {
        assertEquals(List.of("core-semantics", "parameters", "expressions", "data-management",
                        "file-formats", "bioimage-analysis", "integration", "project-management",
                        "running-pipelines", "plugin-dev", "tables"),
                List.of(KdbDomain.values()).stream().map(KdbDomain::getId).toList());
    }
}
