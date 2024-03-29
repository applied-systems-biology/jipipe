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

package org.hkijena.jipipe.extensions.processes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class ProcessesPlugin extends JIPipePrepackagedDefaultJavaPlugin {
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
        registerParameterType("optional-process-environment",
                OptionalProcessEnvironment.class,
                null,
                null,
                "Optional process environment",
                "An optional process environment",
                null);
        registerSettingsSheet(ProcessesExtensionSettings.ID,
                "Processes",
                "Setup external processes to be used in the 'Run process' nodes",
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
