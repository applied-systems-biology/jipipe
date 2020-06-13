package org.hkijena.acaq5.extensions.parameters.functions;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.patterns.StringPatternExtraction;

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
