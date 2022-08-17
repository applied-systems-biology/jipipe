package org.hkijena.jipipe.ui.grapheditor.general.properties;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReference;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.pickers.JIPipeParameterTypeInfoPicker;
import org.hkijena.jipipe.ui.parameters.AdaptiveParameterBuilder;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.python.antlr.ast.Str;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if(node instanceof JIPipeAdaptiveParametersAlgorithm) {
            initializeAdaptiveParameterSettings(formPanel);
        }
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), settings, new MarkdownDocument(), FormPanel.WITH_DOCUMENTATION | FormPanel.DOCUMENTATION_NO_UI);
        parameterPanel.setRedirectDocumentationTarget(formPanel);
        formPanel.addWideToForm(parameterPanel);
        formPanel.addVerticalGlue();
        add(formPanel, BorderLayout.CENTER);
    }

    private void initializeAdaptiveParameterSettings(FormPanel formPanel) {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = UIUtils.createInfoLabel("This node supports adaptive parameters", "Specific parameters are <i>adapted</i> to the properties (e.g., annotations) of the currently processed data batch.");
        buttonPanel.add(infoLabel, BorderLayout.CENTER);
        JButton addButton =new JButton("Add adaptive parameter", UIUtils.getIcon32FromResources("actions/add.png"));
        JPopupMenu addMenu = UIUtils.addPopupMenuToComponent(addButton);
        addMenu.add(UIUtils.createMenuItem("Pick existing parameter", "Picks a parameter from the node. Its current value is added into the list of adapted parameters with the appropriate settings.", UIUtils.getIconFromResources("actions/color-select.png"), this::addAdaptiveParameterFromNode));
        addMenu.add(UIUtils.createMenuItem("Switch/case builder", "Opens an interface that helps creating an adaptive parameter based on branching (switch/case)", UIUtils.getIconFromResources("actions/configure.png"), this::addAdaptiveParameterSwitchCase));
        buttonPanel.add(addButton, BorderLayout.EAST);
        formPanel.addWideToForm(buttonPanel);

        JTextPane helpText = UIUtils.makeBorderlessReadonlyTextPane("To make a parameter adapt to annotations, add an entry into the <strong>Overridden parameters</strong> list below by clicking <strong>Add adaptive parameter &gt; Pick existing parameter</strong>. Adapt the expression accordingly. " +
                "Please note that the expression must return a compatible value or a string in JSON format.<br/><br/>" +
                "Custom parameter values are attached as annotations to generated outputs. You can find all settings in the <strong>Adaptive parameter settings</strong> section below."
                + (settings.parameterSlotAlgorithmSettings != null ? "<br/><br/><i>Please note that multiple parameters (if enabled) are overridden by adaptive parameters.</i>" : ""), false);
        UIUtils.setJTextPaneFont(helpText, infoLabel.getFont(), UIManager.getColor("Label.foreground"));
        helpText.setBorder(BorderFactory.createEmptyBorder(16,43,16,16));
        formPanel.addWideToForm(helpText);

        JIPipeParameterAccess access = settings.getAdaptiveParameterSettings().getParameterAccess("overridden-parameters");
        JIPipeParameterEditorUI editor = JIPipe.getParameterTypes().createEditorFor(getWorkbench(), access);
        formPanel.addWideToForm(editor);

        formPanel.addWideToForm(new JSeparator(SwingConstants.HORIZONTAL));
    }

    private void addAdaptiveParameterSwitchCase() {
        AdaptiveParameterBuilder dialog = new AdaptiveParameterBuilder(getWorkbench(), getNode() != null ? getNode() : settings.adaptiveParameterSettings);
        dialog.setModal(true);
        dialog.setSize(800, 600);
        dialog.revalidate();
        dialog.repaint();
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        StringQueryExpressionAndStringPairParameter.List target = settings.getAdaptiveParameterSettings().getOverriddenParameters();
        if (!dialog.isCanceled() && dialog.getCurrentParameterAccess() != null) {
            DefaultExpressionParameter parameter = dialog.build();
            String uniqueKey = dialog.getParameterTree().getUniqueKey(dialog.getCurrentParameterAccess());
            target.add(new StringQueryExpressionAndStringPairParameter(parameter.getExpression(), uniqueKey));
            settings.getAdaptiveParameterSettings().setParameter("overridden-parameters", target);
        }
    }

    private Set<String> getExistingAdaptiveParameters() {
        Set<String> existing = new HashSet<>();
        StringQueryExpressionAndStringPairParameter.List target = settings.getAdaptiveParameterSettings().getOverriddenParameters();
        for (StringQueryExpressionAndStringPairParameter entry : target) {
            existing.add(entry.getValue());
        }
        return existing;
    }

    private void addAdaptiveParameterFromNode() {
        JIPipeParameterTree parameterTree = new JIPipeParameterTree(node);
        List<Object> selected = ParameterTreeUI.showPickerDialog(this, parameterTree, "Add adaptive parameter");
        boolean skipped = false;
        boolean foundPassThrough = false;
        StringQueryExpressionAndStringPairParameter.List target = settings.getAdaptiveParameterSettings().getOverriddenParameters();
        Set<String> existing = getExistingAdaptiveParameters();
        for (Object parameter : selected) {
            if (parameter != null) {
                for (JIPipeParameterAccess child : parameterTree.getAllChildParameters(parameter)) {
                    if(child.getSource() == settings.adaptiveParameterSettings || child.getSource() == settings.parameterSlotAlgorithmSettings)
                        continue;
                    String uniqueKey = parameterTree.getUniqueKey(child);
                    if("jipipe:algorithm:pass-through".equals(uniqueKey)) {
                        foundPassThrough = true;
                    }
                    if(existing.contains(uniqueKey)) {
                        skipped = true;
                        continue;
                    }
                    Object o = child.get(Object.class);
                    String expression;
                    if(o instanceof Number) {
                        expression = "" + o;
                    }
                    else if(o instanceof String) {
                        expression = (String) o;
                    }
                    else {
                        expression = JsonUtils.toJsonString(o);
                    }
                    target.add(new StringQueryExpressionAndStringPairParameter(expression, uniqueKey));
                }
            }
        }
        settings.getAdaptiveParameterSettings().setParameter("overridden-parameters", target);
        if(skipped) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "Some selected entries were already present in the list of overridden parameters and were skipped.", "Add adaptive parameters", JOptionPane.INFORMATION_MESSAGE);
        }
        if(foundPassThrough) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "Please note that a pass through via adaptive parameters is not always possible for various nodes (as the pass through happens early during processing).\n" +
                    "In these cases, pass through has no effect if set via adaptive parameters.", "Add adaptive parameters", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void initializeMultiParameterQuickSettings(FormPanel formPanel) {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = UIUtils.createInfoLabel("This node supports multiple parameters", "You can supply parameter sets via the <strong>Parameters</strong> input.");
        buttonPanel.add(infoLabel, BorderLayout.CENTER);
        buttonPanel.add(enableMultiParametersToggle, BorderLayout.EAST);
        formPanel.addWideToForm(buttonPanel);

        JTextPane helpText = UIUtils.makeBorderlessReadonlyTextPane("To create multiple parameter sets, utilize the node <strong>Add data &gt; Parameters &gt; Define multiple parameters</strong> that comes with an interactive editor. " +
                "Alternatively, you can use <strong>Generate parameters from expression</strong> if you prefer creating parameter sets automatically.<br/><br/>" +
                "For each parameter set, outputs are labeled by non-<code>#</code> annotations based on the parameter values. If you want to change this behavior, scroll down to the <strong>Multi-parameter settings</strong>.", false);
        UIUtils.setJTextPaneFont(helpText, infoLabel.getFont(), UIManager.getColor("Label.foreground"));
        helpText.setBorder(BorderFactory.createEmptyBorder(16,43,16,16));
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

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
            if(access.getSource() == parameterSlotAlgorithmSettings) {
                if("has-parameter-slot".equals(access.getKey()))
                    return false;
            }
            else if(access.getSource() == adaptiveParameterSettings) {
                if("overridden-parameters".equals(access.getKey()))
                    return false;
            }
            return super.isParameterUIVisible(tree, access);
        }
    }
}
