package org.hkijena.acaq5.utils;

import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
        for (ACAQAlgorithm algorithm : projectGraph.getAlgorithmsWithCompartment(compartment.getProjectCompartmentId())) {
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
     * Creates a tooltip for an {@link ACAQDataSlot}
     *
     * @param slot            the slot
     * @param withAnnotations if annotations should be displayed
     * @return the toopltip
     */
    public static String getSlotInstanceTooltip(ACAQDataSlot slot, boolean withAnnotations) {
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

        // Show annotations
        if (withAnnotations) {
            Set<ACAQTraitDeclaration> slotAnnotations = slot.getSlotAnnotations();
            if (!slotAnnotations.isEmpty()) {
                builder.append("<br/><br/><strong>Annotations<br/>");
                insertTraitTable(builder, slotAnnotations);
            }
        }

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

        Set<ACAQTraitDeclaration> preferredTraits = declaration.getPreferredTraits();
        Set<ACAQTraitDeclaration> unwantedTraits = declaration.getUnwantedTraits();
        Set<ACAQTraitDeclaration> addedTraits = declaration.getSlotTraitConfiguration().getAddedTraits();
        Set<ACAQTraitDeclaration> removedTraits = declaration.getSlotTraitConfiguration().getRemovedTraits();

        if (!preferredTraits.isEmpty() || !unwantedTraits.isEmpty())
            insertOpposingTraitTableContent(builder, preferredTraits, "Good for", unwantedTraits, "Bad for");
        if (!addedTraits.isEmpty() || !removedTraits.isEmpty())
            insertOpposingTraitTableContent(builder, addedTraits, "Adds", removedTraits, "Removes");

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

        Set<ACAQTraitDeclaration> preferredTraits = declaration.getPreferredTraits();
        Set<ACAQTraitDeclaration> unwantedTraits = declaration.getUnwantedTraits();
        Set<ACAQTraitDeclaration> addedTraits = declaration.getSlotTraitConfiguration().getAddedTraits();
        Set<ACAQTraitDeclaration> removedTraits = declaration.getSlotTraitConfiguration().getRemovedTraits();

        if (!preferredTraits.isEmpty() || !unwantedTraits.isEmpty())
            insertOpposingTraitTableContent(builder, preferredTraits, "Good for", unwantedTraits, "Bad for");
        if (!addedTraits.isEmpty() || !removedTraits.isEmpty())
            insertOpposingTraitTableContent(builder, addedTraits, "Adds", removedTraits, "Removes");

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

    //    public static String getTraitTooltip(Class<? extends ACAQTrait> klass) {
//        String name = ACAQTrait.getNameOf(klass);
//        String description = ACAQTrait.getDescriptionOf(klass);
//        StringBuilder builder = new StringBuilder();
//        builder.append("<html><u><strong>");
//        builder.append(name);
//        builder.append("</u></strong>");
//        if(description != null && !description.isEmpty()) {
//            builder.append("<br/>")
//                .append(description);
//        }
//
//        Set<Class<? extends ACAQTrait>> categories = ACAQTrait.getCategoriesOf(klass);
//        if(!categories.isEmpty()) {
//            builder.append("<br/><br/>");
//            builder.append("<strong>Inherited annotations</strong><br/>");
//            builder.append("<table>");
//            for (Class<? extends ACAQTrait> trait : categories) {
//                builder.append("<tr>");
//                builder.append("<td>").append("<img src=\"")
//                        .append(ACAQUITraitRegistry.getInstance().getIconURLFor(trait))
//                        .append("\"/>").append("</td>");
//                builder.append("<td>").append(ACAQTrait.getNameOf(trait)).append("</td>");
//                builder.append("</tr>");
//            }
//            builder.append("</table>");
//        }
//        builder.append("</html>");
//
//        return builder.toString();
//    }

    /**
     * Creates a table that has two columns of {@link ACAQTraitDeclaration}
     *
     * @param builder     the string builder
     * @param leftTraits  left traits
     * @param leftTitle   left title
     * @param rightTraits right traits
     * @param rightTitle  right title
     */
    public static void insertOpposingTraitTableContent(StringBuilder builder, Set<ACAQTraitDeclaration> leftTraits, String leftTitle, Set<ACAQTraitDeclaration> rightTraits, String rightTitle) {
//        builder.append("<table>");
        builder.append("<tr><td><i>").append(leftTitle).append("</i></td><td><i>").append(rightTitle).append("</i></td></tr>");
        Iterator<ACAQTraitDeclaration> leftIterator = leftTraits.iterator();
        Iterator<ACAQTraitDeclaration> rightIterator = rightTraits.iterator();
        for (int i = 0; i < Math.max(leftTraits.size(), rightTraits.size()); ++i) {
            builder.append("<tr>");

            builder.append("<td>");
            if (i < leftTraits.size()) {
                ACAQTraitDeclaration declaration = leftIterator.next();
                builder.append(StringUtils.createIconTextHTMLTableElement(declaration.getName(), ACAQUITraitRegistry.getInstance().getIconURLFor(declaration)));
            }
            builder.append("</td>");
            builder.append("<td>");
            if (i < rightTraits.size()) {
                ACAQTraitDeclaration declaration = rightIterator.next();
                builder.append(StringUtils.createIconTextHTMLTableElement(declaration.getName(), ACAQUITraitRegistry.getInstance().getIconURLFor(declaration)));
            }
            builder.append("</td>");

            builder.append("</tr>");
        }
//        builder.append("</table>");
    }

    /**
     * Creates a table of {@link ACAQTraitDeclaration}
     *
     * @param builder builder
     * @param traits  the traits
     */
    public static void insertTraitTable(StringBuilder builder, Set<ACAQTraitDeclaration> traits) {
        builder.append("<table>");
        for (ACAQTraitDeclaration trait : traits) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(ACAQUITraitRegistry.getInstance().getIconURLFor(trait))
                    .append("\"/>").append("</td>");
            builder.append("<td>").append(trait.getName()).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table>");
    }

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
     * Gets a tooltip for an {@link ACAQTraitDeclaration}
     *
     * @param trait the trait type
     * @return the tooltip
     */
    public static String getTraitTooltip(ACAQTraitDeclaration trait) {
        String name = trait.getName();
        String description = trait.getDescription();
        StringBuilder builder = new StringBuilder();
        builder.append("<html><u><strong>");
        builder.append(name);
        builder.append("</u></strong>");

        if (trait.isDiscriminator()) {
            builder.append("<br/><u>This annotation type allows to store a discriminating value</u><br/>");
        }

        if (description != null && !description.isEmpty()) {
            builder.append("<br/>")
                    .append(description);
        }

        Set<ACAQTraitDeclaration> categories = trait.getInherited();
        if (!categories.isEmpty()) {
            builder.append("<br/><br/>");
            builder.append("<strong>Inherited annotations</strong><br/>");
            builder.append("<table>");
            for (ACAQTraitDeclaration inherited : categories) {
                builder.append("<tr>");
                builder.append("<td>").append("<img src=\"")
                        .append(ACAQUITraitRegistry.getInstance().getIconURLFor(inherited))
                        .append("\"/>").append("</td>");
                builder.append("<td>").append(inherited.getName()).append("</td>");
                builder.append("</tr>");
            }
            builder.append("</table>");
        }
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
     * Creates a trait declaration table
     * Has a HTML root
     *
     * @param traitDeclarations the declarations
     * @return the tooltip
     */
    public static String getTraitTable(Collection<ACAQTraitDeclaration> traitDeclarations) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><table>");
        for (ACAQTraitDeclaration definition : traitDeclarations) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(ACAQUITraitRegistry.getInstance().getIconURLFor(definition))
                    .append("\"/>").append("</td>");
            builder.append("<td>").append(definition.getName()).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table></html>");
        return builder.toString();
    }
}
