package org.hkijena.acaq5.ui.components;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

import javax.swing.*;
import java.util.Arrays;

public class MarkdownPane extends JTextPane {
    static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(), TocExtension.create()));
    static final String[] CSS_RULES = {"body { font-family: \"Sans-serif\"; }",
            "pre { background-color: #f5f2f0; border: 3px #f5f2f0 solid; }",
            "code { background-color: #f5f2f0; }",
            "h2 { padding-top: 30px; }",
            "h3 { padding-top: 30px; }",
            "th { border-bottom: 1px solid #c8c8c8; }",
            ".toc-list { list-style: none; }"};


}
