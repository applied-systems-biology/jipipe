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

import javax.swing.*;
import java.util.function.Consumer;

public class JIPipeDefaultParameterCollectionContextAction implements JIPipeParameterCollectionContextAction {

    private final Consumer<JIPipeWorkbench> function;
    private final String name;
    private final String description;
    private final Icon icon;
    private boolean highlighted;

    public JIPipeDefaultParameterCollectionContextAction(Consumer<JIPipeWorkbench> function, String name, String description, boolean highlighted, Icon icon) {
        this.function = function;
        this.name = name;
        this.description = description;
        this.highlighted = highlighted;
        this.icon = icon;
    }

    @Override
    public void accept(JIPipeWorkbench workbench) {
        function.accept(workbench);
    }

    public Consumer<JIPipeWorkbench> getFunction() {
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
    public boolean isHighlighted() {
        return highlighted;
    }
}
