package org.hkijena.acaq5.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Used as parameter to control the visibility of an existing parameter collection
 */
public class ACAQParameterCollectionVisibilities {

    private Map<String, ACAQParameterAccess> availableParameters;
    private Set<String> visibleParameters = new HashSet<>();

    /**
     * Creates new instance
     */
    public ACAQParameterCollectionVisibilities() {
    }

    /**
     * An optional reference of all available parameters. This is not serialized and can be null
     *
     * @return reference of all available parameters
     */
    public Map<String, ACAQParameterAccess> getAvailableParameters() {
        return availableParameters;
    }

    /**
     * Sets the list of available parameters
     *
     * @param availableParameters list of available parameters
     */
    public void setAvailableParameters(Map<String, ACAQParameterAccess> availableParameters) {
        this.availableParameters = availableParameters;
    }

    /**
     * Sets the visibility of parameter with specified key
     *
     * @param key     Unique parameter key
     * @param visible Visibility
     */
    public void setVisibility(String key, boolean visible) {
        if (visible)
            visibleParameters.add(key);
        else
            visibleParameters.remove(key);
    }

    /**
     * @return Visible parameters
     */
    @JsonGetter("visible-keys")
    public Set<String> getVisibleParameters() {
        return visibleParameters;
    }

    /**
     * Sets visible parameters
     *
     * @param visibleParameters visible parameters
     */
    @JsonSetter("visible-keys")
    public void setVisibleParameters(Set<String> visibleParameters) {
        this.visibleParameters = visibleParameters;
    }

    /**
     * Returns if the parameter is visible
     *
     * @param key Parameter key
     * @return If visible
     */
    public boolean isVisible(String key) {
        return visibleParameters.contains(key);
    }
}
