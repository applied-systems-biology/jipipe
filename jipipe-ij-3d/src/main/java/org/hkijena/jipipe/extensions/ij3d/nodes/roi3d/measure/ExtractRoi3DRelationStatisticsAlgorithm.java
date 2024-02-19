package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.measure;

import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurementSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Extract pairwise 3D ROI statistics", description = "Extracts all pairwise statistics between the 3D ROI")
@DefineJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI 1", create = true)
@AddJIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI 2", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", create = true)
public class ExtractRoi3DRelationStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private ROI3DRelationMeasurementSetParameter measurements = new ROI3DRelationMeasurementSetParameter();

    private boolean measureInPhysicalUnits = true;
    private boolean requireColocalization = true;

    private boolean preciseColocalization = true;

    private boolean ignoreC = true;
    private boolean ignoreT = true;

    public ExtractRoi3DRelationStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractRoi3DRelationStatisticsAlgorithm(ExtractRoi3DRelationStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ROI3DRelationMeasurementSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.requireColocalization = other.requireColocalization;
        this.preciseColocalization = other.preciseColocalization;
        this.ignoreC = other.ignoreC;
        this.ignoreT = other.ignoreT;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi1List = iterationStep.getInputData("ROI 1", ROI3DListData.class, progressInfo);
        ROI3DListData roi2List = iterationStep.getInputData("ROI 2", ROI3DListData.class, progressInfo);
        ImageHandler imageHandler = IJ3DUtils.wrapImage(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));
        ResultsTableData outputResults = new ResultsTableData();

        IJ3DUtils.measureRoi3dRelation(imageHandler,
                roi1List,
                roi2List,
                measurements.getNativeValue(),
                measureInPhysicalUnits,
                requireColocalization,
                preciseColocalization,
                ignoreC,
                ignoreT,
                "",
                outputResults,
                progressInfo.resolve("Measure ROI"));

        iterationStep.addOutputData(getFirstOutputSlot(), outputResults, progressInfo);
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
    public ROI3DRelationMeasurementSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ROI3DRelationMeasurementSetParameter measurements) {
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
