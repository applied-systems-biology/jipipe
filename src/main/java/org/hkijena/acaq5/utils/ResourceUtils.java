/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.utils;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Functions for resource access
 */
public class ResourceUtils {

    public static final String RESOURCE_BASE_PATH = "/org/hkijena/acaq5";

    public static String getResourceBasePath() {
        return "/org/hkijena/acaq5";
    }

    /**
     * Gets an internal resource.
     * This is relative to getResourceBasePath()
     *
     * @param internalResourcePath the internal path
     * @return the absolute resource path
     */
    public static String getResourcePath(String internalResourcePath) {
        if (internalResourcePath.startsWith("/"))
            internalResourcePath = internalResourcePath.substring(1);
        return getResourceBasePath() + "/" + internalResourcePath;
    }

    /**
     * Gets a plugin-internal resource as URL
     *
     * @param internalResourcePath internal path
     * @return resource URL or null if the resource does not exist
     */
    public static URL getPluginResource(String internalResourcePath) {
        return ResourceUtils.class.getResource(getResourcePath(internalResourcePath));
    }

    /**
     * Gets a plugin-internal resource as Stream
     *
     * @param internalResourcePath internal path
     * @return resource stream or null if the resource does not exist
     */
    public static InputStream getPluginResourceAsStream(String internalResourcePath) {
        return ResourceUtils.class.getResourceAsStream(getResourcePath(internalResourcePath));
    }

    /**
     * Iterates through all internal resources and
     *
     * @param folder the internal resource folder path
     * @return all subresources
     */
    public static Set<String> walkInternalResourceFolder(String folder) {
        String globalFolder = getResourceBasePath() + "/" + folder;
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.hkijena.acaq5"))
                .setScanners(new ResourcesScanner()));

        Set<String> allResources = reflections.getResources(Pattern.compile(".*"));
        allResources = allResources.stream().map(s -> {
            if (!s.startsWith("/"))
                return "/" + s;
            else
                return s;
        }).collect(Collectors.toSet());
        return allResources.stream().filter(s -> s.startsWith(globalFolder)).collect(Collectors.toSet());
    }

}
