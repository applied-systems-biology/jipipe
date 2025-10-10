/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.documentation;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.documentation.JIPipeDataCrateMetadataEntry;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.service.components.JIPipeDatatypesServiceComponent;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopDataInfoListCellRenderer;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JIPipeDataTypeCompendiumUI extends JIPipeDesktopCompendiumUI<JIPipeDataInfo> {

    protected List<JIPipeDataInfo> dataInfos;

    public JIPipeDataTypeCompendiumUI() {
        super(MarkdownText.fromPluginResource("documentation/data-type-compendium.md", new HashMap<>()));
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
        return new JIPipeDesktopDataInfoListCellRenderer();
    }

    @Override
    public MarkdownText generateCompendiumFor(JIPipeDataInfo info, boolean forJava) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(info.getName()).append("\n\n");

        builder.append(info.getDescription()).append("\n\n");

        // Trivial conversions
        builder.append("## Trivial conversions\n\n");
        builder.append("A trivial conversion involves no potentially expensive conversion operation. The data will directly match to input slots of the converted type. The edge has a dark-gray color.\n\n");

        builder.append("This type can be trivially converted into following types:\n\n");

        if (forJava) {
            builder.append("<table>");
            {
                builder.append("<tr>");
                int column = 0;
                for (JIPipeDataInfo dataInfo : dataInfos) {
                    if (dataInfo != info) {
                        if (JIPipeDatatypesServiceComponent.isTriviallyConvertible(info.getDataClass(), dataInfo.getDataClass())) {
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
        } else {
            builder.append("<ul>");
            {
                int column = 0;
                for (JIPipeDataInfo dataInfo : dataInfos) {
                    if (dataInfo != info) {
                        if (JIPipeDatatypesServiceComponent.isTriviallyConvertible(info.getDataClass(), dataInfo.getDataClass())) {
                            builder.append("<li>");
                            builder.append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(dataInfo)).append("\"/><span>").append(dataInfo.getName()).append("</span>");
                            builder.append("</li>\n");
                            ++column;
                        }
                    }
                }
                if (column == 0) {
                    builder.append("<li>None</li>");
                }
            }
            builder.append("</ul>\n\n");
        }

        builder.append("This type can be trivially converted from following types:\n\n");
        if (forJava) {
            builder.append("<table>");
            {
                builder.append("<tr>");
                int column = 0;
                for (JIPipeDataInfo dataInfo : dataInfos) {
                    if (dataInfo != info) {
                        if (JIPipeDatatypesServiceComponent.isTriviallyConvertible(dataInfo.getDataClass(), info.getDataClass())) {
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
        } else {
            builder.append("<ul>");
            {
                int column = 0;
                for (JIPipeDataInfo dataInfo : dataInfos) {
                    if (dataInfo != info) {
                        if (JIPipeDatatypesServiceComponent.isTriviallyConvertible(dataInfo.getDataClass(), info.getDataClass())) {
                            builder.append("<li>");
                            builder.append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(dataInfo)).append("\"/><span>").append(dataInfo.getName()).append("</span>");
                            builder.append("</li>\n");
                            ++column;
                        }
                    }
                }
                if (column == 0) {
                    builder.append("<li>None</li>");
                }
            }
            builder.append("</ul>\n\n");
        }

        builder.append("## Non-trivial conversions\n\n");
        builder.append("A non-trivial conversion is defined by the developer and might involve some more complex conversion operations. They are indicated as blue edge.\n\n");

        builder.append("This type can be also converted into following types:\n\n");
        if (forJava) {
            builder.append("<table>");
            {
                builder.append("<tr>");
                int column = 0;
                for (JIPipeDataInfo dataInfo : dataInfos) {
                    if (dataInfo != info) {
                        if (!JIPipeDatatypesServiceComponent.isTriviallyConvertible(info.getDataClass(), dataInfo.getDataClass()) && JIPipe.getDataTypes().isConvertible(info.getDataClass(), dataInfo.getDataClass())) {
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
        } else {
            builder.append("<ul>");
            {

                int column = 0;
                for (JIPipeDataInfo dataInfo : dataInfos) {
                    if (dataInfo != info) {
                        if (!JIPipeDatatypesServiceComponent.isTriviallyConvertible(info.getDataClass(), dataInfo.getDataClass()) && JIPipe.getDataTypes().isConvertible(info.getDataClass(), dataInfo.getDataClass())) {
                            builder.append("<li>");
                            builder.append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(dataInfo)).append("\"/><span>").append(dataInfo.getName()).append("</span>");
                            builder.append("</li>\n");
                            ++column;
                        }
                    }
                }
                if (column == 0) {
                    builder.append("<li>None</li>");
                }
            }
            builder.append("</ul>\n\n");
        }

        builder.append("This type can be also converted from following types:\n\n");
        if (forJava) {
            builder.append("<table>");
            {
                builder.append("<tr>");
                int column = 0;
                for (JIPipeDataInfo dataInfo : dataInfos) {
                    if (dataInfo != info) {
                        if (!JIPipeDatatypesServiceComponent.isTriviallyConvertible(dataInfo.getDataClass(), info.getDataClass()) && JIPipe.getDataTypes().isConvertible(dataInfo.getDataClass(), info.getDataClass())) {
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
        } else {
            builder.append("<ul>");
            {
                int column = 0;
                for (JIPipeDataInfo dataInfo : dataInfos) {
                    if (dataInfo != info) {
                        if (!JIPipeDatatypesServiceComponent.isTriviallyConvertible(dataInfo.getDataClass(), info.getDataClass()) && JIPipe.getDataTypes().isConvertible(dataInfo.getDataClass(), info.getDataClass())) {
                            builder.append("<li>");
                            builder.append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(dataInfo)).append("\"/><span>").append(dataInfo.getName()).append("</span>");
                            builder.append("</li>\n");
                            ++column;
                        }
                    }
                }
                if (column == 0) {
                    builder.append("<li>None</li>");
                }

            }
            builder.append("</ul>\n\n");
        }

        // Storage information
        builder.append("## Data storage (JIPipe data crate)\n\n");
        if (info.getDataCrate().isEmpty()) {
            builder.append("<table>");
            builder.append("<tr><td><strong>ID</strong></td>");
            builder.append("<td><strong>Type</strong></td>");
            builder.append("<td><strong>Protocol</strong></td>");
            builder.append("<td><strong>Path</strong></td>");
            builder.append("<td><strong>Name</strong></td>");
            builder.append("<td><strong>Description</strong></td>");
            builder.append("<td><strong>Encoding format</strong></td></tr>");
            for (JIPipeDataCrateMetadataEntry entry : info.getDataCrate().getEntries()) {
                builder.append("<tr>");
                builder.append("<td>").append(entry.getId()).append("</td>");
                builder.append("<td>").append(entry.getType()).append("</td>");
                builder.append("<td>").append(entry.getIdProtocol()).append("</td>");
                builder.append("<td>").append(entry.getIdPath()).append("</td>");
                builder.append("<td>").append(entry.getName()).append("</td>");
                builder.append("<td>").append(entry.getDescription()).append("</td>");
                builder.append("<td>").append(JsonUtils.toJsonString(entry.getEncodingFormat())).append("</td>");
                builder.append("</tr>");
            }
            builder.append("</table>");
        } else {
            builder.append("* the storage is empty (structural or abstract data type)");
        }
        builder.append("\n\n");

        // ImageJ conversion information
        builder.append("## ImageJ conversion\n\n");
        builder.append("<ul>");
        {
            ImageJDataImporter importer = JIPipe.getImageJAdapters().getDefaultImporterFor(info.getDataClass());
            builder.append("<li>Default importer: ");
            builder.append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(importer.getImportedJIPipeDataType())).append("\"/><span>").append(importer.getName()).append("</span>");
            builder.append("</li>");
        }
        {
            ImageJDataExporter exporter = JIPipe.getImageJAdapters().getDefaultExporterFor(info.getDataClass());
            builder.append("<li>Default exporter: ");
            builder.append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(exporter.getExportedJIPipeDataType())).append("\"/><span>").append(exporter.getName()).append("</span>");
            builder.append("</li>");
        }
        builder.append("</ul>\n\n");

        builder.append("### Available importers\n\n");
        builder.append("<table>");
        for (ImageJDataImporter importer : JIPipe.getImageJAdapters().getAvailableImporters(info.getDataClass(), true)) {
            builder.append("<tr>");
            builder.append("<td><img src=\"").append(JIPipe.getDataTypes().getIconURLFor(importer.getImportedJIPipeDataType())).append("\"/></td><td>").append(HtmlEscapers.htmlEscaper().escape(importer.getName())).append("</td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(importer.getDescription())).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        builder.append("### Available exporters\n\n");
        builder.append("<table>");
        for (ImageJDataExporter exporter : JIPipe.getImageJAdapters().getAvailableExporters(info.getDataClass(), true)) {
            builder.append("<tr>");
            builder.append("<td><img src=\"").append(JIPipe.getDataTypes().getIconURLFor(exporter.getExportedJIPipeDataType())).append("\"/></td><td>").append(HtmlEscapers.htmlEscaper().escape(exporter.getName())).append("</td>");
            builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(exporter.getDescription())).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table>\n\n");

        // Info about the developer
        JIPipeDependency source = JIPipe.getDataTypes().getSourceOf(info.getId());
        if (source != null) {
//            System.out.println(info.getId());
            builder.append("## Developer information\n\n");
            builder.append("<table>");
            builder.append("<tr><td><strong>Data type ID</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(info.getId())).append("</td></tr>");
            for (String dependencyCitation : info.getAdditionalCitations()) {
                builder.append("<tr><td><strong>Refer to/Also cite</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(dependencyCitation)).append("</td></tr>");
            }
            builder.append("<tr><td><strong>Plugin name</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getName())).append("</td></tr>");
            if (source instanceof JIPipeJavaPlugin && ((JIPipeJavaPlugin) source).isBeta()) {
                builder.append("<tr><td><strong>Plugin status</strong></td><td>The plugin is currently in beta-testing. There might be extensive changes in future updates.</td></tr>");
            }
            for (JIPipeAuthorMetadata author : source.getMetadata().getAuthors()) {
                builder.append("<tr><td><strong>Plugin author</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(author.toString())).append("</td></tr>");
            }
            builder.append("<tr><td><strong>Plugin website</strong></td><td><a href=\"").append(source.getMetadata().getWebsite()).append("\">")
                    .append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getWebsite())).append("</a></td></tr>");
            builder.append("<tr><td><strong>Plugin citation</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getCitation())).append("</td></tr>");
            builder.append("<tr><td><strong>Plugin license</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(source.getMetadata().getLicense())).append("</td></tr>");
            for (String dependencyCitation : source.getMetadata().getDependencyCitations()) {
                builder.append("<tr><td><strong>Also cite</strong></td><td>").append(HtmlEscapers.htmlEscaper().escape(dependencyCitation)).append("</td></tr>");
            }
            builder.append("</table>");
        }

        return new MarkdownText(builder.toString());
    }
}
