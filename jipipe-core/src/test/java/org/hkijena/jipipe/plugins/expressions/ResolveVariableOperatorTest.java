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

import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.utils.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that resolving a non-existing variable via $"name" yields a descriptive
 * validation error instead of a bare NullPointerException from the evaluator stack.
 */
class ResolveVariableOperatorTest {

    @BeforeAll
    static void ensureJIPipe() {
        org.hkijena.jipipe.JIPipe.ensureInstance();
    }

    private final JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();

    /**
     * Finds the first report entry whose title or explanation mentions the needle.
     */
    private static JIPipeValidationReportEntry entryMentioning(JIPipeValidationRuntimeException exception, String needle) {
        for (JIPipeValidationReportEntry entry : exception.getReport()) {
            String text = StringUtils.orElse(entry.getTitle(), "") + "\n" + StringUtils.orElse(entry.getExplanation(), "");
            if (text.contains(needle)) {
                return entry;
            }
        }
        return null;
    }

    @Test
    void resolvesExistingVariable() {
        variables.set("Image Z slices", 29);
        Object result = new JIPipeExpressionParameter("$\"Image Z slices\"").evaluate(variables);
        assertEquals(29, result);
    }

    @Test
    void missingVariableThrowsDescriptiveError() {
        JIPipeExpressionParameter expression = new JIPipeExpressionParameter("$\"Image Z slices\"");
        JIPipeValidationRuntimeException exception = assertThrows(JIPipeValidationRuntimeException.class,
                () -> expression.evaluate(variables));
        assertNotNull(entryMentioning(exception, "does not exist"),
                "Error must explain that the variable does not exist. Report was: " + exception.getReport());
        assertNotNull(entryMentioning(exception, "Image Z slices"),
                "Error must name the missing variable. Report was: " + exception.getReport());
    }

    @Test
    void missingVariableInsideFunctionThrowsDescriptiveError() {
        // This is the exact shape of the reported issue 1329: the null from $\"...\"
        // previously crashed inside javaluator's ArrayDeque with an unrelated NPE
        JIPipeExpressionParameter expression = new JIPipeExpressionParameter("MAKE_SEQUENCE(0, TO_NUMBER($\"Image Z slices\") - 1)");
        JIPipeValidationRuntimeException exception = assertThrows(JIPipeValidationRuntimeException.class,
                () -> expression.evaluate(variables));
        assertNotNull(entryMentioning(exception, "does not exist"),
                "Error must explain that the variable does not exist. Report was: " + exception.getReport());
    }

    @Test
    void existsOperatorWorksForExistingAndMissing() {
        variables.set("known", 1);
        // The EXISTS operator receives the variable name as string (not a $ resolution)
        assertEquals(true, new JIPipeExpressionParameter("\"known\" EXISTS").evaluate(variables));
        assertEquals(false, new JIPipeExpressionParameter("\"unknown\" EXISTS").evaluate(variables));
    }

    @Test
    void getVariableFunctionWithoutDefaultThrowsDescriptiveError() {
        JIPipeExpressionParameter expression = new JIPipeExpressionParameter("GET_VARIABLE(\"Image Z slices\")");
        JIPipeValidationRuntimeException exception = assertThrows(JIPipeValidationRuntimeException.class,
                () -> expression.evaluate(variables));
        assertNotNull(entryMentioning(exception, "does not exist"),
                "Error must explain that the variable does not exist. Report was: " + exception.getReport());
    }

    @Test
    void getVariableFunctionWithDefaultReturnsDefault() {
        Object result = new JIPipeExpressionParameter("GET_VARIABLE(\"Image Z slices\", 5)").evaluate(variables);
        assertEquals(5, ((Number) result).intValue());
    }

    // NOTE: The documented 'null' constant is broken independently of this fix (pre-existing):
    // javaluator's value stack cannot hold null, so Constant 'null' falls through to toValue("null")
    // and throws "Unable to find variable 'null'". Fixing this requires a null-sentinel redesign
    // of the evaluator stack and is tracked separately.
}
