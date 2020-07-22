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
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.registries.JIPipeUINodeRegistry;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;

import java.util.Collection;
import java.util.List;

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
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<u><strong>").append(compartment.getName()).append("</strong></u><br/>");
        builder.append("Contains ").append(projectGraph.getAlgorithmsWithCompartment(compartment.getProjectCompartmentId()).size()).append(" algorithms<br/>");
        builder.append("<table>");
        for (JIPipeGraphNode algorithm : projectGraph.getAlgorithmsWithCompartment(compartment.getProjectCompartmentId())) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(JIPipeUINodeRegistry.getInstance().getIconURLFor(algorithm.getInfo()))
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
                        builder.append(StringUtils.createIconTextHTMLTableElement(JIPipeData.getNameOf(inputSlot), JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(inputSlot)));
                    }
                    builder.append("</td>");
                    builder.append("<td>");
                    if (outputSlot != null) {
                        builder.append(StringUtils.createRightIconTextHTMLTableElement(JIPipeData.getNameOf(outputSlot), JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(outputSlot)));
                    }
                    builder.append("</td>");
                    builder.append("</tr>");
                }
            }
        }

        // Write description
        String description = info.getDescription();
        if (description != null && !description.isEmpty())
            builder.append(HtmlEscapers.htmlEscaper().escape(description)).append("</br>");

        builder.append("</table>\n\n");

        // Write author information
        JIPipeDependency source = JIPipeNodeRegistry.getInstance().getSourceOf(info.getId());
        if (source != null) {
            builder.append("## Developer information\n\n");
            builder.append("<table>");
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
                    if (inputSlot != null) {
                        builder.append("<td>");
                        builder.append(StringUtils.createIconTextHTMLTableElement(JIPipeData.getNameOf(inputSlot), JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(inputSlot)));
                        builder.append("</td>");
                    }
                    if (outputSlot != null) {
                        builder.append("<td>");
                        builder.append(StringUtils.createRightIconTextHTMLTableElement(JIPipeData.getNameOf(outputSlot), JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(outputSlot)));
                        builder.append("</td>");
                    }
                    builder.append("</tr>");
                }
            }
        }

        // Write description
        String description = info.getDescription();
        if (description != null && !description.isEmpty())
            builder.append(StringUtils.wordWrappedHTMLElement(description, 50)).append("</br>");

        builder.append("</table>");

//        if (!preferredTraits.isEmpty()) {
//            builder.append("<br/><br/><strong>Good for<br/>");
//            insertTraitTable(builder, preferredTraits);
//        }
//        if (!unwantedTraits.isEmpty()) {
//            builder.append("<br/><br/><strong>Bad for<br/>");
//            insertTraitTable(builder, unwantedTraits);
//        }
//        if (!removedTraits.isEmpty()) {
//            builder.append("<br/><br/><strong>Removes<br/>");
//            insertTraitTable(builder, removedTraits);
//        }
//        if (!addedTraits.isEmpty()) {
//            builder.append("<br/><br/><strong>Adds<br/>");
//            insertTraitTable(builder, addedTraits);
//        }

        builder.append("</html>");
        return builder.toString();
    }

    //    public static String getTraitTooltip(Class<? extends JIPipeAnnotation> klass) {
//        String name = JIPipeAnnotation.getNameOf(klass);
//        String description = JIPipeAnnotation.getDescriptionOf(klass);
//        StringBuilder builder = new StringBuilder();
//        builder.append("<html><u><strong>");
//        builder.append(name);
//        builder.append("</u></strong>");
//        if(description != null && !description.isEmpty()) {
//            builder.append("<br/>")
//                .append(description);
//        }
//
//        Set<Class<? extends JIPipeAnnotation>> categories = JIPipeAnnotation.getCategoriesOf(klass);
//        if(!categories.isEmpty()) {
//            builder.append("<br/><br/>");
//            builder.append("<strong>Inherited annotations</strong><br/>");
//            builder.append("<table>");
//            for (Class<? extends JIPipeAnnotation> trait : categories) {
//                builder.append("<tr>");
//                builder.append("<td>").append("<img src=\"")
//                        .append(JIPipeUITraitRegistry.getInstance().getIconURLFor(trait))
//                        .append("\"/>").append("</td>");
//                builder.append("<td>").append(JIPipeAnnotation.getNameOf(trait)).append("</td>");
//                builder.append("</tr>");
//            }
//            builder.append("</table>");
//        }
//        builder.append("</html>");
//
//        return builder.toString();
//    }

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
                .append(JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(slot.getAcceptedDataType()))
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
                    .append(JIPipeUIDatatypeRegistry.getInstance().getIconURLFor(definition.getDataClass()))
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
