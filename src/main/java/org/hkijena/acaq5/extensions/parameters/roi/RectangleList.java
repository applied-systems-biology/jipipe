package org.hkijena.acaq5.extensions.parameters.roi;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

import java.awt.*;

/**
 * List parameter of {@link Rectangle}
 */
public class RectangleList extends ListParameter<Rectangle> {
    /**
     * Creates a new instance
     */
    public RectangleList() {
        super(Rectangle.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RectangleList(RectangleList other) {
        super(Rectangle.class);
        for (Rectangle rectangle : other) {
            add(new Rectangle(rectangle));
        }
    }
}
