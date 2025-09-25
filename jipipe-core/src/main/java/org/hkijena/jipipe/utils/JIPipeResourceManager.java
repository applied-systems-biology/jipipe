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

import org.hkijena.jipipe.JIPipe;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Simplifies the access to resources for extensions
 */
public class JIPipeResourceManager {

    private final Class<?> resourceClass;
    private final String basePath;

    private final String icons8LightBasePath;
    private final String icons8DarkBasePath;
    private final String icons16LightBasePath;
    private final String icons16DarkBasePath;
    private final String icons12LightBasePath;
    private final String icons12DarkBasePath;
    private final String icons32LightBasePath;
    private final String icons32DarkBasePath;
    private final String icons24LightBasePath;
    private final String icons24DarkBasePath;
    private final String icons64LightBasePath;
    private final String icons64DarkBasePath;
    private final String icons128LightBasePath;
    private final String icons128DarkBasePath;
    private final String resourcesLightBasePath;
    private final String resourcesDarkBasePath;
    private final String templateBasePath;
    private final String schemaBasePath;

    private final Map<String, ImageIcon> iconCache = new HashMap<>();
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    /**
     * Creates a new instance
     *
     * @param resourceClass         the class that acts as the base for accessing the resources. Should be in the same package as the extension
     * @param basePath              absolute resource path to the resource root e.g. /org/hkijena/jipipe/extensions/myextension (must be consistent with the resource directory)
     * @param icons16LightBasePath  resource path to the 16x16 icons
     * @param icons16DarkBasePath   resource path to the dark 16x16 icons (icons themselves are optional; the non-dark versions are automatically utilized if no dark version is available)
     * @param icons32LightBasePath  resource path to the 32x32 icons
     * @param icons32DarkBasePath   resource path to the dark 32x32 icons (icons themselves are optional; the non-dark versions are automatically utilized if no dark version is available)
     * @param icons64LightBasePath  resource path to the 64x64 icons
     * @param icons64DarkBasePath   resource path to the dark 64x64 icons (icons themselves are optional; the non-dark versions are automatically utilized if no dark version is available)
     * @param icons128LightBasePath resource path to the 128x128 icons
     * @param icons128DarkBasePath  resource path to the dark 128x128 icons (icons themselves are optional; the non-dark versions are automatically utilized if no dark version is available)
     * @param templateBasePath      resource path to the template directory
     * @param schemaBasePath        resource path to the schema directory
     */
    public JIPipeResourceManager(Class<?> resourceClass, String basePath, String icons8LightBasePath, String icons8DarkBasePath, String icons16LightBasePath, String icons16DarkBasePath, String icons12LightBasePath, String icons12DarkBasePath,
                                 String icons32LightBasePath, String icons32DarkBasePath, String icons24LightBasePath, String icons24DarkBasePath, String icons64LightBasePath,
                                 String icons64DarkBasePath, String icons128LightBasePath, String icons128DarkBasePath, String resourcesLightBasePath, String resourcesDarkBasePath,
                                 String templateBasePath, String schemaBasePath) {
        this.resourceClass = resourceClass;
        this.basePath = formatBasePath(basePath);
        this.icons8LightBasePath = formatBasePath(icons8LightBasePath);
        this.icons8DarkBasePath = formatBasePath(icons8DarkBasePath);
        this.icons16LightBasePath = formatBasePath(icons16LightBasePath);
        this.icons16DarkBasePath = formatBasePath(icons16DarkBasePath);
        this.icons12LightBasePath = formatBasePath(icons12LightBasePath);
        this.icons12DarkBasePath = formatBasePath(icons12DarkBasePath);
        this.icons32LightBasePath = formatBasePath(icons32LightBasePath);
        this.icons32DarkBasePath = formatBasePath(icons32DarkBasePath);
        this.icons24LightBasePath = formatBasePath(icons24LightBasePath);
        this.icons24DarkBasePath = formatBasePath(icons24DarkBasePath);
        this.icons64LightBasePath = formatBasePath(icons64LightBasePath);
        this.icons64DarkBasePath = formatBasePath(icons64DarkBasePath);
        this.icons128LightBasePath = formatBasePath(icons128LightBasePath);
        this.icons128DarkBasePath = formatBasePath(icons128DarkBasePath);
        this.resourcesLightBasePath = formatBasePath(resourcesLightBasePath);
        this.resourcesDarkBasePath = formatBasePath(resourcesDarkBasePath);
        this.templateBasePath = formatBasePath(templateBasePath);
        this.schemaBasePath = formatBasePath(schemaBasePath);
    }

    /**
     * Creates a new resource manager with the following settings:
     * <ul>
     *     <li>icons 16x16 path: [basePath]/icons/light/icons-16</li>
     *     <li>icons dark 16x16 path: [basePath]/icons/light/icons-16</li>
     *     <li>...</li>
     *     <li>resources path: [basePath]/resources/light</li>
     *     <li>resources dark path: [basePath]/resources/dark</li>
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
        this.icons8LightBasePath = formatBasePath(basePath + "/icons/light/icons-8");
        this.icons8DarkBasePath = formatBasePath(basePath + "/icons/dark/icons-8");
        this.icons16LightBasePath = formatBasePath(basePath + "/icons/light/icons-16");
        this.icons16DarkBasePath = formatBasePath(basePath + "/icons/dark/icons-16");
        this.icons12LightBasePath = formatBasePath(basePath + "/icons/light/icons-12");
        this.icons12DarkBasePath = formatBasePath(basePath + "/icons/dark/icons-12");
        this.icons24LightBasePath = formatBasePath(basePath + "/icons/light/icons-24");
        this.icons24DarkBasePath = formatBasePath(basePath + "/icons/dark/icons-24");
        this.icons32LightBasePath = formatBasePath(basePath + "/icons/light/icons-32");
        this.icons32DarkBasePath = formatBasePath(basePath + "/icons/dark/icons-32");
        this.icons64LightBasePath = formatBasePath(basePath + "/icons/light/icons-64");
        this.icons64DarkBasePath = formatBasePath(basePath + "/icons/dark/icons-64");
        this.icons128LightBasePath = formatBasePath(basePath + "/icons/light/icons-128");
        this.icons128DarkBasePath = formatBasePath(basePath + "/icons/light/dark/icons-128");
        this.resourcesLightBasePath = formatBasePath(basePath + "/resources/light");
        this.resourcesDarkBasePath = formatBasePath(basePath + "/resources/dark");
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

    /**
     * Given a light URL, dark URL, and resource class, find the one matching the current theme mode (light/dark).
     * Returns the default URL if no resolution is made
     *
     * @param urlLight      light URL
     * @param urlDark       dark URL
     * @param resourceClass the resource class
     * @return the URL
     */
    public static URL safeResolveIconURL(String urlLight, String urlDark, Class<?> resourceClass, URL defaultValue) {
        if (resourceClass == null) {
            resourceClass = JIPipe.class;
        }
        if (ThemeUtils.isUsingDarkTheme() && !StringUtils.isNullOrEmpty(urlDark)) {
            URL resource = resourceClass.getResource(urlDark);
            if (resource != null) {
                return resource;
            }
        }
        if (!StringUtils.isNullOrEmpty(urlLight)) {
            URL resource = resourceClass.getResource(urlLight);
            if (resource != null) {
                return resource;
            }
        }
        return defaultValue;
    }

    /**
     * Given a light URL, dark URL, and resource class, find the one matching the current theme mode (light/dark).
     * Returns the URL for missing icons if the resolution fails
     *
     * @param urlLight      light URL
     * @param urlDark       dark URL
     * @param resourceClass the resource class
     * @return the URL
     */
    public static URL safeResolveIcon16URL(String urlLight, String urlDark, Class<?> resourceClass) {
        return safeResolveIconURL(urlLight, urlDark, resourceClass, getMissingIcon16URL());
    }

    /**
     * Given a light URL, dark URL, and resource class, find the one matching the current theme mode (light/dark).
     * Returns the URL for missing icons if the resolution fails
     *
     * @param urlLight      light URL
     * @param urlDark       dark URL
     * @param resourceClass the resource class
     * @param defaultValue  the name of the default icon
     * @return the URL
     */
    public static URL safeResolveIcon16URL(String urlLight, String urlDark, Class<?> resourceClass, String defaultValue) {
        return safeResolveIconURL(urlLight, urlDark, resourceClass, JIPipe.RESOURCES.getIcon16URL(defaultValue));
    }

    /**
     * Safely converts a URL to a 16x16 icon.
     * Returns the missing icon if something goes wrong
     *
     * @param url the URL
     * @return the icon
     */
    public static ImageIcon safeURLToIcon16(URL url) {
        try {
            if (url != null) {
                return new ImageIcon(url);
            }
        } catch (Exception ignored) {
        }
        url = getMissingIcon16URL();
        return new ImageIcon(url);
    }

    public static URL getMissingIcon8URL() {
        return ResourceUtils.getPluginResource("icons/light/icons-8/missing.png");
    }

    public static URL getMissingIcon16URL() {
        return ResourceUtils.getPluginResource("icons/light/icons-16/missing.png");
    }

    public static URL getMissingIcon24URL() {
        return ResourceUtils.getPluginResource("icons/light/icons-24/missing.png");
    }

    public static URL getMissingIcon32URL() {
        return ResourceUtils.getPluginResource("icons/light/icons-32/missing.png");
    }

    public static URL getMissingIcon64URL() {
        return ResourceUtils.getPluginResource("icons/light/icons-64/missing.png");
    }

    public static URL getMissingIcon128URL() {
        return ResourceUtils.getPluginResource("icons/light/icons-128/missing.png");
    }

    public static ImageIcon safeIcon16FromResourceManagerSupplier(String iconName, Class<? extends Supplier<JIPipeResourceManager>> resourceManagerSupplier, ImageIcon defaultIcon) {
        ImageIcon resultIcon = null;
        if (!StringUtils.isNullOrEmpty(iconName)) {
            try {
                JIPipeResourceManager resourceManager;
                if (resourceManagerSupplier != null) {
                    resourceManager = ((Supplier<JIPipeResourceManager>) ReflectionUtils.newInstance(resourceManagerSupplier)).get();
                } else {
                    resourceManager = JIPipe.RESOURCES;
                }
                URL url = resourceManager.getIcon16URL(iconName);
                if (url != null && !Objects.equals(url, JIPipeResourceManager.getMissingIcon16URL())) {
                    resultIcon = JIPipeResourceManager.safeURLToIcon16(url);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return resultIcon != null ? resultIcon : defaultIcon;
    }

    /**
     * Returns the instance for JIPipe core icons and resources
     *
     * @return the manager
     */
    public static JIPipeResourceManager getInstance() {
        return JIPipe.RESOURCES;
    }

    public Class<?> getResourceClass() {
        return resourceClass;
    }

    /**
     * Returns the URL of a 12x12 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon12URL(String iconName) {
        URL resource = null;
        if (ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons12DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons12LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon16URL();
            System.err.println(this + ": unable to find icon12 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 8x8 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon8URL(String iconName) {
        URL resource = null;
        if (ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons8DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons8LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon8URL();
            System.err.println(this + ": unable to find icon8 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 8x8 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon8InvertedURL(String iconName) {
        URL resource = null;
        if (!ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons8DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons8LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon8URL();
            System.err.println(this + ": unable to find icon8 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 16x16 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon12InvertedURL(String iconName) {
        URL resource = null;
        if (!ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons12DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons12LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon16URL();
            System.err.println(this + ": unable to find icon12 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 16x16 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon16URL(String iconName) {
        URL resource = null;
        if (ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons16DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons16LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon16URL();
            System.err.println(this + ": unable to find icon16 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 16x16 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon16InvertedURL(String iconName) {
        URL resource = null;
        if (!ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons16DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons16LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon16URL();
            System.err.println(this + ": unable to find icon16 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 24x24 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon24URL(String iconName) {
        URL resource = null;
        if (ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons24DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons24LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon24URL();
            System.err.println(this + ": unable to find icon24 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 24x24 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon24InvertedURL(String iconName) {
        URL resource = null;
        if (!ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons24DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons24LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon24URL();
            System.err.println(this + ": unable to find icon24 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 32x32 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon32URL(String iconName) {
        URL resource = null;
        if (ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons32DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons32LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon32URL();
            System.err.println(this + ": unable to find icon32 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 32x32 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon32InvertedURL(String iconName) {
        URL resource = null;
        if (!ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons32DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons32LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon32URL();
            System.err.println(this + ": unable to find icon32 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 64x64 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon64URL(String iconName) {
        URL resource = null;
        if (ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons64DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons64LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon64URL();
            System.err.println(this + ": unable to find icon64 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 64x64 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon64InvertedURL(String iconName) {
        URL resource = null;
        if (!ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons64DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons64LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon64URL();
            System.err.println(this + ": unable to find icon64 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 128x128 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon128URL(String iconName) {
        URL resource = null;
        if (ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons128DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons128LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon128URL();
            System.err.println(this + ": unable to find icon128 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a 128x128 icon. Adapts to dark theme.
     *
     * @param iconName the icon name
     * @return the URL or null if the icon does not exist
     */
    public URL getIcon128InvertedURL(String iconName) {
        URL resource = null;
        if (!ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(icons128DarkBasePath + "/" + iconName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(icons128LightBasePath + "/" + iconName);
        if (resource == null) {
            resource = getMissingIcon128URL();
            System.err.println(this + ": unable to find icon128 " + iconName);
        }
        return resource;
    }

    /**
     * Returns the URL of a variant resource (dark/light resources)
     *
     * @param resourceName the resource name
     * @return the URL
     */
    public URL getVariantResourceURL(String resourceName) {
        URL resource = null;
        if (ThemeUtils.isUsingDarkTheme()) {
            resource = resourceClass.getResource(resourcesDarkBasePath + "/" + resourceName);
            if (resource != null) {
                return resource;
            }
        }
        resource = resourceClass.getResource(resourcesLightBasePath + "/" + resourceName);
        if (resource == null) {
            resource = getMissingIcon64URL();
            System.err.println(this + ": unable to find resource " + resourceName);
        }
        return resource;
    }

    /**
     * Returns a variant resource as image
     *
     * @param resourceName the resource name
     * @return the image
     */
    public BufferedImage getVariantResourceAsImage(String resourceName) {
        final String cachePath = "resource-image/" + resourceName;
        BufferedImage image = imageCache.getOrDefault(cachePath, null);
        if (image == null) {
            URL url = getVariantResourceURL(resourceName);
            try {
                image = ImageIO.read(url);
                imageCache.put(cachePath, image);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return image;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon8(String iconName) {
        final String cachePath = "icon/8/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon8URL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon8Inverted(String iconName) {
        final String cachePath = "icon-inverted/8/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon8InvertedURL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon12(String iconName) {
        final String cachePath = "icon/12/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon12URL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon12Inverted(String iconName) {
        final String cachePath = "icon-inverted/12/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon12InvertedURL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon16(String iconName) {
        final String cachePath = "icon/16/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon16URL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon16Inverted(String iconName) {
        final String cachePath = "icon-inverted/16/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon16InvertedURL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon32(String iconName) {
        final String cachePath = "icon/32/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon32URL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon32Inverted(String iconName) {
        final String cachePath = "icon-inverted/32/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon32InvertedURL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon24(String iconName) {
        final String cachePath = "icon/24/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon24URL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon24Inverted(String iconName) {
        final String cachePath = "icon-inverted/24/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon24InvertedURL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon64(String iconName) {
        final String cachePath = "icon/64/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon64URL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIconInverted64(String iconName) {
        final String cachePath = "icon-inverted/64/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon64InvertedURL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon128(String iconName) {
        final String cachePath = "icon/128/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon128URL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    /**
     * Returns an icon from JIPipe resources
     *
     * @param iconName relative to the icons/ plugin resource
     * @return the icon instance
     */
    public ImageIcon getIcon128Inverted(String iconName) {
        final String cachePath = "icon-inverted/128/" + iconName;
        ImageIcon icon = iconCache.getOrDefault(cachePath, null);
        if (icon == null) {
            URL url = getIcon128InvertedURL(iconName);
            icon = new ImageIcon(url);
            iconCache.put(cachePath, icon);
        }
        return icon;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getIcons16LightBasePath() {
        return icons16LightBasePath;
    }

    public String getIcons32LightBasePath() {
        return icons32LightBasePath;
    }

    public String getIcons16DarkBasePath() {
        return icons16DarkBasePath;
    }

    public String getIcons32DarkBasePath() {
        return icons32DarkBasePath;
    }

    public String getTemplateBasePath() {
        return templateBasePath;
    }

    public String getSchemaBasePath() {
        return schemaBasePath;
    }

    public String getIcons64LightBasePath() {
        return icons64LightBasePath;
    }

    public String getIcons64DarkBasePath() {
        return icons64DarkBasePath;
    }

    public String getIcons128LightBasePath() {
        return icons128LightBasePath;
    }

    public String getIcons128DarkBasePath() {
        return icons128DarkBasePath;
    }

    public String getResourcesLightBasePath() {
        return resourcesLightBasePath;
    }

    public String getResourcesDarkBasePath() {
        return resourcesDarkBasePath;
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
