package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.ByteArrayOutputStream;

public class HTMLEditor extends JPanel {

    public static final int NONE = 0;
    public static final int WITH_SCROLL_BAR = 1;

    private JTextPane textPane;
    private HTMLEditorKit editorKit;

    public HTMLEditor(int flags) {
        initialize(flags);
    }

    private void initialize(int flags) {
        textPane = new JTextPane();
        textPane.setContentType("text/html");
        editorKit = new HTMLEditorKit();
        textPane.setEditorKit(editorKit);
        setLayout(new BorderLayout());
        initializeToolBar();

        if((flags & WITH_SCROLL_BAR) == WITH_SCROLL_BAR) {
            add(new JScrollPane(textPane), BorderLayout.CENTER);
        }
        else {
            add(textPane, BorderLayout.CENTER);
        }
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        createFormatActionButton(toolBar, new StyledEditorKit.BoldAction(), "Format bold", "actions/format-text-bold.png");
        createFormatActionButton(toolBar, new StyledEditorKit.ItalicAction(), "Format italic", "actions/format-text-italic.png");
        createFormatActionButton(toolBar, new StyledEditorKit.UnderlineAction(), "Format underlined", "actions/format-text-underline.png");

        toolBar.addSeparator();
        createFormatActionButton(toolBar, new StyledEditorKit.AlignmentAction("Align left", StyleConstants.ALIGN_LEFT), "Align left", "actions/format-justify-left.png");
        createFormatActionButton(toolBar, new StyledEditorKit.AlignmentAction("Align center", StyleConstants.ALIGN_CENTER), "Align center", "actions/format-justify-center.png");
        createFormatActionButton(toolBar, new StyledEditorKit.AlignmentAction("Align right", StyleConstants.ALIGN_RIGHT), "Align right", "actions/format-justify-right.png");
        createFormatActionButton(toolBar, new StyledEditorKit.AlignmentAction("Align justified", StyleConstants.ALIGN_JUSTIFIED), "Align justified", "actions/format-justify-fill.png");

        add(toolBar, BorderLayout.NORTH);
    }

    private void createFormatActionButton(JToolBar toolBar, Action action, String name, String icon) {
        JButton formatTextBold = new JButton();
        formatTextBold.setAction(action);
        formatTextBold.setText("");
        formatTextBold.setIcon(UIUtils.getIconFromResources(icon));
        formatTextBold.setToolTipText(name);
        toolBar.add(formatTextBold);
    }

    public String getPlainText() {
        Document document = textPane.getDocument();
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHTML() {
        try {
            Document document = textPane.getDocument();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream ();
            editorKit.write(byteArrayOutputStream, document, 0, document.getLength());
            return byteArrayOutputStream.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        UIUtils.getThemeFromRawSettings().install();
        JFrame frame = new JFrame();
        frame.setContentPane(new HTMLEditor(WITH_SCROLL_BAR));
        frame.setSize(800,600);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
