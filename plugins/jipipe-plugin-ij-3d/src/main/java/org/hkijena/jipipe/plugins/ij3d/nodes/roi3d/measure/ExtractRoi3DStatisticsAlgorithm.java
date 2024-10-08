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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.measure;

import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Extract ROI 3D statistics", description = "Generates a results table containing 3D ROI statistics. If a reference image is provided, the statistics are calculated for the reference image. Otherwise, " +
        "NaN is returned.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true, description = "Optional image that is the basis for the measurements. If not set, all affected measurements are set to NaN.")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Measurements", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Measure (ROI 3D)")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi = iterationStep.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);
        ImageHandler referenceHandler;
        if (reference == null) {
            referenceHandler = null;
        } else {
            referenceHandler = ImageHandler.wrap(reference.getImage());
        }
        ResultsTableData result = roi.measure(referenceHandler, measurements.getNativeValue(), measureInPhysicalUnits, "", progressInfo.resolve("Measure 3D objects"));
        iterationStep.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to generate")
    @JIPipeParameter("measurements")
    public ROI3DMeasurementSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ROI3DMeasurementSetParameter measurements) {
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
