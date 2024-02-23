package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.ome;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.OMEAccessorParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate with OME metadata", description = "Annotates an OME image with metadata extracted from the OME metadata")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For images")
@AddJIPipeInputSlot(value = OMEImageData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = OMEImageData.class, slotName = "Output", create = true)
public class AnnotateOMEWithMetadataAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);
    private JIPipeTextAnnotationMergeMode annotationMergeMode = JIPipeTextAnnotationMergeMode.Merge;

    public AnnotateOMEWithMetadataAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public AnnotateOMEWithMetadataAlgorithm(AnnotateOMEWithMetadataAlgorithm other) {
        super(other);
        this.entries = new ParameterCollectionList(other.entries);
        this.annotationMergeMode = other.annotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEImageData data = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            String annotationName = entry.getAnnotationName().evaluateToString(variables);
            String annotationValue = StringUtils.nullToEmpty(entry.getAccessor().evaluateToString(data.getMetadata()));
            annotationList.add(new JIPipeTextAnnotation(annotationName, annotationValue));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeMode, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Generated annotations", description = "The list of generated annotations.")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @SetJIPipeDocumentation(name = "Annotation merge mode", description = "Determines how newly generated annotations are merged with existing ones")
    @JIPipeParameter("annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeMode() {
        return annotationMergeMode;
    }

    @JIPipeParameter("annotation-merge-mode")
    public void setAnnotationMergeMode(JIPipeTextAnnotationMergeMode annotationMergeMode) {
        this.annotationMergeMode = annotationMergeMode;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private OMEAccessorParameter accessor = new OMEAccessorParameter();
        private JIPipeExpressionParameter annotationName = new JIPipeExpressionParameter("\"Annotation name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.accessor = new OMEAccessorParameter(other.accessor);
            this.annotationName = new JIPipeExpressionParameter(other.annotationName);
        }

        @SetJIPipeDocumentation(name = "OME metadata", description = "The metadata to query from OME")
        @JIPipeParameter(value = "accessor", uiOrder = -100)
        public OMEAccessorParameter getAccessor() {
            return accessor;
        }

        @JIPipeParameter("accessor")
        public void setAccessor(OMEAccessorParameter accessor) {
            this.accessor = accessor;
        }

        @SetJIPipeDocumentation(name = "Annotation name", description = "The name of the output annotation.")
        @JIPipeParameter(value = "annotation-name", uiOrder = -90)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getAnnotationName() {
            return annotationName;
        }

        @JIPipeParameter("annotation-name")
        public void setAnnotationName(JIPipeExpressionParameter annotationName) {
            this.annotationName = annotationName;
        }
    }
}
