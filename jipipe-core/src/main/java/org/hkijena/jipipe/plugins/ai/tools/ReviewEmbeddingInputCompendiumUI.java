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

package org.hkijena.jipipe.plugins.ai.tools;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeEmbeddingDatabase;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDesktopCompendiumUI;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeInfoListCellRenderer;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A browsable compendium that shows the embedding input text for each node.
 * This allows reviewing what text is sent to the AI embedding model.
 */
public class ReviewEmbeddingInputCompendiumUI extends JIPipeDesktopCompendiumUI<JIPipeNodeInfo> {

    /**
     * Creates a new instance
     */
    public ReviewEmbeddingInputCompendiumUI() {
        super(new MarkdownText("# Review AI Embedding Input\n\n" +
                "This compendium shows the text that is sent to the AI embedding model for each node. " +
                "Select a node from the list to review its embedding input text.\n\n" +
                "The embedding text is generated from the node's name, description, menu location, " +
                "and input/output slot information."));
    }

    @Override
    protected List<JIPipeNodeInfo> getFilteredItems() {
        Predicate<JIPipeNodeInfo> filterFunction = info -> getSearchField().test(
                info.getName() + " " +
                        info.getAliases().stream()
                                .map(location -> location.getCategory().getName() + location.getMenuPath() + location.getAlternativeName())
                                .collect(Collectors.joining(" ")) +
                        " " + info.getDescription() + " " + info.getMenuPath());

        return JIPipe.getNodes().getRegisteredNodeInfos().values().stream()
                .filter(filterFunction)
                .sorted(Comparator.comparing(JIPipeNodeInfo::getName))
                .collect(Collectors.toList());
    }

    @Override
    protected ListCellRenderer<JIPipeNodeInfo> getItemListRenderer() {
        return new JIPipeNodeInfoListCellRenderer();
    }

    @Override
    public MarkdownText generateCompendiumFor(JIPipeNodeInfo info, boolean forJava) {
        StringBuilder builder = new StringBuilder();

        // Header with node name
        builder.append("# ").append(HtmlEscapers.htmlEscaper().escape(info.getName())).append("\n\n");

        // Node metadata table
        builder.append("## Node Information\n\n");
        builder.append("<table>");
        builder.append("<tr><td><strong>Node ID</strong></td><td><code>")
                .append(HtmlEscapers.htmlEscaper().escape(info.getId())).append("</code></td></tr>");
        builder.append("<tr><td><strong>Category</strong></td><td>")
                .append(HtmlEscapers.htmlEscaper().escape(info.getCategory().getName())).append("</td></tr>");
        builder.append("<tr><td><strong>Menu path</strong></td><td>")
                .append(HtmlEscapers.htmlEscaper().escape(info.getMenuPath())).append("</td></tr>");

        // Aliases
        if (!info.getAliases().isEmpty()) {
            builder.append("<tr><td><strong>Aliases</strong></td><td>");
            for (JIPipeNodeMenuLocation location : info.getAliases()) {
                builder.append(HtmlEscapers.htmlEscaper().escape(
                        location.getCategory().getName() + " > " +
                                String.join(" > ", location.getMenuPath().split("\n")) +
                                " > " + StringUtils.orElse(location.getAlternativeName(), info.getName())));
                builder.append("<br/>");
            }
            builder.append("</td></tr>");
        }

        builder.append("</table>\n\n");

        // Generate the embedding input text
        JIPipeNodeDatabaseEntry entry = new CreateNewNodeByInfoDatabaseEntry(info.getId(), info);
        String embeddingText = JIPipeEmbeddingDatabase.entryToText(entry);

        // Show the embedding text in a code block
        builder.append("## Embedding Input Text\n\n");
        builder.append("The following text is sent to the AI embedding model:\n\n");
        builder.append("```\n");
        builder.append(embeddingText);
        builder.append("\n```\n\n");

        // Show character count
        builder.append("<p><i>Text length: ").append(embeddingText.length()).append(" characters</i></p>\n\n");

        return new MarkdownText(builder.toString());
    }
}
