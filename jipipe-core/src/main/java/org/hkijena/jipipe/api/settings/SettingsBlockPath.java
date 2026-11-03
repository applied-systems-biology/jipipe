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

package org.hkijena.jipipe.api.settings;

import java.util.Objects;

/**
 * An immutable reference to a block in a scoped settings document.
 * <p>
 * A block path consists of a block kind prefix, a block ID, and an optional
 * sub-path that addresses a value within the block.
 * Block IDs may contain colons themselves, so only the <em>first</em> colon
 * separates the prefix from the ID, e.g.
 * {@code "sheet:org.hkijena.jipipe:general-ui"}.
 * Raw IDs without a colon, or with a prefix that matches no
 * {@link SettingsKind}, are passed through unchanged; in that case
 * {@link #kind()} is {@code null}.
 */
public final class SettingsBlockPath {
    private final SettingsKind kind;
    private final String rawId;
    private final String subPath;

    /**
     * Creates a path to a block of a known kind.
     *
     * @param kind the block kind
     * @param id the block ID, without the kind prefix
     */
    public SettingsBlockPath(SettingsKind kind, String id) {
        this(Objects.requireNonNull(kind, "kind"),
             kind.prefix() + ":" + Objects.requireNonNull(id, "id"),
             null);
    }

    /**
     * Parses a raw block ID of the form {@code "<prefix>:<id>"}.
     * Only the first colon separates the prefix from the ID.
     * IDs without a colon, or with a prefix that matches no
     * {@link SettingsKind}, are passed through unchanged with
     * {@link #kind()} == {@code null}.
     *
     * @param rawId the raw block ID
     */
    public SettingsBlockPath(String rawId) {
        this(parseKind(rawId), rawId, null);
    }

    /**
     * @param kind the block kind, or {@code null} for pass-through IDs
     * @param rawId the raw block ID
     * @param subPath the sub-path within the block, or {@code null}
     */
    private SettingsBlockPath(SettingsKind kind, String rawId, String subPath) {
        this.kind = kind;
        this.rawId = Objects.requireNonNull(rawId, "rawId");
        this.subPath = subPath;
    }

    /**
     * @return the block kind, or {@code null} if the raw ID has no known prefix (pass-through)
     */
    public SettingsKind kind() {
        return kind;
    }

    /**
     * @return the block ID without the kind prefix, or the whole raw ID for pass-through paths
     */
    public String id() {
        if (kind == null) {
            return rawId;
        }
        return rawId.substring(kind.prefix().length() + 1);
    }

    /**
     * @return the canonical block path, i.e. the raw ID as parsed
     */
    public String path() {
        return rawId;
    }

    /**
     * @return the sub-path within the block, or {@code null} if none is set
     */
    public String subPath() {
        return subPath;
    }

    /**
     * @return the full path including the sub-path: {@code path() + "/" + subPath()}
     * <p>
     * <b>Not strictly injective</b> if a block ID itself contains "/": for
     * {@code ("state:a/b", null)} and {@code ("state:a", "b")} it is
     * {@code "state:a/b"} in both cases. The contract is that <b>block IDs
     * never contain "/"</b> (sub-paths are the only place a slash appears);
     * this method relies on that and does not escape.
     */
    public String pathWithSubPath() {
        if (subPath == null) {
            return path();
        }
        return path() + "/" + subPath;
    }

    /**
     * Creates a copy of this path with a sub-path within the block.
     * <p>
     * Deliberate asymmetry with the constructors: the block-level
     * constructors reject a {@code null} block ID, but {@code withSubPath(null)}
     * is legal — it <em>clears</em> the sub-path (returns the block-only path),
     * because "no sub-path" is a meaningful state for a whole-block writer
     * (see {@code observeBlockWrites}).
     *
     * @param sub the sub-path within the block, or {@code null} to clear it
     * @return a new path with the same kind and ID but the given sub-path
     */
    public SettingsBlockPath withSubPath(String sub) {
        return new SettingsBlockPath(kind, rawId, sub);
    }

    private static SettingsKind parseKind(String rawId) {
        if (rawId == null) {
            return null;
        }
        int colon = rawId.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        return SettingsKind.fromPrefix(rawId.substring(0, colon));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SettingsBlockPath)) {
            return false;
        }
        SettingsBlockPath other = (SettingsBlockPath) obj;
        return Objects.equals(pathWithSubPath(), other.pathWithSubPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathWithSubPath());
    }

    @Override
    public String toString() {
        return pathWithSubPath();
    }
}
