package org.hkijena.jipipe.extensions.parameters.external;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class PythonEnvironmentParameterEditorUI extends JIPipeParameterEditorUI {

    private JLabel typeLabel = new JLabel();
    private JTextField pathLabel = UIUtils.makeReadonlyBorderlessTextField("");
    private JPopupMenu installMenu = new JPopupMenu();

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public PythonEnvironmentParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout(4,0));
        typeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        typeLabel.setBorder(BorderFactory.createEmptyBorder(0,4,0,12));
        typeLabel.setIcon(UIUtils.getIconFromResources("apps/python.png"));
        setOpaque(true);
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(BorderFactory.createEtchedBorder());
        add(typeLabel, BorderLayout.WEST);
        add(pathLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        add(buttonPanel, BorderLayout.EAST);

        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));

        JButton editButton = new JButton("Edit", UIUtils.getIconFromResources("actions/document-edit.png"));
//        UIUtils.makeFlat25x25(editButton);
        editButton.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
        editButton.setOpaque(false);
        editButton.setToolTipText("Edit current environment");
        editButton.addActionListener(e -> editEnvironment());
        buttonPanel.add(editButton);

        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));

        JButton installButton = new JButton("Select/Install", UIUtils.getIconFromResources("actions/browser-download.png"));
//        UIUtils.makeFlat25x25(installButton);
        installButton.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 8));
        installButton.setOpaque(false);
        installButton.setToolTipText("Installs a new environment (if available) or selects an existing one");
        UIUtils.addPopupMenuToComponent(installButton, installMenu);
        buttonPanel.add(installButton);
    }

    private void editEnvironment() {
        PythonEnvironmentParameter parameter = new PythonEnvironmentParameter(getParameter(PythonEnvironmentParameter.class));
        boolean result = ParameterPanel.showDialog(getWorkbench(),
                parameter,
                null,
                "Edit Python environment",
                ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION);
        if(result) {
            setParameter(parameter, true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        PythonEnvironmentSettings settings = getParameterAccess().getAnnotationOfType(PythonEnvironmentSettings.class);
        installMenu.removeAll();
        if(settings != null && settings.installers().length > 0) {
            ArrayList<Class<? extends PythonEnvironmentInstaller>> list = new ArrayList<>(Arrays.asList(settings.installers()));
            list.sort(Comparator.comparing(klass -> {
                JIPipeDocumentation annotation = klass.getAnnotation(JIPipeDocumentation.class);
                if(annotation != null)
                    return annotation.name();
                else
                    return klass.getName();
            }));
            for (Class<? extends PythonEnvironmentInstaller> klass : list) {
                JIPipeDocumentation annotation = klass.getAnnotation(JIPipeDocumentation.class);
                JMenuItem item = new JMenuItem(annotation != null ? annotation.name() : klass.getName(), UIUtils.getIconFromResources("apps/python.png"));
                item.addActionListener(e -> {
                    JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(),
                            (JIPipeRunnable) ReflectionUtils.newInstance(klass, getWorkbench(), getParameterAccess()));
                });
                item.setToolTipText(annotation != null ? annotation.description() : null);
                installMenu.add(item);
            }
        }
        else {
            JMenuItem item = new JMenuItem("No options available");
            item.setEnabled(false);
            installMenu.add(item);
        }

        PythonEnvironmentParameter parameter = getParameter(PythonEnvironmentParameter.class);
        typeLabel.setText(parameter.getType().toString());
        pathLabel.setText(StringUtils.orElse(parameter.getExecutablePath().toString(), "<None selected>"));
        if(parameter.getExecutablePath().toString().isEmpty() || !Files.exists(parameter.getExecutablePath())) {
            pathLabel.setForeground(Color.RED);
        }
        else {
            pathLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
    }
}
