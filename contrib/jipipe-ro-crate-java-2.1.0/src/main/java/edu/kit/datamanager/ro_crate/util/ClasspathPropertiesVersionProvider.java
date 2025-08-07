package edu.kit.datamanager.ro_crate.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class ClasspathPropertiesVersionProvider implements VersionProvider {
    public static final String VERSION_PROPERTIES = "version.properties";

    /**
     * Cached version to avoid repeated file/resource reads.
     */
    private String cachedVersion = null;

    /**
     * Constructs a ClasspathPropertiesVersionProvider that reads the version from a properties file in the classpath.
     */
    public ClasspathPropertiesVersionProvider() {
        // Lazy initialization - version loaded on first access
    }

    @Override
    public String getVersion() {
        if (cachedVersion != null) {
            return cachedVersion;
        }

        URL resource = this.getClass().getResource("/" + VERSION_PROPERTIES);
        assert resource != null : VERSION_PROPERTIES + " not found in classpath";

        try (InputStream input = resource.openStream()) {
            Properties properties = new Properties();
            properties.load(input);
            String version = properties.getProperty("version");
            assert version != null : "Version property not found in " + VERSION_PROPERTIES;
            return version.trim();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read version from properties file", e);
        }
    }
}
