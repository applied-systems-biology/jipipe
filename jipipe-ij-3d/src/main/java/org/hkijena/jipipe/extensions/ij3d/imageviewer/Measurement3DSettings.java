package org.hkijena.jipipe.extensions.ij3d.imageviewer;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DMeasurementSetParameter;

public class Measurement3DSettings extends AbstractJIPipeParameterCollection {

    public static Measurement3DSettings INSTANCE = new Measurement3DSettings();

    private ROI3DMeasurementSetParameter statistics = new ROI3DMeasurementSetParameter();
    private boolean measureInPhysicalUnits = true;

    public Measurement3DSettings() {
        statistics.setCollapsed(false);
    }

    @JIPipeDocumentation(name = "Statistics", description = "The statistics to measure")
    @JIPipeParameter("statistics")
    public ROI3DMeasurementSetParameter getStatistics() {
        return statistics;
    }

    @JIPipeParameter("statistics")
    public void setStatistics(ROI3DMeasurementSetParameter statistics) {
        this.statistics = statistics;
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
