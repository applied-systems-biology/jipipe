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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Assembles the agent facing text of a corpus entry (design spec 5.6): title,
 * kind, domain, resolved node lines, slot facts, the body, and the next steps
 * block.
 * <p>
 * The node facts are the drift kill: display names, deprecation badges, and
 * successor lines are resolved through the node info resolver at render time
 * (design spec 5.5.1: the render time joins are live), never from the corpus
 * and never cached in the entry. An id the resolver cannot resolve renders
 * verbatim: the launcher registry gate is the real check for retired ids, the
 * render must not throw.
 * <p>
 * The rendered text is bounded: the body is the only part that yields to the
 * token bound, title, node lines, and next steps always render completely. The
 * same text is what the vector lane embeds, which keeps the embedding and the
 * retrieval result consistent by construction.
 * <p>
 * Both external inputs are injectable functions, following the injectable
 * embedder pattern of {@link KdbVectorLane}: the node info resolver (production
 * resolves through the live node registry via {@link #liveRegistryResolver()},
 * the vector lane uses {@link #rawIdResolver()} so it never touches the
 * registry) and the neighbor function (production passes
 * {@code KdbEntryGraph::neighbors}; an empty function renders no related
 * entries). The section wording is part of the agent contract: it is written as
 * stable string literals here, never derived from code.
 */
public final class KdbRenderer {

    /**
     * The default token bound of a rendered entry (design spec 5.6: about
     * 1200 tokens; the harness mode based sizing picks the actual value).
     */
    public static final int DEFAULT_MAX_TOKENS = 1200;

    /**
     * The whitespace based token estimate is linear in the character count
     * with this many characters per token. Purpose: a hard, dependency free
     * ceiling for the rendered text. Real tokenizers split subword pieces and
     * price whitespace separately; four characters per token is the standard
     * prose approximation, so the estimate is a consistent budget, not a
     * tokenizer faithful count. The bound is enforced against this same
     * estimate, which keeps the truncation decision and the measured size
     * consistent by construction.
     */
    private static final int TOKEN_CHARS_PER_TOKEN = 4;

    private final Function<String, NodeFacts> nodeInfoResolver;
    private final Function<String, List<RelatedEntry>> neighborFunction;

    /**
     * Creates a renderer that resolves node facts from the live node registry
     * and renders no related entries (no neighbor function).
     */
    public KdbRenderer() {
        this(liveRegistryResolver(), entryKey -> List.of());
    }

    /**
     * Creates a renderer with injectable node facts and neighbors.
     *
     * @param nodeInfoResolver resolves a NODE_REF id to its registry facts, or
     *                         returns null for an unresolvable id (the id
     *                         renders verbatim)
     * @param neighborFunction resolves an entry key to its one hop related
     *                         entries, ordered as they should render
     */
    public KdbRenderer(Function<String, NodeFacts> nodeInfoResolver,
                       Function<String, List<RelatedEntry>> neighborFunction) {
        this.nodeInfoResolver = nodeInfoResolver;
        this.neighborFunction = neighborFunction;
    }

    /**
     * A node info resolver backed by the live node registry: display name,
     * deprecation state, and successor ids are read per call, never cached.
     * A registry that is not running or an unknown id resolves to null (the
     * id renders verbatim): the render must not throw, the launcher registry
     * gate is the real check for retired ids.
     *
     * @return the resolver function
     */
    public static Function<String, NodeFacts> liveRegistryResolver() {
        return KdbRenderer::liveRegistryNodeFacts;
    }

    /**
     * A node info resolver that resolves nothing: every node id renders
     * verbatim and no lifecycle badges appear. For contexts that must not
     * touch the node registry; the vector lane renders with this resolver so
     * the lane stays registry free.
     *
     * @return the resolver function
     */
    public static Function<String, NodeFacts> rawIdResolver() {
        return nodeId -> null;
    }

    private static NodeFacts liveRegistryNodeFacts(String nodeId) {
        if (!JIPipe.isInstantiated()) {
            return null;
        }
        try {
            JIPipeNodeInfo info = JIPipe.getNodes().getRegisteredNodeInfos().get(nodeId);
            if (info == null) {
                return null;
            }
            return new NodeFacts(info.getName(), info.isDeprecated(), info.getSuccessorNodeIds());
        } catch (RuntimeException e) {
            // A half initialized registry must not break the render: the id
            // renders verbatim.
            return null;
        }
    }

    /**
     * Renders the entry. Never throws on corpus content: a missing title
     * falls back to the entry key, an unresolvable node id renders verbatim.
     *
     * @param entry     the entry to render
     * @param maxTokens the token bound; the body yields, the structural
     *                  sections always render completely (a structural part
     *                  alone larger than the bound is never cut)
     * @return the rendered text
     */
    public String render(KdbEntry entry, int maxTokens) {
        StringBuilder text = new StringBuilder();
        String title = entry.getTitle() != null ? entry.getTitle() : entry.getEntryKey();
        text.append("# ").append(title).append("\n\n");
        text.append("Kind: ").append(entry.getKind().getId())
                .append(". Domain: ").append(entry.getDomain().getId()).append(".\n");
        List<String> nodeLines = nodeLines(entry);
        if (!nodeLines.isEmpty()) {
            text.append("\nNodes:\n");
            for (String nodeLine : nodeLines) {
                text.append(nodeLine).append('\n');
            }
        }
        List<String> factLines = factLines(entry);
        if (!factLines.isEmpty()) {
            text.append('\n');
            for (String factLine : factLines) {
                text.append(factLine).append('\n');
            }
        }
        String body = entry.getBody();
        if (body != null && !body.isBlank()) {
            text.append('\n');
            // The body is measured against the bound after the structural
            // part is complete and the next steps block is reserved: the
            // estimate of the whole text stays under the bound whenever the
            // structural sections fit.
            String nextSteps = nextSteps(entry.getEntryKey());
            int used = estimateTokens(text.toString()) + estimateTokens(nextSteps);
            int budget = Math.max(0, maxTokens - used);
            text.append(truncateToTokens(body.strip() + "\n", budget));
            text.append(nextSteps);
        } else {
            text.append(nextSteps(entry.getEntryKey()));
        }
        return text.toString();
    }

    /**
     * The resolved node lines of the NODE_REF slots, in the declared slot
     * order, first seen first, every id once.
     */
    private List<String> nodeLines(KdbEntry entry) {
        Set<String> seen = new LinkedHashSet<>();
        for (KdbSlotDef slotDef : entry.getKind().getSlots()) {
            if (slotDef.type() != KdbValueType.NODE_REF) {
                continue;
            }
            List<Object> values = entry.getSlotValues().get(slotDef.name());
            if (values == null) {
                continue;
            }
            for (Object value : values) {
                if (value instanceof String nodeId) {
                    seen.add(nodeId);
                }
            }
        }
        List<String> lines = new ArrayList<>();
        for (String nodeId : seen) {
            lines.add(nodeLine(nodeId));
        }
        return lines;
    }

    /**
     * One node line. The shapes follow the successor wording family of the
     * node documentation: "- Name (id) - deprecated, use Successor (id)
     * instead", "- Name (id) - deprecated", "- Name (id)", and the raw "- id"
     * for an unresolvable node.
     */
    private String nodeLine(String nodeId) {
        NodeFacts facts = nodeInfoResolver.apply(nodeId);
        if (facts == null) {
            return "- " + nodeId;
        }
        StringBuilder line = new StringBuilder("- ").append(facts.displayName())
                .append(" (").append(nodeId).append(')');
        if (facts.deprecated()) {
            line.append(" - deprecated");
            List<String> successors = facts.successorNodeIds();
            if (!successors.isEmpty()) {
                line.append(", use ");
                for (int i = 0; i < successors.size(); i++) {
                    if (i > 0) {
                        line.append(", ");
                    }
                    String successorId = successors.get(i);
                    NodeFacts successorFacts = nodeInfoResolver.apply(successorId);
                    if (successorFacts != null) {
                        line.append(successorFacts.displayName()).append(" (").append(successorId).append(')');
                    } else {
                        line.append(successorId);
                    }
                }
                line.append(" instead");
            }
        }
        return line.toString();
    }

    /**
     * The slot fact lines: TEXT, NUMBER, and VERSION slots render as "- slot:
     * value" in the declared slot order. NODE_REF slots are the node lines,
     * ENTRY_REF slots surface in the next steps block.
     */
    private List<String> factLines(KdbEntry entry) {
        List<String> lines = new ArrayList<>();
        for (KdbSlotDef slotDef : entry.getKind().getSlots()) {
            if (slotDef.type() == KdbValueType.NODE_REF || slotDef.type() == KdbValueType.ENTRY_REF) {
                continue;
            }
            List<Object> values = entry.getSlotValues().get(slotDef.name());
            if (values == null) {
                continue;
            }
            for (Object value : values) {
                lines.add("- " + slotDef.name() + ": " + value);
            }
        }
        return lines;
    }

    /**
     * The next steps block: the one hop related entries with their relation
     * labels and the {@code get_knowledge_entry} pointer, then the domain
     * list pointer (design spec 5.6).
     */
    private String nextSteps(String entryKey) {
        StringBuilder block = new StringBuilder("\nNext steps:\n");
        for (RelatedEntry neighbor : neighborFunction.apply(entryKey)) {
            block.append("- ").append(neighbor.relation()).append(": ").append(neighbor.entryKey())
                    .append(" (use get_knowledge_entry)\n");
        }
        block.append("- use list_knowledge_domains for the domain list\n");
        return block.toString();
    }

    /**
     * The whitespace based token estimate of a text: the character count
     * divided by {@link #TOKEN_CHARS_PER_TOKEN}, rounded up. Package private
     * for tests that pin the bound against the same measure the renderer
     * uses.
     *
     * @param text the text, or null
     * @return the estimated token count
     */
    static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / (double) TOKEN_CHARS_PER_TOKEN);
    }

    /**
     * Cuts a text down to the token budget: a text that fits returns whole,
     * otherwise it is cut at the budget's character equivalent and backed off
     * to the last word boundary, so the rendered body never ends inside a
     * word. A text without a usable word boundary inside the budget is
     * dropped entirely rather than emitted as a partial word.
     */
    private static String truncateToTokens(String text, int budget) {
        if (budget <= 0 || text.isEmpty()) {
            return "";
        }
        if (estimateTokens(text) <= budget) {
            return text;
        }
        int maxChars = budget * TOKEN_CHARS_PER_TOKEN;
        String cut = text.substring(0, maxChars);
        int boundary = Math.max(cut.lastIndexOf(' '), cut.lastIndexOf('\n'));
        if (boundary <= 0) {
            return "";
        }
        return cut.substring(0, boundary);
    }

    /**
     * The registry facts of one node, as needed by the node lines. Produced
     * by the injectable resolver; a null result (not this record with empty
     * fields) marks the node as unresolvable.
     *
     * @param displayName      the registry display name
     * @param deprecated       whether the node is deprecated
     * @param successorNodeIds the successor ids, ordered from most to least
     *                         preferred; empty when the node has no successor
     */
    public record NodeFacts(String displayName, boolean deprecated, List<String> successorNodeIds) {

        /**
         * Creates the facts. A null successor list becomes empty.
         */
        public NodeFacts {
            successorNodeIds = successorNodeIds == null ? List.of() : List.copyOf(successorNodeIds);
        }
    }
}
