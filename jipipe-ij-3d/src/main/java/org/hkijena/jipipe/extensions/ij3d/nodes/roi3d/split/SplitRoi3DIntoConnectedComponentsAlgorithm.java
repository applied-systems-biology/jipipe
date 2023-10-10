package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.split;

import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurementSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Split 3D ROI into connected components", description = "Splits the input 3D ROI list into multiple ROI lists, one per connected component")
@JIPipeNode(menuPath = "Split", nodeTypeCategory = RoiNodeTypeCategory.class)
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Components", autoCreate = true)
public class SplitRoi3DIntoConnectedComponentsAlgorithm extends JIPipeIteratingAlgorithm {
    private final CustomExpressionVariablesParameter customFilterVariables;
    private OptionalAnnotationNameParameter componentNameAnnotation = new OptionalAnnotationNameParameter("Component", true);
    private DefaultExpressionParameter overlapFilter = new DefaultExpressionParameter("");
    private ROI3DRelationMeasurementSetParameter overlapFilterMeasurements = new ROI3DRelationMeasurementSetParameter();
    private boolean measureInPhysicalUnits = true;
    private boolean requireColocalization = true;
    private boolean preciseColocalization = true;

    private boolean ignoreC = true;
    private boolean ignoreT = true;

    public SplitRoi3DIntoConnectedComponentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        customFilterVariables = new CustomExpressionVariablesParameter(this);
    }

    public SplitRoi3DIntoConnectedComponentsAlgorithm(SplitRoi3DIntoConnectedComponentsAlgorithm other) {
        super(other);
        this.componentNameAnnotation = new OptionalAnnotationNameParameter(other.componentNameAnnotation);
        this.overlapFilter = new DefaultExpressionParameter(other.overlapFilter);
        this.overlapFilterMeasurements = new ROI3DRelationMeasurementSetParameter(other.overlapFilterMeasurements);
        this.customFilterVariables = new CustomExpressionVariablesParameter(other.customFilterVariables, this);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.requireColocalization = other.requireColocalization;
        this.preciseColocalization = other.preciseColocalization;
        this.ignoreC = other.ignoreC;
        this.ignoreT = other.ignoreT;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData roiList = dataBatch.getInputData("Input", ROI3DListData.class, progressInfo);
        ImageHandler imageHandler = IJ3DUtils.wrapImage(dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo));

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customFilterVariables.writeToVariables(variables, true, "custom", true, "custom");

        ResultsTableData measurements = new ResultsTableData();
        IJ3DUtils.measureRoi3dRelation(imageHandler,
                roiList,
                roiList,
                overlapFilterMeasurements.getNativeValue(),
                measureInPhysicalUnits,
                requireColocalization,
                preciseColocalization,
                ignoreC,
                ignoreT,
                "",
                measurements,
                progressInfo.resolve("Measure overlap"));

        progressInfo.log("Generating graph ...");
        DefaultUndirectedGraph<Integer, DefaultEdge> componentGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (int i = 0; i < roiList.size(); i++) {
            componentGraph.addVertex(i);
        }
        for (int row = 0; row < measurements.getRowCount(); row++) {
            int roi1Index = (int) measurements.getValueAsDouble(row, "Current.Index");
            int roi2Index = (int) measurements.getValueAsDouble(row, "Other.Index");
            if (StringUtils.isNullOrEmpty(overlapFilter.getExpression())) {
                if (requireColocalization && preciseColocalization) {
                    // Already fulfilled
                    componentGraph.addEdge(roi1Index, roi2Index);
                } else if (measurements.containsColumn("Colocalization")) {
                    if (measurements.getValueAsDouble(row, "Colocalization") > 0) {
                        componentGraph.addEdge(roi1Index, roi2Index);
                    }
                } else {
                    ROI3D roi1 = roiList.get(roi1Index);
                    ROI3D roi2 = roiList.get((int) measurements.getValueAsDouble(row, "Roi2.Index"));
                    if (roi1.getObject3D().hasOneVoxelColoc(roi2.getObject3D())) {
                        componentGraph.addEdge(roi1Index, roi2Index);
                    }
                }
            } else {
                for (int col = 0; col < measurements.getColumnCount(); col++) {
                    variables.set(measurements.getColumnName(col), measurements.getValueAt(row, col));
                }
                if (overlapFilter.test(variables)) {
                    componentGraph.addEdge(roi1Index, roi2Index);
                }
            }
        }

        progressInfo.log("Processing graph ...");
        ConnectivityInspector<Integer, DefaultEdge> connectivityInspector = new ConnectivityInspector<>(componentGraph);
        List<Set<Integer>> connectedSets = connectivityInspector.connectedSets();
        for (int i = 0; i < connectedSets.size(); i++) {
            Set<Integer> connectedSet = connectedSets.get(i);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            componentNameAnnotation.addAnnotationIfEnabled(annotations, "" + i);

            ROI3DListData componentList = new ROI3DListData();
            for (Integer index : connectedSet) {
                componentList.add(roiList.get(index));
            }
            dataBatch.addOutputData(getFirstOutputSlot(), componentList, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Ignore channel", description = "If enabled, ROI located at different channels are compared")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @JIPipeDocumentation(name = "Ignore frame", description = "If enabled, ROI located at different frames are compared")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }

    @JIPipeDocumentation(name = "Annotate with component", description = "If enabled, an annotation with the numeric component index is generated.")
    @JIPipeParameter("component-name-annotation")
    public OptionalAnnotationNameParameter getComponentNameAnnotation() {
        return componentNameAnnotation;
    }

    @JIPipeParameter("component-name-annotation")
    public void setComponentNameAnnotation(OptionalAnnotationNameParameter componentNameAnnotation) {
        this.componentNameAnnotation = componentNameAnnotation;
    }

    @JIPipeDocumentation(name = "Overlap filter", description = "Determines if two objects overlap. If left empty, the objects are tested for colocalization of at least one voxel.")
    @JIPipeParameter("overlap-filter")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = ROI3DRelationMeasurementExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public DefaultExpressionParameter getOverlapFilter() {
        return overlapFilter;
    }

    @JIPipeParameter("overlap-filter")
    public void setOverlapFilter(DefaultExpressionParameter overlapFilter) {
        this.overlapFilter = overlapFilter;
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements that will be generated")
    @JIPipeParameter("overlap-filter-measurements")
    public ROI3DRelationMeasurementSetParameter getOverlapFilterMeasurements() {
        return overlapFilterMeasurements;
    }

    @JIPipeParameter("overlap-filter-measurements")
    public void setOverlapFilterMeasurements(ROI3DRelationMeasurementSetParameter overlapFilterMeasurements) {
        this.overlapFilterMeasurements = overlapFilterMeasurements;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomFilterVariables() {
        return customFilterVariables;
    }

    @JIPipeDocumentation(name = "Only measure if objects co-localize", description = "If enabled, only co-localizing objects are measured")
    @JIPipeParameter("require-colocalization")
    public boolean isRequireColocalization() {
        return requireColocalization;
    }

    @JIPipeParameter("require-colocalization")
    public void setRequireColocalization(boolean requireColocalization) {
        this.requireColocalization = requireColocalization;
    }

    @JIPipeDocumentation(name = "Precise colocalization", description = "If enabled, the object co-localization for the 'Only measure if objects co-localize' setting tests for voxel colocalization (slower)." +
            " Otherwise, only the bounding boxes are compared (faster).")
    @JIPipeParameter("precise-colocalization")
    public boolean isPreciseColocalization() {
        return preciseColocalization;
    }

    @JIPipeParameter("precise-colocalization")
    public void setPreciseColocalization(boolean preciseColocalization) {
        this.preciseColocalization = preciseColocalization;
    }

    @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }
}
