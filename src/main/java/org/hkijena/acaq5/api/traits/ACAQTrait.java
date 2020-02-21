package org.hkijena.acaq5.api.traits;

import com.google.common.reflect.TypeToken;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQDocumentation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base interface for an algorithm data trait
 * Traits are generated by data slots and accumulated over algorithm depth.
 * Algorithms can modify output traits by adding or removing them.
 */
public interface ACAQTrait {

    /**
     * Returns the name of given trait
     * @param klass
     * @return
     */
    static String getNameOf(Class<? extends ACAQTrait> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if(annotations.length > 0) {
            return annotations[0].name();
        }
        else {
            return klass.getSimpleName();
        }
    }

    /**
     * Returns the description of given trait
     * @param klass
     * @return
     */
    static String getDescriptionOf(Class<? extends ACAQTrait> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if(annotations.length > 0) {
            return annotations[0].description();
        }
        else {
            return null;
        }
    }

    /**
     * Returns all inherited traits marked as category
     * @param klass
     * @return
     */
    static Set<Class<? extends ACAQTrait>> getCategoriesOf(Class<? extends ACAQTrait> klass) {
        Set<Class<? extends ACAQTrait>> result = new HashSet<>();
        for(TypeToken<?> type : TypeToken.of(klass).getTypes().interfaces()) {
            if(type.getRawType().getAnnotationsByType(CategoryTrait.class).length > 0) {
                result.add((Class<? extends ACAQTrait>) type.getRawType());
            }
        }
        return result;
    }

    /**
     * Returns true if the trait is hidden from the user
     * @param klass
     * @return
     */
    static boolean isHidden(Class<? extends ACAQTrait> klass) {
        return klass.getAnnotationsByType(CategoryTrait.class).length > 0;
    }

    static String getTooltipOf(Class<? extends ACAQTrait> klass) {
        String name = getNameOf(klass);
        String description = getDescriptionOf(klass);
        StringBuilder builder = new StringBuilder();
        builder.append("<html><u><strong>");
        builder.append(name);
        builder.append("</u></strong>");
        if(description != null && !description.isEmpty()) {
            builder.append("<br/>")
                .append(description);
        }

        Set<Class<? extends ACAQTrait>> categories = getCategoriesOf(klass);
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
}
