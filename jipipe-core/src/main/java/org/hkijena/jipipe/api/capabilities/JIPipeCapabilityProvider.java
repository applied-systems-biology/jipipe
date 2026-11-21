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

package org.hkijena.jipipe.api.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * One concrete way to obtain a capability: a provider registered by a plugin
 * together with a presence probe that answers whether the provider is
 * currently available.
 * <p>
 * Providers are never serialized: the probe captures the live state of the
 * backing source (artifact repository, environment, service toggle,
 * executable on disk). The probe is therefore consulted on every status
 * query and must return a fresh answer, so that a provider that appears or
 * disappears between two queries is noticed without restarting anything.
 * <p>
 * Providers are built through {@link #builder(Supplier)}:
 * <pre>{@code
 * JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.AVAILABLE)
 *         .kind(JIPipeProviderKind.EXECUTABLE)
 *         .provides("python3")
 *         .build();
 * }</pre>
 */
public final class JIPipeCapabilityProvider {
    private final Supplier<JIPipeCapabilityStatus.State> probe;
    private final JIPipeProviderKind kind;
    private final List<String> provides;
    private final List<Alias> aliases;
    private final JIPipeInstallHint installHint;
    private final String displayName;

    private JIPipeCapabilityProvider(Builder builder) {
        this.probe = builder.probe;
        this.kind = builder.kind;
        this.provides = List.copyOf(builder.provides);
        this.aliases = List.copyOf(builder.aliases);
        this.installHint = builder.installHint;
        this.displayName = builder.displayName;
    }

    /**
     * Starts building a provider.
     *
     * @param probe the presence probe; it must return
     *              {@link JIPipeCapabilityStatus.State#AVAILABLE} when the
     *              provider is present and
     *              {@link JIPipeCapabilityStatus.State#UNAVAILABLE} otherwise
     */
    public static Builder builder(Supplier<JIPipeCapabilityStatus.State> probe) {
        return new Builder(probe);
    }

    /**
     * @return this provider's identity: the kind's prefix applied to the
     * first provided id, when that id does not already carry it
     */
    public String getId() {
        String primary = provides.get(0);
        String prefix = kind == null ? "" : kind.getPrefix();
        return primary.startsWith(prefix) ? primary : prefix + primary;
    }

    /**
     * @return the presence probe of this provider
     */
    public Supplier<JIPipeCapabilityStatus.State> getProbe() {
        return probe;
    }

    /**
     * @return the nature of what this provider is based on, or null when
     * unset
     */
    public JIPipeProviderKind getKind() {
        return kind;
    }

    /**
     * @return the capability ids this provider satisfies, in declaration order
     */
    public List<String> getProvides() {
        return provides;
    }

    /**
     * @return the alias edges declared by this provider, in declaration order
     */
    public List<Alias> getAliases() {
        return aliases;
    }

    /**
     * @return the hint how to install or configure this provider when it is
     * not present, or null when the provider cannot be installed through
     * JIPipe or its cost is not worth describing
     */
    public JIPipeInstallHint getInstallHint() {
        return installHint;
    }

    /**
     * @return the human-readable name shown in ranked candidate lists, or
     * null to fall back to the provider id
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * An alias edge declared by a provider: the plain capability
     * {@code from} derives to the ids {@code to}. Alias edges follow the
     * family a provider belongs to, e.g. {@code python3 -> python}.
     *
     * @param from the more specific id
     * @param to   the ids it derives to
     */
    public record Alias(String from, List<String> to) {
        public Alias {
            to = List.copyOf(to);
        }
    }

    public static final class Builder {
        private final Supplier<JIPipeCapabilityStatus.State> probe;
        private JIPipeProviderKind kind;
        private final List<String> provides = new ArrayList<>();
        private final List<Alias> aliases = new ArrayList<>();
        private JIPipeInstallHint installHint;
        private String displayName;

        private Builder(Supplier<JIPipeCapabilityStatus.State> probe) {
            this.probe = Objects.requireNonNull(probe, "probe");
        }

        /**
         * Sets the nature of what this provider is based on.
         */
        public Builder kind(JIPipeProviderKind kind) {
            this.kind = Objects.requireNonNull(kind, "kind");
            return this;
        }

        /**
         * Adds the capability ids this provider satisfies. At least one id is
         * required; {@link #build()} fails otherwise.
         */
        public Builder provides(String... provides) {
            for (String p : provides) {
                this.provides.add(Objects.requireNonNull(p, "provides entry"));
            }
            return this;
        }

        /**
         * Declares an alias edge: the plain capability {@code from} derives
         * to the ids {@code to}, e.g. {@code python3 -> python}.
         */
        public Builder alias(String from, String... to) {
            Objects.requireNonNull(from, "from");
            for (String t : to) {
                Objects.requireNonNull(t, "to entry");
            }
            aliases.add(new Alias(from, List.of(to)));
            return this;
        }

        /**
         * Sets the hint how to install or configure this provider when it is
         * not present.
         */
        public Builder installHint(JIPipeInstallHint installHint) {
            this.installHint = Objects.requireNonNull(installHint, "installHint");
            return this;
        }

        /**
         * Sets the human-readable name shown in ranked candidate lists.
         */
        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        /**
         * Builds the provider.
         *
         * @throws IllegalArgumentException when no capability id was provided
         */
        public JIPipeCapabilityProvider build() {
            if (provides.isEmpty()) {
                throw new IllegalArgumentException("A capability provider must provide at least one capability id");
            }
            return new JIPipeCapabilityProvider(this);
        }
    }
}
