package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.measure;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.ROI2DRelationMeasurementSetParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Extract pairwise 2D ROI statistics", description = "Extracts all pairwise statistics between the 2D ROI")
@DefineJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI 1", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI 2", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", create = true)
public class ExtractRoi2DRelationStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private ROI2DRelationMeasurementSetParameter measurements = new ROI2DRelationMeasurementSetParameter();

    private boolean measureInPhysicalUnits = true;
    private boolean requireColocalization = true;
    private boolean preciseColocalization = true;

    public ExtractRoi2DRelationStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractRoi2DRelationStatisticsAlgorithm(ExtractRoi2DRelationStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ROI2DRelationMeasurementSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.requireColocalization = other.requireColocalization;
        this.preciseColocalization = other.preciseColocalization;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData roi1List = iterationStep.getInputData("ROI 1", ROIListData.class, progressInfo);
        ROIListData roi2List = iterationStep.getInputData("ROI 2", ROIListData.class, progressInfo);
        ImagePlus reference = ImageJUtils.unwrap(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));
        ResultsTableData outputResults = new ResultsTableData();

        ImageJAlgorithmUtils.measureROIRelation(reference,
                roi1List,
                roi2List,
                measurements.getNativeValue(),
                measureInPhysicalUnits,
                requireColocalization,
                preciseColocalization,
                "",
                outputResults,
                progressInfo.resolve("Measure ROI"));

        iterationStep.addOutputData(getFirstOutputSlot(), outputResults, progressInfo);
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

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements that will be extracted")
    @JIPipeParameter("measurements")
    public ROI2DRelationMeasurementSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ROI2DRelationMeasurementSetParameter measurements) {
        this.measurements = measurements;
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
