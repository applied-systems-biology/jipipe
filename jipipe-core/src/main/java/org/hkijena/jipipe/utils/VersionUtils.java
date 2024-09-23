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

import org.hkijena.jipipe.plugins.core.CorePlugin;

public class VersionUtils {

    public static final String FALLBACK_VERSION = "4.1.0";

    /**
     * Returns a version string for a class
     *
     * @param klass the class
     * @return the version string or 'Development' if none is available
     */
    public static String getVersionString(Class<?> klass) {
        return StringUtils.orElse(klass.getPackage().getImplementationVersion(), FALLBACK_VERSION);
    }

    /**
     * The current version of JIPipe according to the Maven-proved information
     *
     * @return the version string or '4.1.0' if none is available
     */
    public static String getJIPipeVersion() {
        return StringUtils.orElse(CorePlugin.class.getPackage().getImplementationVersion(), FALLBACK_VERSION);
    }

    /**
     * Similar to {@link StringUtils} compareVersions, but supports the 'Development' version
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return -1 if version1 is less than version2. 1 if version2 is less than version1. 0 if equal
     */
    public static int compareVersions(String version1, String version2) {
        if ("Development".equalsIgnoreCase(version1) || "Development".equalsIgnoreCase(version2)) {
            return 0;
        }
        return StringUtils.compareVersions(version1, version2);
    }
}
