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

package org.hkijena.jipipe.utils;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;

import java.util.Collection;
import java.util.Set;

/**
 * Utilities that generate tooltips or other text from JIPipe data data structures
 */
public class TooltipUtils {
    private TooltipUtils() {

    }

    /**
     * Creates a tooltip for a project compartment
     *
     * @param compartment  the compartment
     * @param projectGraph the project graph
     * @return tooltip
     */
    public static String getProjectCompartmentTooltip(JIPipeProjectCompartment compartment, JIPipeGraph projectGraph) {
        Set<JIPipeGraphNode> algorithmsWithCompartment = projectGraph.getNodesWithinCompartment(compartment.getProjectCompartmentUUID());
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<u><strong>").append(compartment.getName()).append("</strong></u><br/>");
        builder.append("Contains ").append(algorithmsWithCompartment.size()).append(" algorithms<br/>");
        builder.append("<table>");
        for (JIPipeGraphNode algorithm : algorithmsWithCompartment) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(JIPipe.getNodes().getIconURLFor(algorithm.getInfo()))
                    .append("\"/>").append("</td>");
            builder.append("<td>").append(algorithm.getName()).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table>");
        builder.append("</html>");

        return builder.toString();
    }

    /**
     * Creates a tooltip for an algorithm. Has a title
     *
     * @param info the algorithm type
     * @return the tooltip
     */
    public static String getAlgorithmTooltip(JIPipeNodeInfo info) {
        return getAlgorithmTooltip(info, true);
    }

    public static MarkdownDocument getAlgorithmDocumentation(JIPipeNodeInfo info) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(info.getName()).append("\n\n");

        // Write description
        String description = info.getDescription().wrap(50).getBody();
        if (description != null && !description.isEmpty())
            builder.append(description).append("</br>");

        // Write algorithm slot info
        builder.append("<table style=\"margin-top: 10px;\">");
        for (JIPipeInputSlot slot : info.getInputSlots()) {
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
            builder.append("</tr>");
        }
        for (JIPipeOutputSlot slot : info.getOutputSlots()) {
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#da4453; color:white;border:3px solid #da4453;border-radius:5px;text-align:center;\">Output</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        builder.append("\n\n");


        // Write author information
        JIPipeDependency source = JIPipe.getNodes().getSourceOf(info.getId());
        if (source != null) {
            builder.append("## Developer information\n\n");
            builder.append("<table>");
            builder.append("<tr><td><strong>Node type ID</strong></td><td><code>").append(HtmlEscapers.htmlEscaper().escape(info.getId())).append("</code></td></tr>");
            for (String dependencyCitation : info.getAdditionalCitations()) {
                builder.append("<tr><td><strong>Node additional citation</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(dependencyCitation)).append("</td></tr>");
            }
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

    public static MarkdownDocument getAlgorithmDocumentation(JIPipeGraphNode node) {
        JIPipeNodeInfo info = node.getInfo();
        StringBuilder builder = new StringBuilder();
        builder.append("<h1>").append(info.getName()).append("</h1>\n\n");

        // Write description
        String description = info.getDescription().getBody();
        if (description != null && !description.isEmpty())
            builder.append(description).append("</br>");

        builder.append("\n\n");

        // Write algorithm slot info
        builder.append("<table style=\"margin-top: 10px;\">");
        for (JIPipeDataSlot slot : node.getInputSlots()) {
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.getAcceptedDataType())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(slot.getName())).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.getAcceptedDataType()).getName())).append(")</i></td>");
            builder.append("</tr>");
        }
        for (JIPipeDataSlot slot : node.getOutputSlots()) {
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#da4453; color:white;border:3px solid #da4453;border-radius:5px;text-align:center;\">Output</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.getAcceptedDataType())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(slot.getName())).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.getAcceptedDataType()).getName())).append(")</i></td>");
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        // Write author information
        JIPipeDependency source = JIPipe.getNodes().getSourceOf(info.getId());
        if (source != null) {
            builder.append("## Developer information\n\n");
            builder.append("<table>");
            builder.append("<tr><td><strong>Node type ID</strong></td><td><code>").append(HtmlEscapers.htmlEscaper().escape(info.getId())).append("</code></td></tr>");
            if (node.getGraph() != null) {
                builder.append("<tr><td><strong>Node UUID</strong></td><td><code>").append(HtmlEscapers.htmlEscaper().escape(node.getUUIDInGraph().toString())).append("</code></td></tr>");
                builder.append("<tr><td><strong>Node alias ID</strong></td><td><code>").append(HtmlEscapers.htmlEscaper().escape(node.getAliasIdInGraph())).append("</code></td></tr>");
                builder.append("<tr><td><strong>Compartment UUID</strong></td><td><code>").append(HtmlEscapers.htmlEscaper().escape(node.getCompartmentUUIDInGraphAsString())).append("</code></td></tr>");
            }
            for (String dependencyCitation : info.getAdditionalCitations()) {
                builder.append("<tr><td><strong>Node additional citation</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(dependencyCitation)).append("</td></tr>");
            }
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

    /**
     * Creates a tooltip for an algorithm
     *
     * @param info      the algorithm
     * @param withTitle if a title is displayed
     * @return the tooltip
     */
    public static String getAlgorithmTooltip(JIPipeNodeInfo info, boolean withTitle) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        if (withTitle)
            builder.append("<u><strong>").append(info.getName()).append("</strong></u><br/>");

        // Write description
        String description = info.getDescription().wrap(50).getBody();
        if (description != null && !description.isEmpty())
            builder.append(description).append("</br>");

        // Write algorithm slot info
        builder.append("<table style=\"margin-top: 10px;\">");
        for (JIPipeInputSlot slot : info.getInputSlots()) {
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
            builder.append("</tr>");
        }
        for (JIPipeOutputSlot slot : info.getOutputSlots()) {
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#da4453; color:white;border:3px solid #da4453;border-radius:5px;text-align:center;\">Output</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        builder.append("</html>");
        return builder.toString();
    }

    /**
     * Creates a tooltip for an algorithm
     *
     * @param node      the algorithm
     * @param withTitle if a title is displayed
     * @return the tooltip
     */
    public static String getAlgorithmTooltip(JIPipeGraphNode node, boolean withTitle) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        if (withTitle)
            builder.append("<p style=\"margin-bottom:10px;\"><u><strong>").append(node.getName()).append("</strong></u></p><br/><br/>");

        // Write description
        String description = node.getCustomDescription().wrap(50).getBody();
        if (description != null && !description.isEmpty())
            builder.append(description).append("</br>");

        // Write algorithm slot info
        builder.append("<table style=\"margin-top: 10px;\">");
        for (JIPipeDataSlot slot : node.getInputSlots()) {
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.getAcceptedDataType())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(slot.getName())).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.getAcceptedDataType()).getName())).append(")</i></td>");
            builder.append("</tr>");
        }
        for (JIPipeDataSlot slot : node.getOutputSlots()) {
            builder.append("<tr>");
            builder.append("<td><p style=\"background-color:#da4453; color:white;border:3px solid #da4453;border-radius:5px;text-align:center;\">Output</p></td>");
            builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.getAcceptedDataType())).append("\"/></td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(slot.getName())).append("</td>");
            builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.getAcceptedDataType()).getName())).append(")</i></td>");
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        builder.append("</html>");
        return builder.toString();
    }

    /**
     * Creates a tooltip for an {@link JIPipeDataSlot}
     *
     * @param slot the slot
     * @return tooltip
     */
    public static String getSlotInstanceTooltip(JIPipeDataSlot slot) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<table>");
        builder.append("<tr>");
        builder.append("<td>").append("<img src=\"")
                .append(JIPipe.getDataTypes().getIconURLFor(slot.getAcceptedDataType()))
                .append("\"/>").append("</td>");
        builder.append("<td>").append(slot.getName()).append("</td>");
        builder.append("</tr>");
        builder.append("</table>");

        builder.append("<br>Data type: <i>").append(JIPipeData.getNameOf(slot.getAcceptedDataType())).append("</i><br>");
        String description = JIPipeData.getDescriptionOf(slot.getAcceptedDataType());
        if (description != null && !description.isEmpty()) {
            builder.append("<br>").append(description).append("<br><br/>");
        }

        if (slot.isInput())
            builder.append("Input");
        else
            builder.append("Output");
        builder.append(" of ").append(slot.getNode().getName()).append("<br/>");

        builder.append("</html>");

        return builder.toString();
    }


    /**
     * Creates a slot definition table.
     * Has a HTML root
     *
     * @param slotDefinitions the slots
     * @return the tooltip
     */
    public static String getSlotTable(Collection<JIPipeDataSlotInfo> slotDefinitions) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><table>");
        for (JIPipeDataSlotInfo definition : slotDefinitions) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(JIPipe.getDataTypes().getIconURLFor(definition.getDataClass()))
                    .append("\"/>").append("</td>");
            builder.append("<td>").append(!StringUtils.isNullOrEmpty(definition.getName()) ? definition.getName() : JIPipeData.getNameOf(definition.getDataClass())).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table></html>");
        return builder.toString();
    }

    /**
     * Creates a tooltip for data
     *
     * @param info the data type
     * @return the tooltip
     */
    public static String getDataTooltip(JIPipeDataInfo info) {
        return "<html><u><strong>" + info.getName() + "</strong></u><br/>" + HtmlEscapers.htmlEscaper().escape(info.getDescription()) + "</html>";
    }
}

