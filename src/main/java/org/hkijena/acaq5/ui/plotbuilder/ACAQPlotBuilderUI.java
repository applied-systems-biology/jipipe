package org.hkijena.acaq5.ui.plotbuilder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataDeclarationRef;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotData;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.acaq5.extensions.standardparametereditors.editors.ACAQDataParameterSettings;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.UserFriendlyErrorUI;
import org.hkijena.acaq5.ui.parameters.ACAQParameterAccessUI;
import org.scijava.Priority;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User interface for displaying and creating plots
 */
public class ACAQPlotBuilderUI extends ACAQWorkbenchPanel implements ACAQParameterCollection, ACAQValidatable {

    private EventBus eventBus = new EventBus();
    private ACAQDataDeclarationRef plotType = new ACAQDataDeclarationRef();
    private PlotData currentPlot;
    private JSplitPane splitPane;
    private List<PlotDataSeries> currentData = new ArrayList<>();

    /**
     * @param workbench the workbench
     */
    public ACAQPlotBuilderUI(ACAQWorkbench workbench) {
        super(workbench);
        initialize();
        rebuildPlot();
        this.eventBus.register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        splitPane = new JSplitPane();
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        splitPane.setRightComponent(new ACAQParameterAccessUI(getWorkbench().getContext(),
                this,
                null,
                true,
                true));
        add(splitPane, BorderLayout.CENTER);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Plot type").checkNonNull(getPlotType().getDeclaration(), this);
        if(currentPlot != null) {
            report.forCategory("Plot parameters").report(currentPlot);
        }
    }

    @ACAQDocumentation(name = "Plot type", description = "The type of plot to be generated.")
    @ACAQParameter(value = "plot-type", priority = Priority.HIGH)
    @ACAQDataParameterSettings(dataBaseClass = PlotData.class)
    public ACAQDataDeclarationRef getPlotType() {
        if(plotType == null) {
            plotType = new ACAQDataDeclarationRef();
        }
        return plotType;
    }

    @ACAQParameter("plot-type")
    public void setPlotType(ACAQDataDeclarationRef plotType) {
        this.plotType = plotType;
        getEventBus().post(new ParameterChangedEvent(this, "plot-type"));
        updatePlotTypeParameters();
    }

    private void updatePlotTypeParameters() {
        if(currentPlot == null || (plotType.getDeclaration() != null &&
                !Objects.equals(plotType.getDeclaration().getDataClass(), currentPlot.getClass()))) {
            if(plotType.getDeclaration() != null) {
                currentPlot = (PlotData) ACAQData.createInstance(plotType.getDeclaration().getDataClass());
                getEventBus().post(new ParameterStructureChangedEvent(this));
            }
        }
        else if(plotType.getDeclaration() == null) {
            currentPlot = null;
            getEventBus().post(new ParameterStructureChangedEvent(this));
        }
    }

    @ACAQDocumentation(name = "Plot parameters")
    @ACAQParameter("plot")
    public PlotData getCurrentPlot() {
        return currentPlot;
    }

    public void setCurrentPlot(PlotData data) {
        this.plotType.setDeclaration(ACAQDataDeclaration.getInstance(data.getClass()));
        this.currentPlot = data;
        this.currentData.clear();
        getEventBus().post(new ParameterChangedEvent(this, "plot-type"));
        getEventBus().post(new ParameterChangedEvent(this, "plot"));
        getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    /**
     * Triggered when a parameter was changed
     * @param event generated event
     */
    @Subscribe
    public void onParameterChanged(ParameterChangedEvent event) {
        rebuildPlot();
    }

    /**
     * Attempts to rebuild the current plot
     */
    public void rebuildPlot() {
        ACAQValidityReport report = new ACAQValidityReport();
        this.reportValidity(report);
        if(!report.isValid()) {
            UserFriendlyErrorUI errorUI = new UserFriendlyErrorUI(null, true);
            errorUI.displayErrors(report);
            splitPane.setLeftComponent(errorUI);
        }

        splitPane.setLeftComponent(null);
    }

    /**
     * Adds data into the data storage
     * @param series the data
     */
    public void addData(PlotDataSeries series) {
        ResultsTable table = series.getTable();
        for(int col = 0; col <= table.getLastColumn(); ++col) {
            ResultsTable column = new ResultsTable();
            column.setColumn(table.getColumnHeading(col), table.getColumnAsVariables(table.getColumnHeading(col)));
            currentData.add(new PlotDataSeries(column));
        }
        getEventBus().post(new ParameterChangedEvent(this, "available-data"));
    }

    /**
     * Contains single-column series that will be later spliced together depending on the user's settings
     * @return
     */
    public List<PlotDataSeries> getCurrentData() {
        return currentData;
    }
}
