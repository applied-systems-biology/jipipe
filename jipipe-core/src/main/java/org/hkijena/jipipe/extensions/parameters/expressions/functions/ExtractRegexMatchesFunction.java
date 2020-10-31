package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JIPipeDocumentation(name = "Match RegEx groups", description = "Function that returns all RegEx groups. The first group (index 0) is always the whole string if it matches. Returns an empty array if no match is found.")
public class ExtractRegexMatchesFunction extends ExpressionFunction {

    public ExtractRegexMatchesFunction() {
        super("REGEX_EXTRACT_MATCHES", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        String haystack = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        Matcher matcher = Pattern.compile(pattern).matcher(haystack);
        if (matcher.find()) {
            ArrayList<Object> result = new ArrayList<>();
            for (int i = 0; i < matcher.groupCount(); i++) {
                result.add(matcher.group(i));
            }
            return result;
        }
        else {
            return new ArrayList<>();
        }
    }
}
