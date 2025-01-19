package org.hkijena.jipipe.plugins.omero.viewers;

import com.formdev.flatlaf.util.StringUtils;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
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
        if(!StringUtils.isEmpty(currentUrl)) {
            try {
                Desktop.getDesktop().browse(new URI(currentUrl));
            }
            catch (Exception e) {
                UIUtils.showErrorDialog(getDesktopWorkbench(), this, e);
            }
        }
    }

    @Override
    public void onDataDownloaded(JIPipeData data) {
        if(data instanceof OMEROAnnotationReferenceData) {
            this.currentUrl = ((OMEROAnnotationReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROAnnotationReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROAnnotationReferenceData) data).getAnnotationId() + "", true), new JLabel("Annotation ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROAnnotationReferenceData) data).getUrl(), true), new JLabel("URL"));
        }
        else if(data instanceof OMERODatasetReferenceData) {
            this.currentUrl = ((OMERODatasetReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMERODatasetReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMERODatasetReferenceData) data).getDatasetId() + "", true), new JLabel("Dataset ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMERODatasetReferenceData) data).getUrl(), true), new JLabel("URL"));
        }
        else if(data instanceof OMEROGroupReferenceData) {
            this.currentUrl = "";
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROGroupReferenceData) data).getGroupId() + "", true), new JLabel("Group ID"));
        }
        else if(data instanceof OMEROImageReferenceData) {
            this.currentUrl = ((OMEROImageReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROImageReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROImageReferenceData) data).getImageId() + "", true), new JLabel("Image ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROImageReferenceData) data).getUrl(), true), new JLabel("URL"));
        }
        else if(data instanceof OMEROProjectReferenceData) {
            this.currentUrl = ((OMEROProjectReferenceData) data).getUrl();
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROProjectReferenceData) data).getName(), true), new JLabel("Name"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROProjectReferenceData) data).getProjectId() + "", true), new JLabel("Project ID"));
            formPanel.addToForm(new JIPipeDesktopReadonlyCopyableTextField(((OMEROProjectReferenceData) data).getUrl(), true), new JLabel("URL"));
        }
    }

    @Override
    public void postOnDataChanged() {
        formPanel.clear();
        getDataViewerWindow().startDownloadFullData();
    }
}
