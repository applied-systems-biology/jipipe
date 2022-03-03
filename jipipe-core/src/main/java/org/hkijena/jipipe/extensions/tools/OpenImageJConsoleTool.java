/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.tools;

import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.command.CommandService;
import org.scijava.ui.swing.console.ShowConsole;

public class OpenImageJConsoleTool extends JIPipeMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public OpenImageJConsoleTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Open ImageJ console");
        setToolTipText("Opens the ImageJ error console.");
        setIcon(UIUtils.getIconFromResources("actions/akonadiconsole.png"));
        addActionListener(e -> showImageJ());
    }

    private void showImageJ() {
        CommandService service = JIPipe.getInstance().getContext().getService(CommandService.class);
        service.run(ShowConsole.class, true);
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Development";
    }
}
