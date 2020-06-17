package org.hkijena.acaq5.api.parameters;

/**
 * An {@link ACAQParameterCollection} that comes with a predefined name
 */
public interface ACAQNamedParameterCollection {
    /**
     * A default name. Can still be overridden by external settings
     * @return A default name. Can still be overridden by external settings
     */
    String getDefaultParameterCollectionName();

    /**
     *  A default description. Can still be overridden by external settings
     * @return A default description. Can still be overridden by external settings
     */
    String getDefaultParameterCollectionDescription();
}
