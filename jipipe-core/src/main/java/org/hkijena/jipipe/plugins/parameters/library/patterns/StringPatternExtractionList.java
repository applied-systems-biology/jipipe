package org.hkijena.jipipe.plugins.parameters.library.patterns;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link StringPatternExtraction}
 */
public class StringPatternExtractionList extends JIPipeListParameter<StringPatternExtraction> {
    /**
     * Creates a new instance
     */
    public StringPatternExtractionList() {
        super(StringPatternExtraction.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringPatternExtractionList(StringPatternExtractionList other) {
        super(StringPatternExtraction.class);
        for (StringPatternExtraction pathPredicate : other) {
            add(new StringPatternExtraction(pathPredicate));
        }
    }
}
