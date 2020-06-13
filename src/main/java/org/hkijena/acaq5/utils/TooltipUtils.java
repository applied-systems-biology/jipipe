package org.hkijena.acaq5.utils;

import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

import java.util.Collection;
import java.util.List;

/**
 * Utilities that generate tooltips or other text from ACAQ5 data data structures
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
    public static String getProjectCompartmentTooltip(ACAQProjectCompartment compartment, ACAQAlgorithmGraph projectGraph) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<u><strong>").append(compartment.getName()).append("</strong></u><br/>");
        builder.append("Contains ").append(projectGraph.getAlgorithmsWithCompartment(compartment.getProjectCompartmentId()).size()).append(" algorithms<br/>");
        builder.append("<table>");
        for (ACAQGraphNode algorithm : projectGraph.getAlgorithmsWithCompartment(compartment.getProjectCompartmentId())) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(ACAQUIDatatypeRegistry.getInstance().getIconURLFor(algorithm.getCategory()))
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
     * @param declaration the algorithm type
     * @return the tooltip
     */
    public static String getAlgorithmTooltip(ACAQAlgorithmDeclaration declaration) {
        return getAlgorithmTooltip(declaration, true);
    }

    public static MarkdownDocument getAlgorithmDocumentation(ACAQAlgorithmDeclaration declaration) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(declaration.getName()).append("\n\n");
        // Write algorithm slot info
        builder.append("<table>");
        {
            List<AlgorithmInputSlot> inputSlots = declaration.getInputSlots();
            List<AlgorithmOutputSlot> outputSlots = declaration.getOutputSlots();

            int displayedSlots = Math.max(inputSlots.size(), outputSlots.size());
            if (displayedSlots > 0) {
                builder.append("<tr><td><i>Input</i></td><td><i>Output</i></td></tr>");
                for (int i = 0; i < displayedSlots; ++i) {
                    Class<? extends ACAQData> inputSlot = i < inputSlots.size() ? inputSlots.get(i).value() : null;
                    Class<? extends ACAQData> outputSlot = i < outputSlots.size() ? outputSlots.get(i).value() : null;
                    builder.append("<tr>");
                    builder.append("<td>");
                    if (inputSlot != null) {
                        builder.append(StringUtils.createIconTextHTMLTableElement(ACAQData.getNameOf(inputSlot), ACAQUIDatatypeRegistry.getInstance().getIconURLFor(inputSlot)));
                    }
                    builder.append("</td>");
                    builder.append("<td>");
                    if (outputSlot != null) {
                        builder.append(StringUtils.createRightIconTextHTMLTableElement(ACAQData.getNameOf(outputSlot), ACAQUIDatatypeRegistry.getInstance().getIconURLFor(outputSlot)));
                    }
                    builder.append("</td>");
                    builder.append("</tr>");
                }
            }
        }

        // Write description
        String description = declaration.getDescription();
        if (description != null && !description.isEmpty())
            builder.append(HtmlEscapers.htmlEscaper().escape(description)).append("</br>");

        builder.append("</table>\n\n");

        // Write author information
        ACAQDependency source = ACAQAlgorithmRegistry.getInstance().getSourceOf(declaration.getId());
        if (source != null) {
            builder.append("## Developer information\n\n");
            builder.append("<table>");
            builder.append("<tr><td><strong>Plugin name</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getName())).append("</td></tr>");
            builder.append("<tr><td><strong>Plugin authors</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getAuthors())).append("</td></tr>");
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
     * @param declaration the algorithm
     * @param withTitle   if a title is displayed
     * @return the tooltip
     */
    public static String getAlgorithmTooltip(ACAQAlgorithmDeclaration declaration, boolean withTitle) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        if (withTitle)
            builder.append("<u><strong>").append(declaration.getName()).append("</strong></u><br/>");

        // Write algorithm slot info
        builder.append("<table>");
        {
            List<AlgorithmInputSlot> inputSlots = declaration.getInputSlots();
            List<AlgorithmOutputSlot> outputSlots = declaration.getOutputSlots();

            int displayedSlots = Math.max(inputSlots.size(), outputSlots.size());
            if (displayedSlots > 0) {
                builder.append("<tr><td><i>Input</i></td><td><i>Output</i></td></tr>");
                for (int i = 0; i < displayedSlots; ++i) {
                    Class<? extends ACAQData> inputSlot = i < inputSlots.size() ? inputSlots.get(i).value() : null;
                    Class<? extends ACAQData> outputSlot = i < outputSlots.size() ? outputSlots.get(i).value() : null;
                    builder.append("<tr>");
                    if (inputSlot != null) {
                        builder.append("<td>");
                        builder.append(StringUtils.createIconTextHTMLTableElement(ACAQData.getNameOf(inputSlot), ACAQUIDatatypeRegistry.getInstance().getIconURLFor(inputSlot)));
                        builder.append("</td>");
                    }
                    if (outputSlot != null) {
                        builder.append("<td>");
                        builder.append(StringUtils.createRightIconTextHTMLTableElement(ACAQData.getNameOf(outputSlot), ACAQUIDatatypeRegistry.getInstance().getIconURLFor(outputSlot)));
                        builder.append("</td>");
                    }
                    builder.append("</tr>");
                }
            }
        }

        // Write description
        String description = declaration.getDescription();
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

    //    public static String getTraitTooltip(Class<? extends ACAQAnnotation> klass) {
//        String name = ACAQAnnotation.getNameOf(klass);
//        String description = ACAQAnnotation.getDescriptionOf(klass);
//        StringBuilder builder = new StringBuilder();
//        builder.append("<html><u><strong>");
//        builder.append(name);
//        builder.append("</u></strong>");
//        if(description != null && !description.isEmpty()) {
//            builder.append("<br/>")
//                .append(description);
//        }
//
//        Set<Class<? extends ACAQAnnotation>> categories = ACAQAnnotation.getCategoriesOf(klass);
//        if(!categories.isEmpty()) {
//            builder.append("<br/><br/>");
//            builder.append("<strong>Inherited annotations</strong><br/>");
//            builder.append("<table>");
//            for (Class<? extends ACAQAnnotation> trait : categories) {
//                builder.append("<tr>");
//                builder.append("<td>").append("<img src=\"")
//                        .append(ACAQUITraitRegistry.getInstance().getIconURLFor(trait))
//                        .append("\"/>").append("</td>");
//                builder.append("<td>").append(ACAQAnnotation.getNameOf(trait)).append("</td>");
//                builder.append("</tr>");
//            }
//            builder.append("</table>");
//        }
//        builder.append("</html>");
//
//        return builder.toString();
//    }

    /**
     * Creates a tooltip for an {@link ACAQDataSlot}
     *
     * @param slot the slot
     * @return tooltip
     */
    public static String getSlotInstanceTooltip(ACAQDataSlot slot) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<table>");
        builder.append("<tr>");
        builder.append("<td>").append("<img src=\"")
                .append(ACAQUIDatatypeRegistry.getInstance().getIconURLFor(slot.getAcceptedDataType()))
                .append("\"/>").append("</td>");
        builder.append("<td>").append(slot.getName()).append("</td>");
        builder.append("</tr>");
        builder.append("</table>");

        builder.append("<br>Data type: <i>").append(ACAQData.getNameOf(slot.getAcceptedDataType())).append("</i><br>");
        String description = ACAQData.getDescriptionOf(slot.getAcceptedDataType());
        if (description != null && !description.isEmpty()) {
            builder.append("<br>").append(description).append("<br><br/>");
        }

        if (slot.isInput())
            builder.append("Input");
        else
            builder.append("Output");
        builder.append(" of ").append(slot.getAlgorithm().getName()).append("<br/>");

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
    public static String getSlotTable(Collection<ACAQSlotDefinition> slotDefinitions) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><table>");
        for (ACAQSlotDefinition definition : slotDefinitions) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(ACAQUIDatatypeRegistry.getInstance().getIconURLFor(definition.getDataClass()))
                    .append("\"/>").append("</td>");
            builder.append("<td>").append(!StringUtils.isNullOrEmpty(definition.getName()) ? definition.getName() : ACAQData.getNameOf(definition.getDataClass())).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table></html>");
        return builder.toString();
    }

    /**
     * Creates a tooltip for data
     *
     * @param declaration the data type
     * @return the tooltip
     */
    public static String getDataTooltip(ACAQDataDeclaration declaration) {
        return "<html><u><strong>" + declaration.getName() + "</strong></u><br/>" + HtmlEscapers.htmlEscaper().escape(declaration.getDescription()) + "</html>";
    }
}
