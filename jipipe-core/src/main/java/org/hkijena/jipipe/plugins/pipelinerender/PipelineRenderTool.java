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

package org.hkijena.jipipe.plugins.pipelinerender;

import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import java.nio.file.Path;

public class PipelineRenderTool extends JIPipeDesktopMenuExtension {

    private static final RenderPipelineRunSettings LAST_SETTINGS = new RenderPipelineRunSettings();

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public PipelineRenderTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Export whole pipeline as *.png");
        setToolTipText("Rebuilds the node alias IDs for all nodes. This can help if the " +
                "generated alias IDs are too long.");
        setIcon(UIUtils.getIconFromResources("actions/camera.png"));
        addActionListener(e -> runRenderTool());
    }

    private void runRenderTool() {
        JIPipeProject project = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProject();
        MarkdownText document = new MarkdownText("# " + getText() + "\n\n" +
                "Please check if you organized your compartments as compact as possible, to minimize computational load of generating a full resolution pipeline.");
        RenderPipelineRunSettings settings = LAST_SETTINGS;

        if (JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(),
                settings,
                document,
                getText(),
                JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION)) {
            Path path = JIPipeFileChooserApplicationSettings.saveFile(getDesktopWorkbench().getWindow(), getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.External, getText(), UIUtils.EXTENSION_FILTER_PNG);
            if (path != null) {
                JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), getDesktopWorkbench().getWindow(), new RenderPipelineRun(project, path, settings));
            }
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Project";
    }
}
