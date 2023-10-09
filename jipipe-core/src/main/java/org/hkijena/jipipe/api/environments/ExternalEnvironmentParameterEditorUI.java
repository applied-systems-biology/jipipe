package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.registries.JIPipeExternalEnvironmentRegistry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

public class ExternalEnvironmentParameterEditorUI extends JIPipeParameterEditorUI implements JIPipeRunnable.FinishedEventListener {

    private final JLabel nameLabel = new JLabel();
    private final JTextField pathLabel = UIUtils.makeReadonlyBorderlessTextField("");
    private final JPopupMenu configureMenu = new JPopupMenu();
    private JButton configureButton;

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public ExternalEnvironmentParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
        JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(4, 0));
        nameLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 12));
        setOpaque(true);
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(BorderFactory.createEtchedBorder());
        add(nameLabel, BorderLayout.WEST);
        add(pathLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        add(buttonPanel, BorderLayout.EAST);

        configureButton = new JButton("Configure ...", UIUtils.getIconFromResources("actions/configure.png"));
        configureButton.setBackground(getBackground());
        configureButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0,
                        UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(4, 3, 4, 3)));
        configureButton.setOpaque(true);
        configureButton.setToolTipText("Edit/select/install environment");
        UIUtils.addReloadablePopupMenuToButton(configureButton, configureMenu, this::reloadInstallMenu);
        buttonPanel.add(configureButton);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(!e.isConsumed()) {
                    e.consume();
                    editEnvironment();
                }
            }
        });
    }

    private void reloadInstallMenu() {
        Class<?> fieldClass = getParameterAccess().getFieldClass();
        configureMenu.removeAll();

        ExternalEnvironmentParameterSettings settings = getParameterAccess().getAnnotationOfType(ExternalEnvironmentParameterSettings.class);

        JMenuItem editMenuItem = UIUtils.createMenuItem("Edit", "Edits the current environment", UIUtils.getIconFromResources("actions/edit.png"), this::editEnvironment);
        configureMenu.add(editMenuItem);

        if (settings == null || settings.allowManagePreset()) {
            JMenu presetMenu = new JMenu("Load preset");
            List<JIPipeExternalEnvironment> presets = JIPipe.getInstance().getExternalEnvironmentRegistry().getPresets(fieldClass);

            for (JIPipeExternalEnvironment preset : presets) {
                JMenuItem presetItem = new JMenuItem(preset.getName(), preset.getIcon());
                presetItem.addActionListener(e -> loadPreset(preset));
                presetMenu.add(presetItem);
            }

            configureMenu.addSeparator();

            if (!presets.isEmpty()) {
                configureMenu.add(presetMenu);
            }

            JMenuItem savePresetItem = new JMenuItem("Save as preset ...", UIUtils.getIconFromResources("actions/save.png"));
            savePresetItem.addActionListener(e -> saveAsPreset());
            configureMenu.add(savePresetItem);
        }
        if (configureMenu.getComponentCount() > 0) {
            configureMenu.addSeparator();
        }
        if (settings == null || settings.allowInstall()) {

            String menuCategory = settings != null ? settings.showCategory() : "";
            boolean foundAdditionalEnvironments = false;
            for (JIPipeExternalEnvironmentRegistry.InstallerEntry installer : JIPipe.getInstance()
                    .getExternalEnvironmentRegistry().getInstallers((Class<? extends JIPipeExternalEnvironment>) fieldClass)) {

                if (!StringUtils.isNullOrEmpty(menuCategory)) {
                    // Check if the category matches
                    ExternalEnvironmentInfo installerInfo = installer.getInstallerClass().getAnnotation(ExternalEnvironmentInfo.class);
                    String installerCategory = "";
                    if (installerInfo != null) {
                        installerCategory = installerInfo.category();
                    }
                    if (!Objects.equals(menuCategory, installerCategory)) {
                        foundAdditionalEnvironments = true;
                        continue;
                    }
                }

                JMenuItem item = new JMenuItem(installer.getName(), installer.getIcon());
                item.setToolTipText(installer.getDescription());
                item.addActionListener(e -> JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(),
                        (JIPipeRunnable) ReflectionUtils.newInstance(installer.getInstallerClass(), getWorkbench(), getParameterAccess())));
                configureMenu.add(item);
            }
            if (foundAdditionalEnvironments) {
                JMenu additionalEnvironmentsMenu = new JMenu("Additional compatible installers");
                for (JIPipeExternalEnvironmentRegistry.InstallerEntry installer : JIPipe.getInstance()
                        .getExternalEnvironmentRegistry().getInstallers((Class<? extends JIPipeExternalEnvironment>) fieldClass)) {
                    if (!StringUtils.isNullOrEmpty(menuCategory)) {
                        // Check if the category matches
                        ExternalEnvironmentInfo installerInfo = installer.getInstallerClass().getAnnotation(ExternalEnvironmentInfo.class);
                        String installerCategory = "";
                        if (installerInfo != null) {
                            installerCategory = installerInfo.category();
                        }
                        if (Objects.equals(menuCategory, installerCategory)) {
                            continue;
                        }
                    }

                    JMenuItem item = new JMenuItem(installer.getName(), installer.getIcon());
                    item.setToolTipText(installer.getDescription());
                    item.addActionListener(e -> JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(),
                            (JIPipeRunnable) ReflectionUtils.newInstance(installer.getInstallerClass(), getWorkbench(), getParameterAccess())));
                    additionalEnvironmentsMenu.add(item);
                }
                configureMenu.add(additionalEnvironmentsMenu);
            }
        }
    }

    private void loadPreset(JIPipeExternalEnvironment preset) {
        if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(), "Do you really want to load " +
                "the preset '" + preset.getName() + "?", "Load preset", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            setParameter(preset, true);
        }
    }

    private void saveAsPreset() {
        JIPipeValidationReport report = new JIPipeValidationReport();
        JIPipeExternalEnvironment parameter = getParameter(JIPipeExternalEnvironment.class);
        parameter.reportValidity(new UnspecifiedValidationReportContext(), report);

        if (!report.isValid()) {
            if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                    "The current settings seem to be invalid. Do you want to save them as preset, anyway?",
                    "Save preset",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                return;
            }
        }

        Class<?> fieldClass = getParameterAccess().getFieldClass();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getInstance().getParameterTypeRegistry().getInfoByFieldClass(fieldClass);

        JIPipeExternalEnvironment duplicate = (JIPipeExternalEnvironment) typeInfo.duplicate(parameter);
        String newName = JOptionPane.showInputDialog(getWorkbench().getWindow(), "Please insert the name of the preset:", duplicate.getName());
        if (StringUtils.isNullOrEmpty(newName))
            return;
        duplicate.setName(newName);

        JIPipe.getInstance().getExternalEnvironmentRegistry().addPreset(fieldClass, duplicate);
    }

    private void editEnvironment() {
        Class<?> fieldClass = getParameterAccess().getFieldClass();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getInstance().getParameterTypeRegistry().getInfoByFieldClass(fieldClass);
        JIPipeExternalEnvironment parameter = (JIPipeExternalEnvironment) typeInfo.duplicate(getParameter(JIPipeExternalEnvironment.class));
        boolean result = ParameterPanel.showDialog(getWorkbench(),
                parameter,
                null,
                "Edit environment",
                ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION);
        if (result) {
            setParameter(parameter, true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
//        ExternalEnvironmentParameterSettings settings = getParameterAccess().getAnnotationOfType(ExternalEnvironmentParameterSettings.class);

        JIPipeExternalEnvironment parameter = getParameter(JIPipeExternalEnvironment.class);
        nameLabel.setIcon(parameter.getIcon());
        nameLabel.setText(parameter.getName());
        pathLabel.setText(StringUtils.orElse(parameter.getInfo(), "<Nothing set>"));
        JIPipeValidationReport report = new JIPipeValidationReport();
        parameter.reportValidity(new UnspecifiedValidationReportContext(), report);
        if (!report.isValid()) {
            pathLabel.setForeground(Color.RED);
        } else {
            pathLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (!isDisplayable()) {
            JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().unsubscribe(this);
            return;
        }
        if (event.getWorker().getRun() instanceof ExternalEnvironmentInstaller) {
            reload();
        }
    }
}
