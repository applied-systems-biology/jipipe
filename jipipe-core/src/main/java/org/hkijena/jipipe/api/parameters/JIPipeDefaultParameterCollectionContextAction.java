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

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;

import java.net.URL;
import java.util.function.Consumer;

public class JIPipeDefaultParameterCollectionContextAction implements JIPipeParameterCollectionContextAction {

    private final SetJIPipeDocumentation documentation;
    private final URL iconURL;
    private final Consumer<JIPipeWorkbench> function;

    public JIPipeDefaultParameterCollectionContextAction(SetJIPipeDocumentation documentation, URL iconURL, Consumer<JIPipeWorkbench> function) {
        this.documentation = documentation;
        this.iconURL = iconURL;
        this.function = function;
    }

    @Override
    public SetJIPipeDocumentation getDocumentation() {
        return documentation;
    }

    @Override
    public URL getIconURL() {
        return iconURL;
    }

    @Override
    public void accept(JIPipeWorkbench workbench) {
        function.accept(workbench);
    }
}
