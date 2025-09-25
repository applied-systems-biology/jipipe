package org.hkijena.jipipe.api.environments;

/**
 * The base archetype of a {@link JIPipeEnvironment} that is set during registration
 */
public enum JIPipeEnvironmentArchetype {
    /**
     * A fully managed environment. Can be registered using {@link RegisterJIPipeEnvironmentUsage} and can be easily accessed using the getEnvironment() method of each node.
     * Creates associated application-wide and project-wide settings
     */
    Managed,
    /**
     * An environment that is meant to be used as a base for other environments. It is still fully registered as a parameter but does not generate any setting.
     * Cannot be registered using {@link RegisterJIPipeEnvironmentUsage}
     */
    Base
}
