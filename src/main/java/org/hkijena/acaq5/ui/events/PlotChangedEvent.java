package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.plotbuilder_old.ACAQLegacyPlot;

/**
 * Event when a plot is changed
 */
public class PlotChangedEvent {
    private ACAQLegacyPlot plot;

    /**
     * @param plot Event source
     */
    public PlotChangedEvent(ACAQLegacyPlot plot) {
        this.plot = plot;
    }

    /**
     * @return The event source
     */
    public ACAQLegacyPlot getPlot() {
        return plot;
    }
}
