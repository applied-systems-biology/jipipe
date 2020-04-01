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

package org.hkijena.acaq5.ui.plotbuilder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.jfree.chart.JFreeChart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A plot
 */
public abstract class ACAQPlot {

    protected List<ACAQPlotSeries> series = new ArrayList<>();
    private EventBus eventBus = new EventBus();
    private List<ACAQPlotSeriesData> seriesDataList;
    private String title = "Plot";

    /**
     * Creates a new instance
     *
     * @param seriesDataList List of data series
     */
    protected ACAQPlot(List<ACAQPlotSeriesData> seriesDataList) {
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
            ACAQPlotSeries s = createSeries();
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
    public void removeSeries(ACAQPlotSeries series) {
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
    public void moveSeriesUp(ACAQPlotSeries series) {
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
    public void moveSeriesDown(ACAQPlotSeries series) {
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
    public void handleSeriesDataChangedEvent(ACAQPlotSeries.DataChangedEvent event) {
        eventBus.post(new PlotChangedEvent(this));
    }

    /**
     * @return Created series
     */
    protected abstract ACAQPlotSeries createSeries();

    /**
     * @return Plot UI instance
     */
    public abstract JFreeChart createPlot();

    /**
     * @return Event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return The plot title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the plot title
     *
     * @param title The title
     */
    public void setTitle(String title) {
        this.title = title;
        eventBus.post(new PlotChangedEvent(this));
    }

    /**
     * @return The currently loaded series
     */
    public List<ACAQPlotSeries> getSeries() {
        return Collections.unmodifiableList(series);
    }

    /**
     * @return The series data
     */
    public List<ACAQPlotSeriesData> getSeriesDataList() {
        return seriesDataList;
    }

    /**
     * Event when a plot is changed
     */
    public static class PlotChangedEvent {
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

    /**
     * Event when the series list is changed
     */
    public static class PlotSeriesListChangedEvent {
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
}
