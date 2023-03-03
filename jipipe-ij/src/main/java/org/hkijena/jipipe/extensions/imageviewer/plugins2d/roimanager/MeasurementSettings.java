package org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;

public class MeasurementSettings extends AbstractJIPipeParameterCollection {

    public static MeasurementSettings INSTANCE = new MeasurementSettings();

    private ImageStatisticsSetParameter statistics = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;

    public MeasurementSettings() {
        statistics.setCollapsed(false);
    }

    @JIPipeDocumentation(name = "Statistics", description = "The statistics to measure")
    @JIPipeParameter("statistics")
    public ImageStatisticsSetParameter getStatistics() {
        return statistics;
    }

    @JIPipeParameter("statistics")
    public void setStatistics(ImageStatisticsSetParameter statistics) {
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
