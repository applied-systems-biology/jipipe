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

package org.hkijena.jipipe.api.metadata;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Models an author with affiliations
 */
public class JIPipeAuthorMetadata extends AbstractJIPipeParameterCollection {
    private String title;
    private String firstName;
    private String lastName;
    private StringList affiliationsLegacy = new StringList();
    private JIPipeOrganizationMetadata.List affiliations = new JIPipeOrganizationMetadata.List();
    private String website;
    private String contact;
    private String email;
    private boolean firstAuthor;
    private boolean correspondingAuthor;
    private String orcid;
    private HTMLText customText = new HTMLText();

    /**
     * Creates a new instance
     */
    public JIPipeAuthorMetadata() {
    }

    public JIPipeAuthorMetadata(JIPipeAuthorMetadata other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.email = other.email;
        this.title = other.title;
        this.affiliationsLegacy = new StringList(other.affiliationsLegacy);
        this.affiliations = new JIPipeOrganizationMetadata.List(other.affiliations);
        this.website = other.website;
        this.contact = other.contact;
        this.correspondingAuthor = other.correspondingAuthor;
        this.firstAuthor = other.firstAuthor;
        this.customText = new HTMLText(other.customText);
        this.orcid = other.orcid;
    }


    /**
     * Opens a list of authors in a window that displays information about them.
     *
     * @param parent       the parent component
     * @param authors      the list of authors
     * @param targetAuthor the author to show first. Can be null.
     * @return the window
     */
    public static JFrame openAuthorInfoWindow(Component parent, Collection<JIPipeAuthorMetadata> authors, JIPipeAuthorMetadata targetAuthor) {
        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.Top);
        for (JIPipeAuthorMetadata author : authors) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<h1>").append(HtmlEscapers.htmlEscaper().escape(author.toString())).append("</h1>");
            stringBuilder.append("<div><small><strong>*</strong> First author</small></div>");
            stringBuilder.append("<div><small><strong>#</strong> Corresponding author</small></div>");
            stringBuilder.append("<br/><br/>");
            if (!StringUtils.isNullOrEmpty(author.getContact())) {
                if (author.getContact().contains("@")) {
                    stringBuilder.append("<div><strong>Contact:</strong> <a href=\"mailto:").append(author.getContact()).append("\">").append(author.getContact()).append("</a></div>");
                } else {
                    stringBuilder.append("<div><strong>Contact:</strong> ").append(HtmlEscapers.htmlEscaper().escape(author.getContact())).append("</div>");
                }
            }
            if (!StringUtils.isNullOrEmpty(author.getEmail())) {
                if (author.getContact().contains("@")) {
                    stringBuilder.append("<div><strong>E-Mail:</strong> <a href=\"mailto:").append(author.getContact()).append("\">").append(author.getContact()).append("</a></div>");
                } else {
                    stringBuilder.append("<div><strong>E-Mail:</strong> ").append(HtmlEscapers.htmlEscaper().escape(author.getContact())).append("</div>");
                }
            }
            if (!StringUtils.isNullOrEmpty(author.getOrcid())) {
                stringBuilder.append("<div><strong>ORCID:</strong> <a href=\"").append(author.getOrcidUrl()).append("\">").append(author.getOrcidUrl()).append("</a></div>");
            }
            if (!StringUtils.isNullOrEmpty(author.getWebsite())) {
                stringBuilder.append("<div><strong>Website:</strong> <a href=\"").append(author.getWebsite()).append("\">").append(author.getWebsite()).append("</a></div>");
            }
            if (!author.getAffiliations().isEmpty()) {
                stringBuilder.append("<h2>Affiliations</h2>");
                stringBuilder.append("<ul>");
                for (JIPipeOrganizationMetadata affiliation : author.getAffiliations()) {
                    stringBuilder.append("<li>").append(HtmlEscapers.htmlEscaper().escape(affiliation.getName()));
                    if (!StringUtils.isNullOrEmpty(affiliation.getRorUrl())) {
                        stringBuilder.append(" (").append(HtmlEscapers.htmlEscaper().escape(affiliation.getRorUrl())).append(")");
                    } else if (!StringUtils.isNullOrEmpty(affiliation.getWebsite())) {
                        stringBuilder.append(" (").append(HtmlEscapers.htmlEscaper().escape(affiliation.getWebsite())).append(")");
                    }
                    stringBuilder.append("</li>");
                }
                stringBuilder.append("</ul>");
            }
            if (author.getCustomText() != null) {
                stringBuilder.append("<br/><br/>");
                stringBuilder.append(author.getCustomText().getBody());
            }
            JIPipeDesktopMarkdownReader reader = new JIPipeDesktopMarkdownReader(false, new MarkdownText(stringBuilder.toString()));
            tabPane.addTab(author.toString(), JIPipe.RESOURCES.getIcon16("actions/im-user.png"), reader, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
            if (author == targetAuthor) {
                tabPane.switchToLastTab();
            }
        }
        JFrame dialog = new JFrame();
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        dialog.setContentPane(tabPane);
        dialog.setTitle("Author information");
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog;
    }

    public String getOrcidUrl() {
        if (!StringUtils.isNullOrEmpty(getOrcid())) {
            if (getOrcid().startsWith("http") || getOrcid().contains("orcid.org")) {
                return getOrcid();
            }
            return "https://orcid.org/" + getOrcid();
        }
        return "";
    }

    /**
     * A fuzzy equals method that attempts to check if the authors are the same
     *
     * @param other the other author
     * @return if they are likely the same
     */
    public boolean fuzzyEquals(JIPipeAuthorMetadata other) {
        // ORCID is the best qualifier, try this first
        if (!StringUtils.isNullOrEmpty(getOrcidUrl()) && !StringUtils.isNullOrEmpty(other.getOrcidUrl())) {
            return getOrcidUrl().equalsIgnoreCase(other.getOrcidUrl());
        }

        // ORCID not present, so we have to use the name
        return StringUtils.nullToEmpty(getFirstName()).trim().equalsIgnoreCase(StringUtils.nullToEmpty(other.getFirstName()).trim()) &&
                StringUtils.nullToEmpty(getLastName()).trim().equalsIgnoreCase(StringUtils.nullToEmpty(other.getLastName()).trim());
    }

    @JIPipeParameter(value = "orcid", uiOrder = 45)
    @JsonGetter("orcid")
    @SetJIPipeDocumentation(name = "ORCID", description = "The ORCID (URL or ID)")
    @StringParameterSettings(monospace = true)
    public String getOrcid() {
        return StringUtils.nullToEmpty(orcid).trim();
    }

    @JIPipeParameter("orcid")
    @JsonSetter("orcid")
    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    @JIPipeParameter(value = "title", uiOrder = -10)
    @JsonGetter("title")
    @SetJIPipeDocumentation(name = "Title", description = "The title (optional)")
    public String getTitle() {
        return title;
    }

    @JsonSetter("title")
    @JIPipeParameter("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JIPipeParameter(value = "first-name", uiOrder = 0)
    @SetJIPipeDocumentation(name = "First name", description = "The first name")
    @JsonGetter("first-name")
    public String getFirstName() {
        return firstName;
    }

    @JIPipeParameter("first-name")
    @JsonSetter("first-name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JIPipeParameter(value = "last-name", uiOrder = 10)
    @SetJIPipeDocumentation(name = "Last name", description = "The last name")
    @JsonGetter("last-name")
    public String getLastName() {
        return lastName;
    }

    @JIPipeParameter("last-name")
    @JsonSetter("last-name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JIPipeParameter(value = "affiliations-list", uiOrder = 30, hidden = true)
    @SetJIPipeDocumentation(name = "Affiliations (deprecated)", description = "Deprecated field kept for backwards compatibility")
    @StringParameterSettings(multiline = true, monospace = true)
    @JsonGetter("affiliations-list")
    @Deprecated
    public StringList getAffiliationsLegacy() {
        if (affiliationsLegacy == null)
            affiliationsLegacy = new StringList();
        return affiliationsLegacy;
    }

    @JIPipeParameter("affiliations-list")
    @JsonSetter("affiliations-list")
    @Deprecated
    public void setAffiliationsLegacy(StringList affiliationsLegacy) {
        this.affiliationsLegacy = affiliationsLegacy;
    }

    @JIPipeParameter(value = "affiliations-list-v2", uiOrder = 30)
    @SetJIPipeDocumentation(name = "Affiliations", description = "Author affiliations")
    @JsonGetter("affiliations-list-v2")
    public JIPipeOrganizationMetadata.List getAffiliations() {
        if (affiliations.isEmpty() && !affiliationsLegacy.isEmpty()) {
            // Transfer legacy affiliations
            for (String s : affiliationsLegacy) {
                affiliations.add(new JIPipeOrganizationMetadata.Builder().name(s).build());
            }
            affiliationsLegacy.clear();
        }
        return affiliations;
    }

    @JIPipeParameter("affiliations-list-v2")
    @JsonSetter("affiliations-list-v2")
    public void setAffiliations(JIPipeOrganizationMetadata.List affiliations) {
        this.affiliations = affiliations;
    }

    @JIPipeParameter(value = "email", uiOrder = 35)
    @StringParameterSettings(monospace = true)
    @SetJIPipeDocumentation(name = "E-Mail", description = "E-Mail")
    @JsonGetter("email")
    public String getEmail() {
        return email;
    }

    @JIPipeParameter("email")
    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @JIPipeParameter(value = "contact", uiOrder = 40)
    @StringParameterSettings(monospace = true)
    @SetJIPipeDocumentation(name = "Contact info (misc)", description = "Additional contact info.")
    @JsonGetter("contact")
    public String getContact() {
        return contact;
    }

    @JIPipeParameter("contact")
    @JsonSetter("contact")
    public void setContact(String contact) {
        this.contact = contact;
    }

    @JIPipeParameter(value = "website", uiOrder = 50)
    @StringParameterSettings(monospace = true)
    @SetJIPipeDocumentation(name = "Website", description = "An optional website URL")
    @JsonGetter("website")
    public String getWebsite() {
        return website;
    }

    @JIPipeParameter("website")
    @JsonSetter("website")
    public void setWebsite(String website) {
        this.website = website;
    }

    @JIPipeParameter(value = "first-author", uiOrder = 60)
    @SetJIPipeDocumentation(name = "Is first author", description = "If this author is marked as first author")
    @JsonGetter("first-author")
    public boolean isFirstAuthor() {
        return firstAuthor;
    }

    @JIPipeParameter("first-author")
    @JsonSetter("first-author")
    public void setFirstAuthor(boolean firstAuthor) {
        this.firstAuthor = firstAuthor;
    }

    @JIPipeParameter(value = "corresponding-author", uiOrder = 70)
    @SetJIPipeDocumentation(name = "Is corresponding author", description = "If this author is marked as corresponding author")
    @JsonGetter("corresponding-author")
    public boolean isCorrespondingAuthor() {
        return correspondingAuthor;
    }

    @JIPipeParameter("corresponding-author")
    @JsonSetter("corresponding-author")
    public void setCorrespondingAuthor(boolean correspondingAuthor) {
        this.correspondingAuthor = correspondingAuthor;
    }

    @JIPipeParameter(value = "custom-text", uiOrder = 80)
    @SetJIPipeDocumentation(name = "Custom text", description = "Will be displayed in the author information window.")
    @JsonGetter("custom-text")
    public HTMLText getCustomText() {
        return customText;
    }

    @JIPipeParameter("custom-text")
    @JsonSetter("custom-text")
    public void setCustomText(HTMLText customText) {
        this.customText = customText;
    }

    @Override
    public String toString() {
        return (StringUtils.nullToEmpty(title) + " " + StringUtils.nullToEmpty(firstName) + " " + StringUtils.nullToEmpty(lastName) + (isFirstAuthor() ? "*" : "") + (isCorrespondingAuthor() ? "#" : "")).trim();
    }

    public String getUniqueId() {
        if(!StringUtils.isNullOrEmpty(getOrcidUrl())) {
            return getOrcidUrl();
        }
        else {
            return getEmail();
        }
    }

    public void mergeWith(JIPipeAuthorMetadata other) {
        if (StringUtils.isNullOrEmpty(orcid) && !StringUtils.isNullOrEmpty(other.getOrcid())) {
            orcid = other.getOrcid();
        }
        if (!StringUtils.isNullOrEmpty(title) && !StringUtils.isNullOrEmpty(other.getTitle())) {
            title = other.getTitle();
        }
        if (!StringUtils.isNullOrEmpty(website) && !StringUtils.isNullOrEmpty(other.getWebsite())) {
            website = other.getWebsite();
        }
        if (!StringUtils.isNullOrEmpty(contact) && !StringUtils.isNullOrEmpty(other.getContact())) {
            contact = other.getContact();
        }
        for (JIPipeOrganizationMetadata affiliation : other.getAffiliations()) {
            int i = affiliations.indexOf(affiliation);
            if(i >= 0) {
                affiliations.get(i).mergeWith(affiliation);
            }
            else {
                affiliations.add(affiliation);
            }
        }
    }

    public static class List extends ListParameter<JIPipeAuthorMetadata> {

        /**
         * Creates a new instance
         */
        public List() {
            super(JIPipeAuthorMetadata.class);
        }

        public List(JIPipeAuthorMetadata... authors) {
            super(JIPipeAuthorMetadata.class);
            for (JIPipeAuthorMetadata metadata : authors) {
                add(new JIPipeAuthorMetadata(metadata));
            }
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

    public static class Builder {
        private final JIPipeAuthorMetadata author = new JIPipeAuthorMetadata();

        public Builder title(String title) {
            author.setTitle(title);
            return this;
        }

        public Builder firstName(String firstName) {
            author.setFirstName(firstName);
            return this;
        }

        public Builder lastName(String lastName) {
            author.setLastName(lastName);
            return this;
        }

        public Builder affiliation(JIPipeOrganizationMetadata affiliation) {
            author.getAffiliations().add(affiliation);
            return this;
        }

        public Builder affiliations(Collection<JIPipeOrganizationMetadata> affiliations) {
            author.getAffiliations().addAll(affiliations);
            return this;
        }

        public Builder website(String website) {
            author.setWebsite(website);
            return this;
        }

        public Builder contact(String contact) {
            author.setContact(contact);
            return this;
        }

        public Builder email(String email) {
            author.setEmail(email);
            return this;
        }

        public Builder firstAuthor(boolean firstAuthor) {
            author.setFirstAuthor(firstAuthor);
            return this;
        }

        public Builder correspondingAuthor(boolean correspondingAuthor) {
            author.setCorrespondingAuthor(correspondingAuthor);
            return this;
        }

        public Builder orcid(String orcid) {
            author.setOrcid(orcid);
            return this;
        }

        public Builder customText(HTMLText customText) {
            author.setCustomText(customText);
            return this;
        }

        public JIPipeAuthorMetadata build() {
            return author;
        }
    }
}
