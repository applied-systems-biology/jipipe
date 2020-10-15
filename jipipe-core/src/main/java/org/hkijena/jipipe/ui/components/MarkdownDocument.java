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

package org.hkijena.jipipe.ui.components;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains Markdown data
 */
public class MarkdownDocument {

    static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(), TocExtension.create()));
    private static Map<Path, MarkdownDocument> fromFileCache = new HashMap<>();
    private static Map<URL, MarkdownDocument> fromResourcesCache = new HashMap<>();
    private String markdown;
    private String renderedHTML;

    /**
     * @param markdown markdown text
     */
    public MarkdownDocument(String markdown) {
        this.markdown = markdown;
        render();
    }

    private void render() {
        Parser parser = Parser.builder(OPTIONS).build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build();
        this.renderedHTML = renderer.render(document);
    }

    public String getRenderedHTML() {
        return renderedHTML;
    }

    public String getMarkdown() {
        return markdown;
    }

    /**
     * Loads a document from file
     *
     * @param fileName filename
     * @return the document
     */
    public static MarkdownDocument fromFile(Path fileName) {
        try {
            MarkdownDocument existing = fromFileCache.getOrDefault(fileName, null);
            if (existing != null)
                return existing;
            String markdown = new String(Files.readAllBytes(fileName), Charsets.UTF_8);
            MarkdownDocument markdownDocument = new MarkdownDocument(markdown);
            fromFileCache.put(fileName, markdownDocument);
            return markdownDocument;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads a document from the JIPipe plugin resources
     *
     * @param internalPath resource path. Relative to JIPipe resources
     * @return the document
     */
    public static MarkdownDocument fromPluginResource(String internalPath) {
        try {
            URL resourcePath = ResourceUtils.getPluginResource(internalPath);
            MarkdownDocument existing = fromResourcesCache.getOrDefault(resourcePath, null);
            if (existing != null)
                return existing;
            String md = Resources.toString(resourcePath, Charsets.UTF_8);
//            md = md.replace("image://", ResourceUtils.getPluginResource("").toString());
            Set<String> imageURLs = new HashSet<>();
            int index = md.indexOf("image://");
            while (index >= 0) {
                if (index > 0) {
                    char lbracket = md.charAt(index - 1);
                    char rbracket;

                    index += "image://".length();

                    if (lbracket == '(')
                        rbracket = ')';
                    else if (lbracket == '"')
                        rbracket = '"';
                    else {
                        continue;
                    }
                    StringBuilder pathString = new StringBuilder();
                    while (md.charAt(index) != rbracket) {
                        pathString.append(md.charAt(index));
                        ++index;
                    }
                    imageURLs.add(pathString.toString());
                }
                index = md.indexOf("image://", index);
            }
            for (String imageURL : imageURLs) {
                URL url = ResourceUtils.getPluginResource(imageURL);
                md = md.replace("image://" + imageURL, "" + url);
            }

            MarkdownDocument markdownDocument = new MarkdownDocument(md);
            fromResourcesCache.put(resourcePath, markdownDocument);
            return markdownDocument;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
