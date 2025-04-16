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

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.plugins.core.CorePlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class VersionUtils {

    public static final String FALLBACK_VERSION = "5.3.0";
    public static final Comparator<? super String> VERSION_COMPARATOR = new VersionComparator();

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
     * @return the version string or FALLBACK_VERSION if none is available
     */
    public static String getJIPipeVersion() {
        String version = CorePlugin.class.getPackage().getImplementationVersion();
        if (version == null) {
            return FALLBACK_VERSION;
        }
        // Remove "-SNAPSHOT" or other classifiers if they are present
        if (version.contains("-")) {
            return version.substring(0, version.indexOf("-"));
        }
        return version;
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

    public static int[] getVersionComponents(String version) {
        String[] items = StringUtils.splitNonRegex(version, ".");
        int[] result = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            result[i] = Integer.parseInt(items[i]);
        }
        return result;
    }

    public List<int[]> equalizeVersionComponents(int[]... versions) {
        List<int[]> result = new ArrayList<>();
        int numComponents = Arrays.stream(versions).mapToInt(v -> v.length).max().orElse(0);
        for (int[] version : versions) {
            TIntList intList = new TIntArrayList(version);
            while (intList.size() < numComponents) {
                intList.add(0);
            }
            result.add(intList.toArray());
        }
        return result;
    }

    public static class VersionComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            return compareVersions(o1, o2);
        }
    }
}
