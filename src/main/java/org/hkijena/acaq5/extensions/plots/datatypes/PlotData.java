package org.hkijena.acaq5.extensions.plots.datatypes;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.ui.events.PlotChangedEvent;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains all necessary data to generate a plot
 */
@ACAQDocumentation(name = "Plot", description = "A plot")
@ACAQHidden
public abstract class PlotData implements ACAQData, ACAQParameterCollection, ACAQValidatable {

    private EventBus eventBus = new EventBus();
    private String title;
    private int exportWidth = 1024;
    private int exportHeight = 768;
    private List<PlotDataSeries> series = new ArrayList<>();

    /**
     * Creates a new empty instance
     */
    public PlotData() {

    }

    /**
     * Creates a copy of the provided data
     * @param other the original
     */
    public PlotData(PlotData other) {
        this.title = other.title;
        this.exportWidth = other.exportWidth;
        this.exportHeight = other.exportHeight;
        for (PlotDataSeries data : other.series) {
            this.series.add(new PlotDataSeries(data));
        }
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        // Export plot
        try {
            JFreeChart chart = getChart();

            // Save as PNG
            ChartUtils.saveChartAsPNG(storageFilePath.resolve(name + ".png").toFile(),
                    chart,
                    exportWidth,
                    exportHeight);

            // Save as SVG
            SVGGraphics2D g2 = new SVGGraphics2D(exportWidth, exportHeight);
            Rectangle r = new Rectangle(0, 0, exportWidth, exportHeight);
            chart.draw(g2, r);
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to export plot!",
                    "Internal plot-export function",
                    "A plot should be saved to '" + storageFilePath + "'. There was an error during this export.",
                    "Please check if you can write to the output folder. Please check if the algorithm inputs are valid. " +
                            "If you cannot solve the issue, please contact the plugin author.");
        }
    }

    @Override
    public ACAQData duplicate() {
        return ACAQData.createInstance(getClass(), this);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public abstract JFreeChart getChart();

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
        eventBus.post(new ParameterChangedEvent(this, "title"));
    }

    @ACAQDocumentation(name = "Export width", description = "Width of the output image generated via an export")
    @ACAQParameter("export-width")
    public int getExportWidth() {
        return exportWidth;
    }

    @ACAQParameter("export-width")
    public void setExportWidth(int exportWidth) {
        this.exportWidth = exportWidth;
        eventBus.post(new ParameterChangedEvent(this, "export-width"));
    }

    @ACAQDocumentation(name = "Export height", description = "Height of the output image generated via an export")
    @ACAQParameter("export-height")
    public int getExportHeight() {
        return exportHeight;
    }

    @ACAQParameter("export-height")
    public void setExportHeight(int exportHeight) {
        this.exportHeight = exportHeight;
        eventBus.post(new ParameterChangedEvent(this, "export-height"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Export width").checkIfWithin(this, exportWidth, 0,Double.POSITIVE_INFINITY, false, true);
        report.forCategory("Export height").checkIfWithin(this, exportHeight, 0,Double.POSITIVE_INFINITY, false, true);
    }

    public List<PlotDataSeries> getSeries() {
        return series;
    }
}
