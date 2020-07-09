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

package org.hkijena.jipipe.extensions.parameters.functions;

import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.patterns.StringPatternExtraction;

/**
 * A function entry that applies pattern extraction to an {@link String}
 */
public class StringPatternExtractionFunction extends FunctionParameter<String, StringPatternExtraction, String> {

    /**
     * Creates a new instance
     */
    public StringPatternExtractionFunction() {
        super(String.class, StringPatternExtraction.class, String.class);
        setInput("");
        setOutput("");
        setParameter(new StringPatternExtraction());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringPatternExtractionFunction(StringPatternExtractionFunction other) {
        super(other);
    }

    /**
     * List of {@link StringPatternExtractionFunction}
     */
    public static class List extends ListParameter<StringPatternExtractionFunction> {
        /**
         * Creates new instance
         */
        public List() {
            super(StringPatternExtractionFunction.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringPatternExtractionFunction.class);
            for (StringPatternExtractionFunction function : other) {
                add(new StringPatternExtractionFunction(function));
            }
        }
    }
}
