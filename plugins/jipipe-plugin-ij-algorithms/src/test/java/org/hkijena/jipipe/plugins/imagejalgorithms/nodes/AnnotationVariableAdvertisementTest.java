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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes;

import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.dimensions.HyperstackSlicerAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.enhance.BleachCorrectionAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.generate.GenerateMissingImageFromMathExpression2D;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels.filter.FilterLabelsByStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math.ApplyMathExpression2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.filter.FilterRoi2dByOverlapOldAlgorithm;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that parameters whose runtime evaluation includes input annotations
 * (issue 1329 fixes) advertise those variables via {@link AddJIPipeExpressionParameterVariable},
 * which is what the expression builder UI reads through
 * {@code JIPipeReflectionParameterAccess.getAnnotationsOfType(...)} (getter annotations).
 */
class AnnotationVariableAdvertisementTest {

    /**
     * Returns true if the given getter advertises annotation variables.
     */
    private static boolean advertisesAnnotationVariables(Class<?> algorithmClass, String getterName) throws NoSuchMethodException {
        Method getter = algorithmClass.getMethod(getterName);
        for (AddJIPipeExpressionParameterVariable variable : getter.getAnnotationsByType(AddJIPipeExpressionParameterVariable.class)) {
            if (variable.fromClass() == JIPipeTextAnnotationsExpressionParameterVariablesInfo.class) {
                return true;
            }
        }
        return false;
    }

    private static void assertAdvertises(Class<?> algorithmClass, String getterName) throws NoSuchMethodException {
        assertTrue(advertisesAnnotationVariables(algorithmClass, getterName),
                algorithmClass.getSimpleName() + "." + getterName + " must advertise annotation variables");
    }

    /**
     * The reported node (issue 1329) and the audited nodes must advertise annotation variables
     * on the parameters that receive them at runtime.
     */
    @Test
    void advertisedAnnotationVariables() throws Exception {
        assertAdvertises(HyperstackSlicerAlgorithm.class, "getIndicesZ");
        assertAdvertises(HyperstackSlicerAlgorithm.class, "getIndicesC");
        assertAdvertises(HyperstackSlicerAlgorithm.class, "getIndicesT");

        assertAdvertises(ApplyMathExpression2DAlgorithm.class, "getTransformation");
        assertAdvertises(GenerateMissingImageFromMathExpression2D.class, "getFunction");
        assertAdvertises(BleachCorrectionAlgorithm.class, "getChannelFilter");
        assertAdvertises(FilterLabelsByStatisticsAlgorithm.class, "getFilters");
        assertAdvertises(FilterRoi2dByOverlapOldAlgorithm.ROIFilterSettings.class, "getOverlapFilter");
    }
}
