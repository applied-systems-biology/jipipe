package org.hkijena.jipipe.extensions.multiparameters.datasources;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.*;

@JIPipeDocumentation(name = "Extract parameters from node", description = "Extracts parameters from the incoming node(s)")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Node", autoCreate = true)
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ExtractParametersAlgorithm extends JIPipeAlgorithm {

    private OptionalAnnotationNameParameter nodeIdAnnotation = new OptionalAnnotationNameParameter("Node ID", true);
    private OptionalAnnotationNameParameter nodeNameAnnotation = new OptionalAnnotationNameParameter("Node name", true);
    private StringQueryExpression parameterKeyFilter = new StringQueryExpression();

    public ExtractParametersAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractParametersAlgorithm(ExtractParametersAlgorithm other) {
        super(other);
        this.nodeIdAnnotation = new OptionalAnnotationNameParameter(other.nodeIdAnnotation);
        this.nodeNameAnnotation = new OptionalAnnotationNameParameter(other.nodeNameAnnotation);
        this.parameterKeyFilter = new StringQueryExpression(other.parameterKeyFilter);
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @JIPipeDocumentation(name = "Annotate with node ID", description = "If enabled, the output parameters are annotated with the unique node ID")
    @JIPipeParameter("node-id-annotation")
    public OptionalAnnotationNameParameter getNodeIdAnnotation() {
        return nodeIdAnnotation;
    }

    @JIPipeParameter("node-id-annotation")
    public void setNodeIdAnnotation(OptionalAnnotationNameParameter nodeIdAnnotation) {
        this.nodeIdAnnotation = nodeIdAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with node name", description = "If enabled, the output parameters are annotated with the node name")
    @JIPipeParameter("node-name-annotation")
    public OptionalAnnotationNameParameter getNodeNameAnnotation() {
        return nodeNameAnnotation;
    }

    @JIPipeParameter("node-name-annotation")
    public void setNodeNameAnnotation(OptionalAnnotationNameParameter nodeNameAnnotation) {
        this.nodeNameAnnotation = nodeNameAnnotation;
    }

    @JIPipeDocumentation(name = "Parameter key filter", description = "Allows to filter only specific parameter keys. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("parameter-key-filter")
    public StringQueryExpression getParameterKeyFilter() {
        return parameterKeyFilter;
    }

    @JIPipeParameter("parameter-key-filter")
    public void setParameterKeyFilter(StringQueryExpression parameterKeyFilter) {
        this.parameterKeyFilter = parameterKeyFilter;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        if (isPassThrough()) {
            return;
        }

        // Collect input nodes
        Set<JIPipeGraphNode> nodeSet = new HashSet<>();
        for (JIPipeDataSlot sourceSlot : getGraph().getSourceSlots(getFirstInputSlot())) {
            nodeSet.add(sourceSlot.getNode());
        }

        for (JIPipeGraphNode node : nodeSet) {
            JIPipeParameterTree tree = new JIPipeParameterTree(node);
            ParametersData parametersData = new ParametersData();

            for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                if(parameterKeyFilter.test(entry.getKey())) {
                    Object value = entry.getValue().get(Object.class);
                    value = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getFieldClass()).duplicate(value);
                    parametersData.getParameterData().put(entry.getKey(), value);
                }
            }

            List<JIPipeAnnotation> annotationList = new ArrayList<>();
            nodeIdAnnotation.addAnnotationIfEnabled(annotationList, node.getIdInGraph());
            nodeNameAnnotation.addAnnotationIfEnabled(annotationList, node.getName());
            getFirstOutputSlot().addData(parametersData, annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting,progressInfo);
        }
    }
}
