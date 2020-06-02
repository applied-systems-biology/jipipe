package org.hkijena.acaq5.ui.parameters;

import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Supplier;

/**
 * UI that generates a set of parameter values
 */
public abstract class ACAQParameterGeneratorUI extends JPanel implements Supplier<List<Object>>, ACAQValidatable {
    private Context context;

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public ACAQParameterGeneratorUI(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    /**
     * Shows a dialog that allows the user to setup the generator
     *
     * @param parent  The parent component
     * @param context The SciJava context
     * @param uiClass The generator UI class
     * @return the generated values or null if the user cancelled
     */
    public static List<Object> showDialog(Component parent, Context context, Class<? extends ACAQParameterGeneratorUI> uiClass) {
        Dialog dialog = new Dialog(SwingUtilities.getWindowAncestor(parent), context, uiClass);
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
     * Dialog around an {@link ACAQParameterGeneratorUI}
     */
    private static class Dialog extends JDialog {

        private boolean cancelled = true;
        private ACAQParameterGeneratorUI generatorUI;

        public Dialog(Window windowAncestor, Context context, Class<? extends ACAQParameterGeneratorUI> uiClass) {
            super(windowAncestor);
            try {
                this.generatorUI = uiClass.getConstructor(Context.class).newInstance(context);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            initialize();
        }

        private void initialize() {
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(generatorUI, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(Box.createHorizontalGlue());

            JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
            cancelButton.addActionListener(e -> {
                this.cancelled = true;
                this.setVisible(false);
            });
            buttonPanel.add(cancelButton);

            JButton confirmButton = new JButton("Generate", UIUtils.getIconFromResources("run.png"));
            confirmButton.addActionListener(e -> {
                ACAQValidityReport report = new ACAQValidityReport();
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
