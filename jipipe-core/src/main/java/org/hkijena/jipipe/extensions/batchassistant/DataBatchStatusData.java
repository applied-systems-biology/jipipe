package org.hkijena.jipipe.extensions.batchassistant;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeJsonData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Data batch status", description = "Structural data indicating the status of a data batch")
@JIPipeHidden
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class DataBatchStatusData extends JIPipeJsonData {

    private ResultsTableData perSlotStatus = new ResultsTableData();
    private boolean statusValid = false;
    private String statusMessage;

    public DataBatchStatusData() {
    }

    public DataBatchStatusData(DataBatchStatusData other) {
        super(other);
        this.perSlotStatus = new ResultsTableData(other.perSlotStatus);
        this.statusValid = other.statusValid;
        this.statusMessage = other.statusMessage;
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new DataBatchStatusData(this);
    }

    @Override
    public Component preview(int width, int height) {
        return new JLabel(getStatusMessage(), UIUtils.getIconFromResources(isStatusValid() ? "emblems/vcs-normal.png" : "emblems/warning.png"), JLabel.LEFT);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        formPanel.addGroupHeader(getStatusMessage(),  UIUtils.getIconFromResources(isStatusValid() ? "emblems/vcs-normal.png" : "emblems/warning.png"));
        JXTable table = new JXTable(new ResultsTableData(getPerSlotStatus()));
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(table, BorderLayout.CENTER);
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        formPanel.addVerticalGlue(tablePanel, null);

        JFrame frame = new JFrame();
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setContentPane(formPanel);
        frame.setTitle("Data batch status");
        frame.pack();
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
    }

    public static DataBatchStatusData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return JIPipeJsonData.importFrom(storagePath, DataBatchStatusData.class);
    }

    @JsonGetter("per-slot-status")
    public ResultsTableData getPerSlotStatus() {
        return perSlotStatus;
    }

    @JsonSetter("per-slot-status")
    public void setPerSlotStatus(ResultsTableData perSlotStatus) {
        this.perSlotStatus = perSlotStatus;
    }

    @JsonGetter("status-valid")
    public boolean isStatusValid() {
        return statusValid;
    }

    @JsonSetter("status-valid")
    public void setStatusValid(boolean statusValid) {
        this.statusValid = statusValid;
    }

    @JsonGetter("status-message")
    public String getStatusMessage() {
        return statusMessage;
    }

    @JsonSetter("status-message")
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}