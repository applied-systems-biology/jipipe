package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.plotbuilder.ACAQPlot;

/**
 * Event when the series list is changed
 */
public class PlotSeriesListChangedEvent {
    private ACAQPlot plot;

    /**
     * @param plot Event source
     */
    public PlotSeriesListChangedEvent(ACAQPlot plot) {
        this.plot = plot;
    }

    /**
     * @return Event source
     */
    public ACAQPlot getPlot() {
        return plot;
    }
}
