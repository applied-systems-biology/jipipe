package org.hkijena.jipipe.extensions.batchassistant;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeFastThumbnail;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeIconLabelThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedJsonObjectData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;

@SetJIPipeDocumentation(name = "Data batch status", description = "Structural data indicating the status of a data batch")
@LabelAsJIPipeHidden
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
@JIPipeFastThumbnail
public class DataBatchStatusData extends JIPipeSerializedJsonObjectData {

    private ResultsTableData perSlotStatus = new ResultsTableData();
    private boolean statusValid = false;

    private int numIncompleteRequired = 0;
    private int numIncompleteOptional = 0;
    private int numMerging = 0;
    private String statusMessage;

    public DataBatchStatusData() {
    }

    public DataBatchStatusData(DataBatchStatusData other) {
        super(other);
        this.perSlotStatus = new ResultsTableData(other.perSlotStatus);
        this.statusValid = other.statusValid;
        this.statusMessage = other.statusMessage;
        this.numIncompleteRequired = other.numIncompleteRequired;
        this.numIncompleteOptional = other.numIncompleteOptional;
        this.numMerging = other.numMerging;
    }

    public static DataBatchStatusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return JIPipeSerializedJsonObjectData.importData(storage, DataBatchStatusData.class);
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
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return  new JIPipeIconLabelThumbnailData(getStatusMessage(), isStatusValid() ? "emblems/vcs-normal.png" : "emblems/warning.png");
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        formPanel.addGroupHeader(getStatusMessage(), UIUtils.getIconFromResources(isStatusValid() ? "emblems/vcs-normal.png" : "emblems/warning.png"));
        JXTable table = new JXTable(new ResultsTableData(getPerSlotStatus()));
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(table, BorderLayout.CENTER);
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        formPanel.addVerticalGlue(tablePanel, null);

        JFrame frame = new JFrame();
        frame.setIconImage(UIUtils.getJIPipeIcon128());
        frame.setContentPane(formPanel);
        frame.setTitle("Data batch status");
        frame.pack();
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
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

    @JsonGetter("num-incomplete-required")
    public int getNumIncompleteRequired() {
        return numIncompleteRequired;
    }

    @JsonSetter("num-incomplete-required")
    public void setNumIncompleteRequired(int numIncompleteRequired) {
        this.numIncompleteRequired = numIncompleteRequired;
    }

    @JsonGetter("num-incomplete-optional")
    public int getNumIncompleteOptional() {
        return numIncompleteOptional;
    }

    @JsonSetter("num-incomplete-optional")
    public void setNumIncompleteOptional(int numIncompleteOptional) {
        this.numIncompleteOptional = numIncompleteOptional;
    }

    @JsonGetter("num-merging")
    public int getNumMerging() {
        return numMerging;
    }

    @JsonSetter("num-merging")
    public void setNumMerging(int numMerging) {
        this.numMerging = numMerging;
    }
}
