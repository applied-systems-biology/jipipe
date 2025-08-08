/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.publish.rocrate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.publish.conditions.*;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

public class ROCratePublisherAssistant extends JIPipeDesktopPublisherAssistant {
    public ROCratePublisherAssistant(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        addAssistantCondition(new TitleAssistantCondition(this));
        addAssistantCondition(new LicenseAssistantCondition(this));
        addAssistantCondition(new DescriptionAssistantCondition(this));
        addAssistantCondition(new SummaryAssistantCondition(this));
        addAssistantCondition(new AuthorsAssistantCondition(this));
        addAssistantCondition(new AuthorAffiliationsAssistantCondition(this));
        addAssistantCondition(new SimpleParametersAssistantCondition(this));
        addAssistantCondition(new ArchiveAssistantCondition(this));
        addAssistantCondition(new SavedProjectAssistantCondition(this));
    }

    @Override
    public String getAssistantTitle() {
        return "Publish project as RO-Crate";
    }

    @Override
    public HTMLText getAssistantDescription() {
        return new HTMLText("This tool will guide you through the process of publishing your JIPipe project as RO-Crate with CWL. This will produce a *.zip file that contains the projects and all inputs together with all necessary instructions to run the project.");
    }

    @Override
    public List<BufferedImage> getAssistantLogos() {
        return List.of(JIPipe.RESOURCES.getVariantResourceAsImage("logos/ro-crate.png"),
                JIPipe.RESOURCES.getVariantResourceAsImage("logos/cwl.png"));
    }

    @Override
    public JIPipeRunnable createAssistantTask() {
        Path crateFile = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.External, "Export as RO-Crate", new HTMLText("Please choose where the RO-Crate will be saved"), PathUtils.EXTENSION_FILTER_WORKFLOW_RO_CRATE);
        if(crateFile != null) {
            return new CreateROCrateRun(getProject(), getDesktopProjectWorkbench().getProjectWindow().getProjectSavePath(),  crateFile);
        }
        return null;
    }

    @Override
    public void onPublicationFinished(JIPipeRunnable runnable) {
        if(runnable instanceof CreateROCrateRun run) {
            if(JOptionPane.showConfirmDialog(this, "<html>The RO-Crate was successfully exported to " + run.getRoCrateFile() + ".<br/>Do you want to open the containing directory?</html>",
                    "Export finished", JOptionPane.YES_NO_OPTION) ==  JOptionPane.YES_OPTION) {
                UIUtils.desktopOpenFile(run.getRoCrateFile().getParent());
            }
        }
    }
}
