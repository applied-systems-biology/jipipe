package org.hkijena.jipipe.api.runtimepartitioning;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
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
        configureMenu.removeAll();
    }

    private void editEnvironment() {
        Class<?> fieldClass = getParameterAccess().getFieldClass();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getInstance().getParameterTypeRegistry().getInfoByFieldClass(fieldClass);
        JIPipeEnvironment parameter = (JIPipeEnvironment) typeInfo.duplicate(getParameter(JIPipeEnvironment.class));
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
        RuntimePartitionReferenceParameter parameter = getParameter(RuntimePartitionReferenceParameter.class);
//        nameLabel.setIcon(parameter.getIcon());
//        nameLabel.setText(parameter.getName());
//        pathLabel.setText(StringUtils.orElse(parameter.getInfo(), "<Nothing set>"));
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
