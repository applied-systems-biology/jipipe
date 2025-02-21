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

package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.html.HtmlEscapers;
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
    private StringList affiliations = new StringList();
    private String website;
    private String contact;
    private boolean firstAuthor;
    private boolean correspondingAuthor;
    private String orcid;
    private HTMLText customText = new HTMLText();

    /**
     * Creates a new instance
     */
    public JIPipeAuthorMetadata() {
    }

    /**
     * Initializes the instance
     *
     * @param title               the title (can be empty)
     * @param firstName           first name
     * @param lastName            last name
     * @param affiliations        list of affiliations
     * @param website             optional website link
     * @param contact             contact information, e.g., an E-Mail address
     * @param firstAuthor         if the author is marked as first author
     * @param correspondingAuthor if the author is marked as corresponding author
     */
    public JIPipeAuthorMetadata(String title, String firstName, String lastName, StringList affiliations, String website, String contact, String orcid, boolean firstAuthor, boolean correspondingAuthor) {
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.affiliations = affiliations;
        this.website = website;
        this.contact = contact;
        this.firstAuthor = firstAuthor;
        this.correspondingAuthor = correspondingAuthor;
        this.orcid = StringUtils.nullToEmpty(orcid);
    }

    public JIPipeAuthorMetadata(JIPipeAuthorMetadata other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.affiliations = new StringList(other.affiliations);
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
        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
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
            if(!StringUtils.isNullOrEmpty(author.getOrcid())) {
                stringBuilder.append("<div><strong>ORCID:</strong> <a href=\"").append(author.getOrcidUrl()).append("\">").append(author.getOrcidUrl()).append("</a></div>");
            }
            if (!StringUtils.isNullOrEmpty(author.getWebsite())) {
                stringBuilder.append("<div><strong>Website:</strong> <a href=\"").append(author.getWebsite()).append("\">").append(author.getWebsite()).append("</a></div>");
            }
            if (!author.getAffiliations().isEmpty()) {
                stringBuilder.append("<h2>Affiliations</h2>");
                stringBuilder.append("<ul>");
                for (String affiliation : author.getAffiliations()) {
                    stringBuilder.append("<li>").append(HtmlEscapers.htmlEscaper().escape(affiliation)).append("</li>");
                }
                stringBuilder.append("</ul>");
            }
            if (author.getCustomText() != null) {
                stringBuilder.append("<br/><br/>");
                stringBuilder.append(author.getCustomText().getBody());
            }
            JIPipeDesktopMarkdownReader reader = new JIPipeDesktopMarkdownReader(false, new MarkdownText(stringBuilder.toString()));
            tabPane.addTab(author.toString(), UIUtils.getIconFromResources("actions/im-user.png"), reader, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
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
        if(!StringUtils.isNullOrEmpty(orcid)) {
            if(orcid.startsWith("http") || orcid.contains("orcid.org")) {
                return orcid;
            }
            return "https://orcid.org/" + orcid;
        }
        return "";
    }

    @JIPipeParameter(value = "orcid", uiOrder = 45)
    @JsonGetter("orcid")
    @SetJIPipeDocumentation(name = "ORCID", description = "The ORCID (URL or ID)")
    @StringParameterSettings(monospace = true)
    public String getOrcid() {
        return orcid;
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

    @JIPipeParameter(value = "affiliations-list", uiOrder = 30)
    @SetJIPipeDocumentation(name = "Affiliations", description = "Author affiliations")
    @StringParameterSettings(multiline = true, monospace = true)
    @JsonGetter("affiliations-list")
    public StringList getAffiliations() {
        if (affiliations == null)
            affiliations = new StringList();
        return affiliations;
    }

    @JIPipeParameter("affiliations-list")
    @JsonSetter("affiliations-list")
    public void setAffiliations(StringList affiliations) {
        this.affiliations = affiliations;
    }

    @JIPipeParameter(value = "contact", uiOrder = 40)
    @StringParameterSettings(monospace = true)
    @SetJIPipeDocumentation(name = "Contact info", description = "Information on how to contact the author, for example an E-Mail address.")
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
}
