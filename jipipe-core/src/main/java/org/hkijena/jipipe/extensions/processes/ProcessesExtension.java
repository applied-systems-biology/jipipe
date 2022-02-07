package org.hkijena.jipipe.extensions.processes;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
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
        return "1.63.0";
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
