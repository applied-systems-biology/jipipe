/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Models an author with affiliations
 */
public class JIPipeAuthorMetadata implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();

    private String firstName;
    private String lastName;
    private String affiliations;

    /**
     * Creates a new instance
     */
    public JIPipeAuthorMetadata() {
    }

    /**
     * Initializes the instance
     *
     * @param firstName    first name
     * @param lastName     last name
     * @param affiliations affiliations
     */
    public JIPipeAuthorMetadata(String firstName, String lastName, String affiliations) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.affiliations = affiliations;
    }

    /**
     * Makes a copy
     *
     * @param other the original
     */
    public JIPipeAuthorMetadata(JIPipeAuthorMetadata other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.affiliations = other.affiliations;
    }

    @JIPipeParameter(value = "first-name", uiOrder = 0)
    @JIPipeDocumentation(name = "First name", description = "The first name")
    @JsonGetter("first-name")
    public String getFirstName() {
        return firstName;
    }

    @JIPipeParameter("first-name")
    @JsonSetter("first-name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        eventBus.post(new ParameterChangedEvent(this, "first-name"));
    }

    @JIPipeParameter(value = "last-name", uiOrder = 1)
    @JIPipeDocumentation(name = "Last name", description = "The last name")
    @JsonGetter("last-name")
    public String getLastName() {
        return lastName;
    }

    @JIPipeParameter("last-name")
    @JsonSetter("last-name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
        eventBus.post(new ParameterChangedEvent(this, "last-name"));
    }

    @JIPipeParameter(value = "affiliations", uiOrder = 3)
    @JIPipeDocumentation(name = "Affiliations", description = "Author affiliations")
    @StringParameterSettings(multiline = true, monospace = true)
    @JsonGetter("affiliations")
    public String getAffiliations() {
        return affiliations;
    }

    @JIPipeParameter("affiliations")
    @JsonSetter("affiliations")
    public void setAffiliations(String affiliations) {
        this.affiliations = affiliations;
        eventBus.post(new ParameterChangedEvent(this, "affiliations"));
    }

    @Override
    public String toString() {
        return StringUtils.nullToEmpty(firstName) + " " + StringUtils.nullToEmpty(lastName);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static class List extends ListParameter<JIPipeAuthorMetadata> {

        /**
         * Creates a new instance
         */
        public List() {
            super(JIPipeAuthorMetadata.class);
        }

        /**
         * Makes a copy
         *
         * @param other the original
         */
        public List(Collection<JIPipeAuthorMetadata> other) {
            super(JIPipeAuthorMetadata.class);
            for (JIPipeAuthorMetadata metadata : other) {
                add(new JIPipeAuthorMetadata(metadata));
            }
        }

        @Override
        public String toString() {
            return this.stream().map(JIPipeAuthorMetadata::toString).collect(Collectors.joining(", "));
        }
    }
}
