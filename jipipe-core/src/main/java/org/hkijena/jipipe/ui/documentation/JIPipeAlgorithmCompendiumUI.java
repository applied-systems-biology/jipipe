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

package org.hkijena.jipipe.ui.documentation;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.renderers.JIPipeNodeInfoListCellRenderer;
import org.hkijena.jipipe.utils.DocumentationUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.util.Comparator;
import java.util.HashMap;
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
        super(MarkdownDocument.fromPluginResource("documentation/algorithm-compendium.md", new HashMap<>()));
    }

    @Override
    protected List<JIPipeNodeInfo> getFilteredItems() {
        Predicate<JIPipeNodeInfo> filterFunction = info -> getSearchField().test(info.getName() + " " + info.getAliases().stream().map(location -> location.getCategory().getName() + location.getMenuPath() + location.getAlternativeName()).collect(Collectors.joining(" ")) + " " + info.getDescription() + " " + info.getMenuPath());

        return JIPipe.getNodes().getRegisteredNodeInfos().values().stream().filter(filterFunction)
                .sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList());
    }

    @Override
    protected ListCellRenderer<JIPipeNodeInfo> getItemListRenderer() {
        return new JIPipeNodeInfoListCellRenderer();
    }

    @Override
    public MarkdownDocument generateCompendiumFor(JIPipeNodeInfo info, boolean forJava) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(info.getName()).append("\n\n");

        if(!info.getAliases().isEmpty()) {
            builder.append("<p>");
            for (JIPipeNodeMenuLocation location : info.getAliases()) {
                builder.append("<i>Alias: ").append(location.getCategory().getName()).append(" &gt; ").append(String.join(" &gt; ", location.getMenuPath().split("\n"))).append(" &gt; ").append(StringUtils.orElse(location.getAlternativeName(), info.getName())).append("<i>\n");
            }
            builder.append("</p><br/>\n\n");
        }

        // Write description
        String description = info.getDescription().getBody();
        if (description != null && !description.isEmpty())
            builder.append(description).append("</br>");

        // Write algorithm slot info
        builder.append("<table style=\"margin-top: 10px;\">");
        for (JIPipeInputSlot slot : info.getInputSlots()) {
            JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(slot.value());
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(dataInfo.getName() + ": " + dataInfo.getDescription())).append(")</i>");
            if (!StringUtils.isNullOrEmpty(slot.description())) {
                builder.append(" ").append(HtmlEscapers.htmlEscaper().escape(slot.description()));
            }
            builder.append("</td></tr>");
        }
        for (JIPipeOutputSlot slot : info.getOutputSlots()) {
            JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(slot.value());
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#da4453; color:white;border:3px solid #da4453;border-radius:5px;text-align:center;\">Output</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(dataInfo.getName() + ": " + dataInfo.getDescription())).append(")</i>");
            if (!StringUtils.isNullOrEmpty(slot.description())) {
                builder.append(" ").append(HtmlEscapers.htmlEscaper().escape(slot.description()));
            }
            builder.append("</td></tr>");
        }
        builder.append("</table>\n\n");


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
            builder.append("## ").append(traversed.getSourceDocumentationName(subParameters)).append("\n\n");
            JIPipeDocumentation documentation = traversed.getSourceDocumentation(subParameters);
            if (documentation != null) {
                builder.append(DocumentationUtils.getDocumentationDescription(documentation)).append("\n\n");
            }
            for (JIPipeParameterAccess parameterAccess : groupedBySource.get(subParameters)) {
                if (!algorithm.isParameterUIVisible(traversed, parameterAccess))
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
            for (String dependencyCitation : info.getAdditionalCitations()) {
                builder.append("<tr><td><strong>Refer to/Also cite</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(dependencyCitation)).append("</td></tr>");
            }
            builder.append("<tr><td><strong>Plugin name</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getName())).append("</td></tr>");
            for (JIPipeAuthorMetadata author : source.getMetadata().getAuthors()) {
                builder.append("<tr><td><strong>Plugin author</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(author.toString())).append("</td></tr>");
            }
            builder.append("<tr><td><strong>Plugin website</strong></td><td><a href=\"").append(source.getMetadata().getWebsite()).append("\">")
                    .append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getWebsite())).append("</a></td></tr>");
            builder.append("<tr><td><strong>Plugin citation</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getCitation())).append("</td></tr>");
            builder.append("<tr><td><strong>Plugin license</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getLicense())).append("</td></tr>");
            for (String dependencyCitation : source.getMetadata().getDependencyCitations()) {
                builder.append("<tr><td><strong>Also cite</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(dependencyCitation)).append("</td></tr>");
            }
            builder.append("</table>");
        }

        return new MarkdownDocument(builder.toString());
    }

    private void generateParameterDocumentation(JIPipeParameterAccess access, StringBuilder builder) {
        builder.append("### ").append(access.getName()).append("\n\n");
        builder.append("<table>");

        if (access.isImportant()) {
            builder.append("<tr><td><img src=\"").append(ResourceUtils.getPluginResource("icons/emblems/important.png")).append("\" /></td>");
            builder.append("<td><strong>Important parameter</strong>: The developer marked this parameter as especially important</td></tr>\n\n");
        }

        builder.append("<tr><td><img src=\"").append(ResourceUtils.getPluginResource("icons/actions/dialog-xml-editor.png")).append("\" /></td>");
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
