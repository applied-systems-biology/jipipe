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
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Models an author with affiliations
 */
public class JIPipeAuthorMetadata implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();

    private String title;
    private String firstName;
    private String lastName;
    private StringList affiliations;
    private String contact;
    private boolean firstAuthor;
    private boolean correspondingAuthor;

    /**
     * Creates a new instance
     */
    public JIPipeAuthorMetadata() {
    }

    /**
     * Initializes the instance
     * @param title the title (can be empty)
     * @param firstName    first name
     * @param lastName     last name
     * @param affiliations list of affiliations
     * @param contact contact information, e.g., an E-Mail address
     * @param firstAuthor if the author is marked as first author
     * @param correspondingAuthor if the author is marked as corresponding author
     */
    public JIPipeAuthorMetadata(String title, String firstName, String lastName, StringList affiliations, String contact, boolean firstAuthor, boolean correspondingAuthor) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.affiliations = affiliations;
        this.contact = contact;
        this.firstAuthor = firstAuthor;
        this.correspondingAuthor = correspondingAuthor;
    }

    public JIPipeAuthorMetadata(JIPipeAuthorMetadata other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.affiliations = new StringList(other.affiliations);
        this.contact = other.contact;
        this.correspondingAuthor = other.correspondingAuthor;
        this.firstAuthor = other.firstAuthor;
    }

    @JIPipeParameter(value = "title", uiOrder = -1)
    @JsonGetter("title")
    @JIPipeDocumentation(name = "Title", description = "The title (optional)")
    public String getTitle() {
        return title;
    }

    @JsonSetter("title")
    @JIPipeParameter("title")
    public void setTitle(String title) {
        this.title = title;
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

    @JIPipeParameter(value = "affiliations-list", uiOrder = 3)
    @JIPipeDocumentation(name = "Affiliations", description = "Author affiliations")
    @StringParameterSettings(multiline = true, monospace = true)
    @JsonGetter("affiliations-list")
    public StringList getAffiliations() {
        return affiliations;
    }

    @JIPipeParameter("affiliations-list")
    @JsonSetter("affiliations-list")
    public void setAffiliations(StringList affiliations) {
        this.affiliations = affiliations;
        eventBus.post(new ParameterChangedEvent(this, "affiliations"));
    }

    @JIPipeParameter(value = "contact", uiOrder = 4)
    @JIPipeDocumentation(name = "Contact info", description = "Information on how to contact the author, for example an E-Mail address.")
    @JsonGetter("contact")
    public String getContact() {
        return contact;
    }

    @JIPipeParameter("contact")
    @JsonSetter("contact")
    public void setContact(String contact) {
        this.contact = contact;
    }

    @JIPipeParameter(value = "first-author", uiOrder = 5)
    @JIPipeDocumentation(name = "Is first author", description = "If this author is marked as first author")
    @JsonGetter("first-author")
    public boolean isFirstAuthor() {
        return firstAuthor;
    }

    @JIPipeParameter("first-author")
    @JsonSetter("first-author")
    public void setFirstAuthor(boolean firstAuthor) {
        this.firstAuthor = firstAuthor;
    }

    @JIPipeParameter(value = "corresponding-author", uiOrder = 6)
    @JIPipeDocumentation(name = "Is corresponding author", description = "If this author is marked as corresponding author")
    @JsonGetter("corresponding-author")
    public boolean isCorrespondingAuthor() {
        return correspondingAuthor;
    }

    @JIPipeParameter("corresponding-author")
    @JsonSetter("corresponding-author")
    public void setCorrespondingAuthor(boolean correspondingAuthor) {
        this.correspondingAuthor = correspondingAuthor;
    }

    @Override
    public String toString() {
        return (StringUtils.nullToEmpty(title) + " " + StringUtils.nullToEmpty(firstName) + " " + StringUtils.nullToEmpty(lastName) + (isFirstAuthor() ? "*" : "") + (isCorrespondingAuthor() ? "#" : "")).trim();
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
