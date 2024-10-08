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

package org.hkijena.jipipe.plugins.parameters.library.markup;

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
import java.util.*;

/**
 * Contains Markdown data
 */
public class MarkdownText {

    static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(), TocExtension.create()));
    private static final Map<Path, MarkdownText> fromFileCache = new HashMap<>();
    private static final Map<String, MarkdownText> fromResourcesCache = new HashMap<>();
    private static final Map<String, URL> resourceReplacementCache = new HashMap<>();
    private static final Map<String, URL> iconResourceReplacementCache = new HashMap<>();
    private String markdown;
    private String renderedHTML;

    public MarkdownText() {
        this("");
    }

    /**
     * @param markdown markdown text
     */
    public MarkdownText(String markdown) {
        this.markdown = markdown;
        render();
    }

    /**
     * Loads a document from file
     *
     * @param fileName filename
     * @return the document
     */
    public static MarkdownText fromFile(Path fileName) {
        try {
            MarkdownText existing = fromFileCache.getOrDefault(fileName, null);
            if (existing != null)
                return existing;
            String markdown = new String(Files.readAllBytes(fileName), Charsets.UTF_8);
            MarkdownText markdownDocument = new MarkdownText(markdown);
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
    public static MarkdownText fromPluginResource(String internalPath) {
        return fromPluginResource(internalPath, new HashMap<>());
    }

    /**
     * Loads a document from the JIPipe plugin resources
     *
     * @param internalPath                resource path. Relative to JIPipe resources
     * @param additionalResourceProtocols Additional protocols. The key is the name of the protocol, while the value sets the resource loader class
     * @return the document
     */
    public static MarkdownText fromPluginResource(String internalPath, Map<String, Class<?>> additionalResourceProtocols) {
        try {
            URL resourcePath = ResourceUtils.getPluginResource(internalPath);
            MarkdownText existing = fromResourcesCache.getOrDefault(resourcePath.toString(), null);
            if (existing != null)
                return existing;
            String md = Resources.toString(resourcePath, Charsets.UTF_8);
            md = replaceResourceURLs(md, additionalResourceProtocols);

            MarkdownText markdownDocument = new MarkdownText(md);
            fromResourcesCache.put(resourcePath.toString(), markdownDocument);
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
    public static MarkdownText fromResourceURL(URL resourcePath, boolean enableResourceProtocol, Map<String, Class<?>> additionalResourceProtocols) {
        try {
            String md = Resources.toString(resourcePath, Charsets.UTF_8);
            if (enableResourceProtocol) {
                md = replaceResourceURLs(md, additionalResourceProtocols);
            }
            return new MarkdownText(md);
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
