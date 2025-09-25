package org.hkijena.jipipe.plugins.parameters.library.functions;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * List of {@link StringPatternExtractionFunction}
 */
public class StringPatternExtractionFunctionList extends JIPipeListParameter<StringPatternExtractionFunction> {

    public StringPatternExtractionFunctionList() {
        super(StringPatternExtractionFunction.class);
    }

    public StringPatternExtractionFunctionList(StringPatternExtractionFunctionList other) {
        super(StringPatternExtractionFunction.class);
        for (StringPatternExtractionFunction function : other) {
            add(new StringPatternExtractionFunction(function));
        }
    }
}
