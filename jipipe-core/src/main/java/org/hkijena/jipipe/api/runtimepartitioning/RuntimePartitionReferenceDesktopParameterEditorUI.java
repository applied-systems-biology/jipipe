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

package org.hkijena.jipipe.api.runtimepartitioning;

import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopRuntimePartitionListEditor;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class RuntimePartitionReferenceDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI implements JIPipeRunnable.FinishedEventListener {

    private final JButton nameLabel = new JButton();
    private final JTextField pathLabel = UIUtils.createReadonlyBorderlessTextField("");
    private final JPopupMenu configureMenu = new JPopupMenu();

    public RuntimePartitionReferenceDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
        reload();
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(4, 0));
        nameLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 12));
        nameLabel.setOpaque(false);
        nameLabel.addActionListener(e -> editCurrentPartition());
        setOpaque(true);
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());
        add(nameLabel, BorderLayout.WEST);
        add(pathLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        add(buttonPanel, BorderLayout.EAST);

        JButton configureButton = new JButton("Configure ...", UIUtils.getIconFromResources("actions/configure.png"));
        configureButton.setBackground(getBackground());
        configureButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0,
                        UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(4, 3, 4, 3)));
        configureButton.setOpaque(true);
        configureButton.setToolTipText("Edit/select/install partition");
        UIUtils.addReloadablePopupMenuToButton(configureButton, configureMenu, this::reloadConfigMenu);
        buttonPanel.add(configureButton);
    }

    private void reloadConfigMenu() {
        configureMenu.removeAll();
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            configureMenu.add(UIUtils.createMenuItem("Edit current", "Edits the current partition", UIUtils.getIconFromResources("actions/edit.png"), this::editCurrentPartition));
            configureMenu.addSeparator();
            RuntimePartitionReferenceParameter parameter = getParameter(RuntimePartitionReferenceParameter.class);
            int index = parameter.getIndex();
            JIPipeRuntimePartition currentPartition = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProject().getRuntimePartitions().get(index);
            JIPipeRuntimePartitionConfiguration runtimePartitions = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProject().getRuntimePartitions();
            for (JIPipeRuntimePartition runtimePartition : runtimePartitions.toList()) {
                JMenuItem menuItem = new JMenuItem(runtimePartitions.getFullName(runtimePartition), runtimePartitions.getIcon(runtimePartition));
                if (currentPartition == runtimePartition) {
                    menuItem.setEnabled(false);
                }
                menuItem.addActionListener(e -> switchPartition(runtimePartition));
                configureMenu.add(menuItem);
            }
        } else {
            JMenuItem menuItem = new JMenuItem("Not available");
            menuItem.setEnabled(false);
            configureMenu.add(menuItem);
        }
    }

    private void switchPartition(JIPipeRuntimePartition runtimePartition) {
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            RuntimePartitionReferenceParameter parameter = getParameter(RuntimePartitionReferenceParameter.class);
            JIPipeRuntimePartitionConfiguration runtimePartitions = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProject().getRuntimePartitions();
            int newIndex = runtimePartitions.indexOf(runtimePartition);
            if (newIndex != -1) {
                parameter.setIndex(newIndex);
                setParameter(parameter, true);
            }
        }
    }

    private void editCurrentPartition() {
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            JIPipeRuntimePartitionConfiguration runtimePartitions = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProject().getRuntimePartitions();
            RuntimePartitionReferenceParameter parameter = getParameter(RuntimePartitionReferenceParameter.class);
            JIPipeRuntimePartition runtimePartition = runtimePartitions.get(parameter.getIndex());
            JIPipeDesktopRuntimePartitionListEditor.editRuntimePartition(getDesktopWorkbench(), runtimePartition);
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
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            int index = parameter.getIndex();
            JIPipeRuntimePartition partition = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProject().getRuntimePartitions().get(index);
            nameLabel.setIcon(partition.getColor().isEnabled() ? new SolidColorIcon(16, 16, partition.getColor().getContent()) : UIUtils.getIconFromResources("actions/runtime-partition.png"));
            nameLabel.setText(index <= 0 ? StringUtils.orElse(partition.getName(), "Default") : StringUtils.orElse(partition.getName(), "Partition " + index));
        } else {
            int index = parameter.getIndex();
            nameLabel.setIcon(UIUtils.getIconFromResources("actions/runtime-partition.png"));
            nameLabel.setText(index <= 0 ? "Default" : "Partition " + index);
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (!isDisplayable()) {
            JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().unsubscribe(this);
            return;
        }
        if (event.getWorker().getRun() instanceof JIPipeExternalEnvironmentInstaller) {
            reload();
        }
    }
}
