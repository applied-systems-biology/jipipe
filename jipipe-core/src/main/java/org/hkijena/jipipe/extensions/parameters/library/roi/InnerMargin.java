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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.NumericFunctionExpression;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

/**
 * Related to {@link Margin}, but with fixed-size objects
 */
public class InnerMargin extends AbstractJIPipeParameterCollection {
    private NumericFunctionExpression left = new NumericFunctionExpression();
    private NumericFunctionExpression top = new NumericFunctionExpression();
    private NumericFunctionExpression right = new NumericFunctionExpression();
    private NumericFunctionExpression bottom = new NumericFunctionExpression();

    /**
     * Creates a new instance
     */
    public InnerMargin() {
        this.left.ensureExactValue(true);
        this.top.ensureExactValue(true);
        this.right.ensureExactValue(true);
        this.bottom.ensureExactValue(true);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public InnerMargin(InnerMargin other) {
        this.left = new NumericFunctionExpression(other.left);
        this.top = new NumericFunctionExpression(other.top);
        this.right = new NumericFunctionExpression(other.right);
        this.bottom = new NumericFunctionExpression(other.bottom);
    }

    @Override
    public String toString() {
        return "InnerMargin{" +
                "left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                '}';
    }


    @SetJIPipeDocumentation(name = "Left")
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

    @SetJIPipeDocumentation(name = "Top")
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

    @SetJIPipeDocumentation(name = "Right")
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

    @SetJIPipeDocumentation(name = "Bottom")
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

    /**
     * List parameter of {@link InnerMargin}
     */
    public static class List extends ListParameter<InnerMargin> {
        /**
         * Creates a new instance
         */
        public List() {
            super(InnerMargin.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(InnerMargin.class);
            for (InnerMargin rectangle : other) {
                add(new InnerMargin(rectangle));
            }
        }
    }
}
