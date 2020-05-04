/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.plotbuilder_old;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.ui.events.PlotChangedEvent;
import org.hkijena.acaq5.ui.events.PlotSeriesListChangedEvent;
import org.jfree.chart.JFreeChart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A plot
 */
public abstract class ACAQLegacyPlot implements ACAQParameterCollection {

    protected List<ACAQLegacyPlotSeries> series = new ArrayList<>();
    private EventBus eventBus = new EventBus();
    private List<ACAQLegacyPlotSeriesData> seriesDataList;
    private String title = "Plot";

    /**
     * Creates a new instance
     *
     * @param seriesDataList List of data series
     */
    protected ACAQLegacyPlot(List<ACAQLegacyPlotSeriesData> seriesDataList) {
        this.seriesDataList = seriesDataList;
    }

    /**
     * @return If series can be removed
     */
    public boolean canRemoveSeries() {
        return true;
    }

    /**
     * @return If series can be added
     */
    public boolean canAddSeries() {
        return true;
    }

    /**
     * Adds a new series
     */
    public void addSeries() {
        if (canAddSeries()) {
            ACAQLegacyPlotSeries s = createSeries();
            s.getEventBus().register(this);
            series.add(s);
            eventBus.post(new PlotSeriesListChangedEvent(this));
            eventBus.post(new PlotChangedEvent(this));
        }
    }

    /**
     * Removes the series
     *
     * @param series The removed series
     */
    public void removeSeries(ACAQLegacyPlotSeries series) {
        if (canRemoveSeries()) {
            this.series.remove(series);
            eventBus.post(new PlotSeriesListChangedEvent(this));
            eventBus.post(new PlotChangedEvent(this));
        }
    }

    /**
     * Moves the series up in order.
     * Silently fails if the index is already 0
     *
     * @param series The series
     */
    public void moveSeriesUp(ACAQLegacyPlotSeries series) {
        int index = this.series.indexOf(series);
        if (index > 0) {
            this.series.set(index, this.series.get(index - 1));
            this.series.set(index - 1, series);
            eventBus.post(new PlotSeriesListChangedEvent(this));
            eventBus.post(new PlotChangedEvent(this));
        }
    }

    /**
     * Moves the series down in order.
     * Silently fails if the index is already maximum.
     *
     * @param series The series
     */
    public void moveSeriesDown(ACAQLegacyPlotSeries series) {
        int index = this.series.indexOf(series);
        if (index >= 0 && index < this.series.size() - 1) {
            this.series.set(index, this.series.get(index + 1));
            this.series.set(index + 1, series);
            eventBus.post(new PlotSeriesListChangedEvent(this));
            eventBus.post(new PlotChangedEvent(this));
        }
    }

    /**
     * Triggered when a series' data is changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void handleSeriesDataChangedEvent(ACAQLegacyPlotSeries.DataChangedEvent event) {
        eventBus.post(new PlotChangedEvent(this));
    }

    /**
     * @return Created series
     */
    protected abstract ACAQLegacyPlotSeries createSeries();

    /**
     * @return Plot UI instance
     */
    public abstract JFreeChart createPlot();

    /**
     * @return Event bus
     */
    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return The plot title
     */
    @ACAQDocumentation(name = "Title", description = "The title of this plot.")
    @ACAQParameter("title")
    public String getTitle() {
        return title;
    }

    /**
     * Sets the plot title
     *
     * @param title The title
     */
    @ACAQParameter("title")
    public void setTitle(String title) {
        this.title = title;
        eventBus.post(new PlotChangedEvent(this));
        eventBus.post(new ParameterChangedEvent(this, "title"));
    }

    /**
     * @return The currently loaded series
     */
    public List<ACAQLegacyPlotSeries> getSeries() {
        return Collections.unmodifiableList(series);
    }

    /**
     * @return The series data
     */
    public List<ACAQLegacyPlotSeriesData> getSeriesDataList() {
        return seriesDataList;
    }

}
