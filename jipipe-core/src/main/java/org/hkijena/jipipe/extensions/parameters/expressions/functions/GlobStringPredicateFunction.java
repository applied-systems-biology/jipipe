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

package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.Function;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.List;

public class GlobStringPredicateFunction extends ExpressionFunction {

    public GlobStringPredicateFunction() {
        super("GLOB", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters) {
        return null;
    }
}
