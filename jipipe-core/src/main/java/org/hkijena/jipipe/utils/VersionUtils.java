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
 *
 */

package org.hkijena.jipipe.utils;

public class VersionUtils {

    /**
     * Returns a version string for a class
     *
     * @param klass the class
     * @return the version string or 'Development' if none is available
     */
    public static String getVersionString(Class<?> klass) {
        return StringUtils.orElse(klass.getPackage().getImplementationVersion(), "Development");
    }
}
