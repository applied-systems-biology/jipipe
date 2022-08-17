package org.hkijena.jipipe.ui.grapheditor.general.properties;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeAdvancedParameterEditorUI extends JIPipeWorkbenchPanel {

    private final JIPipeGraphNode node;
    private final JToggleButton enableMultiParametersToggle = new JToggleButton("Enable multiple parameters", UIUtils.getIcon32FromResources("actions/button_ok.png"));

    private final Settings settings;

    public JIPipeAdvancedParameterEditorUI(JIPipeWorkbench workbench, JIPipeGraphNode node) {
        super(workbench);
        this.node = node;
        this.settings = new Settings(node);
        initialize();
        if(node instanceof JIPipeParameterSlotAlgorithm) {
            ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().getEventBus().register(this);
        }
        if(node instanceof JIPipeAdaptiveParametersAlgorithm) {
            ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings().getEventBus().register(this);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(FormPanel.WITH_SCROLLING);
        if(node instanceof JIPipeParameterSlotAlgorithm) {
            initializeMultiParameterQuickSettings(formPanel);
        }
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), settings, new MarkdownDocument(), FormPanel.NONE);
        parameterPanel.setRedirectDocumentationTarget(formPanel);
        formPanel.addWideToForm(parameterPanel);
        formPanel.addVerticalGlue();
        add(formPanel, BorderLayout.CENTER);
    }

    private void initializeMultiParameterQuickSettings(FormPanel formPanel) {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(UIUtils.createInfoLabel("This node supports multiple parameters", "You can supply parameter sets via the <strong>Parameters</strong> input."), BorderLayout.CENTER);
        buttonPanel.add(enableMultiParametersToggle, BorderLayout.EAST);
        formPanel.addWideToForm(buttonPanel);

        JTextPane helpText = UIUtils.makeBorderlessReadonlyTextPane("To create multiple parameter sets, utilize the node <strong>Add data &gt; Parameters &gt; Define multiple parameters</strong> that comes with an interactive editor. " +
                "Alternatively, you can use <strong>Generate parameters from expression</strong> if you prefer creating parameter sets automatically.<br/><br/>" +
                "For each parameter set, outputs are labeled by non-<code>#</code> annotations based on the parameter values. If you want to change this behavior, scroll down to the <strong>Multi-parameter settings</strong>.", false);
        helpText.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        formPanel.addWideToForm(helpText);
        formPanel.addWideToForm(new JSeparator(SwingConstants.HORIZONTAL));

        enableMultiParametersToggle.addActionListener(e -> {
            ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings().setParameter("has-parameter-slot", enableMultiParametersToggle.isSelected());
        });
    }

    public static boolean supports(JIPipeGraphNode node) {
        return (node instanceof JIPipeParameterSlotAlgorithm) || (node instanceof JIPipeAdaptiveParametersAlgorithm);
    }

    public JIPipeGraphNode getNode() {
        return node;
    }

    @Subscribe
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if(event.getSource() instanceof JIPipeParameterSlotAlgorithmSettings) {
            enableMultiParametersToggle.setSelected(((JIPipeParameterSlotAlgorithmSettings) event.getSource()).isHasParameterSlot());
        }
        else if(event.getSource() instanceof JIPipeAdaptiveParameterSettings) {

        }
    }

    public static class Settings extends AbstractJIPipeParameterCollection {
        private final JIPipeParameterSlotAlgorithmSettings parameterSlotAlgorithmSettings;
        private final JIPipeAdaptiveParameterSettings adaptiveParameterSettings;

        public Settings(JIPipeGraphNode node) {
            if(node instanceof JIPipeParameterSlotAlgorithm) {
                parameterSlotAlgorithmSettings = ((JIPipeParameterSlotAlgorithm) node).getParameterSlotAlgorithmSettings();
                parameterSlotAlgorithmSettings.getEventBus().register(this);
            }
            else {
                parameterSlotAlgorithmSettings = null;
            }
            if(node instanceof JIPipeAdaptiveParametersAlgorithm) {
                adaptiveParameterSettings = ((JIPipeAdaptiveParametersAlgorithm) node).getAdaptiveParameterSettings();
                adaptiveParameterSettings.getEventBus().register(this);
            }
            else {
                adaptiveParameterSettings = null;
            }
        }

        @JIPipeDocumentation(name = "Multi-parameter settings")
        @JIPipeParameter(value = "jipipe:parameter-slot-algorithm", collapsed = true,
                iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/wrench.png",
                iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/wrench.png")
        public JIPipeParameterSlotAlgorithmSettings getParameterSlotAlgorithmSettings() {
            return parameterSlotAlgorithmSettings;
        }

        @JIPipeDocumentation(name = "Adaptive parameter settings")
        @JIPipeParameter(value = "jipipe:adaptive-parameters", collapsed = true,
                iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-function.png",
                iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-function.png")
        public JIPipeAdaptiveParameterSettings getAdaptiveParameterSettings() {
            return adaptiveParameterSettings;
        }
    }
}
