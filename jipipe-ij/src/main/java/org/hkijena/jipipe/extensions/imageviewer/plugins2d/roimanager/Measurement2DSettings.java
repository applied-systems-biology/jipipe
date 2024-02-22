package org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;

public class Measurement2DSettings extends AbstractJIPipeParameterCollection {

    public static Measurement2DSettings INSTANCE = new Measurement2DSettings();

    private ImageStatisticsSetParameter statistics = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;

    public Measurement2DSettings() {
        statistics.setCollapsed(false);
    }

    @SetJIPipeDocumentation(name = "Statistics", description = "The statistics to measure")
    @JIPipeParameter("statistics")
    public ImageStatisticsSetParameter getStatistics() {
        return statistics;
    }

    @JIPipeParameter("statistics")
    public void setStatistics(ImageStatisticsSetParameter statistics) {
        this.statistics = statistics;
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
