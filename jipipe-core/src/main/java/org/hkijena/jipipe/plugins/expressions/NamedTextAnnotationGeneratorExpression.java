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

package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameter;

import java.util.Collection;

public class NamedTextAnnotationGeneratorExpression extends PairParameter<AnnotationGeneratorExpression, String> {
    public NamedTextAnnotationGeneratorExpression() {
        super(AnnotationGeneratorExpression.class, String.class);
        setKey(new AnnotationGeneratorExpression());
        setValue("");
    }

    public NamedTextAnnotationGeneratorExpression(AnnotationGeneratorExpression expression, String name) {
        super(AnnotationGeneratorExpression.class, String.class);
        setKey(expression);
        setValue(name);
    }

    public NamedTextAnnotationGeneratorExpression(PairParameter<AnnotationGeneratorExpression, String> other) {
        super(other);
    }

    /**
     * Generates an annotation
     *
     * @param annotations existing annotations for the data
     * @param variableSet existing variables
     * @return the annotation
     */
    public JIPipeTextAnnotation generateTextAnnotation(Collection<JIPipeTextAnnotation> annotations, JIPipeExpressionVariablesMap variableSet) {
        return new JIPipeTextAnnotation(getValue(), getKey().generateAnnotationValue(annotations, variableSet));
    }

    public static class List extends ListParameter<NamedTextAnnotationGeneratorExpression> {

        public List() {
            super(NamedTextAnnotationGeneratorExpression.class);
        }

        public List(List other) {
            super(NamedTextAnnotationGeneratorExpression.class);
            for (NamedTextAnnotationGeneratorExpression expression : other) {
                add(new NamedTextAnnotationGeneratorExpression(expression));
            }
        }
    }
}
