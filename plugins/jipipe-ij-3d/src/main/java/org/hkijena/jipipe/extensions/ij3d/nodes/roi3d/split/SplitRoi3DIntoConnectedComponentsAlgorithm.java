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

package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.split;

import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurementSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SetJIPipeDocumentation(name = "Split 3D ROI into connected components", description = "Splits the input 3D ROI list into multiple ROI lists, one per connected component")
@ConfigureJIPipeNode(menuPath = "Split", nodeTypeCategory = RoiNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, slotName = "Components", create = true)
public class SplitRoi3DIntoConnectedComponentsAlgorithm extends JIPipeIteratingAlgorithm {
    private OptionalTextAnnotationNameParameter componentNameAnnotation = new OptionalTextAnnotationNameParameter("Component", true);
    private JIPipeExpressionParameter overlapFilter = new JIPipeExpressionParameter("");
    private ROI3DRelationMeasurementSetParameter overlapFilterMeasurements = new ROI3DRelationMeasurementSetParameter();
    private boolean measureInPhysicalUnits = true;
    private boolean requireColocalization = true;
    private boolean preciseColocalization = true;

    private boolean ignoreC = true;
    private boolean ignoreT = true;

    public SplitRoi3DIntoConnectedComponentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitRoi3DIntoConnectedComponentsAlgorithm(SplitRoi3DIntoConnectedComponentsAlgorithm other) {
        super(other);
        this.componentNameAnnotation = new OptionalTextAnnotationNameParameter(other.componentNameAnnotation);
        this.overlapFilter = new JIPipeExpressionParameter(other.overlapFilter);
        this.overlapFilterMeasurements = new ROI3DRelationMeasurementSetParameter(other.overlapFilterMeasurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.requireColocalization = other.requireColocalization;
        this.preciseColocalization = other.preciseColocalization;
        this.ignoreC = other.ignoreC;
        this.ignoreT = other.ignoreT;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData roiList = iterationStep.getInputData("Input", ROI3DListData.class, progressInfo);
        ImageHandler imageHandler = IJ3DUtils.wrapImage(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap()
                .putAnnotations(iterationStep.getMergedTextAnnotations())
                .putCustomVariables(getDefaultCustomExpressionVariables());

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
            iterationStep.addOutputData(getFirstOutputSlot(), componentList, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Ignore channel", description = "If enabled, ROI located at different channels are compared")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @SetJIPipeDocumentation(name = "Ignore frame", description = "If enabled, ROI located at different frames are compared")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }

    @SetJIPipeDocumentation(name = "Annotate with component", description = "If enabled, an annotation with the numeric component index is generated.")
    @JIPipeParameter("component-name-annotation")
    public OptionalTextAnnotationNameParameter getComponentNameAnnotation() {
        return componentNameAnnotation;
    }

    @JIPipeParameter("component-name-annotation")
    public void setComponentNameAnnotation(OptionalTextAnnotationNameParameter componentNameAnnotation) {
        this.componentNameAnnotation = componentNameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Overlap filter", description = "Determines if two objects overlap. If left empty, the objects are tested for colocalization of at least one voxel.")
    @JIPipeParameter("overlap-filter")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = ROI3DRelationMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public JIPipeExpressionParameter getOverlapFilter() {
        return overlapFilter;
    }

    @JIPipeParameter("overlap-filter")
    public void setOverlapFilter(JIPipeExpressionParameter overlapFilter) {
        this.overlapFilter = overlapFilter;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements that will be generated")
    @JIPipeParameter("overlap-filter-measurements")
    public ROI3DRelationMeasurementSetParameter getOverlapFilterMeasurements() {
        return overlapFilterMeasurements;
    }

    @JIPipeParameter("overlap-filter-measurements")
    public void setOverlapFilterMeasurements(ROI3DRelationMeasurementSetParameter overlapFilterMeasurements) {
        this.overlapFilterMeasurements = overlapFilterMeasurements;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Only measure if objects co-localize", description = "If enabled, only co-localizing objects are measured")
    @JIPipeParameter("require-colocalization")
    public boolean isRequireColocalization() {
        return requireColocalization;
    }

    @JIPipeParameter("require-colocalization")
    public void setRequireColocalization(boolean requireColocalization) {
        this.requireColocalization = requireColocalization;
    }

    @SetJIPipeDocumentation(name = "Precise colocalization", description = "If enabled, the object co-localization for the 'Only measure if objects co-localize' setting tests for voxel colocalization (slower)." +
            " Otherwise, only the bounding boxes are compared (faster).")
    @JIPipeParameter("precise-colocalization")
    public boolean isPreciseColocalization() {
        return preciseColocalization;
    }

    @JIPipeParameter("precise-colocalization")
    public void setPreciseColocalization(boolean preciseColocalization) {
        this.preciseColocalization = preciseColocalization;
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }
}
