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

package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.net.URL;
import java.util.function.Consumer;

public interface JIPipeParameterCollectionContextAction extends Consumer<JIPipeWorkbench> {

    /**
     * Documentation of this action
     *
     * @return the documentation
     */
    SetJIPipeDocumentation getDocumentation();

    /**
     * URL to the icon
     *
     * @return the icon URL
     */
    URL getIconURL();
}
