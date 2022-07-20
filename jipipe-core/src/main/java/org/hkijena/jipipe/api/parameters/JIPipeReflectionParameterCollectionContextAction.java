package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

public class JIPipeReflectionParameterCollectionContextAction implements JIPipeParameterCollectionContextAction {
    private final Object target;
    private final Method function;
    private final URL iconURL;
    private final JIPipeDocumentation documentation;

    public JIPipeReflectionParameterCollectionContextAction(Object target, Method function, URL iconURL, JIPipeDocumentation documentation) {
        this.target = target;
        this.function = function;
        this.iconURL = iconURL;
        this.documentation = documentation;
    }

    public Object getTarget() {
        return target;
    }

    public Method getFunction() {
        return function;
    }

    @Override
    public JIPipeDocumentation getDocumentation() {
        return documentation;
    }

    @Override
    public URL getIconURL() {
        return iconURL;
    }

    @Override
    public void accept(JIPipeWorkbench workbench) {
        try {
            function.invoke(target, workbench);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
