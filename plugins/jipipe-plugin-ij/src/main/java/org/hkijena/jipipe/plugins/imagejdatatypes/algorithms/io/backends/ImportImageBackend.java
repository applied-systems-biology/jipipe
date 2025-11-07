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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.backends;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public abstract class ImportImageBackend extends AbstractJIPipeParameterCollection implements Comparable<ImportImageBackend> {
    private boolean enabled = true;
    private int priority = 0;

    public ImportImageBackend() {

    }

    public ImportImageBackend(ImportImageBackend other) {
        this.enabled = other.enabled;
        this.priority = other.priority;
    }

    @SetJIPipeDocumentation(name = "Enabled", description = "Enable this backend")
    @JIPipeParameter(value = "enabled", uiOrder = -100)
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    @SetJIPipeDocumentation(name = "Priority", description = "The lower the value the earlier the backend is processed. The first matching backend is used for the import process.")
    @JIPipeParameter("priority")
    public int getPriority() {
        return priority;
    }

    @JIPipeParameter("priority")
    public void setPriority(int priority) {
        this.priority = priority;
    }


    public abstract boolean canImport(Path path, JIPipeExpressionVariablesMap variablesMap);

    public abstract ImagePlus doImport(Path path, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo);

    @Override
    public int compareTo(@NotNull ImportImageBackend o) {
        return Integer.compare(priority, o.priority);
    }
}
