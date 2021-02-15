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

package org.hkijena.jipipe.ui.compendium;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.ui.components.JIPipeDataInfoListCellRenderer;
import org.hkijena.jipipe.ui.components.MarkdownDocument;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JIPipeDataTypeCompendiumUI extends JIPipeCompendiumUI<JIPipeDataInfo> {

    List<JIPipeDataInfo> dataInfos;

    public JIPipeDataTypeCompendiumUI() {
        super(MarkdownDocument.fromPluginResource("documentation/data-type-compendium.md"));
    }

    @Override
    protected List<JIPipeDataInfo> getFilteredItems() {
        if (dataInfos == null)
            dataInfos = JIPipe.getDataTypes().getRegisteredDataTypes().values().stream().map(JIPipeDataInfo::getInstance).sorted(Comparator.comparing(JIPipeDataInfo::getName)).collect(Collectors.toList());
        Predicate<JIPipeDataInfo> filterFunction = info -> getSearchField().test(info.getName() + " " + info.getDescription() + " " + info.getMenuPath());
        return dataInfos.stream().filter(filterFunction).collect(Collectors.toList());
    }

    @Override
    protected ListCellRenderer<JIPipeDataInfo> getItemListRenderer() {
        return new JIPipeDataInfoListCellRenderer();
    }

    @Override
    protected MarkdownDocument generateCompendiumFor(JIPipeDataInfo info) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(info.getName()).append("\n\n");

        builder.append(info.getDescription()).append("\n\n");

        // Trivial conversions
        builder.append("## Trivial conversions\n\n");
        builder.append("A trivial conversion involves no potentially expensive conversion operation. The data will directly match to input slots of the converted type. The edge has a dark-gray color.\n\n");

        builder.append("This type can be trivially converted into following types:\n\n");
        builder.append("<table>");
        {
            builder.append("<tr>");
            int column = 0;
            for (JIPipeDataInfo dataInfo : dataInfos) {
                if (dataInfo != info) {
                    if (JIPipeDatatypeRegistry.isTriviallyConvertible(info.getDataClass(), dataInfo.getDataClass())) {
                        builder.append("<td><img src=\"").append(JIPipe.getDataTypes().getIconURLFor(dataInfo)).append("\"/></td><td>").append(dataInfo.getName()).append("</td>");
                        ++column;
                        if (column % 5 == 0) {
                            builder.append("</tr><tr>");
                        }
                    }
                }
            }
            if (column == 0) {
                builder.append("<td>None</td>");
            }
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        builder.append("This type can be trivially converted from following types:\n\n");
        builder.append("<table>");
        {
            builder.append("<tr>");
            int column = 0;
            for (JIPipeDataInfo dataInfo : dataInfos) {
                if (dataInfo != info) {
                    if (JIPipeDatatypeRegistry.isTriviallyConvertible(dataInfo.getDataClass(), info.getDataClass())) {
                        builder.append("<td><img src=\"").append(JIPipe.getDataTypes().getIconURLFor(dataInfo)).append("\"/></td><td>").append(dataInfo.getName()).append("</td>");
                        ++column;
                        if (column % 5 == 0) {
                            builder.append("</tr><tr>");
                        }
                    }
                }
            }
            if (column == 0) {
                builder.append("<td>None</td>");
            }
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        builder.append("## Non-trivial conversions\n\n");
        builder.append("A non-trivial conversion is defined by the developer and might involve some more complex conversion operations. They are indicated as blue edge.\n\n");

        builder.append("This type can be also converted into following types:\n\n");
        {
            builder.append("<tr>");
            int column = 0;
            for (JIPipeDataInfo dataInfo : dataInfos) {
                if (dataInfo != info) {
                    if (!JIPipeDatatypeRegistry.isTriviallyConvertible(info.getDataClass(), dataInfo.getDataClass()) && JIPipe.getDataTypes().isConvertible(info.getDataClass(), dataInfo.getDataClass())) {
                        builder.append("<td><img src=\"").append(JIPipe.getDataTypes().getIconURLFor(dataInfo)).append("\"/></td><td>").append(dataInfo.getName()).append("</td>");
                        ++column;
                        if (column % 5 == 0) {
                            builder.append("</tr><tr>");
                        }
                    }
                }
            }
            if (column == 0) {
                builder.append("<td>None</td>");
            }
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        builder.append("This type can be also converted from following types:\n\n");
        builder.append("<table>");
        {
            builder.append("<tr>");
            int column = 0;
            for (JIPipeDataInfo dataInfo : dataInfos) {
                if (dataInfo != info) {
                    if (!JIPipeDatatypeRegistry.isTriviallyConvertible(dataInfo.getDataClass(), info.getDataClass()) && JIPipe.getDataTypes().isConvertible(dataInfo.getDataClass(), info.getDataClass())) {
                        builder.append("<td><img src=\"").append(JIPipe.getDataTypes().getIconURLFor(dataInfo)).append("\"/></td><td>").append(dataInfo.getName()).append("</td>");
                        ++column;
                        if (column % 5 == 0) {
                            builder.append("</tr><tr>");
                        }
                    }
                }
            }
            if (column == 0) {
                builder.append("<td>None</td>");
            }
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        // Info about the developer
        JIPipeDependency source = JIPipe.getDataTypes().getSourceOf(info.getId());
        if (source != null) {
            builder.append("# Developer information\n\n");
            builder.append("<table>");
            builder.append("<tr><td><strong>Data type ID</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(info.getId())).append("</td></tr>");
            for (String dependencyCitation : info.getAdditionalCitations()) {
                builder.append("<tr><td><strong>Data type additional citation</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(dependencyCitation)).append("</td></tr>");
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
}
