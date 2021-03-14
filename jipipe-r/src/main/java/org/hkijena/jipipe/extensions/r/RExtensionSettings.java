/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.r;

import com.github.rcaller.util.Globals;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;

public class RExtensionSettings implements JIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:r";

    private final EventBus eventBus = new EventBus();

    private OptionalPathParameter RExecutable = new OptionalPathParameter();
    private OptionalPathParameter RScriptExecutable = new OptionalPathParameter();

    public RExtensionSettings() {
        if(!StringUtils.isNullOrEmpty(Globals.R_current)) {
            try {
                RExecutable.setContent(Paths.get(Globals.R_current));
            }catch (Exception e) {
            }
        }
        if(!StringUtils.isNullOrEmpty(Globals.Rscript_current)) {
            try {
                RScriptExecutable.setContent(Paths.get(Globals.Rscript_current));
            }catch (Exception e) {
            }
        }
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "R executable", description = "Allows to override the R executable. Must point to R.exe (Windows) or equivalent on other systems.")
    @JIPipeParameter("r-executable")
    @FilePathParameterSettings(pathMode = PathEditor.PathMode.FilesOnly, ioMode = PathEditor.IOMode.Open)
    public OptionalPathParameter getRExecutable() {
        return RExecutable;
    }

    @JIPipeParameter("r-executable")
    public void setRExecutable(OptionalPathParameter RExecutable) {
        this.RExecutable = RExecutable;
    }

    @JIPipeDocumentation(name = "RScript executable", description = "Allows to override the RScript executable. Must point to RScript.exe (Windows) or equivalent on other systems.")
    @JIPipeParameter("rscript-executable")
    @FilePathParameterSettings(pathMode = PathEditor.PathMode.FilesOnly, ioMode = PathEditor.IOMode.Open)
    public OptionalPathParameter getRScriptExecutable() {
        return RScriptExecutable;
    }

    @JIPipeParameter("rscript-executable")
    public void setRScriptExecutable(OptionalPathParameter RScriptExecutable) {
        this.RScriptExecutable = RScriptExecutable;
    }

    public static RExtensionSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, RExtensionSettings.class);
    }
}
