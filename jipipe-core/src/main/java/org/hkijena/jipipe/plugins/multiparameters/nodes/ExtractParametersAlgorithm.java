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

package org.hkijena.jipipe.plugins.multiparameters.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.*;

@SetJIPipeDocumentation(name = "Extract parameters from node", description = "Extracts parameters from the incoming node(s)")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Node", create = true)
@AddJIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ExtractParametersAlgorithm extends JIPipeAlgorithm {

    private OptionalTextAnnotationNameParameter nodeUUIDAnnotation = new OptionalTextAnnotationNameParameter("Node UUID", true);
    private OptionalTextAnnotationNameParameter nodeAliasIDAnnotation = new OptionalTextAnnotationNameParameter("Node alias ID", true);
    private OptionalTextAnnotationNameParameter nodeNameAnnotation = new OptionalTextAnnotationNameParameter("Node name", true);
    private StringQueryExpression parameterKeyFilter = new StringQueryExpression();

    public ExtractParametersAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractParametersAlgorithm(ExtractParametersAlgorithm other) {
        super(other);
        this.nodeUUIDAnnotation = new OptionalTextAnnotationNameParameter(other.nodeUUIDAnnotation);
        this.nodeAliasIDAnnotation = new OptionalTextAnnotationNameParameter(other.nodeAliasIDAnnotation);
        this.nodeNameAnnotation = new OptionalTextAnnotationNameParameter(other.nodeNameAnnotation);
        this.parameterKeyFilter = new StringQueryExpression(other.parameterKeyFilter);
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Annotate with node UUID", description = "If enabled, the output parameters are annotated with the unique node ID")
    @JIPipeParameter("node-uuid-annotation")
    public OptionalTextAnnotationNameParameter getNodeUUIDAnnotation() {
        return nodeUUIDAnnotation;
    }

    @JIPipeParameter("node-uuid-annotation")
    public void setNodeUUIDAnnotation(OptionalTextAnnotationNameParameter nodeUUIDAnnotation) {
        this.nodeUUIDAnnotation = nodeUUIDAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with node alias ID", description = "If enabled, the output parameters are annotated with the unique node ID (human-readable)")
    @JIPipeParameter("node-alias-id-annotation")
    public OptionalTextAnnotationNameParameter getNodeAliasIDAnnotation() {
        return nodeAliasIDAnnotation;
    }

    @JIPipeParameter("node-alias-id-annotation")
    public void setNodeAliasIDAnnotation(OptionalTextAnnotationNameParameter nodeAliasIDAnnotation) {
        this.nodeAliasIDAnnotation = nodeAliasIDAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with node name", description = "If enabled, the output parameters are annotated with the node name")
    @JIPipeParameter("node-name-annotation")
    public OptionalTextAnnotationNameParameter getNodeNameAnnotation() {
        return nodeNameAnnotation;
    }

    @JIPipeParameter("node-name-annotation")
    public void setNodeNameAnnotation(OptionalTextAnnotationNameParameter nodeNameAnnotation) {
        this.nodeNameAnnotation = nodeNameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Parameter key filter", description = "Allows to filter only specific parameter keys. ")
    @JIPipeParameter("parameter-key-filter")
    public StringQueryExpression getParameterKeyFilter() {
        return parameterKeyFilter;
    }

    @JIPipeParameter("parameter-key-filter")
    public void setParameterKeyFilter(StringQueryExpression parameterKeyFilter) {
        this.parameterKeyFilter = parameterKeyFilter;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (isPassThrough()) {
            return;
        }

        // Collect input nodes
        Set<JIPipeGraphNode> nodeSet = new HashSet<>();
        for (JIPipeDataSlot sourceSlot : getParentGraph().getInputIncomingSourceSlots(getFirstInputSlot())) {
            nodeSet.add(sourceSlot.getNode());
        }

        for (JIPipeGraphNode node : nodeSet) {
            JIPipeParameterTree tree = new JIPipeParameterTree(node);
            ParametersData parametersData = new ParametersData();

            for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                if (parameterKeyFilter.test(entry.getKey())) {
                    Object value = entry.getValue().get(Object.class);
                    value = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getFieldClass()).duplicate(value);
                    parametersData.getParameterData().put(entry.getKey(), value);
                }
            }

            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            nodeUUIDAnnotation.addAnnotationIfEnabled(annotationList, node.getUUIDInParentGraph().toString());
            nodeAliasIDAnnotation.addAnnotationIfEnabled(annotationList, node.getAliasIdInParentGraph());
            nodeNameAnnotation.addAnnotationIfEnabled(annotationList, node.getName());
            getFirstOutputSlot().addData(parametersData, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, JIPipeDataContext.create(this), progressInfo);
        }
    }
}
