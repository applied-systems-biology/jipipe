package org.hkijena.jipipe.extensions.processes;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class ProcessesExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Processes environments";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Data types for running processes");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:processes";
    }

    @Override
    public String getDependencyVersion() {
        return "1.52.2";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/utilities-terminal.png"));
    }

    @Override
    public void register() {
        ProcessesExtensionSettings extensionSettings = new ProcessesExtensionSettings();

        registerEnvironment(ProcessEnvironment.class,
                ProcessEnvironment.List.class,
                extensionSettings,
                "process",
                "Process environment",
                "A process environment",
                UIUtils.getIconFromResources("apps/utilities-terminal.png"));
        registerSettingsSheet(ProcessesExtensionSettings.ID,
                "Processes",
                UIUtils.getIconFromResources("apps/utilities-terminal.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                extensionSettings);
    }
}
