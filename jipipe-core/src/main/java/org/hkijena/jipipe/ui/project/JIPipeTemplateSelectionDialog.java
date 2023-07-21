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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.registries.JIPipeProjectTemplateRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.renderers.TemplateProjectListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.project.templatedownloader.ProjectTemplateDownloaderRun;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Dialog for selecting templates
 */
public class JIPipeTemplateSelectionDialog extends JDialog implements JIPipeProjectTemplateRegistry.TemplatesUpdatedEventListener {

    private final JIPipeWorkbench workbench;
    private final SearchTextField templateSearch = new SearchTextField();
    private JList<JIPipeProjectTemplate> templateJList;
    private boolean isConfirmed = false;

    public JIPipeTemplateSelectionDialog(JIPipeWorkbench workbench, Window owner) {
        super(owner);
        this.workbench = workbench;
        initialize();
        refreshTemplateProjects();
        templateJList.setSelectedIndex(0);
        JIPipe.getInstance().getProjectTemplateRegistry().getTemplatesUpdatedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setTitle("Select project template");
        setModal(true);
        UIUtils.addEscapeListener(this);

        getContentPane().setLayout(new BorderLayout());
        templateJList = new JList<>();
        templateJList.setCellRenderer(new TemplateProjectListCellRenderer());
        templateJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    isConfirmed = true;
                    setVisible(false);
                } else {
                    if (templateJList.getMousePosition().x > templateJList.getWidth() - 50) {
                        isConfirmed = true;
                        setVisible(false);
                    }
                }
            }
        });
        JScrollPane templateListScrollPane = new JScrollPane(templateJList);
        getContentPane().add(templateListScrollPane, BorderLayout.CENTER);
        // Init search
        templateSearch.addActionListener(e -> refreshTemplateProjects());

        getContentPane().add(templateListScrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(templateSearch);
        JButton downloadTemplatesButton = new JButton("Get more templates", UIUtils.getIconFromResources("actions/browser-download.png"));
        downloadTemplatesButton.addActionListener(e -> downloadTemplates());
        toolBar.add(downloadTemplatesButton);

        getContentPane().add(toolBar, BorderLayout.NORTH);

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

    private void downloadTemplates() {
        JIPipeRunExecuterUI.runInDialog(this, new ProjectTemplateDownloaderRun(workbench));
    }

    private void refreshTemplateProjects() {
        DefaultListModel<JIPipeProjectTemplate> model = new DefaultListModel<>();
        for (JIPipeProjectTemplate template : JIPipe.getInstance().getProjectTemplateRegistry().getSortedRegisteredTemplates()) {
            if (templateSearch.test(template.getMetadata().getName() + " " + template.getMetadata().getTemplateDescription())) {
                model.addElement(template);
            }
        }
        if (model.getSize() == 0 && templateSearch.getSearchStrings().length == 0) {
            model.addElement(null);
        }
        templateJList.setModel(model);
    }

    public JIPipeProjectTemplate getSelectedTemplate() {
        return isConfirmed ? templateJList.getSelectedValue() : null;
    }

    @Override
    public void onJIPipeTemplatesUpdated(JIPipeProjectTemplateRegistry.TemplatesUpdatedEvent event) {
        refreshTemplateProjects();
    }
}
