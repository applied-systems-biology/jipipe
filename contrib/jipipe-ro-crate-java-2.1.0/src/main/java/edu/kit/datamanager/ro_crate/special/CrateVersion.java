package edu.kit.datamanager.ro_crate.special;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Represents a crate version and has information for each version available.
 * 
 * It offers also convenience functionality like version comparison similar to
 * numbers.
 */
public enum CrateVersion {
    // NOTE: Java defines compareTo for enums to work in the order of the defined
    // elements. This means higher versions have to be defined below the lower
    // versions. Consider this when adding a new version.
    //
    // NOTE: To add a new version, simply append a new variant with the correct
    // data and read the comment below the versions.
    V1P1("https://w3id.org/ro/crate/1.1"),
    V1P2_DRAFT("https://w3id.org/ro/crate/1.2-DRAFT");

    // NOTE: Do not forget to adjust the following when adding a version:
    public static final CrateVersion LATEST_STABLE = CrateVersion.V1P1;
    public static final CrateVersion LATEST_UNSTABLE = CrateVersion.V1P2_DRAFT;

    /**
     * The String which is referred to by the crates conformsTo value.
     * Should lead to the specification when resolved as a URL.
     */
    public final String conformsTo;

    /**
     * The String representation of the version.
     * 
     * Example: assertEquals("1.2-DRAFT", CrateVersion.V1P2_DRAFT);
     */
    public final String version;

    /**
     * Basically a constructor of a version, if it needs to be created from the spec
     * URI.
     * 
     * @param conformsTo the specification URI. Example:
     *                   https://w3id.org/ro/crate/1.1
     * @return the matching CrateVersion enum, if the URI matches any. Empty if not.
     */
    public static Optional<CrateVersion> fromSpecUri(String conformsTo) {
        return Optional.ofNullable(crateVersionOfConformsTo(conformsTo));
    }

    /**
     * Private constructor which is being used for the internally given information
     * above.
     * 
     * @param spec the specification URI / conformsTo value
     */
    private CrateVersion(String spec) {
        this.conformsTo = spec;
        this.version = getVersionFromConformsToString(spec);
    }

    public boolean isStable() {
        return !this.version.endsWith("-DRAFT");
    }

    public URI getConformsToUri() {
        try {
            return new URI(this.conformsTo);
        } catch (URISyntaxException e) {
            // can not happen as the user can not set this string
            return null;
        }
    }

    /**
     * Extracts the version from the spec uri.
     * 
     * @param specUri the uri of the spec
     * @return the version of the spec
     */
    private static String getVersionFromConformsToString(String specUri) {
        String[] parts = specUri.split("/");
        return parts[parts.length - 1];
    }

    private static CrateVersion crateVersionOfConformsTo(String conformsTo) {
        for (CrateVersion e : values()) {
            if (e.conformsTo.equals(conformsTo)) {
                return e;
            }
        }
        return null;
    }

    public boolean isGreaterThan(CrateVersion other) {
        return this.compareTo(other) > 0;
    }

    public boolean isGreaterOrEqualThan(CrateVersion other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isLowerThan(CrateVersion other) {
        return this.compareTo(other) < 0;
    }

    public boolean isLowerOrEqualThan(CrateVersion other) {
        return this.compareTo(other) <= 0;
    }
}
