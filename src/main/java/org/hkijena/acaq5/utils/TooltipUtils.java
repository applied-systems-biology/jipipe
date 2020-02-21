package org.hkijena.acaq5.utils;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.util.Set;

public class TooltipUtils {
    private TooltipUtils() {

    }

    public static String getSlotInstanceTooltip(ACAQDataSlot<?> slot, ACAQAlgorithmGraph graph) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<table>");
        builder.append("<tr>");
        builder.append("<td>").append("<img src=\"")
                .append(ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconURLFor(slot.getAcceptedDataType()))
                .append("\"/>").append("</td>");
        builder.append("<td>").append(slot.getName()).append("</td>");
        builder.append("</tr>");
        builder.append("</table>");

        if(slot.isInput())
            builder.append("Input");
        else
            builder.append("Output");
        builder.append(" of ").append(slot.getAlgorithm().getName()).append("<br/>");

        // Show annotations
        Set<Class<? extends ACAQTrait>> traits = graph.getAlgorithmTraits().get(slot);
        if(traits != null && !traits.isEmpty()) {
            builder.append("<br/><br/><strong>Annotations<br/>");
            insertTraitTable(builder, traits);
        }

        builder.append("</html>");

        return builder.toString();
    }

    public static String getAlgorithmTooltip(Class<? extends ACAQAlgorithm> algorithmClass) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<u><strong>").append(ACAQAlgorithm.getNameOf(algorithmClass)).append("</strong></u><br/>");
        String description = ACAQAlgorithm.getDescriptionOf(algorithmClass);
        if(description != null && !description.isEmpty())
            builder.append(description).append("</br>");

        Set<Class<? extends ACAQTrait>> preferredTraits = ACAQRegistryService.getInstance().getAlgorithmRegistry().getPreferredTraitsOf(algorithmClass);
        Set<Class<? extends ACAQTrait>> unwantedTraits = ACAQRegistryService.getInstance().getAlgorithmRegistry().getUnwantedTraitsOf(algorithmClass);

        if(!preferredTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Good for<br/>");
            insertTraitTable(builder, preferredTraits);
        }
        if(!unwantedTraits.isEmpty()) {
            builder.append("<br/><br/><strong>Bad for<br/>");
            insertTraitTable(builder, unwantedTraits);
        }

        builder.append("</html>");
        return builder.toString();
    }

    public static String getTraitTooltip(Class<? extends ACAQTrait> klass) {
        String name = ACAQTrait.getNameOf(klass);
        String description = ACAQTrait.getDescriptionOf(klass);
        StringBuilder builder = new StringBuilder();
        builder.append("<html><u><strong>");
        builder.append(name);
        builder.append("</u></strong>");
        if(description != null && !description.isEmpty()) {
            builder.append("<br/>")
                .append(description);
        }

        Set<Class<? extends ACAQTrait>> categories = ACAQTrait.getCategoriesOf(klass);
        if(!categories.isEmpty()) {
            builder.append("<br/><br/>");
            builder.append("<strong>Inherited annotations</strong><br/>");
            builder.append("<table>");
            for (Class<? extends ACAQTrait> trait : categories) {
                builder.append("<tr>");
                builder.append("<td>").append("<img src=\"")
                        .append(ACAQRegistryService.getInstance().getUITraitRegistry().getIconURLFor(trait))
                        .append("\"/>").append("</td>");
                builder.append("<td>").append(ACAQTrait.getNameOf(trait)).append("</td>");
                builder.append("</tr>");
            }
            builder.append("</table>");
        }
        builder.append("</html>");

        return builder.toString();
    }

    public static void insertTraitTable(StringBuilder builder, Set<Class<? extends ACAQTrait>> traits) {
        builder.append("<table>");
        for (Class<? extends ACAQTrait> trait : traits) {
            builder.append("<tr>");
            builder.append("<td>").append("<img src=\"")
                    .append(ACAQRegistryService.getInstance().getUITraitRegistry().getIconURLFor(trait))
                    .append("\"/>").append("</td>");
            builder.append("<td>").append(ACAQTrait.getNameOf(trait)).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table>");
    }
}
