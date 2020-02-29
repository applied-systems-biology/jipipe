package org.hkijena.acaq5.utils;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class TooltipUtils {
    private TooltipUtils() {

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
        if(description != null && !description.isEmpty()) {
            builder.append("<br>").append(description).append("<br><br/>");
        }

        if(slot.isInput())
            builder.append("Input");
        else
            builder.append("Output");
        builder.append(" of ").append(slot.getAlgorithm().getName()).append("<br/>");

        // Show annotations
        if(withAnnotations) {
            Set<Class<? extends ACAQTrait>> traits = graph.getAlgorithmTraits().getOrDefault(slot, Collections.emptySet());
            if (traits != null && !traits.isEmpty()) {
                builder.append("<br/><br/><strong>Annotations<br/>");
                insertTraitTable(builder, traits);
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
        if(withTitle)
            builder.append("<u><strong>").append(declaration.getName()).append("</strong></u><br/>");

        // Write algorithm slot info
        builder.append("<table>");
        builder.append("<tr><td>");
        for(Class<? extends ACAQData> slot : declaration.getInputSlots().stream().map(AlgorithmInputSlot::value).collect(Collectors.toSet())) {
            builder.append("<img src=\"").append(ACAQUIDatatypeRegistry.getInstance().getIconURLFor(slot)).append("\"/>");
        }
        builder.append("</td>");
        builder.append("<td><img src=\"").append(ResourceUtils.getPluginResource("icons/chevron-right.png")).append("\" /></td>");
        for(Class<? extends ACAQData> slot : declaration.getOutputSlots().stream()
                .map(AlgorithmOutputSlot::value).collect(Collectors.toSet())) {
            builder.append("<img src=\"").append(ACAQUIDatatypeRegistry.getInstance().getIconURLFor(slot)).append("\"/>");
        }
        builder.append("</tr>");
        builder.append("</table>");

        // Write description
        String description = declaration.getDescription();
        if(description != null && !description.isEmpty())
            builder.append(description).append("</br>");

        Set<Class<? extends ACAQTrait>> preferredTraits = declaration.getPreferredTraits();
        Set<Class<? extends ACAQTrait>> unwantedTraits = declaration.getUnwantedTraits();
        Set<Class<? extends ACAQTrait>> addedTraits = declaration.getAddedTraits()
                .stream().map(AddsTrait::value).collect(Collectors.toSet());
        Set<Class<? extends ACAQTrait>> removedTraits = declaration.getRemovedTraits()
                .stream().map(RemovesTrait::value).collect(Collectors.toSet());

        if(!preferredTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Good for<br/>");
            insertTraitTable(builder, preferredTraits);
        }
        if(!unwantedTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Bad for<br/>");
            insertTraitTable(builder, unwantedTraits);
        }
        if(!removedTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Removes<br/>");
            insertTraitTable(builder, removedTraits);
        }
        if(!addedTraits.isEmpty()) {
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

    public static void insertTraitTable(StringBuilder builder, Set<Class<? extends ACAQTrait>> traits) {
        builder.append("<table>");
        for (Class<? extends ACAQTrait> trait : traits) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(ACAQUITraitRegistry.getInstance().getIconURLFor(trait))
                    .append("\"/>").append("</td>");
            builder.append("<td>").append(ACAQTrait.getNameOf(trait)).append("</td>");
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
        if(description != null && !description.isEmpty()) {
            builder.append("<br>").append(description).append("<br><br/>");
        }

        if(slot.isInput())
            builder.append("Input");
        else
            builder.append("Output");
        builder.append(" of ").append(slot.getAlgorithm().getName()).append("<br/>");

        builder.append("</html>");

        return builder.toString();
    }
}
