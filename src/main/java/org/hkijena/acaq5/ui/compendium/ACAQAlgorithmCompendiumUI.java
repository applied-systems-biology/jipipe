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

package org.hkijena.acaq5.ui.compendium;

import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQAuthorMetadata;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.ui.components.ACAQAlgorithmDeclarationListCellRenderer;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A browsable list of algorithms with full documentation
 */
public class ACAQAlgorithmCompendiumUI extends ACAQCompendiumUI<ACAQAlgorithmDeclaration> {

    /**
     * Creates a new instance
     */
    public ACAQAlgorithmCompendiumUI() {
        super(MarkdownDocument.fromPluginResource("documentation/algorithm-compendium.md"));
    }

    @Override
    protected List<ACAQAlgorithmDeclaration> getFilteredItems(String[] searchStrings) {
        Predicate<ACAQAlgorithmDeclaration> filterFunction = declaration -> {
            if (searchStrings != null && searchStrings.length > 0) {
                boolean matches = true;
                String name = declaration.getName() + " " + declaration.getDescription() + " " + declaration.getMenuPath();
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

        return ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values().stream().filter(filterFunction)
                .sorted(Comparator.comparing(ACAQAlgorithmDeclaration::getName)).collect(Collectors.toList());
    }

    @Override
    protected ListCellRenderer<ACAQAlgorithmDeclaration> getItemListRenderer() {
        return new ACAQAlgorithmDeclarationListCellRenderer();
    }

    @Override
    protected MarkdownDocument generateCompendiumFor(ACAQAlgorithmDeclaration declaration) {
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

        // Write parameter documentation
        builder.append("# Parameters").append("\nIn the following section, you will find a description of all parameters.\n\n");
        ACAQGraphNode algorithm = declaration.newInstance();
        ACAQParameterTree traversed = new ACAQParameterTree(algorithm);
        Map<ACAQParameterCollection, List<ACAQParameterAccess>> groupedBySource =
                traversed.getParameters().values().stream().collect(Collectors.groupingBy(ACAQParameterAccess::getSource));

        if (groupedBySource.containsKey(algorithm)) {
            groupedBySource.get(algorithm).sort(Comparator.comparing(ACAQParameterAccess::getUIOrder).thenComparing(ACAQParameterAccess::getName));
            for (ACAQParameterAccess parameterAccess : groupedBySource.get(algorithm)) {
                generateParameterDocumentation(parameterAccess, builder);
                builder.append("\n\n");
            }
        }
        for (ACAQParameterCollection subParameters : groupedBySource.keySet().stream()
                .sorted(Comparator.nullsFirst(Comparator.comparing(traversed::getSourceDocumentationName))).collect(Collectors.toList())) {
            if (subParameters == algorithm)
                continue;
            ACAQParameterVisibility sourceVisibility = traversed.getSourceVisibility(subParameters);
            builder.append("## ").append(traversed.getSourceDocumentationName(subParameters)).append("\n\n");
            ACAQDocumentation documentation = traversed.getSourceDocumentation(subParameters);
            if (documentation != null) {
                builder.append(documentation.description()).append("\n\n");
            }
            for (ACAQParameterAccess parameterAccess : groupedBySource.get(subParameters)) {
                ACAQParameterVisibility visibility = parameterAccess.getVisibility();
                if (!visibility.isVisibleIn(sourceVisibility))
                    continue;
                generateParameterDocumentation(parameterAccess, builder);
                builder.append("\n\n");
            }
        }


        // Write author information
        ACAQDependency source = ACAQAlgorithmRegistry.getInstance().getSourceOf(declaration.getId());
        if (source != null) {
            builder.append("# Developer information\n\n");
            builder.append("<table>");
            builder.append("<tr><td><strong>Plugin name</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getName())).append("</td></tr>");
            for (ACAQAuthorMetadata author : source.getMetadata().getAuthors()) {
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

    private void generateParameterDocumentation(ACAQParameterAccess access, StringBuilder builder) {
        builder.append("### ").append(access.getName()).append("\n\n");
        ACAQParameterTypeDeclaration declaration = ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(access.getFieldClass());
        if (declaration != null) {
            builder.append("<table><tr>");
            builder.append("<td><img src=\"").append(ResourceUtils.getPluginResource("icons/wrench.png")).append("\" /></td>");
            builder.append("<td><strong>").append(HtmlEscapers.htmlEscaper().escape(declaration.getName())).append("</strong>: ");
            builder.append(HtmlEscapers.htmlEscaper().escape(declaration.getDescription())).append("</td></tr></table>\n\n");
        }
        if (access.getDescription() != null && !access.getDescription().isEmpty()) {
            builder.append(access.getDescription());
        } else {
            builder.append("No description provided.");
        }
    }
}
