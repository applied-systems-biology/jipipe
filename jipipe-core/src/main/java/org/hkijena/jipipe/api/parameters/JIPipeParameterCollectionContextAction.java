package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.function.Consumer;

public interface JIPipeParameterCollectionContextAction extends Consumer<JIPipeWorkbench> {

    /**
     * Documentation of this action
     * @return the documentation
     */
    JIPipeDocumentation getDocumentation();

    /**
     * URL to the icon
     * @return the icon URL
     */
    URL getIconURL();
}
