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

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Simplifies the access to resources for extensions
 */
public class JIPipeResourceManager {
    private final Class<?> resourceClass;
    private final String basePath;
    private final String icons16BasePath;

    private final String darkIcons16BasePath;
    private final String icons32BasePath;
    private final String darkIcons32BasePath;
    private final String icons64BasePath;
    private final String darkIcons64BasePath;
    private final String icons128BasePath;
    private final String darkIcons128BasePath;
    private final String templateBasePath;
    private final String schemaBasePath;

    private final Map<String, ImageIcon> icon16FromResourceCache = new HashMap<>();
    private final Map<String, ImageIcon> icon32FromResourceCache = new HashMap<>();
    private final Map<String, ImageIcon> icon64FromResourceCache = new HashMap<>();
    private final Map<String, ImageIcon> icon128FromResourceCache = new HashMap<>();

    /**
     * Creates a new instance
     *
     * @param resourceClass        the class that acts as the base for accessing the resources. Should be in the same package as the extension
     * @param basePath             absolute resource path to the resource root e.g. /org/hkijena/jipipe/extensions/myextension (must be consistent with the resource directory)
     * @param icons16BasePath      resource path to the 16x16 icons
     * @param darkIcons16BasePath  resource path to the dark 16x16 icons (icons themselves are optional; the non-dark versions are automatically utilized if no dark version is available)
     * @param icons32BasePath      resource path to the 32x32 icons
     * @param darkIcons32BasePath  resource path to the dark 32x32 icons (icons themselves are optional; the non-dark versions are automatically utilized if no dark version is available)
     * @param icons64BasePath  resource path to the 64x64 icons
     * @param darkIcons64BasePath resource path to the dark 64x64 icons (icons themselves are optional; the non-dark versions are automatically utilized if no dark version is available)
     * @param icons128BasePath resource path to the 128x128 icons
     * @param darkIcons128BasePath resource path to the dark 128x128 icons (icons themselves are optional; the non-dark versions are automatically utilized if no dark version is available)
     * @param templateBasePath     resource path to the template directory
     * @param schemaBasePath       resource path to the schema directory
     */
    public JIPipeResourceManager(Class<?> resourceClass, String basePath, String icons16BasePath, String darkIcons16BasePath,
                                 String icons32BasePath, String darkIcons32BasePath, String icons64BasePath,
                                 String darkIcons64BasePath, String icons128BasePath, String darkIcons128BasePath,
                                 String templateBasePath, String schemaBasePath) {
        this.resourceClass = resourceClass;
        this.basePath = formatBasePath(basePath);
        this.icons16BasePath = formatBasePath(icons16BasePath);
        this.darkIcons16BasePath = formatBasePath(darkIcons16BasePath);
        this.icons32BasePath = formatBasePath(icons32BasePath);
        this.darkIcons32BasePath = formatBasePath(darkIcons32BasePath);
        this.icons64BasePath = formatBasePath(icons64BasePath);
        this.darkIcons64BasePath = formatBasePath(darkIcons64BasePath);
        this.icons128BasePath = formatBasePath(icons128BasePath);
        this.darkIcons128BasePath = formatBasePath(darkIcons128BasePath);
        this.templateBasePath = formatBasePath(templateBasePath);
        this.schemaBasePath = formatBasePath(schemaBasePath);
    }

    /**
     * Creates a new resource manager with the following settings:
     * <ul>
     *     <li>icons 16x16 path: [basePath]/icons</li>
     *     <li>icons 32x32 path: [basePath]/icons-32</li>
     *     <li>dark icons 16x16 path: [basePath]/dark/icons</li>
     *     <li>dark icons 32x32 path: [basePath]/dark/icons-32</li>
     *     <li>templates path: [basePath]/templates</li>
     *     <li>schemas path: [basePath]/schemas</li>
     * </ul>
     *
     * @param resourceClass the class that acts as the base for accessing the resources. Should be in the same package as the extension
     * @param basePath      absolute resource path to the resource root e.g. /org/hkijena/jipipe/extensions/myextension (must be consistent with the resource directory)
     */
    public JIPipeResourceManager(Class<?> resourceClass, String basePath) {
        this.resourceClass = resourceClass;
        this.basePath = formatBasePath(basePath);
        this.icons16BasePath = formatBasePath(basePath + "/icons");
        this.darkIcons16BasePath = formatBasePath(basePath + "/dark/icons");
        this.icons32BasePath = formatBasePath(basePath + "/icons-32");
        this.darkIcons32BasePath = formatBasePath(basePath + "/dark/icons-32");
        this.icons64BasePath = formatBasePath(basePath + "/icons-64");
        this.darkIcons64BasePath = formatBasePath(basePath + "/dark/icons-64");
        this.icons128BasePath = formatBasePath(basePath + "/icons-128");
        this.darkIcons128BasePath = formatBasePath(basePath + "/dark/icons-128");
        this.templateBasePath = formatBasePath(basePath + "/templates");
        this.schemaBasePath = formatBasePath(basePath + "/schemas");
    }

    /**
     * Applies the appropriate formatting for a base path
     *
     * @param path the base path
     * @return the fixed base path
     */
    public static String formatBasePath(String path) {
        String result = path.replace('\\', '/');
        while (result.contains("//"))
            result = result.replace("//", "/");
        if (!result.startsWith("/"))
            result = "/" + result;
        while (result.endsWith("/"))
            result = result.substring(0, result.length() - 1);
        return result;
    }

    public Class<?> getResourceClass() {
        return resourceClass;
    }

    /**
     * Returns the URL of a 16x16 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exists
     */
    public URL getIconURLFromResources(String iconName) {
        if (UIUtils.DARK_THEME) {
            URL resource = resourceClass.getResource(darkIcons16BasePath + "/" + iconName);
            if (resource != null)
                return resource;
        }
        return resourceClass.getResource(icons16BasePath + "/" + iconName);
    }

    /**
     * Returns the URL of a 16x16 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exists
     */
    public URL getIcon16URLFromResources(String iconName) {
        return getIconURLFromResources(iconName);
    }

    /**
     * Returns the URL of a 32x32 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exists
     */
    public URL getIcon32URLFromResources(String iconName) {
        if (UIUtils.DARK_THEME) {
            URL resource = resourceClass.getResource(darkIcons32BasePath + "/" + iconName);
            if (resource != null)
                return resource;
        }
        return resourceClass.getResource(icons32BasePath + "/" + iconName);
    }

    /**
     * Returns the URL of a 64x64 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exists
     */
    public URL getIcon64URLFromResources(String iconName) {
        if (UIUtils.DARK_THEME) {
            URL resource = resourceClass.getResource(darkIcons64BasePath + "/" + iconName);
            if (resource != null)
                return resource;
        }
        return resourceClass.getResource(icons64BasePath + "/" + iconName);
    }

    /**
     * Returns the URL of a 128x128 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exists
     */
    public URL getIcon128URLFromResources(String iconName) {
        if (UIUtils.DARK_THEME) {
            URL resource = resourceClass.getResource(darkIcons128BasePath + "/" + iconName);
            if (resource != null)
                return resource;
        }
        return resourceClass.getResource(icons128BasePath + "/" + iconName);
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIconFromResources(String iconName) {
        ImageIcon icon = icon16FromResourceCache.getOrDefault(iconName, null);
        if (icon == null) {
            URL url = getIconURLFromResources(iconName);
            icon = new ImageIcon(url);
            icon16FromResourceCache.put(iconName, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon32FromResources(String iconName) {
        ImageIcon icon = icon32FromResourceCache.getOrDefault(iconName, null);
        if (icon == null) {
            URL url = getIcon32URLFromResources(iconName);
            icon = new ImageIcon(url);
            icon32FromResourceCache.put(iconName, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon64FromResources(String iconName) {
        ImageIcon icon = icon64FromResourceCache.getOrDefault(iconName, null);
        if (icon == null) {
            URL url = getIcon64URLFromResources(iconName);
            icon = new ImageIcon(url);
            icon64FromResourceCache.put(iconName, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon128FromResources(String iconName) {
        ImageIcon icon = icon128FromResourceCache.getOrDefault(iconName, null);
        if (icon == null) {
            URL url = getIcon128URLFromResources(iconName);
            icon = new ImageIcon(url);
            icon128FromResourceCache.put(iconName, icon);
        }
        return icon;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getIcons16BasePath() {
        return icons16BasePath;
    }

    public String getIcons32BasePath() {
        return icons32BasePath;
    }

    public String getDarkIcons16BasePath() {
        return darkIcons16BasePath;
    }

    public String getDarkIcons32BasePath() {
        return darkIcons32BasePath;
    }

    public String getTemplateBasePath() {
        return templateBasePath;
    }

    public String getSchemaBasePath() {
        return schemaBasePath;
    }

    public String getIcons64BasePath() {
        return icons64BasePath;
    }

    public String getDarkIcons64BasePath() {
        return darkIcons64BasePath;
    }

    public String getIcons128BasePath() {
        return icons128BasePath;
    }

    public String getDarkIcons128BasePath() {
        return darkIcons128BasePath;
    }

    /**
     * Converts a relative resource path to an absolute resource path
     *
     * @param internalResourcePath the internal path
     * @return the absolute resource path
     */
    public String relativeToAbsoluteResourcePath(String internalResourcePath) {
        if (internalResourcePath.startsWith("/"))
            internalResourcePath = internalResourcePath.substring(1);
        return getBasePath() + "/" + internalResourcePath;
    }

    /**
     * Gets a plugin-internal resource as URL
     *
     * @param internalResourcePath internal path relative to the resource base path
     * @return resource URL or null if the resource does not exist
     */
    public URL getResourceURL(String internalResourcePath) {
        return resourceClass.getResource(relativeToAbsoluteResourcePath(internalResourcePath));
    }

    /**
     * Gets a plugin-internal resource as stream
     *
     * @param internalResourcePath internal path relative to the resource base path
     * @return resource stream or null
     */
    public InputStream getResourceAsStream(String internalResourcePath) {
        return resourceClass.getResourceAsStream(relativeToAbsoluteResourcePath(internalResourcePath));
    }

    /**
     * Saves a resource as file
     *
     * @param internalResourcePath internal path relative to the resource base path
     * @param outputFile           the output file
     */
    public void exportResourceToFile(String internalResourcePath, Path outputFile) {
        try {
            Files.copy(getResourceAsStream(internalResourcePath), outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
