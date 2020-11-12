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

package org.hkijena.jipipe.extensions.parameters.expressions;

import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameter;

import java.util.Collection;

public class NamedAnnotationGeneratorExpression extends PairParameter<AnnotationGeneratorExpression, String> {
    public NamedAnnotationGeneratorExpression() {
        super(AnnotationGeneratorExpression.class, String.class);
        setKey(new AnnotationGeneratorExpression());
        setValue("");
    }

    public NamedAnnotationGeneratorExpression(PairParameter<AnnotationGeneratorExpression, String> other) {
        super(other);
    }

    /**
     * Generates an annotation
     *
     * @param annotations existing annotations for the data
     * @param dataString  the data as string
     * @return the annotation
     */
    public JIPipeAnnotation generateAnnotation(Collection<JIPipeAnnotation> annotations, String dataString) {
        return new JIPipeAnnotation(getValue(), getKey().generateAnnotationValue(annotations, dataString));
    }

    public static class List extends ListParameter<NamedAnnotationGeneratorExpression> {

        public List() {
            super(NamedAnnotationGeneratorExpression.class);
        }

        public List(List other) {
            super(NamedAnnotationGeneratorExpression.class);
            for (NamedAnnotationGeneratorExpression expression : other) {
                add(new NamedAnnotationGeneratorExpression(expression));
            }
        }
    }
}
