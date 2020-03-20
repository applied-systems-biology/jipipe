package org.hkijena.acaq5.utils;

import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;

import java.util.List;
import java.util.Set;

public class TooltipUtils {
    private TooltipUtils() {

    }

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

    public static String getSlotInstanceTooltip(ACAQDataSlot slot, ACAQAlgorithmGraph graph, boolean withAnnotations) {
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

    public static String getAlgorithmTooltip(ACAQAlgorithmDeclaration declaration) {
        return getAlgorithmTooltip(declaration, true);
    }

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
                builder.append("<tr><td></td><td><i>Input</i></td><td><i>Output</i></td><td></td></tr>");
                for (int i = 0; i < displayedSlots; ++i) {
                    Class<? extends ACAQData> inputSlot = i < inputSlots.size() ? inputSlots.get(i).value() : null;
                    Class<? extends ACAQData> outputSlot = i < inputSlots.size() ? outputSlots.get(i).value() : null;
                    builder.append("<tr>");
                    if (inputSlot != null) {
                        builder.append("<td><img src=\"").append(ACAQUIDatatypeRegistry.getInstance().getIconURLFor(inputSlot)).append("\"/></td>");
                        builder.append("<td>").append(ACAQData.getNameOf(inputSlot)).append("</td>");
                    }
                    if (outputSlot != null) {
                        builder.append("<td>").append(ACAQData.getNameOf(outputSlot)).append("</td>");
                        builder.append("<td><img src=\"").append(ACAQUIDatatypeRegistry.getInstance().getIconURLFor(outputSlot)).append("\"/></td>");
                    }
                    builder.append("</tr>");
                }
            }
        }
        builder.append("</table>");

        // Write description
        String description = declaration.getDescription();
        if (description != null && !description.isEmpty())
            builder.append(description).append("</br>");

        Set<ACAQTraitDeclaration> preferredTraits = declaration.getPreferredTraits();
        Set<ACAQTraitDeclaration> unwantedTraits = declaration.getUnwantedTraits();
        Set<ACAQTraitDeclaration> addedTraits = declaration.getSlotTraitConfiguration().getAddedTraits();
        Set<ACAQTraitDeclaration> removedTraits = declaration.getSlotTraitConfiguration().getRemovedTraits();

        if (!preferredTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Good for<br/>");
            insertTraitTable(builder, preferredTraits);
        }
        if (!unwantedTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Bad for<br/>");
            insertTraitTable(builder, unwantedTraits);
        }
        if (!removedTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Removes<br/>");
            insertTraitTable(builder, removedTraits);
        }
        if (!addedTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Adds<br/>");
            insertTraitTable(builder, addedTraits);
        }

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
}
