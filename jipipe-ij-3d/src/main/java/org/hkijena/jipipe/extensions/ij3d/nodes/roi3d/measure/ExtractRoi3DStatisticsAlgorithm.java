package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.measure;

import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DMeasurementSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Extract ROI 3D statistics", description = "Generates a results table containing 3D ROI statistics. If a reference image is provided, the statistics are calculated for the reference image. Otherwise, " +
        "NaN is returned.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true, description = "Optional image that is the basis for the measurements. If not set, all affected measurements are set to NaN.")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Measure (ROI 3D)")
public class ExtractRoi3DStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean measureInPhysicalUnits = true;
    private ROI3DMeasurementSetParameter measurements = new ROI3DMeasurementSetParameter();

    public ExtractRoi3DStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractRoi3DStatisticsAlgorithm(ExtractRoi3DStatisticsAlgorithm other) {
        super(other);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.measurements = new ROI3DMeasurementSetParameter(other.measurements);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi = dataBatch.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData reference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);
        ImageHandler referenceHandler;
        if (reference == null) {
            referenceHandler = null;
        } else {
            referenceHandler = ImageHandler.wrap(reference.getImage());
        }
        ResultsTableData result = roi.measure(referenceHandler, measurements.getNativeValue(), measureInPhysicalUnits, "", progressInfo.resolve("Measure 3D objects"));
        dataBatch.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements to generate")
    @JIPipeParameter("measurements")
    public ROI3DMeasurementSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ROI3DMeasurementSetParameter measurements) {
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
