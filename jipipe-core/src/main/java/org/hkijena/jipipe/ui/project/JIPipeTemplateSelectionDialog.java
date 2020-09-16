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

package org.hkijena.jipipe.ui.project;

import org.hkijena.jipipe.api.JIPipeProjectTemplate;
import org.hkijena.jipipe.ui.components.TemplateProjectListCellRenderer;
import org.hkijena.jipipe.utils.CustomScrollPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Dialog for selecting templates
 */
public class JIPipeTemplateSelectionDialog extends JDialog {

    private JList<JIPipeProjectTemplate> templateJList;
    private boolean isConfirmed = false;

    public JIPipeTemplateSelectionDialog(Window owner) {
        super(owner);
        initialize();
    }

    private void initialize() {
        setTitle("Select project template");
        setModal(true);
        UIUtils.addEscapeListener(this);

        getContentPane().setLayout(new BorderLayout());
        JIPipeProjectTemplate[] array = JIPipeProjectTemplate.listTemplates().toArray(new JIPipeProjectTemplate[0]);
        Arrays.sort(array, Comparator.comparing(t -> t.getMetadata().getName()));
        templateJList = new JList<>(array);
        templateJList.setCellRenderer(new TemplateProjectListCellRenderer());
        templateJList.setSelectedIndex(0);
        templateJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    isConfirmed = true;
                    setVisible(false);
                }
            }
        });
        getContentPane().add(new CustomScrollPane(templateJList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(cancelButton);

        JButton exportButton = new JButton("New project", UIUtils.getIconFromResources("actions/document-new.png"));
        exportButton.setDefaultCapable(true);
        exportButton.addActionListener(e -> {
            isConfirmed = true;
            setVisible(false);
        });
        buttonPanel.add(exportButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setSize(800, 600);
        revalidate();
        repaint();
    }

    public JIPipeProjectTemplate getSelectedTemplate() {
        return isConfirmed ? templateJList.getSelectedValue() : null;
    }
}
