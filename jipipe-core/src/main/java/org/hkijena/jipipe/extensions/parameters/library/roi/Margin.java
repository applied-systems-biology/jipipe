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

package org.hkijena.jipipe.extensions.parameters.library.roi;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.NumericFunctionExpression;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter that allows users to define a rectangle ROI.
 * Users can define a rectangle the classical way (x, y, width, height), but also other ways.
 */
public class Margin implements JIPipeParameterCollection {
    public static final int PARAM_LEFT = 1;
    public static final int PARAM_TOP = 2;
    public static final int PARAM_RIGHT = 4;
    public static final int PARAM_BOTTOM = 8;
    public static final int PARAM_WIDTH = 16;
    public static final int PARAM_HEIGHT = 32;
    private EventBus eventBus = new EventBus();
    private NumericFunctionExpression left = new NumericFunctionExpression();
    private NumericFunctionExpression top = new NumericFunctionExpression();
    private NumericFunctionExpression right = new NumericFunctionExpression();
    private NumericFunctionExpression bottom = new NumericFunctionExpression();
    private NumericFunctionExpression width = new NumericFunctionExpression();
    private NumericFunctionExpression height = new NumericFunctionExpression();
    private Anchor anchor = Anchor.TopLeft;

    /**
     * Creates a new instance
     */
    public Margin() {
        this.left.ensureExactValue(true);
        this.top.ensureExactValue(true);
        this.right.ensureExactValue(true);
        this.bottom.ensureExactValue(true);
        this.anchor = Anchor.CenterCenter;
        this.width.ensureExactValue(false);
        this.height.ensureExactValue(false);
    }

    /**
     * Creates a margin from a rectangle
     *
     * @param rectangle the rectangle
     */
    public Margin(Rectangle rectangle) {
        this.left.ensureExactValue(true);
        this.top.ensureExactValue(true);
        this.right.ensureExactValue(false);
        this.bottom.ensureExactValue(false);
        this.anchor = Anchor.TopLeft;
        this.width.ensureExactValue(true);
        this.height.ensureExactValue(true);
        this.left.setExactValue(rectangle.x);
        this.top.setExactValue(rectangle.y);
        this.width.setExactValue(rectangle.width);
        this.height.setExactValue(rectangle.height);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Margin(Margin other) {
        this.left = new NumericFunctionExpression(other.left);
        this.top = new NumericFunctionExpression(other.top);
        this.right = new NumericFunctionExpression(other.right);
        this.bottom = new NumericFunctionExpression(other.bottom);
        this.width = new NumericFunctionExpression(other.width);
        this.height = new NumericFunctionExpression(other.height);
        this.anchor = other.anchor;
    }

    public Margin(Anchor anchor) {
        this.anchor = anchor;
    }

    @Override
    public String toString() {
        return "Margin{" +
                "left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", width=" + width +
                ", height=" + height +
                ", anchor=" + anchor +
                '}';
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

    @JIPipeDocumentation(name = "Left")
    @JIPipeParameter(value = "left", uiOrder = 0)
    @JsonGetter("left")
    public NumericFunctionExpression getLeft() {
        return left;
    }

    @JIPipeParameter("left")
    @JsonSetter("left")
    public void setLeft(NumericFunctionExpression left) {
        this.left = left;
    }

    @JIPipeDocumentation(name = "Top")
    @JIPipeParameter(value = "top", uiOrder = 1)
    @JsonGetter("top")
    public NumericFunctionExpression getTop() {
        return top;
    }

    @JIPipeParameter("top")
    @JsonSetter("top")
    public void setTop(NumericFunctionExpression top) {
        this.top = top;
    }

    @JIPipeDocumentation(name = "Right")
    @JIPipeParameter(value = "right", uiOrder = 2)
    @JsonGetter("right")
    public NumericFunctionExpression getRight() {
        return right;
    }

    @JIPipeParameter("right")
    @JsonSetter("right")
    public void setRight(NumericFunctionExpression right) {
        this.right = right;
    }

    @JIPipeDocumentation(name = "Bottom")
    @JIPipeParameter(value = "bottom", uiOrder = 3)
    @JsonGetter("bottom")
    public NumericFunctionExpression getBottom() {
        return bottom;
    }

    @JIPipeParameter("bottom")
    @JsonSetter("bottom")
    public void setBottom(NumericFunctionExpression bottom) {
        this.bottom = bottom;
    }

    @JIPipeDocumentation(name = "Width")
    @JIPipeParameter(value = "width", uiOrder = 4)
    @JsonGetter("width")
    public NumericFunctionExpression getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    @JsonSetter("width")
    public void setWidth(NumericFunctionExpression width) {
        this.width = width;
    }

    @JIPipeDocumentation(name = "Height")
    @JIPipeParameter(value = "height", uiOrder = 5)
    @JsonGetter("height")
    public NumericFunctionExpression getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    @JsonSetter("height")
    public void setHeight(NumericFunctionExpression height) {
        this.height = height;
    }

    @JIPipeDocumentation(name = "Anchor")
    @JIPipeParameter("anchor")
    @JsonGetter("anchor")
    public Anchor getAnchor() {
        return anchor;
    }

    @JIPipeParameter("anchor")
    @JsonSetter("anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    /**
     * Generates the rectangle defined by the definition.
     * If the rectangle is invalid, null is returned
     *
     * @param availableArea rectangle describing the available area.
     * @return Rectangle within the area
     */
    public Rectangle getInsideArea(Rectangle availableArea) {
        final int left_ = (int) left.apply(availableArea.width);
        final int top_ = (int) top.apply(availableArea.height);
        final int right_ = (int) right.apply(availableArea.width);
        final int bottom_ = (int) bottom.apply(availableArea.height);
        final int width_ = (int) width.apply(availableArea.width);
        final int height_ = (int) height.apply(availableArea.height);
        final int aw = availableArea.width;
        final int ah = availableArea.height;

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

        return new Rectangle(ox + availableArea.x, oy + availableArea.y, ow, oh);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
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
