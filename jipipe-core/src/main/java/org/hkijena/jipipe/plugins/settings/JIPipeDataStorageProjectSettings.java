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

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeDataStorageProjectSettings extends JIPipeDefaultProjectSettingsSheet {
    public static String ID = "org.hkijena.jipipe:data-storage";

    private boolean forceGlobalTempDirectory = false;
    private OptionalPathParameter overrideTempDirectory = new OptionalPathParameter();

    @SetJIPipeDocumentation(name = "Force global temporary directory", description = "If enabled, force using the application-wide temporary directory")
    @JIPipeParameter("force-global-temp-directory")
    public boolean isForceGlobalTempDirectory() {
        return forceGlobalTempDirectory;
    }

    @JIPipeParameter("force-global-temp-directory")
    public void setForceGlobalTempDirectory(boolean forceGlobalTempDirectory) {
        this.forceGlobalTempDirectory = forceGlobalTempDirectory;
    }

    @SetJIPipeDocumentation(name = "Override temporary directory", description = "If enabled, override the temporary directory used for this project")
    @JIPipeParameter("override-temp-directory")
    public OptionalPathParameter getOverrideTempDirectory() {
        return overrideTempDirectory;
    }

    @JIPipeParameter("override-temp-directory")
    public void setOverrideTempDirectory(OptionalPathParameter overrideTempDirectory) {
        this.overrideTempDirectory = overrideTempDirectory;
    }

    @Override
    public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultProjectSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/folder-tree.png");
    }

    @Override
    public String getName() {
        return "Data storage";
    }

    @Override
    public String getDescription() {
        return "Settings related to per-project data storage";
    }
}
