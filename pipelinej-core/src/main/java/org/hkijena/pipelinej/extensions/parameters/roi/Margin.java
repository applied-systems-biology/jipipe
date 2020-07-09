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

package org.hkijena.pipelinej.extensions.parameters.roi;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.extensions.parameters.collections.ListParameter;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Parameter that allows users to define a rectangle ROI.
 * Users can define a rectangle the classical way (x, y, width, height), but also other ways.
 */
public class Margin implements Function<Rectangle, Rectangle>, ACAQParameterCollection {
    private static final int PARAM_LEFT = 1;
    private static final int PARAM_TOP = 2;
    private static final int PARAM_RIGHT = 4;
    private static final int PARAM_BOTTOM = 8;
    private static final int PARAM_WIDTH = 16;
    private static final int PARAM_HEIGHT = 32;
    private EventBus eventBus = new EventBus();
    private IntModificationParameter left = new IntModificationParameter();
    private IntModificationParameter top = new IntModificationParameter();
    private IntModificationParameter right = new IntModificationParameter();
    private IntModificationParameter bottom = new IntModificationParameter();
    private IntModificationParameter width = new IntModificationParameter();
    private IntModificationParameter height = new IntModificationParameter();
    private Anchor anchor = Anchor.TopLeft;

    /**
     * Creates a new instance
     */
    public Margin() {
        this.left.setUseExactValue(true);
        this.top.setUseExactValue(true);
        this.right.setUseExactValue(true);
        this.bottom.setUseExactValue(true);
        this.width.setUseExactValue(true);
        this.height.setUseExactValue(true);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Margin(Margin other) {
        this.left = new IntModificationParameter(other.left);
        this.top = new IntModificationParameter(other.top);
        this.right = new IntModificationParameter(other.right);
        this.bottom = new IntModificationParameter(other.bottom);
        this.width = new IntModificationParameter(other.width);
        this.height = new IntModificationParameter(other.height);
        this.anchor = other.anchor;
    }

    /**
     * Finds the parameter keys that are relevant according to the current anchor setting
     * The anchor key 'anchor' is not part of the result.
     *
     * @return
     */
    public Set<String> getRelevantParameterKeys() {
        Set<String> result = new HashSet<>();
        if ((getAnchor().getRelevantParameters() & PARAM_LEFT) == PARAM_LEFT)
            result.add("left");
        if ((getAnchor().getRelevantParameters() & PARAM_RIGHT) == PARAM_RIGHT)
            result.add("right");
        if ((getAnchor().getRelevantParameters() & PARAM_TOP) == PARAM_TOP)
            result.add("top");
        if ((getAnchor().getRelevantParameters() & PARAM_BOTTOM) == PARAM_BOTTOM)
            result.add("bottom");
        if ((getAnchor().getRelevantParameters() & PARAM_WIDTH) == PARAM_WIDTH)
            result.add("width");
        if ((getAnchor().getRelevantParameters() & PARAM_HEIGHT) == PARAM_HEIGHT)
            result.add("height");
        return result;
    }

    @ACAQDocumentation(name = "Left")
    @ACAQParameter(value = "left", uiOrder = 0)
    @JsonGetter("left")
    public IntModificationParameter getLeft() {
        return left;
    }

    @ACAQParameter("left")
    @JsonSetter("left")
    public void setLeft(IntModificationParameter left) {
        this.left = left;
    }

    @ACAQDocumentation(name = "Top")
    @ACAQParameter(value = "top", uiOrder = 1)
    @JsonGetter("top")
    public IntModificationParameter getTop() {
        return top;
    }

    @ACAQParameter("top")
    @JsonSetter("top")
    public void setTop(IntModificationParameter top) {
        this.top = top;
    }

    @ACAQDocumentation(name = "Right")
    @ACAQParameter(value = "right", uiOrder = 2)
    @JsonGetter("right")
    public IntModificationParameter getRight() {
        return right;
    }

    @ACAQParameter("right")
    @JsonSetter("right")
    public void setRight(IntModificationParameter right) {
        this.right = right;
    }

    @ACAQDocumentation(name = "Bottom")
    @ACAQParameter(value = "bottom", uiOrder = 3)
    @JsonGetter("bottom")
    public IntModificationParameter getBottom() {
        return bottom;
    }

    @ACAQParameter("bottom")
    @JsonSetter("bottom")
    public void setBottom(IntModificationParameter bottom) {
        this.bottom = bottom;
    }

    @ACAQDocumentation(name = "Width")
    @ACAQParameter(value = "width", uiOrder = 4)
    @JsonGetter("width")
    public IntModificationParameter getWidth() {
        return width;
    }

    @ACAQParameter("width")
    @JsonSetter("width")
    public void setWidth(IntModificationParameter width) {
        this.width = width;
    }

    @ACAQDocumentation(name = "Height")
    @ACAQParameter(value = "height", uiOrder = 5)
    @JsonGetter("height")
    public IntModificationParameter getHeight() {
        return height;
    }

    @ACAQParameter("height")
    @JsonSetter("height")
    public void setHeight(IntModificationParameter height) {
        this.height = height;
    }

    @ACAQDocumentation(name = "Anchor")
    @ACAQParameter("anchor")
    @JsonGetter("anchor")
    public Anchor getAnchor() {
        return anchor;
    }

    @ACAQParameter("anchor")
    @JsonSetter("anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    /**
     * Generates the rectangle defined by the definition.
     * If the rectangle is invalid, null is returned
     *
     * @param rectangle rectangle describing the available area.
     * @return Rectangle within the area
     */
    @Override
    public Rectangle apply(Rectangle rectangle) {
        final int left_ = left.apply(rectangle.width);
        final int top_ = top.apply(rectangle.width);
        final int right_ = right.apply(rectangle.width);
        final int bottom_ = bottom.apply(rectangle.width);
        final int width_ = width.apply(rectangle.width);
        final int height_ = height.apply(rectangle.width);
        final int aw = rectangle.width;
        final int ah = rectangle.height;

        int ox;
        int oy;
        int ow;
        int oh;

        switch (anchor) {
            case TopLeft: {
                ox = left_;
                oy = top_;
                ow = width_;
                oh = height_;
            }
            break;
            case TopRight: {
                ox = aw - right_ - width_;
                oy = top_;
                ow = width_;
                oh = height_;
            }
            break;
            case TopCenter: {
                ox = left_;
                oy = top_;
                ow = aw - left_ - right_;
                oh = height_;
            }
            break;
            case BottomLeft: {
                ox = left_;
                oy = ah - bottom_ - height_;
                ow = width_;
                oh = height_;
            }
            break;
            case BottomRight: {
                ox = aw - right_ - width_;
                oy = ah - bottom_ - height_;
                ow = width_;
                oh = height_;
            }
            break;
            case BottomCenter: {
                ox = left_;
                oy = ah - bottom_ - height_;
                ow = aw - left_ - right_;
                oh = height_;
            }
            break;
            case CenterLeft: {
                ox = left_;
                oy = top_;
                ow = width_;
                oh = ah - top_ - bottom_;
            }
            break;
            case CenterRight: {
                ox = aw - width_ - right_;
                oy = ah - top_ - bottom_;
                ow = width_;
                oh = ah - top_ - bottom_;
            }
            break;
            case CenterCenter: {
                ox = left_;
                oy = top_;
                ow = aw - left_ - right_;
                oh = ah - top_ - bottom_;
            }
            break;
            default:
                throw new UnsupportedOperationException("Unsupported: " + anchor);
        }

        if (ox < 0 || oy < 0 || ow < 0 || oh < 0) {
            return null;
        }

        return new Rectangle(ox + rectangle.x, oy + rectangle.y, ow, oh);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Available anchors
     */
    public enum Anchor {
        TopLeft(PARAM_LEFT | PARAM_TOP | PARAM_WIDTH | PARAM_HEIGHT),
        TopCenter(PARAM_LEFT | PARAM_TOP | PARAM_RIGHT | PARAM_HEIGHT),
        TopRight(PARAM_RIGHT | PARAM_TOP | PARAM_WIDTH | PARAM_HEIGHT),
        BottomLeft(PARAM_LEFT | PARAM_BOTTOM | PARAM_HEIGHT | PARAM_WIDTH),
        BottomCenter(PARAM_LEFT | PARAM_BOTTOM | PARAM_HEIGHT | PARAM_RIGHT),
        BottomRight(PARAM_WIDTH | PARAM_BOTTOM | PARAM_HEIGHT | PARAM_RIGHT),
        CenterLeft(PARAM_TOP | PARAM_BOTTOM | PARAM_WIDTH | PARAM_LEFT),
        CenterRight(PARAM_TOP | PARAM_BOTTOM | PARAM_WIDTH | PARAM_RIGHT),
        CenterCenter(PARAM_LEFT | PARAM_TOP | PARAM_RIGHT | PARAM_BOTTOM);

        private final int relevantParameters;

        Anchor(int relevantParameters) {
            this.relevantParameters = relevantParameters;
        }

        public int getRelevantParameters() {
            return relevantParameters;
        }
    }

    /**
     * List parameter of {@link Margin}
     */
    public static class List extends ListParameter<Margin> {
        /**
         * Creates a new instance
         */
        public List() {
            super(Margin.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(Margin.class);
            for (Margin rectangle : other) {
                add(new Margin(rectangle));
            }
        }
    }
}
