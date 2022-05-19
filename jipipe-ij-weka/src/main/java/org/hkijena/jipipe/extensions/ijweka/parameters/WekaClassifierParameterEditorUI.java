package org.hkijena.jipipe.extensions.ijweka.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.pickers.JIPipeDataTypePicker;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.classfilters.AnyClassFilter;
import org.hkijena.jipipe.utils.classfilters.ClassFilter;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.CustomDisplayStringProvider;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WekaClassifierParameterEditorUI extends JIPipeParameterEditorUI {
    private JButton currentlyDisplayed;
    private boolean isReloading = false;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public WekaClassifierParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.addActionListener(e -> pick());
        UIUtils.makeFlat(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.makeFlat(selectButton);
        selectButton.setToolTipText("Select classifier");
        selectButton.addActionListener(e -> pick());
        buttonPanel.add(selectButton);

        JButton configureButton = new JButton(UIUtils.getIconFromResources("actions/configure.png"));
        UIUtils.makeFlat(configureButton);
        configureButton.setToolTipText("Configure the classifier");
        configureButton.addActionListener(e -> pick());
        buttonPanel.add(configureButton);

        add(buttonPanel, BorderLayout.EAST);
    }

    private void pick() {

        // Use the Weka native dialogs
        WekaClassifierParameter parameter = getParameter(WekaClassifierParameter.class);
        GenericObjectEditor editor = new GenericObjectEditor();
        editor.setClassType(Classifier.class);
        editor.setValue(parameter.getClassifier());
        PropertyPanel propertyPanel = new PropertyPanel(editor);

        // Include into a form panel
        FormPanel formPanel = new FormPanel(new MarkdownDocument("# Edit classifier\n\n" +
                "Click 'Choose' to change the type of classifier. To edit classifier parameters, " +
                "click the white area next to 'Choose'."), FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION);
        formPanel.addToForm(propertyPanel, new JLabel("Classifier"), null);

        if(UIUtils.showOKCancelDialog(getWorkbench(), formPanel, "Edit classifier")) {
            WekaClassifierParameter newParameter = new WekaClassifierParameter();
            newParameter.setClassifier((Classifier)editor.getValue());
            setParameter(newParameter, true);
        }
    }

    @Override
    public void reload() {
        if (isReloading)
            return;
        isReloading = true;
        WekaClassifierParameter parameter = getParameter(WekaClassifierParameter.class);
        if(parameter.getClassifier() instanceof OptionHandler)
            currentlyDisplayed.setText(parameter.getClassifier().getClass().getSimpleName() + " " + Utils.joinOptions(((OptionHandler)parameter.getClassifier()).getOptions()));
        else if(parameter.getClassifier() instanceof CustomDisplayStringProvider)
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
