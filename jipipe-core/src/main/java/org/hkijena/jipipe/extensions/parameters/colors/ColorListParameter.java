package org.hkijena.jipipe.extensions.parameters.colors;

import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;

import java.awt.Color;
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