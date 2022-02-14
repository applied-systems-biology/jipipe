package org.hkijena.jipipe.extensions.parameters.library.colors;

import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

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
