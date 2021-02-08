package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.BooleanSupplier;

public class HTMLEditor extends JPanel {

    public static final int NONE = 0;
    public static final int WITH_SCROLL_BAR = 1;
    public static final int WITHOUT_TOOLBAR = 2;

    private JTextPane textPane;
    private HTMLEditorKit editorKit;
    private Map<JToggleButton, BooleanSupplier> updatedButtons = new HashMap<>();
    private JComboBox<String> fontSelection;
    private JComboBox<Integer> sizeSelection;
    private ColorChooserButton foregroundColorButton;
    private boolean isUpdating = false;
    private final Set<String> availableFonts = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    private final Map<String, Action> availableEditorKitActions = new HashMap<>();

    public HTMLEditor(int flags) {
        initialize(flags);
        initializeEvents();
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    public Document getDocument() {
        return textPane.getDocument();
    }

    private void initializeEvents() {
        textPane.addCaretListener(e -> updateSelection());
    }

    public void updateSelection() {
        isUpdating = true;
        try {
            for (Map.Entry<JToggleButton, BooleanSupplier> entry : updatedButtons.entrySet()) {
                entry.getKey().setSelected(entry.getValue().getAsBoolean());
            }
            fontSelection.setSelectedItem(getSelectionFontFamily());
            sizeSelection.setSelectedItem(getSelectionFontSize());
            foregroundColorButton.setSelectedColor(getSelectionForegroundColor());
        }
        finally {
            isUpdating = false;
        }
    }

    private void initialize(int flags) {
        textPane = new JTextPane();
        textPane.setContentType("text/html");
        editorKit = new HTMLEditorKit();
        for (Action action : editorKit.getActions()) {
            availableEditorKitActions.put(action.getValue(Action.NAME) + "", action);
        }

        textPane.setEditorKit(editorKit);
        editorKit.getStyleSheet().addRule("body { font-family: Dialog; }");
        textPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
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

        JPanel toolbarPanel = new JPanel(new WrapLayout(WrapLayout.LEFT));

        fontSelection = new JComboBox<>();
        fontSelection.setModel(new DefaultComboBoxModel<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        fontSelection.setEditable(true);
        fontSelection.setMinimumSize(new Dimension(20, 25));
        fontSelection.setPreferredSize(new Dimension(125, 25));
        fontSelection.setSelectedItem("Dialog");
        fontSelection.addItemListener(e -> {
            if(!isUpdating) {
                String family = fontSelection.getSelectedItem() instanceof String ? (String) fontSelection.getSelectedItem() : "Dialog";
                if(availableFonts.contains(family)) {
                    new StyledEditorKit.FontFamilyAction(family, family).actionPerformed(new ActionEvent(textPane, e.getID(), family));
                }
                updateSelection();
                textPane.requestFocusInWindow();
            }
        });

        sizeSelection = new JComboBox<>();
        sizeSelection.setPrototypeDisplayValue(99);
        sizeSelection.setModel(new DefaultComboBoxModel<>(new Integer[] { 8,9,10,11,12,14,16,18,20,22,24,26,28,36,48,72 }));
        sizeSelection.setSelectedItem(12);
        sizeSelection.setEditable(true);
        sizeSelection.setMinimumSize(new Dimension(20, 25));
        sizeSelection.setPreferredSize(new Dimension(50, 25));
        sizeSelection.addItemListener(e -> {
            if(!isUpdating) {
                int size = sizeSelection.getSelectedItem() instanceof Integer ? (int) sizeSelection.getSelectedItem() : 12;
                size = Math.max(1,size);
                new StyledEditorKit.FontSizeAction("set-font-size", size).actionPerformed(new ActionEvent(textPane, e.getID(), "set-font-size"));
                updateSelection();
                textPane.requestFocusInWindow();
            }
        });

        // Font family/size
        JPanel fontSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        fontSizePanel.setBorder(null);
        fontSizePanel.add(fontSelection);
        fontSizePanel.add(sizeSelection);
        toolbarPanel.add(fontSizePanel);

        // Standard formats
        JPanel standardFormatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        standardFormatPanel.add(createFormatActionButton(new StyledEditorKit.BoldAction(),
                this::selectionIsBold,
                "Format bold",
                "actions/format-text-bold.png"));
        standardFormatPanel.add(createFormatActionButton(new StyledEditorKit.ItalicAction(),
                this::selectionIsItalic,
                "Format italic",
                "actions/format-text-italic.png"));
        standardFormatPanel.add(createFormatActionButton(new StyledEditorKit.UnderlineAction(),
                this::selectionIsUnderline,
                "Format underlined",
                "actions/format-text-underline.png"));
        toolbarPanel.add(standardFormatPanel);

        // Extended formats
        JPanel extendedFormatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        extendedFormatPanel.add(createFormatActionButton(new StrikeThroughAction(),
                this::selectionIsStrikeThrough,
                "Format strike-through",
                "actions/format-text-strikethrough.png"));
        extendedFormatPanel.add(createFormatActionButton(new SubscriptAction(),
                this::selectionIsSubscript,
                "Format as sub-script",
                "actions/format-text-subscript.png"));
        extendedFormatPanel.add(createFormatActionButton(new SuperscriptAction(),
                this::selectionIsSuperscript,
                "Format as super-script",
                "actions/format-text-superscript.png"));

        foregroundColorButton = new ColorChooserButton("");
        UIUtils.makeFlat25x25(foregroundColorButton);
        toolbarPanel.add(foregroundColorButton,  new GridBagConstraints() {
            {
                gridx = 6;
                gridy = 1;
                anchor = GridBagConstraints.WEST;
            }
        });
        foregroundColorButton.setToolTipText("Set color");
        foregroundColorButton.getEventBus().register(new Object() {
            @Subscribe
            public void onColorSelected(ColorChooserButton.ColorChosenEvent event) {
                if(!isUpdating) {
                    new StyledEditorKit.ForegroundAction("set-foreground", event.getColor()).actionPerformed(
                            new ActionEvent(textPane, 0, "set-foreground")
                    );
                    updateSelection();
                    textPane.requestFocusInWindow();
                }
            }
        });
        extendedFormatPanel.add(foregroundColorButton);
        toolbarPanel.add(extendedFormatPanel);

        // Align panel
        JPanel alignPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        alignPanel.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align left", StyleConstants.ALIGN_LEFT),
                this::selectionIsAlignLeft,
                "Align left",
                "actions/format-justify-left.png"));
        alignPanel.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align center", StyleConstants.ALIGN_CENTER),
                this::selectionIsAlignCenter,
                "Align center",
                "actions/format-justify-center.png"));
        alignPanel.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align right", StyleConstants.ALIGN_RIGHT),
                this::selectionIsAlignRight,
                "Align right",
                "actions/format-justify-right.png"));
        alignPanel.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align justified", StyleConstants.ALIGN_JUSTIFIED),
                this::selectionIsAlignJustified,
                "Align justified",
                "actions/format-justify-fill.png"));
        toolbarPanel.add(alignPanel);

        if((flags & WITHOUT_TOOLBAR) != WITHOUT_TOOLBAR) {
            add(toolbarPanel, BorderLayout.NORTH);
        }
    }

    public int getSelectionFontSize() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return 12;
        int value = 0;
        for (int i = textPane.getSelectionStart(); i <= textPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value = Math.max(value, StyleConstants.getFontSize(element.getAttributes()));
        }
        return value;
    }

    public Color getSelectionForegroundColor() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return Color.BLACK;
        Element element = document.getCharacterElement(textPane.getSelectionStart());
        return StyleConstants.getForeground(element.getAttributes());
    }

    public String getSelectionFontFamily() {
        StyledDocument document = textPane.getStyledDocument();
        if(document.getLength() == 0)
            return "Dialog";
        Element element = document.getCharacterElement(textPane.getSelectionStart());
        return StyleConstants.getFontFamily(element.getAttributes());
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
            updateSelection();
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
            String value = byteArrayOutputStream.toString();
            int bodyStart = value.indexOf("<body>") + "<body>".length();
            int bodyEnd = value.indexOf("</body>");
            String body = value.substring(bodyStart, bodyEnd).trim();
            body = body.replace("\n", "<br/>");
            return value.substring(0, bodyStart) + body + "</body></html>";
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

    public void setText(String value) {
        textPane.setText(StringUtils.nullToEmpty(value));
        // Workaround https://stackoverflow.com/questions/1527021/html-jtextpane-newline-support
//        textPane.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "<br/>\n");
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
