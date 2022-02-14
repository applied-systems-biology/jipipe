/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.library.colors;

import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;

import java.awt.*;

/**
 * Optional {@link Color}
 */
public class OptionalColorParameter extends OptionalParameter<Color> {

    /**
     * Creates a new instance
     */
    public OptionalColorParameter() {
        super(Color.class);
    }

    public OptionalColorParameter(Color color, boolean enabled) {
        this();
        setEnabled(enabled);
        setContent(color);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalColorParameter(OptionalColorParameter other) {
        super(other);
        this.setContent(new Color(other.getContent().getRed(),
                other.getContent().getGreen(),
                other.getContent().getBlue(),
                other.getContent().getAlpha()));
    }

    @Override
    public Color setNewInstance() {
        Color color = Color.WHITE;
        setContent(color);
        return color;
    }
}
