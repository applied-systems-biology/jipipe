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

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

public class JIPipeReflectionParameterCollectionContextAction implements JIPipeParameterCollectionContextAction {
    private final Object target;
    private final Method function;
    private final String name;
    private final String description;
    private final Icon icon;

    public JIPipeReflectionParameterCollectionContextAction(Object target, Method function, String name, String description, Icon icon) {
        this.target = target;
        this.function = function;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public Object getTarget() {
        return target;
    }

    public Method getFunction() {
        return function;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public void accept(JIPipeWorkbench workbench) {
        try {
            if (function.getParameters().length == 0) {
                function.invoke(target);
            } else {
                function.invoke(target, workbench);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
