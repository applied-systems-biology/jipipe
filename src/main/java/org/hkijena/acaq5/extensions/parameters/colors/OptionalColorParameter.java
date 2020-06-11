package org.hkijena.acaq5.extensions.parameters.colors;

import org.hkijena.acaq5.extensions.parameters.optional.OptionalParameter;

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
