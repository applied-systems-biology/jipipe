package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class HTMLEditor extends JPanel {

    public static final int NONE = 0;
    public static final int WITH_SCROLL_BAR = 1;
    public static final int WITHOUT_TOOLBAR = 2;

    private JTextPane textPane;
    private HTMLEditorKit editorKit;
    private Map<JToggleButton, BooleanSupplier> updatedButtons = new HashMap<>();

    public HTMLEditor(int flags) {
        initialize(flags);
        initializeEvents();
    }

    private void initializeEvents() {
        textPane.addCaretListener(e -> updateSelectionToggles());
    }

    public void updateSelectionToggles() {
        for (Map.Entry<JToggleButton, BooleanSupplier> entry : updatedButtons.entrySet()) {
            entry.getKey().setSelected(entry.getValue().getAsBoolean());
        }
    }

    private void initialize(int flags) {
        textPane = new JTextPane();
        textPane.setContentType("text/html");
        editorKit = new HTMLEditorKit();
        textPane.setEditorKit(editorKit);
        editorKit.getStyleSheet().addRule("body { font-family: Dialog; }");
        setLayout(new BorderLayout());
        initializeToolBar(flags);

        if((flags & WITH_SCROLL_BAR) == WITH_SCROLL_BAR) {
            add(new JScrollPane(textPane), BorderLayout.CENTER);
        }
        else {
            add(textPane, BorderLayout.CENTER);
        }
    }

    private void initializeToolBar(int flags) {

        JPanel toolbarPanel = new JPanel(new GridBagLayout());

        JComboBox<String> fontSelection = new JComboBox<>();
        fontSelection.setEditable(true);
        fontSelection.setMinimumSize(new Dimension(20, 25));
        fontSelection.setPreferredSize(new Dimension(150, 25));
        toolbarPanel.add(fontSelection, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridwidth = 6;
                anchor = GridBagConstraints.WEST;
            }
        });

        JComboBox<Integer> sizeSelection = new JComboBox<>();
        sizeSelection.setPrototypeDisplayValue(99);
        sizeSelection.setModel(new DefaultComboBoxModel<>(new Integer[] { 8,9,10,11,12,14,16,18,20,22,24,26,28,36,48,72 }));
        sizeSelection.setEditable(true);
        sizeSelection.setMinimumSize(new Dimension(20, 25));
        sizeSelection.setPreferredSize(new Dimension(100, 25));
//        sizeSelection.setMaximumSize(new Dimension(20, 25));
        toolbarPanel.add(sizeSelection, new GridBagConstraints() {
            {
                gridx = 6;
                gridy = 0;
                gridwidth = 4;
                anchor = GridBagConstraints.WEST;
            }
        });

        toolbarPanel.add(createFormatActionButton(new StyledEditorKit.BoldAction(),
                this::selectionIsBold,
                "Format bold",
                "actions/format-text-bold.png"), new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(createFormatActionButton(new StyledEditorKit.ItalicAction(),
                this::selectionIsItalic,
                "Format italic",
                "actions/format-text-italic.png"), new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(createFormatActionButton(new StyledEditorKit.UnderlineAction(),
                this::selectionIsUnderline,
                "Format underlined",
                "actions/format-text-underline.png"), new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(createFormatActionButton(new StrikeThroughAction(),
                this::selectionIsStrikeThrough,
                "Format strike-through",
                "actions/format-text-strikethrough.png"), new GridBagConstraints() {
            {
                gridx = 3;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(createFormatActionButton(new SubscriptAction(),
                this::selectionIsSubscript,
                "Format as sub-script",
                "actions/format-text-subscript.png"), new GridBagConstraints() {
            {
                gridx = 4;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(createFormatActionButton(new SuperscriptAction(),
                this::selectionIsSuperscript,
                "Format as super-script",
                "actions/format-text-superscript.png"), new GridBagConstraints() {
            {
                gridx = 5;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });

        toolbarPanel.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align left", StyleConstants.ALIGN_LEFT),
                this::selectionIsAlignLeft,
                "Align left",
                "actions/format-justify-left.png"), new GridBagConstraints() {
            {
                gridx = 6;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align center", StyleConstants.ALIGN_CENTER),
                this::selectionIsAlignCenter,
                "Align center",
                "actions/format-justify-center.png"), new GridBagConstraints() {
            {
                gridx = 7;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align right", StyleConstants.ALIGN_RIGHT),
                this::selectionIsAlignRight,
                "Align right",
                "actions/format-justify-right.png"), new GridBagConstraints() {
            {
                gridx = 8;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align justified", StyleConstants.ALIGN_JUSTIFIED),
                this::selectionIsAlignJustified,
                "Align justified",
                "actions/format-justify-fill.png"), new GridBagConstraints() {
            {
                gridx = 9;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        toolbarPanel.add(new JPanel(), new GridBagConstraints() {
            {
                gridx = 20;
                gridy = 1;
                weightx = 1;
            }
        });

        if((flags & WITHOUT_TOOLBAR) != WITHOUT_TOOLBAR) {
            add(toolbarPanel, BorderLayout.NORTH);
        }
    }

    public boolean selectionIsBold() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isBold(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsItalic() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isItalic(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsUnderline() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isUnderline(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsStrikeThrough() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isStrikeThrough(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsSubscript() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isSubscript(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsSuperscript() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isSuperscript(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsAlignLeft() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getParagraphElement(i);
            value &= (StyleConstants.getAlignment(element.getAttributes()) == StyleConstants.ALIGN_LEFT);
        }
        return value;
    }

    public boolean selectionIsAlignCenter() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getParagraphElement(i);
            value &= (StyleConstants.getAlignment(element.getAttributes()) == StyleConstants.ALIGN_CENTER);
        }
        return value;
    }

    public boolean selectionIsAlignRight() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getParagraphElement(i);
            value &= (StyleConstants.getAlignment(element.getAttributes()) == StyleConstants.ALIGN_RIGHT);
        }
        return value;
    }

    public boolean selectionIsAlignJustified() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getParagraphElement(i);
            value &= (StyleConstants.getAlignment(element.getAttributes()) == StyleConstants.ALIGN_JUSTIFIED);
        }
        return value;
    }

    private JToggleButton createFormatActionButton(Action action, BooleanSupplier toggled, String name, String icon) {
        JToggleButton button = new JToggleButton();
        button.addActionListener(e -> {
            action.actionPerformed(e);
            updateSelectionToggles();
            textPane.requestFocusInWindow();
        });
        button.setText("");
        button.setIcon(UIUtils.getIconFromResources(icon));
        button.setToolTipText(name);
        UIUtils.makeFlat25x25(button);
        updatedButtons.put(button, toggled);
        return button;
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

    public static class StrikeThroughAction extends StyledEditorKit.StyledTextAction {

        /**
         * Constructs a new StrikeThroughAction.
         */
        public StrikeThroughAction() {
            super("font-strike-through");
        }

        /**
         * Toggles the strikethrough attribute.
         *
         * @param e the action event
         */
        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                StyledEditorKit kit = getStyledEditorKit(editor);
                MutableAttributeSet attr = kit.getInputAttributes();
                boolean value = (StyleConstants.isStrikeThrough(attr)) ? false : true;
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setStrikeThrough(sas, value);
                setCharacterAttributes(editor, sas, false);
            }
        }
    }

    public static class SubscriptAction extends StyledEditorKit.StyledTextAction {

        /**
         * Constructs a new StrikeThroughAction.
         */
        public SubscriptAction() {
            super("font-subscript");
        }

        /**
         * Toggles the strikethrough attribute.
         *
         * @param e the action event
         */
        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                StyledEditorKit kit = getStyledEditorKit(editor);
                MutableAttributeSet attr = kit.getInputAttributes();
                boolean value = (StyleConstants.isSubscript(attr)) ? false : true;
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setSubscript(sas, value);
                setCharacterAttributes(editor, sas, false);
            }
        }
    }

    public static class SuperscriptAction extends StyledEditorKit.StyledTextAction {

        /**
         * Constructs a new StrikeThroughAction.
         */
        public SuperscriptAction() {
            super("font-superscript");
        }

        /**
         * Toggles the strikethrough attribute.
         *
         * @param e the action event
         */
        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                StyledEditorKit kit = getStyledEditorKit(editor);
                MutableAttributeSet attr = kit.getInputAttributes();
                boolean value = (StyleConstants.isSuperscript(attr)) ? false : true;
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setSuperscript(sas, value);
                setCharacterAttributes(editor, sas, false);
            }
        }
    }
}
