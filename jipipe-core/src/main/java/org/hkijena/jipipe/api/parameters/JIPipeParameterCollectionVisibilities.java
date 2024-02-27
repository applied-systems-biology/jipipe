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

package org.hkijena.jipipe.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Used as parameter to control the visibility of an existing parameter collection
 */
public class JIPipeParameterCollectionVisibilities {

    private Map<String, JIPipeParameterAccess> availableParameters;
    private Set<String> visibleParameters = new HashSet<>();

    /**
     * Creates new instance
     */
    public JIPipeParameterCollectionVisibilities() {
        this.availableParameters = new HashMap<>();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public JIPipeParameterCollectionVisibilities(JIPipeParameterCollectionVisibilities other) {
        this.availableParameters = new HashMap<>(other.availableParameters);
        this.visibleParameters = new HashSet<>(other.visibleParameters);
    }

    /**
     * An optional reference of all available parameters. This is not serialized and can be null
     *
     * @return reference of all available parameters
     */
    public Map<String, JIPipeParameterAccess> getAvailableParameters() {
        return availableParameters;
    }

    /**
     * Sets the list of available parameters
     *
     * @param availableParameters list of available parameters
     */
    public void setAvailableParameters(Map<String, JIPipeParameterAccess> availableParameters) {
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
