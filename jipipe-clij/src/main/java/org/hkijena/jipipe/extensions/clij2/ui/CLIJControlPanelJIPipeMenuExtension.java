package org.hkijena.jipipe.extensions.clij2.ui;

import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.UIUtils;

/**
 * Adds a menu item that allows to control CLIJ
 */
public class CLIJControlPanelJIPipeMenuExtension extends JIPipeMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public CLIJControlPanelJIPipeMenuExtension(JIPipeWorkbench workbench) {
        super(workbench);
        setText("CLIJ2 control panel");
        setIcon(UIUtils.getIconFromResources("apps/clij.png"));
        setToolTipText("Opens a control panel to setup CLIJ2");
        addActionListener(e -> openControlPanel());
    }

    private void openControlPanel() {
        for (DocumentTabPane.DocumentTab tab : getWorkbench().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof CLIJControlPanel) {
                getWorkbench().getDocumentTabPane().switchToContent(tab.getContent());
                return;
            }
        }
        try {
            getWorkbench().getDocumentTabPane().addTab("CLIJ2 control panel",
                    UIUtils.getIconFromResources("apps/clij.png"),
                    new CLIJControlPanel(getWorkbench()),
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    false);
            getWorkbench().getDocumentTabPane().switchToLastTab();
        } catch (Exception e) {
            UIUtils.openErrorDialog(this, new UserFriendlyRuntimeException(
                    e,
                    "Could not open CLIJ2 control panel!",
                    "CLIJ2 control panel",
                    "This might indicate that your system is missing an essential library.",
                    "Please update your graphics card drivers. On Linux, CLIJ2 makes use of libOpencv.so. On Ubuntu, this library must be manually installed via the ocl-icd-opencl-dev package. Check if it is installed."
            ));
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Extensions";
    }
}
