package org.hkijena.jipipe.plugins.omero.viewers;

import com.formdev.flatlaf.util.StringUtils;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopReadonlyCopyableTextField;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.omero.datatypes.*;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class OMERODataViewer extends JIPipeDesktopDataViewer {

    private final JIPipeDesktopFormPanel formPanel;
    private String currentUrl = "";

    public OMERODataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        this.formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        add(formPanel, BorderLayout.CENTER);
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Band generalOMEROBand = ribbon.getOrCreateTask("General").getOrCreateBand("OMERO");
        generalOMEROBand.addLargeButton("Open URL", "Opens the URL to the OMERO entry in your web browser", UIUtils.getIcon32FromResources("actions/internet-amarok.png"), this::openUrl);
    }

    private void openUrl() {
        if (!StringUtils.isEmpty(currentUrl)) {
            try {
                Desktop.getDesktop().browse(new URI(currentUrl));
            } catch (Exception e) {
                UIUtils.showErrorDialog(getDesktopWorkbench(), this, e);
            }
        }
    }

    @Override
    public void onDataDownloaded(JIPipeData data) {
        if (data instanceof OMEROAnnotationReferenceData) {
            this.currentUrl = ((OMEROAnnotationReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROAnnotationReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROAnnotationReferenceData) data).getAnnotationId() + "", true), new JLabel("Annotation ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROAnnotationReferenceData) data).getUrl(), true), new JLabel("URL"));
        } else if (data instanceof OMERODatasetReferenceData) {
            this.currentUrl = ((OMERODatasetReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMERODatasetReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMERODatasetReferenceData) data).getDescription(), true), new JLabel("Description"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMERODatasetReferenceData) data).getDatasetId() + "", true), new JLabel("Dataset ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMERODatasetReferenceData) data).getUrl(), true), new JLabel("URL"));
        } else if (data instanceof OMEROGroupReferenceData) {
            this.currentUrl = "";
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROGroupReferenceData) data).getGroupId() + "", true), new JLabel("Group ID"));
        } else if (data instanceof OMEROImageReferenceData) {
            this.currentUrl = ((OMEROImageReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROImageReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROImageReferenceData) data).getDescription(), true), new JLabel("Description"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROImageReferenceData) data).getImageId() + "", true), new JLabel("Image ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROImageReferenceData) data).getUrl(), true), new JLabel("URL"));
        } else if (data instanceof OMEROProjectReferenceData) {
            this.currentUrl = ((OMEROProjectReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROProjectReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROProjectReferenceData) data).getDescription(), true), new JLabel("Description"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROProjectReferenceData) data).getProjectId() + "", true), new JLabel("Project ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROProjectReferenceData) data).getUrl(), true), new JLabel("URL"));
        }
        else if (data instanceof OMEROPlateReferenceData) {
            this.currentUrl = ((OMEROPlateReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROPlateReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROPlateReferenceData) data).getPlateId() + "", true), new JLabel("Plate ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROPlateReferenceData) data).getUrl(), true), new JLabel("URL"));
        }
        else if (data instanceof OMEROScreenReferenceData) {
            this.currentUrl = ((OMEROScreenReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROScreenReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROScreenReferenceData) data).getDescription(), true), new JLabel("Description"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROScreenReferenceData) data).getProtocolIdentifier(), true), new JLabel("Protocol identifier"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROScreenReferenceData) data).getProtocolDescription(), true), new JLabel("Protocol description"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROScreenReferenceData) data).getReagentIdentifier(), true), new JLabel("Reagent identifier"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROScreenReferenceData) data).getReagentDescription(), true), new JLabel("Reagent description"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROScreenReferenceData) data).getScreenId() + "", true), new JLabel("Screen ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROScreenReferenceData) data).getUrl(), true), new JLabel("URL"));
        }
        else if (data instanceof OMEROWellReferenceData) {
            this.currentUrl = ((OMEROWellReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROWellReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROWellReferenceData) data).getWellId() + "", true), new JLabel("Well ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROWellReferenceData) data).getWellType(), true), new JLabel("Well type"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROWellReferenceData) data).getStatus(), true), new JLabel("Status"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROWellReferenceData) data).getRow() + ", " + ((OMEROWellReferenceData) data).getColumn(), true), new JLabel("Row, Column"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROWellReferenceData) data).getRed() + ", " + ((OMEROWellReferenceData) data).getGreen() + ", " + ((OMEROWellReferenceData) data).getBlue(), true), new JLabel("Red, Green, Blue"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROWellReferenceData) data).getUrl(), true), new JLabel("URL"));
        }
    }

    @Override
    public void postOnDataChanged() {
        formPanel.clear();
        getDataViewerWindow().startDownloadFullData();
    }
}
