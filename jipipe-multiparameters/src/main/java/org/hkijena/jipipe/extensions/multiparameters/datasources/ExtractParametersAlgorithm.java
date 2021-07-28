package org.hkijena.jipipe.extensions.multiparameters.datasources;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Extract parameters from node", description = "Extracts parameters from the incoming node(s)")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Node", autoCreate = true)
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ExtractParametersAlgorithm extends JIPipeAlgorithm {

    private OptionalAnnotationNameParameter nodeUUIDAnnotation = new OptionalAnnotationNameParameter("Node UUID", true);
    private OptionalAnnotationNameParameter nodeAliasIDAnnotation = new OptionalAnnotationNameParameter("Node alias ID", true);
    private OptionalAnnotationNameParameter nodeNameAnnotation = new OptionalAnnotationNameParameter("Node name", true);
    private StringQueryExpression parameterKeyFilter = new StringQueryExpression();

    public ExtractParametersAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractParametersAlgorithm(ExtractParametersAlgorithm other) {
        super(other);
        this.nodeUUIDAnnotation = new OptionalAnnotationNameParameter(other.nodeUUIDAnnotation);
        this.nodeAliasIDAnnotation = new OptionalAnnotationNameParameter(other.nodeAliasIDAnnotation);
        this.nodeNameAnnotation = new OptionalAnnotationNameParameter(other.nodeNameAnnotation);
        this.parameterKeyFilter = new StringQueryExpression(other.parameterKeyFilter);
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @JIPipeDocumentation(name = "Annotate with node UUID", description = "If enabled, the output parameters are annotated with the unique node ID")
    @JIPipeParameter("node-uuid-annotation")
    public OptionalAnnotationNameParameter getNodeUUIDAnnotation() {
        return nodeUUIDAnnotation;
    }

    @JIPipeParameter("node-uuid-annotation")
    public void setNodeUUIDAnnotation(OptionalAnnotationNameParameter nodeUUIDAnnotation) {
        this.nodeUUIDAnnotation = nodeUUIDAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with node alias ID", description = "If enabled, the output parameters are annotated with the unique node ID (human-readable)")
    @JIPipeParameter("node-alias-id-annotation")
    public OptionalAnnotationNameParameter getNodeAliasIDAnnotation() {
        return nodeAliasIDAnnotation;
    }

    @JIPipeParameter("node-alias-id-annotation")
    public void setNodeAliasIDAnnotation(OptionalAnnotationNameParameter nodeAliasIDAnnotation) {
        this.nodeAliasIDAnnotation = nodeAliasIDAnnotation;
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
                if (parameterKeyFilter.test(entry.getKey())) {
                    Object value = entry.getValue().get(Object.class);
                    value = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getFieldClass()).duplicate(value);
                    parametersData.getParameterData().put(entry.getKey(), value);
                }
            }

            List<JIPipeAnnotation> annotationList = new ArrayList<>();
            nodeUUIDAnnotation.addAnnotationIfEnabled(annotationList, node.getUUIDInGraph().toString());
            nodeAliasIDAnnotation.addAnnotationIfEnabled(annotationList, node.getAliasIdInGraph());
            nodeNameAnnotation.addAnnotationIfEnabled(annotationList, node.getName());
            getFirstOutputSlot().addData(parametersData, annotationList, JIPipeAnnotationMergeStrategy.OverwriteExisting, progressInfo);
        }
    }
}
