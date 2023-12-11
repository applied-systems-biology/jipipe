package org.hkijena.jipipe.api.runtimepartitioning;

import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.settings.JIPipeRuntimePartitionListEditor;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class RuntimePartitionReferenceParameterEditorUI extends JIPipeParameterEditorUI implements JIPipeRunnable.FinishedEventListener {

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
    public RuntimePartitionReferenceParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
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
        UIUtils.addReloadablePopupMenuToButton(configureButton, configureMenu, this::reloadConfigMenu);
        buttonPanel.add(configureButton);
    }

    private void reloadConfigMenu() {
        configureMenu.removeAll();
        if(getWorkbench() instanceof JIPipeProjectWorkbench) {
            configureMenu.add(UIUtils.createMenuItem("Edit current", "Edits the current partition", UIUtils.getIconFromResources("actions/edit.png"), this::editCurrentPartition));
            configureMenu.addSeparator();
            RuntimePartitionReferenceParameter parameter = getParameter(RuntimePartitionReferenceParameter.class);
            int index = parameter.getIndex();
            JIPipeRuntimePartition currentPartition = ((JIPipeProjectWorkbench) getWorkbench()).getProject().getRuntimePartitions().get(index);
            JIPipeRuntimePartitionConfiguration runtimePartitions = ((JIPipeProjectWorkbench) getWorkbench()).getProject().getRuntimePartitions();
            for (JIPipeRuntimePartition runtimePartition : runtimePartitions.toList()) {
                JMenuItem menuItem = new JMenuItem(runtimePartitions.getFullName(runtimePartition), runtimePartitions.getIcon(runtimePartition));
                if(currentPartition == runtimePartition) {
                    menuItem.setEnabled(false);
                }
                menuItem.addActionListener(e -> switchPartition(runtimePartition));
                configureMenu.add(menuItem);
            }
        }
        else {
            JMenuItem menuItem = new JMenuItem("Not available");
            menuItem.setEnabled(false);
            configureMenu.add(menuItem);
        }
    }

    private void switchPartition(JIPipeRuntimePartition runtimePartition) {
        if(getWorkbench() instanceof JIPipeProjectWorkbench) {
            RuntimePartitionReferenceParameter parameter = getParameter(RuntimePartitionReferenceParameter.class);
            JIPipeRuntimePartitionConfiguration runtimePartitions = ((JIPipeProjectWorkbench) getWorkbench()).getProject().getRuntimePartitions();
            int newIndex = runtimePartitions.indexOf(runtimePartition);
            if(newIndex != -1) {
                parameter.setIndex(newIndex);
                setParameter(parameter, true);
            }
        }
    }

    private void editCurrentPartition() {
        if(getWorkbench() instanceof JIPipeProjectWorkbench) {
            JIPipeRuntimePartitionConfiguration runtimePartitions = ((JIPipeProjectWorkbench) getWorkbench()).getProject().getRuntimePartitions();
            RuntimePartitionReferenceParameter parameter = getParameter(RuntimePartitionReferenceParameter.class);
            JIPipeRuntimePartition runtimePartition = runtimePartitions.get(parameter.getIndex());
            JIPipeRuntimePartitionListEditor.editRuntimePartition(getWorkbench(), runtimePartition);
            reload();
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        RuntimePartitionReferenceParameter parameter = getParameter(RuntimePartitionReferenceParameter.class);
        if(getWorkbench() instanceof JIPipeProjectWorkbench) {
            int index = parameter.getIndex();
            JIPipeRuntimePartition partition = ((JIPipeProjectWorkbench) getWorkbench()).getProject().getRuntimePartitions().get(index);
            nameLabel.setIcon(partition.getColor().isEnabled() ? new SolidColorIcon(16, 16, partition.getColor().getContent()) : UIUtils.getIconFromResources("actions/runtime-partition.png"));
            nameLabel.setText(index <= 0 ? StringUtils.orElse(partition.getName(), "Default") : StringUtils.orElse(partition.getName(), "Partition " + index));
        }
        else {
            int index = parameter.getIndex();
            nameLabel.setIcon(UIUtils.getIconFromResources("actions/runtime-partition.png"));
            nameLabel.setText(index <= 0 ? "Default" : "Partition " + index);
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
