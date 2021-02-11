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

package org.hkijena.jipipe.ui.compendium;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.ui.components.JIPipeNodeInfoListCellRenderer;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A browsable list of algorithms with full documentation
 */
public class JIPipeAlgorithmCompendiumUI extends JIPipeCompendiumUI<JIPipeNodeInfo> {

    /**
     * Creates a new instance
     */
    public JIPipeAlgorithmCompendiumUI() {
        super(MarkdownDocument.fromPluginResource("documentation/algorithm-compendium.md"));
    }

    @Override
    protected List<JIPipeNodeInfo> getFilteredItems() {
        Predicate<JIPipeNodeInfo> filterFunction = info -> getSearchField().test(info.getName() + " " + info.getDescription() + " " + info.getMenuPath());

        return JIPipe.getNodes().getRegisteredNodeInfos().values().stream().filter(filterFunction)
                .sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList());
    }

    @Override
    protected ListCellRenderer<JIPipeNodeInfo> getItemListRenderer() {
        return new JIPipeNodeInfoListCellRenderer();
    }

    @Override
    protected MarkdownDocument generateCompendiumFor(JIPipeNodeInfo info) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(info.getName()).append("\n\n");
        // Write algorithm slot info
        builder.append("<table>");
        {
            List<JIPipeInputSlot> inputSlots = info.getInputSlots();
            List<JIPipeOutputSlot> outputSlots = info.getOutputSlots();

            int displayedSlots = Math.max(inputSlots.size(), outputSlots.size());
            if (displayedSlots > 0) {
                builder.append("<tr><td><i>Input</i></td><td><i>Output</i></td></tr>");
                for (int i = 0; i < displayedSlots; ++i) {
                    Class<? extends JIPipeData> inputSlot = i < inputSlots.size() ? inputSlots.get(i).value() : null;
                    Class<? extends JIPipeData> outputSlot = i < outputSlots.size() ? outputSlots.get(i).value() : null;
                    builder.append("<tr>");
                    builder.append("<td>");
                    if (inputSlot != null) {
                        builder.append(StringUtils.createIconTextHTMLTableElement(JIPipeData.getNameOf(inputSlot), JIPipe.getDataTypes().getIconURLFor(inputSlot)));
                    }
                    builder.append("</td>");
                    builder.append("<td>");
                    if (outputSlot != null) {
                        builder.append(StringUtils.createRightIconTextHTMLTableElement(JIPipeData.getNameOf(outputSlot), JIPipe.getDataTypes().getIconURLFor(outputSlot)));
                    }
                    builder.append("</td>");
                    builder.append("</tr>");
                }
            }
        }
        builder.append("</table>\n\n");

        // Write description
        String description = info.getDescription().getBody();
        if (description != null && !description.isEmpty())
            builder.append(description).append("</br>");


        // Write parameter documentation
        builder.append("# Parameters").append("\nIn the following section, you will find a description of all parameters.\n\n");
        JIPipeGraphNode algorithm = info.newInstance();
        JIPipeParameterTree traversed = new JIPipeParameterTree(algorithm);
        Map<JIPipeParameterCollection, List<JIPipeParameterAccess>> groupedBySource =
                traversed.getParameters().values().stream().collect(Collectors.groupingBy(JIPipeParameterAccess::getSource));

        if (groupedBySource.containsKey(algorithm)) {
            groupedBySource.get(algorithm).sort(Comparator.comparing(JIPipeParameterAccess::getUIOrder).thenComparing(JIPipeParameterAccess::getName));
            for (JIPipeParameterAccess parameterAccess : groupedBySource.get(algorithm)) {
                generateParameterDocumentation(parameterAccess, builder);
                builder.append("\n\n");
            }
        }
        for (JIPipeParameterCollection subParameters : groupedBySource.keySet().stream()
                .sorted(Comparator.nullsFirst(Comparator.comparing(traversed::getSourceDocumentationName))).collect(Collectors.toList())) {
            if (subParameters == algorithm)
                continue;
            JIPipeParameterVisibility sourceVisibility = traversed.getSourceVisibility(subParameters);
            builder.append("## ").append(traversed.getSourceDocumentationName(subParameters)).append("\n\n");
            JIPipeDocumentation documentation = traversed.getSourceDocumentation(subParameters);
            if (documentation != null) {
                builder.append(documentation.description()).append("\n\n");
            }
            for (JIPipeParameterAccess parameterAccess : groupedBySource.get(subParameters)) {
                JIPipeParameterVisibility visibility = parameterAccess.getVisibility();
                if (!visibility.isVisibleIn(sourceVisibility))
                    continue;
                generateParameterDocumentation(parameterAccess, builder);
                builder.append("\n\n");
            }
        }


        // Write author information
        JIPipeDependency source = JIPipe.getNodes().getSourceOf(info.getId());
        if (source != null) {
            builder.append("# Developer information\n\n");
            builder.append("<table>");
            builder.append("<tr><td><strong>Node type ID</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(info.getId())).append("</td></tr>");
            builder.append("<tr><td><strong>Plugin name</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getName())).append("</td></tr>");
            for (JIPipeAuthorMetadata author : source.getMetadata().getAuthors()) {
                builder.append("<tr><td><strong>Plugin author</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(author.getFirstName() + " " + author.getLastName())).append("</td></tr>");
            }
            builder.append("<tr><td><strong>Plugin website</strong></td><td><a href=\"").append(source.getMetadata().getWebsite()).append("\">")
                    .append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getWebsite())).append("</a></td></tr>");
            builder.append("<tr><td><strong>Plugin citation</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getCitation())).append("</td></tr>");
            builder.append("<tr><td><strong>Plugin license</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getLicense())).append("</td></tr>");
            for (String dependencyCitation : source.getMetadata().getDependencyCitations()) {
                builder.append("<tr><td><strong>Additional citation</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(dependencyCitation)).append("</td></tr>");
            }
            builder.append("</table>");
        }

        return new MarkdownDocument(builder.toString());
    }

    private void generateParameterDocumentation(JIPipeParameterAccess access, StringBuilder builder) {
        builder.append("### ").append(access.getName()).append("\n\n");
        builder.append("<table><tr>");
        builder.append("<td><img src=\"").append(ResourceUtils.getPluginResource("icons/actions/dialog-xml-editor.png")).append("\" /></td>");
        builder.append("<td><strong>Unique identifier</strong>: <code>");
        builder.append(HtmlEscapers.htmlEscaper().escape(access.getKey())).append("</code></td></tr>\n\n");

        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(access.getFieldClass());
        if (info != null) {
            builder.append("<td><img src=\"").append(ResourceUtils.getPluginResource("icons/data-types/data-type.png")).append("\" /></td>");
            builder.append("<td><strong>").append(HtmlEscapers.htmlEscaper().escape(info.getName())).append("</strong>: ");
            builder.append(HtmlEscapers.htmlEscaper().escape(info.getDescription())).append("</td></tr>");
        }
        builder.append("</table>\n\n");
        if (access.getDescription() != null && !access.getDescription().isEmpty()) {
            builder.append(access.getDescription());
        } else {
            builder.append("No description provided.");
        }
    }
}
