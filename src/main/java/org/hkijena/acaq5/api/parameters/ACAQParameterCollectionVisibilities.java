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

    public ACAQParameterCollectionVisibilities() {

    }

    /**
     * An optional reference of all available parameters. This is not serialized and can be null
     *
     * @return
     */
    public Map<String, ACAQParameterAccess> getAvailableParameters() {
        return availableParameters;
    }

    public void setAvailableParameters(Map<String, ACAQParameterAccess> availableParameters) {
        this.availableParameters = availableParameters;
    }

    public void setVisibility(String key, boolean visible) {
        if (visible)
            visibleParameters.add(key);
        else
            visibleParameters.remove(key);
    }

    @JsonGetter("visible-keys")
    public Set<String> getVisibleParameters() {
        return visibleParameters;
    }

    @JsonSetter("visible-keys")
    public void setVisibleParameters(Set<String> visibleParameters) {
        this.visibleParameters = visibleParameters;
    }

    public boolean isVisible(String key) {
        return visibleParameters.contains(key);
    }
}
