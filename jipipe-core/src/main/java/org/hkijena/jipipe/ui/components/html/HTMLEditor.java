package org.hkijena.jipipe.ui.components.html;

import com.google.common.eventbus.Subscribe;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ColorChooserButton;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.icons.OverlayColorIcon;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.scijava.ui.swing.script.EditorPane;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;

public class HTMLEditor extends JIPipeWorkbenchPanel {

    public static final int NONE = 0;
    public static final int WITH_SCROLL_BAR = 1;
    public static final int WITH_DIALOG_EDITOR_BUTTON = 2;
    private final Set<String> availableFonts = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    private final Map<String, Action> availableEditorKitActions = new HashMap<>();
    private final boolean enableDialogEditor;
    private final Map<JToggleButton, BooleanSupplier> updatedButtons = new HashMap<>();
    private JTextPane wysiwygEditorPane;
    private EditorPane htmlEditorPane;
    private HTMLEditorKit wysiwygEditorKit;
    private JComboBox<String> fontSelection;
    private JComboBox<Integer> sizeSelection;
    private ColorChooserButton foregroundColorButton;
    private boolean isUpdating = false;
    private Mode mode;
    private JPanel toolBarContainer;
    private JPanel editorContainer;
    private JButton modeButton;
    private JButton editInDialogButton;
    private JButton insertImageButton;
    private JButton insertLinkButton;

    public HTMLEditor(JIPipeWorkbench workbench, Mode mode, int flags) {
        super(workbench);
        this.enableDialogEditor = (flags & WITH_DIALOG_EDITOR_BUTTON) == WITH_DIALOG_EDITOR_BUTTON;
        initialize(flags);
        setMode(mode);
        initializeEvents();
    }

    public static void main(String[] args) {
        JIPipe.ensureInstance();
        UIUtils.getThemeFromRawSettings().install();
        JFrame frame = new JFrame();
        frame.setContentPane(new HTMLEditor(new JIPipeDummyWorkbench(), Mode.Compact, WITH_SCROLL_BAR));
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        if (mode != this.mode) {
            if (this.mode == Mode.HTML) {
                // Copy to wysiwyg
                wysiwygEditorPane.setText(htmlEditorPane.getText());
            } else {
                // Copy to HTML
                htmlEditorPane.setText(wysiwygEditorPane.getText());
            }
            this.mode = mode;
            modeButton.setText(mode.toString());
            reloadToolbar();
            reloadEditor();
        }
    }

    private void reloadEditor() {
        editorContainer.removeAll();
        if (mode == Mode.HTML) {
            htmlEditorPane.setText(wysiwygEditorPane.getText());
            editorContainer.add(htmlEditorPane, BorderLayout.CENTER);
        } else {
            wysiwygEditorPane.setText(htmlEditorPane.getText());
            editorContainer.add(wysiwygEditorPane, BorderLayout.CENTER);
        }
        editorContainer.revalidate();
        editorContainer.repaint();
    }

    public Document getDocument() {
        return wysiwygEditorPane.getDocument();
    }

    private void initializeEvents() {
        wysiwygEditorPane.addCaretListener(e -> updateSelection());
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
        } finally {
            isUpdating = false;
        }
    }

    private void reloadToolbar() {
        toolBarContainer.removeAll();
        if (mode == Mode.Compact) {
            JToolBar toolBar = new JToolBar();
            toolBar.add(foregroundColorButton);
            toolBar.add(createFormatActionButton(new StyledEditorKit.BoldAction(),
                    this::selectionIsBold,
                    "Format bold",
                    "actions/format-text-bold.png"));
            toolBar.add(createFormatActionButton(new StyledEditorKit.ItalicAction(),
                    this::selectionIsItalic,
                    "Format italic",
                    "actions/format-text-italic.png"));
            toolBar.add(createFormatActionButton(new StyledEditorKit.UnderlineAction(),
                    this::selectionIsUnderline,
                    "Format underlined",
                    "actions/format-text-underline.png"));
            toolBar.addSeparator();
            toolBar.add(insertImageButton);
            toolBar.add(Box.createHorizontalGlue());
            toolBar.add(modeButton);
            if (enableDialogEditor)
                toolBar.add(editInDialogButton);
            toolBarContainer.add(toolBar);
        } else if (mode == Mode.Full) {

            // Toolbar 1
            JToolBar toolBar1 = new JToolBar();

            toolBar1.add(fontSelection);
            toolBar1.add(sizeSelection);
            toolBar1.addSeparator();

            toolBar1.add(foregroundColorButton);

            toolBar1.add(Box.createHorizontalGlue());
            toolBar1.add(modeButton);
            if (enableDialogEditor)
                toolBar1.add(editInDialogButton);

            // Toolbar 2
            JToolBar toolBar2 = new JToolBar();

            toolBar2.add(createFormatActionButton(new StyledEditorKit.BoldAction(),
                    this::selectionIsBold,
                    "Format bold",
                    "actions/format-text-bold.png"));
            toolBar2.add(createFormatActionButton(new StyledEditorKit.ItalicAction(),
                    this::selectionIsItalic,
                    "Format italic",
                    "actions/format-text-italic.png"));
            toolBar2.add(createFormatActionButton(new StyledEditorKit.UnderlineAction(),
                    this::selectionIsUnderline,
                    "Format underlined",
                    "actions/format-text-underline.png"));
            toolBar2.add(createFormatActionButton(new StrikeThroughAction(),
                    this::selectionIsStrikeThrough,
                    "Format strike-through",
                    "actions/format-text-strikethrough.png"));
            toolBar2.add(createFormatActionButton(new SubscriptAction(),
                    this::selectionIsSubscript,
                    "Format as sub-script",
                    "actions/format-text-subscript.png"));
            toolBar2.add(createFormatActionButton(new SuperscriptAction(),
                    this::selectionIsSuperscript,
                    "Format as super-script",
                    "actions/format-text-superscript.png"));

            toolBar2.addSeparator();

            toolBar2.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align left", StyleConstants.ALIGN_LEFT),
                    this::selectionIsAlignLeft,
                    "Align left",
                    "actions/format-justify-left.png"));
            toolBar2.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align center", StyleConstants.ALIGN_CENTER),
                    this::selectionIsAlignCenter,
                    "Align center",
                    "actions/format-justify-center.png"));
            toolBar2.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align right", StyleConstants.ALIGN_RIGHT),
                    this::selectionIsAlignRight,
                    "Align right",
                    "actions/format-justify-right.png"));
            toolBar2.add(createFormatActionButton(new StyledEditorKit.AlignmentAction("Align justified", StyleConstants.ALIGN_JUSTIFIED),
                    this::selectionIsAlignJustified,
                    "Align justified",
                    "actions/format-justify-fill.png"));

            toolBar2.add(Box.createHorizontalGlue());

            //Toolbar 3
            JToolBar toolBar3 = new JToolBar();

            toolBar3.add(insertLinkButton);
            toolBar3.add(insertImageButton);
            toolBar3.add(Box.createHorizontalGlue());

            // Add the toolbars
            toolBarContainer.add(toolBar1);
            toolBarContainer.add(toolBar2);
            toolBarContainer.add(toolBar3);
        } else {
            JToolBar toolBar = new JToolBar();
            toolBar.add(Box.createHorizontalGlue());
            toolBar.add(modeButton);
            if (enableDialogEditor)
                toolBar.add(editInDialogButton);
            toolBarContainer.add(toolBar);
        }
        revalidate();
        repaint();
    }

    private void initialize(int flags) {
        setLayout(new BorderLayout());

        // WYSIWYG editor
        wysiwygEditorPane = new JTextPane();
        wysiwygEditorPane.setContentType("text/html");
        wysiwygEditorKit = new ExtendedHTMLEditorKit();
        for (Action action : wysiwygEditorKit.getActions()) {
            availableEditorKitActions.put(action.getValue(Action.NAME) + "", action);
        }

        wysiwygEditorPane.setEditorKit(wysiwygEditorKit);
        wysiwygEditorKit.getStyleSheet().addRule("body { font-family: Dialog; }" +
                "p { margin: 0; }");
        wysiwygEditorPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
//        wysiwygEditorPane.getDocument().addDocumentListener(new DocumentChangeListener() {
//            @Override
//            public void changed(DocumentEvent documentEvent) {
//                if(mode != Mode.HTML) {
//                    htmlEditorPane.setText(wysiwygEditorPane.getText());
//                }
//            }
//        });
        wysiwygEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    if (e.getURL() == null) {
                        JOptionPane.showMessageDialog(this, "This link is invalid! (Content is '" + e.getDescription() + "')", "Visit URL", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (JOptionPane.showConfirmDialog(this, "Do you really want to visit " + e.getURL() + "?", "Visit URL", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // HTML code editor
        htmlEditorPane = new EditorPane();
        UIUtils.applyThemeToCodeEditor(htmlEditorPane);
        htmlEditorPane.setBackground(UIManager.getColor("TextArea.background"));
        htmlEditorPane.setHighlightCurrentLine(false);
        htmlEditorPane.setTabSize(4);
        htmlEditorPane.setSyntaxEditingStyle("text/html");
        htmlEditorPane.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (mode == Mode.HTML) {
                    wysiwygEditorPane.setText(htmlEditorPane.getText());
                }
            }
        });

        // The editor container
        editorContainer = new JPanel(new BorderLayout());
        if ((flags & WITH_SCROLL_BAR) == WITH_SCROLL_BAR) {
            add(new JScrollPane(editorContainer), BorderLayout.CENTER);
        } else {
            add(editorContainer, BorderLayout.CENTER);
        }

        // The toolbar container
        toolBarContainer = new JPanel();
        toolBarContainer.setLayout(new BoxLayout(toolBarContainer, BoxLayout.Y_AXIS));
        add(toolBarContainer, BorderLayout.NORTH);

        initializeToolBarComponents();
    }

    private void initializeToolBarComponents() {
        // The font selection
        fontSelection = new JComboBox<>();
        fontSelection.setModel(new DefaultComboBoxModel<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        fontSelection.setEditable(true);
        fontSelection.setMinimumSize(new Dimension(20, 25));
        fontSelection.setPreferredSize(new Dimension(125, 25));
        fontSelection.setSelectedItem("Dialog");
        fontSelection.addItemListener(e -> {
            if (!isUpdating) {
                String family = fontSelection.getSelectedItem() instanceof String ? (String) fontSelection.getSelectedItem() : "Dialog";
                if (availableFonts.contains(family)) {
                    new StyledEditorKit.FontFamilyAction(family, family).actionPerformed(new ActionEvent(wysiwygEditorPane, e.getID(), family));
                }
                updateSelection();
                wysiwygEditorPane.requestFocusInWindow();
            }
        });

        // The size selection
        sizeSelection = new JComboBox<>();
        sizeSelection.setPrototypeDisplayValue(99);
        sizeSelection.setModel(new DefaultComboBoxModel<>(new Integer[]{8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72}));
        sizeSelection.setSelectedItem(12);
        sizeSelection.setEditable(true);
        sizeSelection.setMinimumSize(new Dimension(20, 25));
        sizeSelection.setPreferredSize(new Dimension(50, 25));
        sizeSelection.addItemListener(e -> {
            if (!isUpdating) {
                int size = sizeSelection.getSelectedItem() instanceof Integer ? (int) sizeSelection.getSelectedItem() : 12;
                size = Math.max(1, size);
                new StyledEditorKit.FontSizeAction("set-font-size", size).actionPerformed(new ActionEvent(wysiwygEditorPane, e.getID(), "set-font-size"));
                updateSelection();
                wysiwygEditorPane.requestFocusInWindow();
            }
        });

        // Color selection
        foregroundColorButton = new ColorChooserButton("");
        foregroundColorButton.setIcon(new OverlayColorIcon(UIUtils.getIconFromResources("actions/format-stroke-color.png"),
                new Rectangle(0, 14, 16, 2), true, false));
        UIUtils.makeFlat25x25(foregroundColorButton);
        foregroundColorButton.setToolTipText("Set color");
        foregroundColorButton.getEventBus().register(new Object() {
            @Override
            public void onColorSelected(ColorChooserButton.ColorChosenEvent event) {
                if (!isUpdating) {
                    new StyledEditorKit.ForegroundAction("set-foreground", event.getColor()).actionPerformed(
                            new ActionEvent(wysiwygEditorPane, 0, "set-foreground")
                    );
                    updateSelection();
                    wysiwygEditorPane.requestFocusInWindow();
                }
            }
        });

        // Compact mode selection
        modeButton = new JButton(UIUtils.getIconFromResources("actions/arrow-down.png"));
        modeButton.setHorizontalTextPosition(SwingConstants.LEFT);
        UIUtils.makeFlat(modeButton);
        JPopupMenu modeMenu = UIUtils.addPopupMenuToComponent(modeButton);
        {
            JMenuItem compactModeItem = new JMenuItem("Compact", UIUtils.getIconFromResources("actions/edit-select-text.png"));
            compactModeItem.addActionListener(e -> setMode(Mode.Compact));
            modeMenu.add(compactModeItem);

            JMenuItem fullModeItem = new JMenuItem("Full", UIUtils.getIconFromResources("actions/edit-select-text.png"));
            fullModeItem.addActionListener(e -> setMode(Mode.Full));
            modeMenu.add(fullModeItem);

            JMenuItem htmlModeItem = new JMenuItem("HTML code", UIUtils.getIconFromResources("actions/format-text-code.png"));
            htmlModeItem.addActionListener(e -> setMode(Mode.HTML));
            modeMenu.add(htmlModeItem);
        }

        // Edit in dialog button
        editInDialogButton = new JButton(UIUtils.getIconFromResources("actions/link.png"));
        UIUtils.makeFlat25x25(editInDialogButton);
        editInDialogButton.setToolTipText("Edit in dedicated window");
        editInDialogButton.addActionListener(e -> {
            HTMLText result = UIUtils.getHTMLByDialog(getWorkbench(), this, "Edit", null, new HTMLText(getHTML()));
            if (result != null) {
                setText(result.getHtml());
            }
        });

        // Insert image
        insertImageButton = new JButton("Image", UIUtils.getIconFromResources("actions/insert-image.png"));
        UIUtils.makeFlat(insertImageButton);
        JPopupMenu insertImageMenu = UIUtils.addPopupMenuToComponent(insertImageButton);
        {
            JMenuItem insertImageFromFileItem = new JMenuItem("Embed from file ...", UIUtils.getIconFromResources("actions/document-open-folder.png"));
            insertImageFromFileItem.addActionListener(e -> insertImageFromFile());
            insertImageMenu.add(insertImageFromFileItem);

            JMenuItem insertImageFromClipboardItem = new JMenuItem("Embed from clipboard", UIUtils.getIconFromResources("actions/edit-paste.png"));
            insertImageFromClipboardItem.addActionListener(e -> insertImageFromClipboard());
            insertImageMenu.add(insertImageFromClipboardItem);

            insertImageMenu.addSeparator();

            JMenuItem insertImageFromURL = new JMenuItem("Link from URL", UIUtils.getIconFromResources("actions/edit-link.png"));
            insertImageFromURL.addActionListener(e -> insertImageURL());
            insertImageMenu.add(insertImageFromURL);
        }

        // Insert link
        insertLinkButton = new JButton("Link", UIUtils.getIconFromResources("actions/insert-link.png"));
        UIUtils.makeFlat(insertLinkButton);
        insertLinkButton.addActionListener(e -> insertLink());
    }

    private void insertImageURL() {
        String urlString = JOptionPane.showInputDialog(this, "Please insert the URL of the image into the following box.\nPlease note that the image will not be embedded into the document.", "Insert image", JOptionPane.PLAIN_MESSAGE);
        try {
            URL url = new URL(urlString);
            try {
                int caretPosition = wysiwygEditorPane.getCaretPosition();
                HTMLDocument document = (HTMLDocument) wysiwygEditorPane.getDocument();
                wysiwygEditorKit.insertHTML(document, caretPosition, "<img src=\"" + url + "\" ></img>", 0, 0, HTML.Tag.IMG);
            } catch (BadLocationException | IOException e) {
                throw new RuntimeException(e);
            }
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this, "'" + urlString + "' is not a valid URL!");
        }
    }

    private void insertLink() {
        JIPipeDynamicParameterCollection parameterCollection = new JIPipeDynamicParameterCollection();
        parameterCollection.addParameter("url", String.class, "URL", "The URL");
        parameterCollection.addParameter("text", String.class, "Text", "The link text");
        parameterCollection.setAllowUserModification(false);
        if (ParameterPanel.showDialog(getWorkbench(), parameterCollection, null, "Insert link", FormPanel.WITH_SCROLLING)) {
            try {
                int caretPosition = wysiwygEditorPane.getCaretPosition();
                HTMLDocument document = (HTMLDocument) wysiwygEditorPane.getDocument();
                String url = parameterCollection.get("url").get(String.class);
                String text = StringUtils.orElse(parameterCollection.get("text").get(String.class), url);
                wysiwygEditorKit.insertHTML(document, caretPosition, "<a href=\"" + MacroUtils.escapeString(url) + "\" >" + text + "</a>", 0, 0, HTML.Tag.A);
            } catch (BadLocationException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void insertImageFromClipboard() {
        BufferedImage image = BufferedImageUtils.getImageFromClipboard(BufferedImage.TYPE_INT_RGB);
        if (image != null) {
            insertImage(image);
        }
    }

    private void insertImageFromFile() {
        Path path = FileChooserSettings.openFile(this, FileChooserSettings.LastDirectoryKey.Data, "Insert image from file", UIUtils.EXTENSION_FILTER_IMAGEIO_IMAGES);
        if (path != null) {
            try {
                insertImage(ImageIO.read(path.toFile()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void insertImage(BufferedImage image) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String base64;
        try {
            base64 = BufferedImageUtils.imageToBase64(image, "png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        if (base64.length() > 128 * 1024) {
            Object result = JOptionPane.showInputDialog(this, "The image has a size of " + image.getWidth() + " x " + image.getHeight() + " pixels (" + (base64.length() / 1024) + "KB)." +
                            "Images of this size can impact the performance of the editor.\nIn the following setting, you can downscale the image to a specified data size or scale.", "Insert image", JOptionPane.WARNING_MESSAGE,
                    UIUtils.getIconFromResources("apps/jipipe.png"), new Object[]{
                            "512KB",
                            "256KB",
                            "128KB",
                            "64KB",
                            "32KB",
                            "100%",
                            "80%",
                            "75%",
                            "60%",
                            "50%",
                            "40%",
                            "25%",
                            "20%",
                            "10%"
                    }, "128KB");
            if (result instanceof String) {
                String s = result.toString();
                double percentage;
                if (s.endsWith("KB")) {
                    double targetBytes = Integer.parseInt(s.substring(0, s.length() - 2)) * 1024;
                    percentage = Math.sqrt(targetBytes / base64.length());
                } else {
                    percentage = Double.parseDouble(s.substring(0, s.length() - 1)) / 100.0;
                }
                if (percentage < 1) {
                    ImageProcessor processor = new ColorProcessor(image);
                    processor = processor.resize((int) (processor.getWidth() * percentage), (int) (processor.getHeight() * percentage));
                    image = processor.getBufferedImage();
                    try {
                        base64 = BufferedImageUtils.imageToBase64(image, "png");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                return;
            }
        }

        JIPipeDynamicParameterCollection parameterCollection = new JIPipeDynamicParameterCollection();
        parameterCollection.setAllowUserModification(false);
        parameterCollection.addParameter("width", String.class, "Width", "");
        parameterCollection.addParameter("height", String.class, "Height", "");

        // Has no effect currently, so it's disabled
//        if(!ParameterPanel.showDialog(getWorkbench(), parameterCollection, null, "Insert image", WITH_SCROLL_BAR)) {
//            return;
//        }

        int caretPosition = wysiwygEditorPane.getCaretPosition();
        try {
            HTMLDocument document = (HTMLDocument) wysiwygEditorPane.getDocument();
            StringBuilder html = new StringBuilder();
            html.append("<img").append(" src=\"").append("data:image/png;base64,").append(base64).append("\"");
            if (!StringUtils.isNullOrEmpty(parameterCollection.get("width").get(String.class))) {
                html.append(" width=\"").append(parameterCollection.get("width").get(String.class)).append("\"");
            }
            if (!StringUtils.isNullOrEmpty(parameterCollection.get("height").get(String.class))) {
                html.append(" height=\"").append(parameterCollection.get("height").get(String.class)).append("\"");
            }
            html.append("></img>");
            wysiwygEditorKit.insertHTML(document, caretPosition, html.toString(), 0, 0, HTML.Tag.IMG);
        } catch (BadLocationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getSelectionFontSize() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return 12;
        int value = 0;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value = Math.max(value, StyleConstants.getFontSize(element.getAttributes()));
        }
        return value;
    }

    public Color getSelectionForegroundColor() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return Color.BLACK;
        Element element = document.getCharacterElement(wysiwygEditorPane.getSelectionStart());
        return StyleConstants.getForeground(element.getAttributes());
    }

    public String getSelectionFontFamily() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return "Dialog";
        Element element = document.getCharacterElement(wysiwygEditorPane.getSelectionStart());
        return StyleConstants.getFontFamily(element.getAttributes());
    }

    public boolean selectionIsBold() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isBold(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsItalic() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isItalic(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsUnderline() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isUnderline(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsStrikeThrough() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isStrikeThrough(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsSubscript() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isSubscript(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsSuperscript() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getCharacterElement(i);
            value &= StyleConstants.isSuperscript(element.getAttributes());
        }
        return value;
    }

    public boolean selectionIsAlignLeft() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getParagraphElement(i);
            value &= (StyleConstants.getAlignment(element.getAttributes()) == StyleConstants.ALIGN_LEFT);
        }
        return value;
    }

    public boolean selectionIsAlignCenter() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getParagraphElement(i);
            value &= (StyleConstants.getAlignment(element.getAttributes()) == StyleConstants.ALIGN_CENTER);
        }
        return value;
    }

    public boolean selectionIsAlignRight() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
            Element element = document.getParagraphElement(i);
            value &= (StyleConstants.getAlignment(element.getAttributes()) == StyleConstants.ALIGN_RIGHT);
        }
        return value;
    }

    public boolean selectionIsAlignJustified() {
        StyledDocument document = wysiwygEditorPane.getStyledDocument();
        if (document.getLength() == 0)
            return false;
        boolean value = true;
        for (int i = wysiwygEditorPane.getSelectionStart(); i <= wysiwygEditorPane.getSelectionEnd(); i++) {
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
            wysiwygEditorPane.requestFocusInWindow();
        });
        button.setText("");
        button.setIcon(UIUtils.getIconFromResources(icon));
        button.setToolTipText(name);
        UIUtils.makeFlat25x25(button);
        updatedButtons.put(button, toggled);
        return button;
    }

    public String getPlainText() {
        Document document = wysiwygEditorPane.getDocument();
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHTML() {
        try {
            Document document = wysiwygEditorPane.getDocument();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            wysiwygEditorKit.write(byteArrayOutputStream, document, 0, document.getLength());
            //            int bodyStart = value.indexOf("<body>") + "<body>".length();
//            int bodyEnd = value.indexOf("</body>");
//            String body = value.substring(bodyStart, bodyEnd).trim();
//            body = body.replace("\n", "<br/>");
//            System.out.println(body);
//            return value.substring(0, bodyStart) + body + "</body></html>";
            return byteArrayOutputStream.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setText(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            wysiwygEditorPane.setText("<html><body><p></p></body></html>");
        } else {
            try {
                int bodyStart = value.indexOf("<body>") + "<body>".length();
                int bodyEnd = value.indexOf("</body>");
                String body = value.substring(bodyStart, bodyEnd).trim();
                if (!body.startsWith("<"))
                    body = "<p>" + body + "</p>";
                wysiwygEditorPane.setText("<html><body>" + body + "</body></html>");
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                wysiwygEditorPane.setText("<html><body><p></p></body></html>");
            }
        }

        // Workaround https://stackoverflow.com/questions/1527021/html-jtextpane-newline-support
//        textPane.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "<br/>\n");
    }

    public enum Mode {
        Compact,
        Full,
        HTML
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
