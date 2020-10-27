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

package org.hkijena.jipipe.extensions.parameters.formula.predicates;

import org.hkijena.jipipe.extensions.parameters.formula.FormulaPredicate;
import org.hkijena.jipipe.extensions.parameters.formula.FormulaPredicateInfo;

@FormulaPredicateInfo(arguments = { Boolean.class, Boolean.class }, functionName = "OR", symbol = "∨")
public class OrPredicate implements FormulaPredicate {
    @Override
    public boolean test(Object[] objects) {
        return (boolean)objects[0] || (boolean)objects[1];
    }
}
