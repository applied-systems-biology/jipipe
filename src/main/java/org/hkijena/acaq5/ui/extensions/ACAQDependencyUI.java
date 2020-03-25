package org.hkijena.acaq5.ui.extensions;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQDependencyUI extends JPanel {
    private ACAQDependency dependency;

    public ACAQDependencyUI(ACAQDependency dependency) {
        this.dependency = dependency;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(null, false, false);

        // Add general metadata
        formPanel.addGroupHeader("About", UIUtils.getIconFromResources("info.png"));
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getName()), new JLabel("Name"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getDependencyId()), new JLabel("ID"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getDependencyVersion()), new JLabel("Version"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getAuthors()), new JLabel("Authors"), null);
        if(!StringUtils.isNullOrEmpty(dependency.getMetadata().getWebsite()))
            formPanel.addToForm(UIUtils.makeURLLabel(dependency.getMetadata().getWebsite()), new JLabel("Website"), null);
        if(!StringUtils.isNullOrEmpty(dependency.getMetadata().getCitation()))
            formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getCitation()), new JLabel("Citation"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField(dependency.getMetadata().getLicense()), new JLabel("License"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextField("" + dependency.getDependencyLocation()), new JLabel("Defining file"), null);
        formPanel.addToForm(UIUtils.makeReadonlyTextArea(dependency.getMetadata().getDescription()), new JLabel("Description"), null);

        formPanel.addVerticalGlue();

        add(formPanel, BorderLayout.CENTER);
    }
}
