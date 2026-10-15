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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The {@link NodeRelationSource} backed by node template graphs
 * (spec §10.2, source 2).
 * <p>
 * Mines <b>every</b> loaded template graph — System, Global, and Project
 * scope (the user directed "mine the full extent from the templates"; the
 * database already holds all scopes, so relations are per-entry data across
 * the whole set). For each in-template connection {@code A → B} it emits one
 * {@link Edge} in the graph's natural direction, so {@code next(A) += B} and
 * {@code previous(B) += A}.
 * <p>
 * <b>Stale edges.</b> A template's serialized graph can reference a node id
 * that no longer resolves in the registry (the node was removed, or an
 * extension failed to load). Such an edge is skipped with a
 * {@code logger.warn} — never propagated, never thrown — so a stale template
 * cannot break the index rebuild or any {@code relations()} call
 * (Review Focus #2).
 */
public class TemplateRelationSource implements NodeRelationSource {

    private static final Logger logger = LoggerFactory.getLogger(TemplateRelationSource.class);

    /** The project whose project-scoped templates are mined in addition to
     * System and Global templates. May be {@code null} (then only System and
     * Global templates are mined). */
    private final JIPipeProject project;

    public TemplateRelationSource(JIPipeProject project) {
        this.project = project;
    }

    @Override
    public void contribute(Consumer<Edge> sink) {
        List<JIPipeNodeTemplate> templates = JIPipe.getNodeTemplates().getAllTemplates(project);
        for (JIPipeNodeTemplate template : templates) {
            // A single corrupted template (invalid serialized data, a graph
            // that throws on access, ...) must not abort the whole index
            // build — the same "never fail" intent as the stale-edge case,
            // and consistent with the database builder's own
            // catch (Throwable ignored) around template entry construction.
            try {
                contributeTemplate(template, sink);
            } catch (Throwable e) {
                logger.warn("Skipping corrupted template \"{}\" in relation mining", template.getName(), e);
            }
        }
    }

    /**
     * Mines the edges of a single template graph. Any failure here (a
     * corrupted graph, a slot whose node has no info, ...) is caught by
     * {@link #contribute(Consumer)} and that template is skipped.
     */
    private void contributeTemplate(JIPipeNodeTemplate template, Consumer<Edge> sink) {
        JIPipeGraph graph = template.getGraph();
        if (graph == null) {
            return;
        }
        String templateName = template.getName();
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> connection : graph.getSlotEdges()) {
            // getSlotEdges(): key = source slot, value = target slot
            JIPipeGraphNode sourceNode = connection.getKey().getNode();
            JIPipeGraphNode targetNode = connection.getValue().getNode();
            String fromId = resolvedNodeId(sourceNode);
            String toId = resolvedNodeId(targetNode);
            if (fromId == null || toId == null) {
                logger.warn("Skipping stale template edge in template '{}': unresolved node info id (from='{}', to='{}')",
                    templateName, idOr(sourceNode), idOr(targetNode));
                continue;
            }
            sink.accept(new Edge(fromId, toId));
        }
    }

    /**
     * Resolves a template-graph node to its node-info id, or {@code null} if
     * the node (or its info id) no longer resolves in the registry.
     */
    private static String resolvedNodeId(JIPipeGraphNode node) {
        if (node == null || node.getInfo() == null) {
            return null;
        }
        JIPipeNodeInfo info = node.getInfo();
        if (info.getId() == null) {
            return null;
        }
        return JIPipe.getNodes().hasNodeInfoWithId(info.getId()) ? info.getId() : null;
    }

    private static String idOr(JIPipeGraphNode node) {
        return (node != null && node.getInfo() != null) ? node.getInfo().getId() : String.valueOf(node);
    }
}
