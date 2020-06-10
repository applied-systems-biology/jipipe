package org.hkijena.acaq5.ui.compendium;

import com.google.common.collect.Comparators;
import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.components.ACAQAlgorithmDeclarationListCellRenderer;
import org.hkijena.acaq5.ui.components.ACAQTraitDeclarationListCellRenderer;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hkijena.acaq5.utils.TooltipUtils.insertOpposingTraitTableContent;

/**
 * A browsable list of traits with full documentation
 */
public class ACAQTraitCompendiumUI extends ACAQCompendiumUI<ACAQTraitDeclaration> {

    /**
     * Creates a new instance
     */
    public ACAQTraitCompendiumUI() {
        super(MarkdownDocument.fromPluginResource("documentation/trait-compendium.md"));
    }

    @Override
    protected List<ACAQTraitDeclaration> getFilteredItems(String[] searchStrings) {
        Predicate<ACAQTraitDeclaration> filterFunction = declaration -> {
            if (searchStrings != null && searchStrings.length > 0) {
                boolean matches = true;
                String name = declaration.getName() + " " + declaration.getDescription() + " " + declaration.getId();
                for (String searchString : searchStrings) {
                    if (!name.toLowerCase().contains(searchString.toLowerCase())) {
                        matches = false;
                        break;
                    }
                }
                return matches;
            } else {
                return true;
            }
        };

        return ACAQTraitRegistry.getInstance().getRegisteredTraits().values().stream().filter(filterFunction)
                .sorted(Comparator.comparing(ACAQTraitDeclaration::isHidden).thenComparing(ACAQTraitDeclaration::getName)).collect(Collectors.toList());
    }

    @Override
    protected ListCellRenderer<ACAQTraitDeclaration> getItemListRenderer() {
        return new ACAQTraitDeclarationListCellRenderer();
    }

    @Override
    protected MarkdownDocument generateCompendiumFor(ACAQTraitDeclaration declaration) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(declaration.getName()).append("\n\n");

        builder.append("\n\nUnique ID: `").append(declaration.getId()).append("`\n\n");

        if(declaration.isHidden()) {
            builder.append("\n\nThis annotation type was marked as 'Hidden' it cannot be assigned manually in some cases.\n\n");
        }

        // Write description
        String description = declaration.getDescription();
        if (description != null && !description.isEmpty())
            builder.append(HtmlEscapers.htmlEscaper().escape(description)).append("</br>");

        // Write parameter documentation

        if(!declaration.getInherited().isEmpty()) {
            builder.append("# Inherited annotations\n\n");
            builder.append("<table>");
            builder.append("<tr><td></td><td>Name</td><td>ID</td><td>Description</td></tr>");
            for (ACAQTraitDeclaration inherited : declaration.getInherited()) {
                builder.append("<tr>");
                builder.append("<td><img src=\"").append(ACAQUITraitRegistry.getInstance().getIconURLFor(inherited)).append("\"/></td>");
                builder.append("<td><strong>").append(HtmlEscapers.htmlEscaper().escape(inherited.getName())).append("</strong></td>");
                builder.append("<td><i>").append(HtmlEscapers.htmlEscaper().escape(inherited.getId())).append("</i></td>");
                builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(inherited.getDescription())).append("</td>");
                builder.append("</tr>");
            }
            builder.append("</table>\n\n");
        }

        // Write author information
        ACAQDependency source = ACAQTraitRegistry.getInstance().getSourceOf(declaration.getId());
        if (source != null) {
            builder.append("\n# Developer information\n\n");
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
}
