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

package org.hkijena.jipipe.extensions.plots;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.plotbuilder.PlotEditor;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New plot" to the JIPipe menu
 */
public class NewPlotJIPipeMenuExtension extends JIPipeMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public NewPlotJIPipeMenuExtension(JIPipeWorkbench workbench) {
        super(workbench);
        setText("New plot");
        setIcon(UIUtils.getIconFromResources("data-types/data-type-plot.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PlotEditor plotBuilderUI = new PlotEditor(getWorkbench());
        getWorkbench().getDocumentTabPane().addTab("Plot", UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
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
