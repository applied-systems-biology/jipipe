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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the agent facing renderer (design spec 5.6): the node lines carry
 * deprecation and successor badges resolved through an injectable node info
 * resolver, the body is the only part that yields to the token bound, and the
 * next steps block lists the one hop neighbors with the
 * {@code get_knowledge_entry} pointer.
 * <p>
 * The resolver based tests use a stub resolver for determinism (the recommended
 * pattern: the resolver is the seam); one integration test borrows a real
 * registered node id from the live registry and pins that the default resolver
 * reads display name, deprecation, and successors from it. A registry-free
 * resolver (the one the vector lane uses) renders raw ids and no badges.
 */
class KdbRendererTest {

    private static final String DEPRECATED_ID = "renderer-legacy-node";
    private static final String FRESH_ID = "renderer-fresh-node";
    private static final String SUCCESSOR_ID = "renderer-successor-node";

    private static final String HUB_KEY = "bioimage-analysis/patterns/renderer-hub";
    private static final String TARGET_KEY = "bioimage-analysis/concepts/renderer-target";

    @BeforeAll
    static void init() {
        // Only the integration test needs the registry; ensureInstance() is
        // idempotent and the ImageJLegacyPatcherLauncherSessionListener of this
        // module has already preinitted the legacy patcher.
        JIPipe.ensureInstance();
    }

    private static Path fixture() {
        try {
            return Paths.get(KdbRendererTest.class.getResource("/kdb-fixtures/renderer").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<KdbEntry> entries() {
        return new KdbEntryLoader().loadAll(fixture()).getEntries();
    }

    private static KdbEntry entryByKey(List<KdbEntry> entries, String entryKey) {
        return entries.stream().filter(entry -> entry.getEntryKey().equals(entryKey)).findFirst().orElseThrow();
    }

    /**
     * The stub resolver: display names and lifecycle state are fixed, every
     * other id (including the fixture's renderer-unknown-node) is
     * unresolvable.
     */
    private static Function<String, KdbRenderer.NodeFacts> stubResolver() {
        return nodeId -> switch (nodeId) {
            case DEPRECATED_ID -> new KdbRenderer.NodeFacts("Deprecated node", true, List.of(SUCCESSOR_ID));
            case FRESH_ID -> new KdbRenderer.NodeFacts("Fresh node", false, List.of());
            case SUCCESSOR_ID -> new KdbRenderer.NodeFacts("Successor node", false, List.of());
            default -> null;
        };
    }

    private static KdbRenderer stubRenderer() {
        // The neighbor function is wired like production does: over a graph
        // of the fixture corpus.
        KdbEntryGraph graph = new KdbEntryGraph(entries());
        return new KdbRenderer(stubResolver(), graph::neighbors);
    }

    /**
     * A hand built single node entry for the tests that need full control over
     * the body or the referenced node id.
     */
    private static KdbEntry entryWith(String nodeId, String body) {
        return new KdbEntry("bioimage-analysis/patterns/hand-built", UUID.randomUUID(),
                KdbEntryKind.PATTERN, KdbDomain.BIOIMAGE_ANALYSIS, "Bounded entry",
                Map.of("nodeChain", List.of(nodeId)), body,
                List.of(), null, null, List.of(), Map.of());
    }

    @Test
    void deprecatedNodeRendersWithSuccessorLine() {
        String text = stubRenderer().render(entryByKey(entries(), HUB_KEY), 1200);
        assertTrue(text.contains("- Deprecated node (" + DEPRECATED_ID + ") - deprecated, use Successor node ("
                        + SUCCESSOR_ID + ") instead"),
                "The deprecation badge and the successor line come from the resolver: " + text);
        assertTrue(text.contains("deprecated"));
        assertTrue(text.contains("instead"));
    }

    @Test
    void freshNodeRendersPlainLineWithoutBadges() {
        // The target entry references only the fresh node: no lifecycle
        // wording at all.
        String text = stubRenderer().render(entryByKey(entries(), TARGET_KEY), 1200);
        assertTrue(text.contains("- Fresh node (" + FRESH_ID + ")"),
                "A current node renders name and id only: " + text);
        assertFalse(text.contains("deprecated"), "No deprecation badge for a current node");
        assertFalse(text.contains("instead"), "No successor wording for a current node");
    }

    @Test
    void unresolvableNodeIdRendersRawWithoutThrowing() {
        String text = assertDoesNotThrow(
                () -> stubRenderer().render(entryByKey(entries(), HUB_KEY), 1200),
                "The render must never throw on an id the resolver does not know");
        assertTrue(text.contains("- " + "renderer-unknown-node"),
                "An unresolvable id renders verbatim: " + text);
    }

    @Test
    void renderIsTokenBoundedAndKeepsStructureComplete() {
        // The fixture hub, but with a body far beyond any bound: the copy
        // keeps the entry key, so the stub renderer resolves its neighbors
        // and the next steps block carries the pointer line.
        KdbEntry hub = entryByKey(entries(), HUB_KEY);
        String sentinel = "TAILSENTINELBEYONDTHEBOUND";
        String body = ("filler words padding the body. ").repeat(400) + sentinel;
        KdbEntry bounded = new KdbEntry(hub.getEntryKey(), hub.getId(), hub.getKind(), hub.getDomain(),
                hub.getTitle(), hub.getSlotValues(), body, hub.getAliases(), hub.getSinceVersion(),
                hub.getUntilVersion(), hub.getSourceRefs(), hub.getProperties());
        String text = stubRenderer().render(bounded, 120);
        assertTrue(KdbRenderer.estimateTokens(text) <= 120,
                "The render respects the bound: " + KdbRenderer.estimateTokens(text));
        assertTrue(text.contains(hub.getTitle()), "The title survives the bound");
        assertTrue(text.contains("- Deprecated node (" + DEPRECATED_ID + ") - deprecated, use Successor node ("
                        + SUCCESSOR_ID + ") instead"),
                "The node lines survive the bound: " + text);
        assertTrue(text.contains("Next steps:"), "The next steps block survives the bound");
        assertTrue(text.contains("- elaborates: " + TARGET_KEY + " (use get_knowledge_entry)"),
                "The neighbor line with the pointer survives the bound: " + text);
        assertFalse(text.contains(sentinel), "The body is the part that yields to the bound");
        int nextStepsStart = text.indexOf("\nNext steps:");
        String beforeNextSteps = text.substring(0, nextStepsStart).strip();
        String lastWord = beforeNextSteps.substring(beforeNextSteps.lastIndexOf(' ') + 1);
        assertTrue(body.contains(" " + lastWord + " ") || body.endsWith(lastWord + " "),
                "The body truncation cuts between words, not inside one: last word '" + lastWord + "'");
    }

    @Test
    void tinyBoundDropsTheBodyButKeepsTheStructure() {
        String text = stubRenderer().render(entryWith(DEPRECATED_ID, "A body that cannot fit."), 1);
        assertTrue(text.contains("# Bounded entry"), "The title survives even a one token bound");
        assertTrue(text.contains("(" + DEPRECATED_ID + ")"), "The node line survives");
        assertFalse(text.contains("A body that cannot fit"), "The body yields");
    }

    @Test
    void nextStepsListRelatedEntriesByKeyWithPointer() {
        String text = stubRenderer().render(entryByKey(entries(), HUB_KEY), 1200);
        assertTrue(text.contains("- elaborates: " + TARGET_KEY + " (use get_knowledge_entry)"),
                "Outgoing relations list the target key under the declared label: " + text);
        assertTrue(text.contains("list_knowledge_domains"), "The domain list pointer is part of the block");
    }

    @Test
    void incomingRelationsCarryTheInverseLabel() {
        // The target is referenced by the hub's elaborates slot, so its render
        // lists the hub under the inverse relation.
        String text = stubRenderer().render(entryByKey(entries(), TARGET_KEY), 1200);
        assertTrue(text.contains("- elaborated-by: " + HUB_KEY + " (use get_knowledge_entry)"),
                "Incoming relations list the source under the inverse label: " + text);
    }

    @Test
    void rawIdResolverRendersRawIdsWithoutBadges() {
        // The resolver the vector lane uses: no registry access, raw ids.
        KdbRenderer renderer = new KdbRenderer(KdbRenderer.rawIdResolver(), entryKey -> List.of());
        String text = renderer.render(entryWith(DEPRECATED_ID, "A body."), 1200);
        assertTrue(text.contains("- " + DEPRECATED_ID), "Every id renders raw: " + text);
        assertFalse(text.contains("deprecated"), "No registry, no badges");
        assertFalse(text.contains("instead"), "No registry, no successor wording");
    }

    @Test
    void missingTitleAndBodyStillRender() {
        KdbEntry bare = new KdbEntry("bioimage-analysis/patterns/bare", UUID.randomUUID(),
                KdbEntryKind.PATTERN, KdbDomain.BIOIMAGE_ANALYSIS, null,
                Map.of(), null, List.of(), null, null, List.of(), Map.of());
        String text = assertDoesNotThrow(() -> stubRenderer().render(bare, 1200),
                "A hand built entry without title and body must not break the render");
        assertTrue(text.contains("bioimage-analysis/patterns/bare"),
                "A missing title falls back to the entry key: " + text);
    }

    @Test
    void liveRegistryResolvesNodeFactsFromRealRegisteredNodes() {
        Map<String, JIPipeNodeInfo> infos = JIPipe.getNodes().getRegisteredNodeInfos();
        String deprecatedId = infos.entrySet().stream()
                .filter(entry -> entry.getValue().isDeprecated())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        assertNotNull(deprecatedId, "The core registry registers at least one deprecated node");
        JIPipeNodeInfo info = infos.get(deprecatedId);
        // The default renderer resolves through the live registry.
        String text = new KdbRenderer().render(entryWith(deprecatedId, "A body."), 1200);
        assertTrue(text.contains("deprecated"),
                "The deprecation badge comes from the live registry info: " + text);
        assertTrue(text.contains(info.getName()),
                "The display name comes from the live registry info: " + text);
        List<String> successors = info.getSuccessorNodeIds();
        if (successors.isEmpty()) {
            assertFalse(text.contains(" instead"), "No declared successor, no instead wording: " + text);
        } else {
            assertTrue(text.contains("instead"), "A declared successor renders the instead wording: " + text);
            for (String successorId : successors) {
                assertNotNull(infos.get(successorId), "Successors of registered nodes resolve");
                assertTrue(text.contains(infos.get(successorId).getName()),
                        "The successor display name comes from the live registry: " + text);
            }
        }
    }
}
