/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
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

public abstract class ACAQPlot {

    protected List<ACAQPlotSeries> series = new ArrayList<>();
    private EventBus eventBus = new EventBus();
    private List<ACAQPlotSeriesData> seriesDataList;
    private String title = "Plot";

    protected ACAQPlot(List<ACAQPlotSeriesData> seriesDataList) {
        this.seriesDataList = seriesDataList;
    }

    public boolean canRemoveSeries() {
        return true;
    }

    public boolean canAddSeries() {
        return true;
    }

    public void addSeries() {
        if (canAddSeries()) {
            ACAQPlotSeries s = createSeries();
            s.getEventBus().register(this);
            series.add(s);
            eventBus.post(new PlotSeriesListChangedEvent(this));
            eventBus.post(new PlotChangedEvent(this));
        }
    }

    public void removeSeries(ACAQPlotSeries series) {
        if (canRemoveSeries()) {
            this.series.remove(series);
            eventBus.post(new PlotSeriesListChangedEvent(this));
            eventBus.post(new PlotChangedEvent(this));
        }
    }

    public void moveSeriesUp(ACAQPlotSeries series) {
        int index = this.series.indexOf(series);
        if (index > 0) {
            this.series.set(index, this.series.get(index - 1));
            this.series.set(index - 1, series);
            eventBus.post(new PlotSeriesListChangedEvent(this));
            eventBus.post(new PlotChangedEvent(this));
        }
    }

    public void moveSeriesDown(ACAQPlotSeries series) {
        int index = this.series.indexOf(series);
        if (index >= 0 && index < this.series.size() - 1) {
            this.series.set(index, this.series.get(index + 1));
            this.series.set(index + 1, series);
            eventBus.post(new PlotSeriesListChangedEvent(this));
            eventBus.post(new PlotChangedEvent(this));
        }
    }

    @Subscribe
    public void handleSeriesDataChangedEvent(ACAQPlotSeries.DataChangedEvent event) {
        eventBus.post(new PlotChangedEvent(this));
    }

    protected abstract ACAQPlotSeries createSeries();

    public abstract JFreeChart createPlot();

    public EventBus getEventBus() {
        return eventBus;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        eventBus.post(new PlotChangedEvent(this));
    }

    public List<ACAQPlotSeries> getSeries() {
        return Collections.unmodifiableList(series);
    }

    public List<ACAQPlotSeriesData> getSeriesDataList() {
        return seriesDataList;
    }

    public static class PlotChangedEvent {
        private ACAQPlot plot;

        public PlotChangedEvent(ACAQPlot plot) {
            this.plot = plot;
        }

        public ACAQPlot getPlot() {
            return plot;
        }
    }

    public static class PlotSeriesListChangedEvent {
        private ACAQPlot plot;

        public PlotSeriesListChangedEvent(ACAQPlot plot) {
            this.plot = plot;
        }

        public ACAQPlot getPlot() {
            return plot;
        }
    }
}
