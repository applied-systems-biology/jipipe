package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.measure;

import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurementSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Extract pairwise 3D ROI statistics", description = "Extracts all pairwise statistics between the 3D ROI")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI 1", autoCreate = true)
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI 2", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", autoCreate = true)
public class ExtractRoi3DRelationStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private ROI3DRelationMeasurementSetParameter measurements = new ROI3DRelationMeasurementSetParameter();

    private boolean measureInPhysicalUnits = true;
    private boolean requireColocalization = true;

    private boolean preciseColocalization = true;

    public ExtractRoi3DRelationStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractRoi3DRelationStatisticsAlgorithm(ExtractRoi3DRelationStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ROI3DRelationMeasurementSetParameter(other.measurements);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.requireColocalization = other.requireColocalization;
        this.preciseColocalization = other.preciseColocalization;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi1List = dataBatch.getInputData("ROI 1", ROI3DListData.class, progressInfo);
        ROI3DListData roi2List = dataBatch.getInputData("ROI 2", ROI3DListData.class, progressInfo);
        ImageHandler imageHandler = IJ3DUtils.wrapImage(dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo));
        ResultsTableData outputResults = new ResultsTableData();

        IJ3DUtils.measureRoi3dRelation(imageHandler, roi1List, roi2List, measurements.getNativeValue(), measureInPhysicalUnits, requireColocalization, preciseColocalization, "", outputResults, progressInfo.resolve("Measure ROI"));

        dataBatch.addOutputData(getFirstOutputSlot(), outputResults, progressInfo);
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

    @JIPipeDocumentation(name = "Measurements", description = "The measurements that will be extracted")
    @JIPipeParameter("measurements")
    public ROI3DRelationMeasurementSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ROI3DRelationMeasurementSetParameter measurements) {
        this.measurements = measurements;
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
