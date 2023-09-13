/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Provides the list of available variables.
 */
public interface ExpressionParameterVariableSource {
    /**
     * Returns the list of known variables for the user interface.
     *
     * @param parameterTree the parameter tree that contains the access. can be null.
     * @param parameterAccess the parameter access that holds the {@link AbstractExpressionParameter} instance. can be null.
     * @return the set of variables
     */
    Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess);

    /**
     * Helper method that attempts to find the underlying graph node of a parameter access and returns its cache if available
     * @param parameterTree the parameter tree. can be null.
     * @param parameterAccess the parameter access. can be null.
     * @return the graph node caches (by predecessor node UUIDs) or an empty map
     */
    static Map<UUID, Map<String, JIPipeDataTable>> findPredecessorNodeCache(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        JIPipeGraphNode node = ExpressionParameterVariableSource.findNode(parameterTree, parameterAccess);
        Map<UUID, Map<String, JIPipeDataTable>> result = new HashMap<>();
        if(node != null) {
            JIPipeProject project = node.getParentGraph().getProject();
            if(project != null) {
                // Remap to project node
                JIPipeGraph graph = project.getGraph();
                node = graph.getNodeByUUID(node.getUUIDInParentGraph());
                if(node != null) {
                    for (JIPipeGraphNode predecessorNode : graph.getPredecessorNodes(node, graph.traverse())) {
                        Map<String, JIPipeDataTable> cache = project.getCache().query(predecessorNode, predecessorNode.getUUIDInParentGraph(), new JIPipeProgressInfo());
                        if(!cache.isEmpty()) {
                            result.put(predecessorNode.getUUIDInParentGraph(), cache);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Helper method that attempts to find the underlying graph node of a parameter access
     * @param parameterTree the parameter tree. can be null.
     * @param parameterAccess the parameter access. can be null.
     * @return the graph node or null
     */
    static JIPipeGraphNode findNode(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        if(parameterAccess != null) {
            if (parameterAccess.getSource() instanceof JIPipeGraphNode) {
                return (JIPipeGraphNode) parameterAccess.getSource();
            }
        }
        if(parameterTree != null && parameterAccess != null && parameterAccess.getSource() != null) {
            JIPipeParameterTree.Node node = parameterTree.getSourceNode(parameterAccess.getSource());
            while(node != null) {
                if(node.getCollection() instanceof JIPipeGraphNode) {
                    return (JIPipeGraphNode) node.getCollection();
                }
                node = node.getParent();
            }
        }
        if(parameterTree != null && parameterTree.getRoot() != null && parameterTree.getRoot().getCollection() instanceof JIPipeGraphNode) {
            return (JIPipeGraphNode) parameterTree.getRoot().getCollection();
        }
        return null;
    }
}
