package org.hkijena.jipipe.api.environments;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.registries.JIPipeExternalEnvironmentRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

public class ExternalEnvironmentParameterEditorUI extends JIPipeParameterEditorUI {

    private JLabel nameLabel = new JLabel();
    private JTextField pathLabel = UIUtils.makeReadonlyBorderlessTextField("");
    private JPopupMenu installMenu = new JPopupMenu();
    private JButton editButton;
    private JButton installButton;

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public ExternalEnvironmentParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
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

        editButton = new JButton("Edit", UIUtils.getIconFromResources("actions/document-edit.png"));
        editButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0,
                UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(4, 3, 4, 3)));
        editButton.setOpaque(false);
        editButton.setToolTipText("Edit current environment");
        editButton.addActionListener(e -> editEnvironment());
        buttonPanel.add(editButton);

        installButton = new JButton("Select/Install", UIUtils.getIconFromResources("actions/browser-download.png"));
        installButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0,
                UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(4, 3, 4, 8)));
        installButton.setOpaque(false);
        installButton.setToolTipText("Installs a new environment (if available) or selects an existing one");
        UIUtils.addReloadablePopupMenuToComponent(installButton, installMenu, this::reloadInstallMenu);
        buttonPanel.add(installButton);
    }

    private void reloadInstallMenu() {
        Class<?> fieldClass = getParameterAccess().getFieldClass();
        installMenu.removeAll();

        ExternalEnvironmentParameterSettings settings = getParameterAccess().getAnnotationOfType(ExternalEnvironmentParameterSettings.class);

        if (settings == null || settings.allowManagePreset()) {
            JMenu presetMenu = new JMenu("Load preset");
            List<ExternalEnvironment> presets = JIPipe.getInstance().getExternalEnvironmentRegistry().getPresets(fieldClass);

            for (ExternalEnvironment preset : presets) {
                JMenuItem presetItem = new JMenuItem(preset.getName(), preset.getIcon());
                presetItem.addActionListener(e -> loadPreset(preset));
                presetMenu.add(presetItem);
            }

            if (!presets.isEmpty()) {
                installMenu.add(presetMenu);
            }

            JMenuItem savePresetItem = new JMenuItem("Save as preset ...", UIUtils.getIconFromResources("actions/save.png"));
            savePresetItem.addActionListener(e -> saveAsPreset());
            installMenu.add(savePresetItem);
        }
        if (installMenu.getComponentCount() > 0) {
            installMenu.addSeparator();
        }
        if (settings == null || settings.allowInstall()) {
            for (JIPipeExternalEnvironmentRegistry.InstallerEntry installer : JIPipe.getInstance()
                    .getExternalEnvironmentRegistry().getInstallers((Class<? extends ExternalEnvironment>) fieldClass)) {
                JMenuItem item = new JMenuItem(installer.getName(), installer.getIcon());
                item.setToolTipText(installer.getDescription());
                item.addActionListener(e -> JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(),
                        (JIPipeRunnable) ReflectionUtils.newInstance(installer.getInstallerClass(), getWorkbench(), getParameterAccess())));
                installMenu.add(item);
            }
        }
    }

    private void loadPreset(ExternalEnvironment preset) {
        if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(), "Do you really want to load " +
                "the preset '" + preset.getName() + "?", "Load preset", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            setParameter(preset, true);
        }
    }

    private void saveAsPreset() {
        JIPipeValidityReport report = new JIPipeValidityReport();
        ExternalEnvironment parameter = getParameter(ExternalEnvironment.class);
        parameter.reportValidity(report);

        if (!report.isValid()) {
            if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                    "The current settings seem to be invalid. Do you want to save them as preset, anyways?",
                    "Save preset",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                return;
            }
        }

        Class<?> fieldClass = getParameterAccess().getFieldClass();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getInstance().getParameterTypeRegistry().getInfoByFieldClass(fieldClass);

        ExternalEnvironment duplicate = (ExternalEnvironment) typeInfo.duplicate(parameter);
        String newName = JOptionPane.showInputDialog(getWorkbench().getWindow(), "Please insert the name of the preset:", duplicate.getName());
        if (StringUtils.isNullOrEmpty(newName))
            return;
        duplicate.setName(newName);

        JIPipe.getInstance().getExternalEnvironmentRegistry().addPreset(fieldClass, duplicate);
    }

    private void editEnvironment() {
        Class<?> fieldClass = getParameterAccess().getFieldClass();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getInstance().getParameterTypeRegistry().getInfoByFieldClass(fieldClass);
        ExternalEnvironment parameter = (ExternalEnvironment) typeInfo.duplicate(getParameter(ExternalEnvironment.class));
        boolean result = ParameterPanel.showDialog(getWorkbench(),
                parameter,
                null,
                "Edit environment",
                ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION);
        if (result) {
            setParameter(parameter, true);
        }
    }

    /**
     * Workaround for bug #458 due to modal windows
     */
    @Subscribe
    public void onInstallationFinished(RunUIWorkerFinishedEvent event) {
        if (!isDisplayable()) {
            JIPipeRunnerQueue.getInstance().getEventBus().unregister(this);
            return;
        }
        if (event.getWorker().getRun() instanceof ExternalEnvironmentInstaller) {
            reload();
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        ExternalEnvironmentParameterSettings settings = getParameterAccess().getAnnotationOfType(ExternalEnvironmentParameterSettings.class);
        if (settings != null) {
            editButton.setVisible(settings.allowEditButton());
            installButton.setVisible(settings.allowInstallButton());
        } else {
            editButton.setVisible(true);
            installButton.setVisible(true);
        }

        ExternalEnvironment parameter = getParameter(ExternalEnvironment.class);
        nameLabel.setIcon(parameter.getIcon());
        nameLabel.setText(parameter.getName());
        pathLabel.setText(StringUtils.orElse(parameter.getInfo(), "<Nothing set>"));
        JIPipeValidityReport report = new JIPipeValidityReport();
        parameter.reportValidity(report);
        if (!report.isValid()) {
            pathLabel.setForeground(Color.RED);
        } else {
            pathLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
    }
}
