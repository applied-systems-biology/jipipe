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

package org.hkijena.jipipe.desktop.app.project;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.registries.JIPipeProjectTemplateRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.project.templatedownloader.JIPipeDesktopProjectTemplateDownloaderRun;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopTemplateProjectListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Dialog for selecting templates
 */
public class JIPipeDesktopJIPipeTemplateSelectionUI extends JDialog implements JIPipeProjectTemplateRegistry.TemplatesUpdatedEventListener {

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopSearchTextField templateSearch = new JIPipeDesktopSearchTextField();
    private JList<JIPipeProjectTemplate> templateJList;
    private boolean isConfirmed = false;

    public JIPipeDesktopJIPipeTemplateSelectionUI(JIPipeDesktopWorkbench workbench, Window owner) {
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
        templateJList.setCellRenderer(new JIPipeDesktopTemplateProjectListCellRenderer());
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
        JButton downloadTemplatesButton = new JButton("Get more templates", UIUtils.getIconFromResources("actions/download.png"));
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
        JIPipeDesktopRunExecuteUI.runInDialog(workbench, this, new JIPipeDesktopProjectTemplateDownloaderRun(workbench));
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
