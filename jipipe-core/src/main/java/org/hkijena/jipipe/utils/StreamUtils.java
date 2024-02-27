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

package org.hkijena.jipipe.utils;

import java.util.function.Predicate;

/**
 * Utils for Java's stream API
 */
public class StreamUtils {
    /**
     * Returns false if the nullable is null. Otherwise, evaluates the predicate.
     *
     * @param nullable  the object
     * @param ifNotNull evaluated if the nullable is not null
     * @param <T>       object type
     * @return false if nullable is null. Otherwise, the result of the predicate.
     */
    public static <T> boolean nullToFalseOrPredicate(T nullable, Predicate<T> ifNotNull) {
        if (nullable == null) {
            return false;
        }
        return ifNotNull.test(nullable);
    }

    /**
     * Returns ifNull if the nullable is null. Otherwise, evaluates the predicate.
     *
     * @param nullable  the object
     * @param ifNull    returned if the nullable is null
     * @param ifNotNull evaluated if the nullable is not null
     * @param <T>       object type
     * @return ifNull if nullable is null. Otherwise, the result of the predicate.
     */
    public static <T> boolean ifNullOr(T nullable, boolean ifNull, Predicate<T> ifNotNull) {
        if (nullable == null) {
            return ifNull;
        }
        return ifNotNull.test(nullable);
    }
}
