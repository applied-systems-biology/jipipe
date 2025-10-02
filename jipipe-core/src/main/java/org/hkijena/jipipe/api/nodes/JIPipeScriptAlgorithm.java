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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

/**
 * Interface that exposes a central {@link org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter} that
 * can be targeted by the code editor dock.
 * {@link org.hkijena.jipipe.plugins.parameters.ui.api.JIPipeDesktopScriptParameterEditorUI} will automatically move code editing
 * into a specific dock.
 */
public interface JIPipeScriptAlgorithm {
    /**
     * The parameter access that points towards a parameter of the type {@link org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter} (or implementations).
     * Will be called by {@link org.hkijena.jipipe.plugins.parameters.ui.api.JIPipeDesktopScriptParameterEditorUI} to defer the script editor to the dedicated script editor dock panel if available.
     * @return the script parameter access
     */
    JIPipeParameterAccess getScriptParameterAccess();
}
