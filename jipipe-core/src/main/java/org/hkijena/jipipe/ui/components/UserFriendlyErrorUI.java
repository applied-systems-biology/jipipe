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

package org.hkijena.jipipe.ui.components;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyException;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Displays exceptions in a user-friendly way.
 * Can handle {@link org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException} and {@link JIPipeIssueReport}
 */
public class UserFriendlyErrorUI extends FormPanel {

    private List<Entry> entries = new ArrayList<>();

    /**
     * @param helpDocument the help document to be displayed. If null, no help is displayed
     * @param flags        FormPanel flags
     */
    public UserFriendlyErrorUI(MarkdownDocument helpDocument, int flags) {
        super(helpDocument, flags);
    }

    /**
     * Adds an exception to the display.
     * Does not add the necessary vertical glue.
     *
     * @param e the exception
     */
    public void displayErrors(Throwable e) {
        if (e instanceof UserFriendlyException) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            UserFriendlyException exception = (UserFriendlyException) e;
            addEntry(new Entry(exception.getUserWhat(),
                    exception.getUserWhere(),
                    exception.getUserWhy(),
                    exception.getUserHow(),
                    writer.toString()));
        } else {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            addEntry(new Entry("Internal error",
                    "Internal plugin, JIPipe, or ImageJ code.",
                    "Cannot be determined. But the response was: " + e.getMessage(),
                    "Please check if your input data has the right format and is not corrupted. " +
                            "Try to use the quick run on input algorithms and check if the generated data satisfies the problematic algorithm's assumptions." +
                            "If you cannot solve the issue yourself, contact the JIPipe or the algorithm/plugin developer.",
                    writer.toString()));
        }
        if (e.getCause() instanceof Exception) {
            displayErrors((Exception) e.getCause());
        }
    }

    /**
     * Adds a report to the display
     * Does not add the necessary vertical glue.
     *
     * @param report the report
     */
    public void displayErrors(JIPipeIssueReport report) {
        for (Map.Entry<String, JIPipeIssueReport.Issue> entry : report.getIssues().entries()) {
            String key = entry.getKey();
            JIPipeIssueReport.Issue issue = entry.getValue();
            addEntry(new Entry(issue.getUserWhat(),
                    key,
                    issue.getUserWhy(),
                    issue.getUserHow(),
                    issue.getDetails()));
        }
    }

    /**
     * Adds an entry to the UI
     *
     * @param entry the entry
     */
    public void addEntry(Entry entry) {
        entries.add(entry);
        addWideToForm(new EntryUI(entry), null);
    }

    /**
     * Entry to be displayed
     */
    public static class Entry {
        private final String userWhat;
        private final String userWhere;
        private final String userWhy;
        private final String userHow;
        private final String details;

        /**
         * @param userWhat  what happened
         * @param userWhere where it happened
         * @param userWhy   why it happened
         * @param userHow   how to solve the issue
         * @param details   optional details. Can be null.
         */
        public Entry(String userWhat, String userWhere, String userWhy, String userHow, String details) {
            this.userWhat = userWhat;
            this.userWhere = userWhere;
            this.userWhy = userWhy;
            this.userHow = userHow;
            this.details = details;
        }

        public String getUserWhat() {
            return userWhat;
        }

        public String getUserWhere() {
            return userWhere;
        }

        public String getUserWhy() {
            return userWhy;
        }

        public String getUserHow() {
            return userHow;
        }

        public String getDetails() {
            return details;
        }
    }

    /**
     * UI for {@link Entry}
     */
    public static class EntryUI extends FormPanel {

        public static final String[] CSS_RULES = {"body { font-family: \"Sans-serif\"; }",
                "pre { background-color: #f5f2f0; border: 3px #f5f2f0 solid; }",
                "code { background-color: #f5f2f0; }",
                "h1 { font-size: 20px; }",
                "h2 { padding-top: 30px; }",
                "h3 { padding-top: 30px; }",
                "th { border-bottom: 1px solid #c8c8c8; }",
                ".toc-list { list-style: none; }"};

        private final Entry entry;

        /**
         * @param entry the entry
         */
        public EntryUI(Entry entry) {
            super(null, NONE);
            this.entry = entry;
            initialize();
        }

        private void initialize() {
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                    BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true)));

            String markdown = "<table><tr><td><img src=\"" + ResourceUtils.getPluginResource("icons-32/error.png") +
                    "\" /></td><td><strong>" + HtmlEscapers.htmlEscaper().escape(entry.getUserWhat()) + "</strong></td></tr></table>" +
                    "<table>" +
                    "<tr><td><span style=\"color: blue\">Where?</span></td><td>" +
                    StringUtils.wordWrappedHTMLElement(entry.getUserWhere(), 70) + "</td></tr>" +
                    "<tr><td><span style=\"color: blue\">Why?</span></td><td>" +
                    StringUtils.wordWrappedHTMLElement(entry.getUserWhy(), 70) + "</td></tr>" +
                    "<tr><td><span style=\"color: green\">Solution?</span></td><td>" +
                    StringUtils.wordWrappedHTMLElement(entry.getUserHow(), 70) + "</td></tr>" +
                    "</table>";
            JTextPane mainMessageReader = UIUtils.makeMarkdownReader(new MarkdownDocument(markdown), CSS_RULES);
            mainMessageReader.setEditable(false);
            mainMessageReader.setOpaque(false);

            addWideToForm(mainMessageReader, null);

            if (!StringUtils.isNullOrEmpty(entry.getDetails())) {
                GroupHeaderPanel groupHeaderPanel = addGroupHeader("Please provide these technical details on contacting a developer", UIUtils.getIconFromResources("actions/help-info.png"));
                JToggleButton showDetailsButton = new JToggleButton("Show details");
                groupHeaderPanel.addColumn(showDetailsButton);
                JTextArea details = UIUtils.makeReadonlyTextArea(entry.getDetails());
                addWideToForm(details, null);
                details.setVisible(false);
                showDetailsButton.addActionListener(e -> details.setVisible(showDetailsButton.isSelected()));
            }
        }
    }
}
