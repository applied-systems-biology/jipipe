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

package org.hkijena.jipipe.ui.components.markdown;

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
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Contains Markdown data
 */
public class MarkdownDocument {

    static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(), TocExtension.create()));
    private static Map<Path, MarkdownDocument> fromFileCache = new HashMap<>();
    private static Map<URL, MarkdownDocument> fromResourcesCache = new HashMap<>();
    private static Map<String, URL> resourceReplacementCache = new HashMap<>();
    private static Map<String, URL> iconResourceReplacementCache = new HashMap<>();
    private String markdown;
    private String renderedHTML;

    /**
     * @param markdown markdown text
     */
    public MarkdownDocument(String markdown) {
        this.markdown = markdown;
        render();
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
     * @param internalPath                resource path. Relative to JIPipe resources
     * @param additionalResourceProtocols Additional protocols. The key is the name of the protocol, while the value sets the resource loader class
     * @return the document
     */
    public static MarkdownDocument fromPluginResource(String internalPath, Map<String, Class<?>> additionalResourceProtocols) {
        try {
            URL resourcePath = ResourceUtils.getPluginResource(internalPath);
            MarkdownDocument existing = fromResourcesCache.getOrDefault(resourcePath, null);
            if (existing != null)
                return existing;
            String md = Resources.toString(resourcePath, Charsets.UTF_8);
            md = replaceResourceURLs(md, additionalResourceProtocols);

            MarkdownDocument markdownDocument = new MarkdownDocument(md);
            fromResourcesCache.put(resourcePath, markdownDocument);
            return markdownDocument;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replaces resource URLs inside the Markdown document
     *
     * @param md                          Markdown string
     * @param additionalResourceProtocols Additional protocols. The key is the name of the protocol, while the value sets the resource loader class
     * @return modified markdown
     */
    private static String replaceResourceURLs(String md, Map<String, Class<?>> additionalResourceProtocols) {
        {
            Set<String> resourceURLs = getResourceURLs(md, "resource://");
            for (String imageURL : resourceURLs) {
                URL url = resourceReplacementCache.getOrDefault(imageURL, null);
                if (url == null) {
                    url = ResourceUtils.getPluginResource(imageURL);
                }
                md = md.replace("resource://" + imageURL, "" + url);
            }
        }
        for (Map.Entry<String, Class<?>> entry : additionalResourceProtocols.entrySet()) {
            Set<String> resourceURLs = getResourceURLs(md, entry.getKey());
            for (String imageURL : resourceURLs) {
                md = md.replace("resource://" + imageURL, "" + entry.getValue().getResource(imageURL));
            }
        }
        return md;
    }

    /**
     * Loads a document from a resource URL
     *
     * @param resourcePath                resource path
     * @param enableResourceProtocol      Allows to target core JIPipe resources with resource://
     * @param additionalResourceProtocols Additional protocols. The key is the name of the protocol, while the value sets the resource loader class
     * @return the document
     */
    public static MarkdownDocument fromResourceURL(URL resourcePath, boolean enableResourceProtocol, Map<String, Class<?>> additionalResourceProtocols) {
        try {
            String md = Resources.toString(resourcePath, Charsets.UTF_8);
            if (enableResourceProtocol) {
                md = replaceResourceURLs(md, additionalResourceProtocols);
            }
            return new MarkdownDocument(md);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> getResourceURLs(String md, String protocol) {
        Set<String> imageURLs = new HashSet<>();
        int index = md.indexOf(protocol);
        while (index >= 0) {
            if (index > 0) {
                char lbracket = md.charAt(index - 1);
                char rbracket;

                index += protocol.length();

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
            index = md.indexOf(protocol, index);
        }
        return imageURLs;
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

    public HTMLText getRenderedHTMLText() {
        return new HTMLText(getRenderedHTML());
    }

    public String getMarkdown() {
        return markdown;
    }


}
