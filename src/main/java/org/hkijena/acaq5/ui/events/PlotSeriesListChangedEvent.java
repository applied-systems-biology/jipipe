package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.plotbuilder_old.ACAQLegacyPlot;

/**
 * Event when the series list is changed
 */
public class PlotSeriesListChangedEvent {
    private ACAQLegacyPlot plot;

    /**
     * @param plot Event source
     */
    public PlotSeriesListChangedEvent(ACAQLegacyPlot plot) {
        this.plot = plot;
    }

    /**
     * @return Event source
     */
    public ACAQLegacyPlot getPlot() {
        return plot;
    }
}
