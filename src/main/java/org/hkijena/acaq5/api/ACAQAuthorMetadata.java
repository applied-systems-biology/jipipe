package org.hkijena.acaq5.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;

import java.util.Collection;

/**
 * Models an author with affiliations
 */
public class ACAQAuthorMetadata implements ACAQParameterCollection {

    private final EventBus eventBus = new EventBus();

    private String firstName;
    private String lastName;
    private String affiliations;

    /**
     * Creates a new instance
     */
    public ACAQAuthorMetadata() {
    }

    /**
     * Initializes the instance
     * @param firstName first name
     * @param lastName last name
     * @param affiliations affiliations
     */
    public ACAQAuthorMetadata(String firstName, String lastName, String affiliations) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.affiliations = affiliations;
    }

    /**
     * Makes a copy
     * @param other the original
     */
    public ACAQAuthorMetadata(ACAQAuthorMetadata other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.affiliations = other.affiliations;
    }

    @ACAQParameter(value = "first-name", uiOrder = 0)
    @ACAQDocumentation(name = "First name", description = "The first name")
    @JsonGetter("first-name")
    public String getFirstName() {
        return firstName;
    }

    @ACAQParameter("first-name")
    @JsonSetter("first-name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        eventBus.post(new ParameterChangedEvent(this, "first-name"));
    }

    @ACAQParameter(value = "last-name", uiOrder = 1)
    @ACAQDocumentation(name = "Last name", description = "The last name")
    @JsonGetter("last-name")
    public String getLastName() {
        return lastName;
    }

    @ACAQParameter("last-name")
    @JsonSetter("last-name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
        eventBus.post(new ParameterChangedEvent(this, "last-name"));
    }

    @ACAQParameter(value = "affiliations", uiOrder = 3)
    @ACAQDocumentation(name = "Affiliations", description = "Author affiliations")
    @StringParameterSettings(multiline = true, monospace = true)
    @JsonGetter("affiliations")
    public String getAffiliations() {
        return affiliations;
    }

    @ACAQParameter("affiliations")
    @JsonSetter("affiliations")
    public void setAffiliations(String affiliations) {
        this.affiliations = affiliations;
        eventBus.post(new ParameterChangedEvent(this, "affiliations"));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static class List extends ListParameter<ACAQAuthorMetadata> {

        /**
         * Creates a new instance
         */
        public List() {
            super(ACAQAuthorMetadata.class);
        }

        /**
         * Makes a copy
         * @param other the original
         */
        public List(Collection<ACAQAuthorMetadata> other) {
            super(ACAQAuthorMetadata.class);
            for (ACAQAuthorMetadata metadata : other) {
                add(new ACAQAuthorMetadata(metadata));
            }
        }
    }
}
