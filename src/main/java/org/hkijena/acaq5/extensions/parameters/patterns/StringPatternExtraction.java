package org.hkijena.acaq5.extensions.parameters.patterns;

import java.util.function.Function;

/**
 * A parameter that extracts a pattern from a string
 */
public class StringPatternExtraction implements Function<String, String> {



    @Override
    public String apply(String s) {
        return null;
    }

    /**
     * Available modes
     */
    public enum Mode {
        SplitAndPick,
        SplitAndFind,
        Regex
    }
}
