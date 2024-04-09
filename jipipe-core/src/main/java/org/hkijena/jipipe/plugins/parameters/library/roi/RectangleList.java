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

package org.hkijena.jipipe.plugins.parameters.library.roi;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;

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
