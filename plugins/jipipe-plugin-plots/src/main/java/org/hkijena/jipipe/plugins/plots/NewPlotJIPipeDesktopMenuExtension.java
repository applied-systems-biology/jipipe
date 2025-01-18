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

package org.hkijena.jipipe.plugins.plots;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.ploteditor.JFreeChartPlotEditor;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New plot" to the JIPipe menu
 */
public class NewPlotJIPipeDesktopMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public NewPlotJIPipeDesktopMenuExtension(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("New plot");
        setIcon(UIUtils.getIconFromResources("data-types/data-type-plot.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFreeChartPlotEditor plotBuilderUI = new JFreeChartPlotEditor(getDesktopWorkbench());
        getDesktopWorkbench().getDocumentTabPane().addTab("Plot", UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI, JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton, true);
        getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectMainMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }
}
