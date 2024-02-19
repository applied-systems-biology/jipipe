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
