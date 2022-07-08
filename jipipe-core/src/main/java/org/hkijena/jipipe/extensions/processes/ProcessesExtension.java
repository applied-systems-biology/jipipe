package org.hkijena.jipipe.extensions.processes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

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
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
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

    @Override
    public boolean isCoreExtension() {
        return true;
    }
}
