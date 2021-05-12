package org.hkijena.jipipe.extensions.environments;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalEnvironmentParameterEditorUI extends JIPipeParameterEditorUI {

    private JLabel typeLabel = new JLabel();
    private JTextField pathLabel = UIUtils.makeReadonlyBorderlessTextField("");
    private JPopupMenu installMenu = new JPopupMenu();

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
    }

    private void initialize() {
        setLayout(new BorderLayout(4,0));
        typeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        typeLabel.setBorder(BorderFactory.createEmptyBorder(0,4,0,12));
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
        Class<?> fieldClass = getParameterAccess().getFieldClass();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getInstance().getParameterTypeRegistry().getInfoByFieldClass(fieldClass);
        ExternalEnvironment parameter = (ExternalEnvironment) typeInfo.duplicate(getParameter(ExternalEnvironment.class));
        boolean result = ParameterPanel.showDialog(getWorkbench(),
                parameter,
                null,
                "Edit environment",
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
        installMenu.removeAll();
        Class<?> fieldClass = getParameterAccess().getFieldClass();
        List<Class<?>> installers = JIPipe.getInstance().getUtilityRegistry().getUtilitiesFor(fieldClass).stream().filter(ExternalEnvironmentInstaller.class::isAssignableFrom).
                sorted(Comparator.comparing(klass -> {
                    JIPipeDocumentation annotation = klass.getAnnotation(JIPipeDocumentation.class);
                    if (annotation != null)
                        return annotation.name();
                    else
                        return klass.getName();
                })).collect(Collectors.toList());
        if(!installers.isEmpty()) {
            for (Class<?> klass : installers) {
                JIPipeDocumentation annotation = klass.getAnnotation(JIPipeDocumentation.class);
                JMenuItem item = new JMenuItem(annotation != null ? annotation.name() : klass.getName(),
                        UIUtils.getIconFromResources("actions/configure.png"));
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

        ExternalEnvironment parameter = getParameter(ExternalEnvironment.class);
        typeLabel.setIcon(parameter.getIcon());
        typeLabel.setText(parameter.getStatus());
        pathLabel.setText(StringUtils.orElse(parameter.getInfo(), "<Nothing set>"));
        JIPipeValidityReport report = new JIPipeValidityReport();
        parameter.reportValidity(report);
        if(!report.isValid()) {
            pathLabel.setForeground(Color.RED);
        }
        else {
            pathLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
    }
}
