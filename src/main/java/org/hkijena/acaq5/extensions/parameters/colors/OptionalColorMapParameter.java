package org.hkijena.acaq5.extensions.parameters.colors;

import org.hkijena.acaq5.extensions.parameters.OptionalParameter;

/**
 * Optional {@link ColorMap}
 */
public class OptionalColorMapParameter extends OptionalParameter<ColorMap> {

    /**
     * Creates a new instance
     */
    public OptionalColorMapParameter() {
        super(ColorMap.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalColorMapParameter(OptionalColorMapParameter other) {
        super(ColorMap.class);
        this.setContent(other.getContent());
    }

    @Override
    public ColorMap setNewInstance() {
        setContent(ColorMap.viridis);
        return ColorMap.viridis;
    }
}
