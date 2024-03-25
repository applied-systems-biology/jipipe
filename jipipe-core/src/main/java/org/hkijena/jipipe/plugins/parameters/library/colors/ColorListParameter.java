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

package org.hkijena.jipipe.plugins.parameters.library.colors;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;

import java.awt.*;
import java.util.Collection;

public class ColorListParameter extends ListParameter<Color> {
    public ColorListParameter() {
        super(Color.class);
    }

    public ColorListParameter(Collection<Color> other) {
        super(Color.class);
        addAll(other);
    }

    @Override
    public Color addNewInstance() {
        add(Color.RED);
        return Color.RED;
    }
}
