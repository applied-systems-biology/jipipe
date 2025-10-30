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

package org.hkijena.jipipe.plugins.ijweka.parameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.UIUtils;
import weka.classifiers.Classifier;
import weka.core.CustomDisplayStringProvider;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopWekaClassifierParameterEditorUI extends JIPipeDesktopParameterEditorUI<WekaClassifierParameter> {
    private JButton currentlyDisplayed;
    private boolean isReloading = false;

    public JIPipeDesktopWekaClassifierParameterEditorUI(InitializationParameters parameters) {
        super(WekaClassifierParameter.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.addActionListener(e -> pick());
        UIUtils.setStandardButtonBorder(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        JButton selectButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit.png"));
        UIUtils.setStandardButtonBorder(selectButton);
        selectButton.setToolTipText("Select classifier");
        selectButton.addActionListener(e -> pick());
        buttonPanel.add(selectButton);

        JButton configureButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        UIUtils.setStandardButtonBorder(configureButton);
        configureButton.setToolTipText("Configure the classifier");
        configureButton.addActionListener(e -> pick());
        buttonPanel.add(configureButton);

        add(buttonPanel, BorderLayout.EAST);
    }

    private void pick() {

        // Use the Weka native dialogs
        WekaClassifierParameter parameter = getParameter();
        GenericObjectEditor editor = new GenericObjectEditor();
        editor.setClassType(Classifier.class);
        editor.setValue(parameter.getClassifier());
        PropertyPanel propertyPanel = new PropertyPanel(editor);

        // Include into a form panel
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(new MarkdownText("# Edit classifier\n\n" +
                "Click 'Choose' to change the type of classifier. To edit classifier parameters, " +
                "click the white area next to 'Choose'."), JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopFormPanel.WITH_DOCUMENTATION);
        formPanel.addToForm(propertyPanel, new JLabel("Classifier"), null);

        if (UIUtils.showOKCancelDialog(getDesktopWorkbench().getWindow(), formPanel, "Edit classifier")) {
            WekaClassifierParameter newParameter = new WekaClassifierParameter();
            newParameter.setClassifier((Classifier) editor.getValue());
            setParameter(newParameter, true);
        }
    }

    @Override
    public void reload() {
        if (isReloading)
            return;
        isReloading = true;
        WekaClassifierParameter parameter = getParameter();
        if (parameter.getClassifier() instanceof OptionHandler)
            currentlyDisplayed.setText(parameter.getClassifier().getClass().getSimpleName() + " " + Utils.joinOptions(((OptionHandler) parameter.getClassifier()).getOptions()));
        else if (parameter.getClassifier() instanceof CustomDisplayStringProvider)
            currentlyDisplayed.setText(((CustomDisplayStringProvider) parameter.getClassifier()).toDisplay());
        else
            currentlyDisplayed.setText("" + parameter.getClassifier());
        isReloading = false;
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
