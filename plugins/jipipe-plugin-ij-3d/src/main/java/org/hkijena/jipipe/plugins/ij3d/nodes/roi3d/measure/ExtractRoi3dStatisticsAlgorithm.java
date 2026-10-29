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
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.IJ3DFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiMeasurementsFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;

@SetJIPipeDocumentation(name = "Extract IJ3D ROI statistics", description = "Generates a results table containing 3D ROI statistics. If a reference image is provided, the statistics are calculated for the reference image. Otherwise, " +
        "NaN is returned.")
@ConfigureJIPipeNode(function = RoiMeasurementsFunction.class, framework = IJ3DFramework.class, spatialDimensions = {SPATIAL_2D, SPATIAL_3D})
@AddJIPipeInputSlot(value = Ij3dSuiteRoiListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true, description = "Optional image that is the basis for the measurements. If not set, all affected measurements are set to NaN.")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Measurements", create = true)
public class ExtractRoi3dStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean measureInPhysicalUnits = true;
    private Roi3dMeasurementSetParameter measurements = new Roi3dMeasurementSetParameter();

    public ExtractRoi3dStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractRoi3dStatisticsAlgorithm(ExtractRoi3dStatisticsAlgorithm other) {
        super(other);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.measurements = new Roi3dMeasurementSetParameter(other.measurements);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Ij3dSuiteRoiListData roi = iterationStep.getInputData("ROI", Ij3dSuiteRoiListData.class, progressInfo);
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
    public Roi3dMeasurementSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(Roi3dMeasurementSetParameter measurements) {
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
