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

package org.hkijena.jipipe.extensions.parameters.expressions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Panel that allows the user to select an {@link ExpressionFunction}
 */
public class FunctionSelectorList extends JList<JIPipeExpressionRegistry.ExpressionFunctionEntry> {

    public FunctionSelectorList() {
        initialize();
    }

    private void initialize() {
        DefaultListModel<JIPipeExpressionRegistry.ExpressionFunctionEntry> model = new DefaultListModel<>();
        for (JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry : JIPipe.getInstance().getExpressionRegistry().getRegisteredExpressionFunctions().values().stream()
                .sorted(Comparator.comparing(JIPipeExpressionRegistry.ExpressionFunctionEntry::getName)).collect(Collectors.toList())) {
               model.addElement(functionEntry);
        }
        setCellRenderer(new ExpressionFunctionRenderer());
        setModel(model);
        setSelectedIndex(0);
    }

    public static JIPipeExpressionRegistry.ExpressionFunctionEntry showDialog(Component parent) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));

        FunctionSelectorList functionSelectorList = new FunctionSelectorList();
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JScrollPane(functionSelectorList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(cancelButton);

        AtomicBoolean confirmed = new AtomicBoolean(false);
        JButton confirmButton = new JButton("Pick", UIUtils.getIconFromResources("actions/checkmark.png"));
        confirmButton.addActionListener(e -> {
            confirmed.set(true);
            dialog.setVisible(false); });
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.setModal(true);
        dialog.setTitle("Select expression function");
        dialog.pack();
        dialog.setSize(640,480);
        dialog.setLocationRelativeTo(null);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
        return confirmed.get() ? functionSelectorList.getSelectedValue() : null;
    }

    public static class ExpressionFunctionRenderer extends JPanel implements ListCellRenderer<JIPipeExpressionRegistry.ExpressionFunctionEntry> {

        private JLabel idLabel;
        private JLabel nameLabel;
        private JLabel descriptionLabel;

        public ExpressionFunctionRenderer() {
            initialize();
        }

        private void initialize() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            Insets border = new Insets(2, 4, 2, 2);

            JLabel iconLabel = new JLabel(UIUtils.getIconFromResources("actions/insert-math-expression.png"));
            add(iconLabel, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 0;
                    anchor = WEST;
                    insets = border;
                }
            });

            idLabel = new JLabel();
            idLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
            add(idLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 0;
                    anchor = WEST;
                    insets = border;
                }
            });

            nameLabel = new JLabel();
            add(nameLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 1;
                    anchor = WEST;
                    insets = border;
                }
            });

            descriptionLabel = new JLabel();
            descriptionLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
            add(descriptionLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 2;
                    anchor = WEST;
                    insets = border;
                }
            });
            JPanel glue = new JPanel();
            glue.setOpaque(false);
            add(glue, new GridBagConstraints() {
                {
                    gridx = 2;
                    weightx = 1;
                }
            });
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeExpressionRegistry.ExpressionFunctionEntry> list, JIPipeExpressionRegistry.ExpressionFunctionEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            String id = value.getId();
            // TODO: Signature instead of Id
            idLabel.setText(id);
            nameLabel.setText(value.getName());
            descriptionLabel.setText(value.getDescription());
            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
        }
    }
}
