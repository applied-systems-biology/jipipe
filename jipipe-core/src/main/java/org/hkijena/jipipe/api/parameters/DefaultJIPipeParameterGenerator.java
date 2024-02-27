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

package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * A default implementation of {@link JIPipeParameterGenerator} that shows all parameters of this {@link JIPipeParameterCollection}
 * in a dialog before generating the data. The dialog input is checked by the validation function.
 */
public abstract class DefaultJIPipeParameterGenerator extends AbstractJIPipeParameterCollection implements JIPipeParameterGenerator, JIPipeValidatable {

    @Override
    public <T> List<T> generate(JIPipeWorkbench workbench, Component parent, Class<T> klass) {
        Dialog dialog = new Dialog(SwingUtilities.getWindowAncestor(parent), workbench, this);
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
            return generateAfterDialog(workbench, parent, klass);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
    }

    /**
     * This function generates the values after the dialog was confirmed
     *
     * @param <T>    the generated class
     * @param parent the parent
     * @return the list of generated objects
     */
    public abstract <T> List<T> generateAfterDialog(JIPipeWorkbench workbench, Component parent, Class<T> klass);

    /**
     * Dialog around an {@link DefaultJIPipeParameterGenerator}
     */
    private static class Dialog extends JDialog {

        private final JIPipeWorkbench workbench;
        private final DefaultJIPipeParameterGenerator generator;
        private boolean cancelled = true;

        public Dialog(Window windowAncestor, JIPipeWorkbench workbench, DefaultJIPipeParameterGenerator generator) {
            super(windowAncestor);
            this.workbench = workbench;
            this.generator = generator;
            initialize();
        }

        private void initialize() {
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(new ParameterPanel(workbench, generator,
                    new MarkdownDocument("# " + generator.getName() + "\n\n" + generator.getDescription()),
                    ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING), BorderLayout.CENTER);

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
                JIPipeValidationReport report = new JIPipeValidationReport();
                generator.reportValidity(new UnspecifiedValidationReportContext(), report);
                if (!report.isEmpty()) {
                    UIUtils.openValidityReportDialog(workbench, this, report, "Invalid settings detected", "Please resolve the following issues:", true);
                    return;
                }
                this.cancelled = false;
                this.setVisible(false);
            });
            buttonPanel.add(confirmButton);

            getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
