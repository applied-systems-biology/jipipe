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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.NumericFunctionExpression;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Related to {@link Margin}, but with fixed-size objects
 */
public class FixedMargin extends AbstractJIPipeParameterCollection {
    public static final int PARAM_LEFT = 1;
    public static final int PARAM_TOP = 2;
    public static final int PARAM_RIGHT = 4;
    public static final int PARAM_BOTTOM = 8;
    public static final int PARAM_WIDTH = 16;
    public static final int PARAM_HEIGHT = 32;
    private NumericFunctionExpression left = new NumericFunctionExpression();
    private NumericFunctionExpression top = new NumericFunctionExpression();
    private NumericFunctionExpression right = new NumericFunctionExpression();
    private NumericFunctionExpression bottom = new NumericFunctionExpression();
    private Anchor anchor = Anchor.TopLeft;

    /**
     * Creates a new instance
     */
    public FixedMargin() {
        this.left.ensureExactValue(true);
        this.top.ensureExactValue(true);
        this.right.ensureExactValue(true);
        this.bottom.ensureExactValue(true);
        this.anchor = Anchor.TopLeft;
    }

    /**
     * Creates a margin from a rectangle
     *
     * @param rectangle the rectangle
     */
    public FixedMargin(Rectangle rectangle) {
        this.left.ensureExactValue(true);
        this.top.ensureExactValue(true);
        this.right.ensureExactValue(false);
        this.bottom.ensureExactValue(false);
        this.anchor = Anchor.TopLeft;
        this.left.setExactValue(rectangle.x);
        this.top.setExactValue(rectangle.y);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FixedMargin(FixedMargin other) {
        this.left = new NumericFunctionExpression(other.left);
        this.top = new NumericFunctionExpression(other.top);
        this.right = new NumericFunctionExpression(other.right);
        this.bottom = new NumericFunctionExpression(other.bottom);
        this.anchor = other.anchor;
    }

    public FixedMargin(Anchor anchor) {
        this.anchor = anchor;
    }

    @Override
    public String toString() {
        return "FixedSizePlacement{" +
                "left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", anchor=" + anchor +
                '}';
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
     * @param object        the object to be placed
     * @param availableArea rectangle describing the available area.
     * @param parameters    additional expression variables
     * @return Rectangle within the area
     */
    public Rectangle place(Rectangle object, Rectangle availableArea, JIPipeExpressionVariablesMap parameters) {
        final int left_ = (int) left.apply(availableArea.width, parameters);
        final int top_ = (int) top.apply(availableArea.height, parameters);
        final int right_ = (int) right.apply(availableArea.width, parameters);
        final int bottom_ = (int) bottom.apply(availableArea.height, parameters);
        final int width_ = object.width;
        final int height_ = object.height;
        final int aw = availableArea.width;
        final int ah = availableArea.height;

        int ox;
        int oy;

        switch (anchor) {
            case TopLeft: {
                ox = left_;
                oy = top_;
            }
            break;
            case TopRight: {
                ox = aw - right_ - width_;
                oy = top_;
            }
            break;
            case TopCenter: {
                ox = aw / 2 - width_ / 2;
                oy = top_;
            }
            break;
            case BottomLeft: {
                ox = left_;
                oy = ah - bottom_ - height_;
            }
            break;
            case BottomRight: {
                ox = aw - right_ - width_;
                oy = ah - bottom_ - height_;
            }
            break;
            case BottomCenter: {
                ox = aw / 2 - width_ / 2;
                oy = ah - bottom_ - height_;
            }
            break;
            case CenterLeft: {
                ox = left_;
                oy = ah / 2 - height_ / 2;
            }
            break;
            case CenterRight: {
                ox = aw - width_ - right_;
                oy = ah / 2 - height_ / 2;
            }
            break;
            case CenterCenter: {
                ox = aw / 2 - width_ / 2;
                oy = ah / 2 - height_ / 2;
            }
            break;
            default:
                throw new UnsupportedOperationException("Unsupported: " + anchor);
        }

        return new Rectangle(ox + availableArea.x, oy + availableArea.y, width_, height_);
    }

    /**
     * Finds the parameter keys that are relevant according to the current anchor setting
     * The anchor key 'anchor' is not part of the result.
     *
     * @return the keys
     */
    public Set<String> getRelevantParameterKeys() {
        Set<String> result = new HashSet<>();
        switch (anchor) {
            case TopLeft: {
                result.add("left");
                result.add("top");
            }
            break;
            case TopRight: {
                result.add("top");
                result.add("right");
            }
            break;
            case TopCenter: {
                result.add("top");
            }
            break;
            case BottomLeft: {
                result.add("left");
                result.add("bottom");
            }
            break;
            case BottomRight: {
                result.add("right");
                result.add("bottom");
            }
            break;
            case BottomCenter: {
                result.add("bottom");
            }
            break;
            case CenterLeft: {
                result.add("left");
            }
            break;
            case CenterRight: {
                result.add("right");
            }
            break;
            case CenterCenter: {
            }
            break;
        }
        return result;
    }

    /**
     * List parameter of {@link FixedMargin}
     */
    public static class List extends ListParameter<FixedMargin> {
        /**
         * Creates a new instance
         */
        public List() {
            super(FixedMargin.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(FixedMargin.class);
            for (FixedMargin rectangle : other) {
                add(new FixedMargin(rectangle));
            }
        }
    }
}
