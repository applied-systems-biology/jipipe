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

package org.hkijena.jipipe.plugins.dataenvironment;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class DataDirectoriesPlugin extends JIPipePrepackagedDefaultJavaPlugin {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Sample data";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Parameter types and settings for sample data");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        JIPipeDataDirectoryEnvironmentApplicationSettings settings = new JIPipeDataDirectoryEnvironmentApplicationSettings();
        registerApplicationSettingsSheet(settings);
        registerEnvironment(JIPipeDataDirectoryEnvironment.class,
                JIPipeDataDirectoryEnvironment.List.class,
                settings,
                "data-directory",
                "Data directory",
                "Directory containing data",
                UIUtils.getIconFromResources("actions/vcs-update-cvs-cervisia.png"));
        registerParameterType("optional-data-directory",
                OptionalJIPipeDataDirectoryEnvironment.class,
                "Optional data directory",
                "Directory containing data");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:data-artifacts";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
