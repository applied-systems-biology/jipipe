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

package org.hkijena.jipipe.ui.parameters;

import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;
import java.util.function.Supplier;

/**
 * UI that generates a set of parameter values
 */
public abstract class JIPipeParameterGeneratorUI extends JIPipeWorkbenchPanel implements Supplier<List<Object>>, JIPipeValidatable {

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     */
    public JIPipeParameterGeneratorUI(JIPipeWorkbench workbench) {
        super(workbench);
    }

    /**
     * Shows a dialog that allows the user to setup the generator
     *
     * @param parent    The parent component
     * @param workbench The workbench
     * @param uiClass   The generator UI class
     * @return the generated values or null if the user cancelled
     */
    public static List<Object> showDialog(Component parent, JIPipeWorkbench workbench, Class<? extends JIPipeParameterGeneratorUI> uiClass) {
        Dialog dialog = new Dialog(SwingUtilities.getWindowAncestor(parent), workbench, uiClass);
        dialog.setTitle("");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 400));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);

        if (dialog.cancelled)
            return null;
        else
            return dialog.getGeneratedValue();
    }

    /**
     * Dialog around an {@link JIPipeParameterGeneratorUI}
     */
    private static class Dialog extends JDialog {

        private boolean cancelled = true;
        private JIPipeParameterGeneratorUI generatorUI;

        public Dialog(Window windowAncestor, JIPipeWorkbench workbench, Class<? extends JIPipeParameterGeneratorUI> uiClass) {
            super(windowAncestor);
            this.generatorUI = (JIPipeParameterGeneratorUI) ReflectionUtils.newInstance(uiClass, workbench);
            initialize();
        }

        private void initialize() {
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(generatorUI, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(Box.createHorizontalGlue());

            JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
            cancelButton.addActionListener(e -> {
                this.cancelled = true;
                this.setVisible(false);
            });
            buttonPanel.add(cancelButton);

            JButton confirmButton = new JButton("Generate", UIUtils.getIconFromResources("actions/run-build.png"));
            confirmButton.addActionListener(e -> {
                JIPipeIssueReport report = new JIPipeIssueReport();
                generatorUI.reportValidity(report);
                if (!report.isValid()) {
                    UIUtils.openValidityReportDialog(this, report, true);
                    return;
                }
                this.cancelled = false;
                this.setVisible(false);
            });
            buttonPanel.add(confirmButton);

            getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        }

        public List<Object> getGeneratedValue() {
            return generatorUI.get();
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
