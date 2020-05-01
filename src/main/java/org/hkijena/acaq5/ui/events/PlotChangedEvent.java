package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.plotbuilder.ACAQPlot;

/**
 * Event when a plot is changed
 */
public class PlotChangedEvent {
    private ACAQPlot plot;

    /**
     * @param plot Event source
     */
    public PlotChangedEvent(ACAQPlot plot) {
        this.plot = plot;
    }

    /**
     * @return The event source
     */
    public ACAQPlot getPlot() {
        return plot;
    }
}
