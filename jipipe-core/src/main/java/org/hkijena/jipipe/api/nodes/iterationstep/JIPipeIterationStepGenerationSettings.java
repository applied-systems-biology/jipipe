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

package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

public interface JIPipeIterationStepGenerationSettings extends JIPipeParameterCollection {
    /**
     * Creates an object that contains information for the {@link org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI} on the state of the current generation settings.
     *
     * @return the visualization
     */
    JIPipeIterationStepGenerationSettingsVisualization createVisualization();
}
