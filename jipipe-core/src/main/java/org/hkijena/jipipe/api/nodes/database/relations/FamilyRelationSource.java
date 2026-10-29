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
import org.hkijena.jipipe.api.nodes.taxonomy.FamilyAssembler;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamily;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.function.Consumer;

/**
 * The {@link NodeRelationSource} backed by the taxonomy's workflow families
 * (spec §10.2, source 1).
 * <p>
 * For every registered family it reads the directed step-workflow graph from
 * the {@link FamilyAssembler} and emits one {@link Edge} per workflow
 * connection - in the graph's natural direction, predecessor step →
 * successor step - so a family step {@code A → B} contributes
 * {@code next(A) += B} and {@code previous(B) += A}. The family graph
 * direction is what supplies the previously-missing <em>predecessor</em>
 * direction (the legacy {@code followUps} query only exposed successors).
 * <p>
 * The family graph's vertices are exactly the family's step ids, so every
 * edge maps to a step of that family - there is nothing to resolve or skip
 * here (contrast with {@link TemplateRelationSource}, whose graphs can
 * reference removed nodes and must be guarded).
 */
public class FamilyRelationSource implements NodeRelationSource {

    /**
     * Strips the {@code familyId + "#"} prefix from a step id to recover the
     * node id it refers to.
     */
    private static String nodeId(String familyId, String stepId) {
        String prefix = FamilyAssembler.stepId(familyId, "");
        return stepId.startsWith(prefix) ? stepId.substring(prefix.length()) : stepId;
    }

    @Override
    public void contribute(Consumer<Edge> sink) {
        FamilyAssembler assembler = JIPipe.getNodes().getFamilyAssembler();
        for (JIPipeNodeTaxonomyFamily family : JIPipe.getNodes().getFamilies().values()) {
            String familyId = family.getId();
            Graph<String, DefaultEdge> graph = assembler.graph(family);
            for (DefaultEdge edge : graph.edgeSet()) {
                // In the assembler's graph the edge points predecessor -> successor.
                String fromNodeId = nodeId(familyId, graph.getEdgeSource(edge));
                String toNodeId = nodeId(familyId, graph.getEdgeTarget(edge));
                sink.accept(new Edge(fromNodeId, toNodeId));
            }
        }
    }
}
