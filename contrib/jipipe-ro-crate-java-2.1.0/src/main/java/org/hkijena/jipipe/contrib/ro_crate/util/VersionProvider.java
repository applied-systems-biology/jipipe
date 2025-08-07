package org.hkijena.jipipe.contrib.ro_crate.util;

public interface VersionProvider {
    /**
     * Returns the version of the ro-crate-java library.
     *
     * @return The version string.
     */
    String getVersion();
}
