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

package org.hkijena.jipipe.desktop.commons.components.markup;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.color.JIPipeDesktopColorChooserButton;
import org.hkijena.jipipe.desktop.commons.components.icons.OverlayJIPipeDesktopColorIcon;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.scripting.ScriptUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
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

public class JIPipeDesktopHTMLEditor extends JIPipeDesktopWorkbenchPanel {

    public static final int NONE = 0;
    public static final int WITH_SCROLL_BAR = 1;
    public static final int WITH_DIALOG_EDITOR_BUTTON = 2;
    public static final int NO_TOOLBAR = 4;
    private final Set<String> availableFonts = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    private final Map<String, Action> availableEditorKitActions = new HashMap<>();
    private final boolean enableDialogEditor;
    private final boolean showToolBar;
    private final Map<JToggleButton, BooleanSupplier> updatedButtons = new HashMap<>();
    private JTextPane wysiwygEditorPane;
    private JEditorPane previewPane;
    private JTextArea sourceArea;
    private HTMLEditorKit wysiwygEditorKit;
    private JComboBox<String> fontSelection;
    private JComboBox<Integer> sizeSelection;
    private JIPipeDesktopColorChooserButton foregroundColorButton;
    private boolean isUpdating = false;
    private View view = View.WYSIWYG;
    private boolean expanded = false;
    private JPanel toolBarContainer;
    private JPanel editorContainer;
    private JButton viewButton;
    private JButton densityButton;
    private JButton editInDialogButton;
    private JButton insertImageButton;
    private JButton insertLinkButton;

    public JIPipeDesktopHTMLEditor(JIPipeDesktopWorkbench workbench, Mode mode, int flags) {
        super(workbench);
        this.enableDialogEditor = (flags & WITH_DIALOG_EDITOR_BUTTON) == WITH_DIALOG_EDITOR_BUTTON;
        this.showToolBar = (flags & NO_TOOLBAR) == 0;
        initialize(flags);
        setMode(mode);
        initializeEvents();
    }

    /**
     * Themes an editable combo to the JIPipe look (#1339): one rounded
     * themed outline around the whole box, a flat (unbeveled) field, a flat
     * arrow inside the same box, and a themed selection highlight. Without
     * this, the editable font/size combos fall back to Metal's unthemed
     * bevels - the dark outline around the field and the separate
     * full-bordered arrow button the user reported.
     */
    private static void themeEditableComboBox(JComboBox<?> combo) {
        // Force the (Metal) UI delegate to be installed so the editable field
        // and the dropdown arrow button exist as child components - they are
        // created lazily by the UI, not by the JComboBox constructor.
        combo.getUI();
        Color selBg = ThemeUtils.getCurrentStyle().getSelectionBackground();
        Color selFg = ThemeUtils.getCurrentStyle().getTextForeground();
        // One themed rounded outline around the whole combo (the theme sets
        // ComboBox.background but not ComboBox.border, so Metal's bevel
        // "EditorBorder" was drawn by the field instead - the dark outline).
        Border fieldBorder = UIManager.getBorder("TextField.border");
        combo.setBorder(fieldBorder);

        // The combo's editable field + dropdown arrow (both created by the
        // Metal UI): flatten them so they sit inside the single rounded box.
        for (Component c : combo.getComponents()) {
            JTextComponent tc = findTextComponent(c);
            if (tc != null) {
                tc.setBackground(ThemeUtils.getCurrentStyle().getFormBackground());
                // Drop the Metal bevel, keep the themed field padding.
                tc.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
                tc.setSelectionColor(selBg);
                tc.setSelectedTextColor(selFg);
            }
            if (c instanceof JButton && c.getClass().getName().endsWith("ComboBoxButton")) {
                // The opaque, beveled MetalComboBoxButton → flat arrow with no
                // separate button frame.
                JButton arrow = (JButton) c;
                arrow.setOpaque(false);
                arrow.setContentAreaFilled(false);
                arrow.setBorderPainted(false);
                arrow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
                arrow.setFocusable(false);
            }
        }
        // The dropdown popup's item selection (opaque renderer).
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : value.toString());
            label.setOpaque(true);
            label.setBackground(isSelected ? selBg : ThemeUtils.getCurrentStyle().getFormBackground());
            label.setForeground(selFg);
            return label;
        });
    }

    /**
     * Finds the first descendant {@link JTextComponent} (null if none).
     */
    private static JTextComponent findTextComponent(Component c) {
        if (c instanceof JTextComponent)
            return (JTextComponent) c;
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTextComponent found = findTextComponent(child);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Makes a flat, 24×24 toolbar button (spec §8.2: toolbar buttons flat
     * 24×24). Kept out of {@code UIUtils} (which only offers 25×25) so the row
     * height stays 24px.
     */
    private static void makeToolbarButton24(AbstractButton b) {
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        b.setFocusable(false);
        b.setPreferredSize(new Dimension(24, 24));
        b.setMinimumSize(new Dimension(24, 24));
        b.setMaximumSize(new Dimension(24, 24));
    }

    /**
     * Returns the legacy {@link Mode} derived from the current view + density
     * state (spec §8.2: the public API is kept stable for existing callers -
     * the parameter editor, the expression/function-builder contexts).
     */
    public Mode getMode() {
        if (view == View.SOURCE) {
            return Mode.HTML;
        }
        return expanded ? Mode.Full : Mode.Compact;
    }

    /**
     * Maps a legacy {@link Mode} onto the new view + density state:
     * HTML → Source view, Full → expanded toolbars, Compact → the compact
     * single-row toolbar. The current view is kept unless the mode demands a
     * different one (HTML → Source), so switching between Compact and Full
     * only changes the toolbar density.
     */
    public void setMode(Mode mode) {
        if (mode == this.modeFromState()) {
            return;
        }
        switch (mode) {
            case HTML -> {
                if (view != View.SOURCE) {
                    setView(View.SOURCE);
                }
            }
            case Full -> {
                if (view == View.SOURCE) {
                    setView(View.WYSIWYG);
                }
                expanded = true;
            }
            case Compact -> {
                if (view == View.SOURCE) {
                    setView(View.WYSIWYG);
                }
                expanded = false;
            }
        }
        reloadToolbar();
        reloadEditor();
    }

    private Mode modeFromState() {
        if (view == View.SOURCE) {
            return Mode.HTML;
        }
        return expanded ? Mode.Full : Mode.Compact;
    }

    public View getView() {
        return view;
    }

    /**
     * Switches the view (WYSIWYG / Preview / Source), syncing content between
     * the panes on each transition (spec §8.2).
     */
    public void setView(View view) {
        if (view == this.view) {
            return;
        }
        switch (this.view) {
            case WYSIWYG -> wysiwygToOut(view);
            case PREVIEW -> wysiwygToOut(view);
            case SOURCE -> sourceToWysiwyg();
        }
        this.view = view;
        updateViewButton();
        reloadToolbar();
        reloadEditor();
    }

    /**
     * Cycles WYSIWYG → Preview → Source → WYSIWYG.
     */
    public void cycleView() {
        View next = switch (view) {
            case WYSIWYG -> View.PREVIEW;
            case PREVIEW -> View.SOURCE;
            case SOURCE -> View.WYSIWYG;
        };
        setView(next);
    }

    /**
     * Toggles the toolbar between the compact single row and the expanded toolbars.
     */
    public void toggleExpanded() {
        expanded = !expanded;
        updateDensityButton();
        reloadToolbar();
    }

    public boolean isExpanded() {
        return expanded;
    }

    /**
     * WYSIWYG -> Preview/Source: copy the rendered HTML into the target pane.
     */
    private void wysiwygToOut(View target) {
        String html;
        try {
            Document document = wysiwygEditorPane.getDocument();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wysiwygEditorKit.write(out, document, 0, document.getLength());
            html = out.toString();
        } catch (Exception e) {
            html = wysiwygEditorPane.getText();
        }
        switch (target) {
            case PREVIEW -> {
                previewPane.setText(html);
                previewPane.setCaretPosition(0);
            }
            case SOURCE -> sourceArea.setText(html);
            default -> {
            }
        }
    }

    /**
     * Source -> WYSIWYG: parse the edited markup back into the document.
     */
    private void sourceToWysiwyg() {
        setText(sourceArea.getText());
    }

    private void updateViewButton() {
        String icon = switch (view) {
            case WYSIWYG -> "actions/edit-select-text.png";
            case PREVIEW -> "actions/view-preview.png";
            case SOURCE -> "actions/format-text-code.png";
        };
        String tip = switch (view) {
            case WYSIWYG -> "View: WYSIWYG (click for Preview)";
            case PREVIEW -> "View: Preview (click for Source)";
            case SOURCE -> "View: Source (click for WYSIWYG)";
        };
        viewButton.setIcon(JIPipe.RESOURCES.getIcon16(icon));
        viewButton.setToolTipText(tip);
    }

    private void updateDensityButton() {
        String icon = expanded ? "actions/view-compact.png" : "actions/view-fullscreen.png";
        String tip = expanded ? "Collapse the toolbar" : "Expand the toolbar";
        densityButton.setIcon(JIPipe.RESOURCES.getIcon16(icon));
        densityButton.setToolTipText(tip);
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
        if (!showToolBar) {
            // No toolbar at all (the embedded parameter view, #1339): the body is
            // editable, but the formatting controls live in the full dialog
            // opened from the name-row "Edit" button.
            revalidate();
            repaint();
            return;
        }
        if (!expanded) {
            // Compact: a single 24px icon-only row (spec §8.2). B, I, U, color,
            // image, |, view toggle, density toggle, Edit-in-dialog.
            JToolBar toolBar = createCompactToolBar();
            toolBarContainer.add(toolBar);
        } else {
            // Expanded: the former Full toolbars (font/size/color, formatting,
            // alignment, insert), plus the view + density toggles.
            JToolBar toolBar1 = new JToolBar();
            toolBar1.setAlignmentX(0f);
            toolBar1.add(fontSelection);
            toolBar1.add(sizeSelection);
            toolBar1.addSeparator();
            toolBar1.add(foregroundColorButton);
            toolBar1.add(createFormatActionButton(new StyledEditorKit.BoldAction(),
                    this::selectionIsBold,
                    "Format bold",
                    "actions/format-text-bold.png"));
            toolBar1.add(createFormatActionButton(new StyledEditorKit.ItalicAction(),
                    this::selectionIsItalic,
                    "Format italic",
                    "actions/format-text-italic.png"));
            toolBar1.add(createFormatActionButton(new StyledEditorKit.UnderlineAction(),
                    this::selectionIsUnderline,
                    "Format underlined",
                    "actions/format-text-underline.png"));
            toolBar1.add(createFormatActionButton(new StrikeThroughAction(),
                    this::selectionIsStrikeThrough,
                    "Format strike-through",
                    "actions/format-text-strikethrough.png"));
            toolBar1.addSeparator();
            toolBar1.add(insertLinkButton);
            toolBar1.add(insertImageButton);
            toolBar1.add(Box.createHorizontalGlue());
            toolBar1.add(viewButton);
            toolBar1.add(densityButton);
            if (enableDialogEditor)
                toolBar1.add(editInDialogButton);

            JToolBar toolBar2 = new JToolBar();
            toolBar2.setAlignmentX(0f);
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
            // A trailing glue (like toolBar1's) makes this row's max width
            // unbounded, so the BoxLayout stretches it to the full editor width
            // - otherwise the row (and its bottom border line) stops at the last
            // icon (#1339).
            toolBar2.add(Box.createHorizontalGlue());

            toolBarContainer.add(toolBar1);
            toolBarContainer.add(toolBar2);
        }
        revalidate();
        repaint();
    }

    private JToolBar createCompactToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setAlignmentX(0f);
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
        toolBar.add(foregroundColorButton);
        toolBar.addSeparator();
        toolBar.add(insertImageButton);
        toolBar.addSeparator();
        toolBar.add(viewButton);
        toolBar.add(densityButton);
        if (enableDialogEditor)
            toolBar.add(editInDialogButton);
        return toolBar;
    }

    private void reloadEditor() {
        editorContainer.removeAll();
        switch (view) {
            case PREVIEW -> editorContainer.add(previewPane, BorderLayout.CENTER);
            case SOURCE -> editorContainer.add(new JScrollPane(sourceArea), BorderLayout.CENTER);
            default -> editorContainer.add(wysiwygEditorPane, BorderLayout.CENTER);
        }
        editorContainer.revalidate();
        editorContainer.repaint();
    }

    private void initialize(int flags) {
        setLayout(new BorderLayout());

        // WYSIWYG editor
        wysiwygEditorPane = new JTextPane();
        wysiwygEditorPane.setContentType("text/html");
        wysiwygEditorKit = new JIPipeDesktopHTMLEditorKit();
        for (Action action : wysiwygEditorKit.getActions()) {
            availableEditorKitActions.put(action.getValue(Action.NAME) + "", action);
        }

        wysiwygEditorPane.setEditorKit(wysiwygEditorKit);
        wysiwygEditorKit.getStyleSheet().addRule("body { font-family: Dialog; }" +
                "p { margin: 0; }");
        wysiwygEditorPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        wysiwygEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    if (e.getURL() == null) {
                        JOptionPane.showMessageDialog(this, "This link is invalid! (Content is '" + e.getDescription() + "')", "Visit URL", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (JOptionPane.showConfirmDialog(this, "Do you really want to visit " + e.getURL() + "?", "Visit URL", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        UIUtils.desktopOpenURL(e.getURL().toURI(), false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Source view: a plain text area showing the raw HTML markup (no
        // syntax-highlight dependency - #1124 will replace this later).
        sourceArea = new JTextArea();
        sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
        sourceArea.setBackground(UIManager.getColor("TextArea.background"));
        sourceArea.setLineWrap(false);
        sourceArea.setWrapStyleWord(false);

        // Preview view: a read-only render of the current HTML.
        previewPane = new JEditorPane();
        previewPane.setEditable(false);
        previewPane.setContentType("text/html");
        previewPane.setEditorKit(new HTMLEditorKit());

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
        updateViewButton();
        updateDensityButton();
        // Chrome (spec §8.2): the embedded parameter view (which carries the
        // name-row "Edit" button that opens the dialog) uses a modern
        // left-accent bar (#1339); the standalone full dialog carries none -
        // its own window is the frame.
        setBorder(enableDialogEditor ? UIUtils.createLeftAccentBorder() : BorderFactory.createEmptyBorder());
        reloadToolbar();
        reloadEditor();
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
        // The font/size combos are editable, so focusing one selects all its
        // text - and the unthemed Metal bevels/selection draw the dark
        // outlines in the font/size combos (the "weird combobox" design,
        // #1339). Theme the whole combo to the JIPipe look.
        themeEditableComboBox(fontSelection);
        themeEditableComboBox(sizeSelection);

        // Color selection
        foregroundColorButton = new JIPipeDesktopColorChooserButton("");
        foregroundColorButton.setIcon(new OverlayJIPipeDesktopColorIcon(JIPipe.RESOURCES.getIcon16("actions/format-stroke-color.png"),
                new Rectangle(0, 14, 16, 2), true, false));
        UIUtils.makeButtonFlat25x25(foregroundColorButton);
        foregroundColorButton.setPreferredSize(new Dimension(24, 24));
        foregroundColorButton.setMinimumSize(new Dimension(24, 24));
        foregroundColorButton.setMaximumSize(new Dimension(24, 24));
        foregroundColorButton.setToolTipText("Set color");
        foregroundColorButton.getColorChosenEventEmitter().subscribeLambda((emitter, event) -> {
            if (!isUpdating) {
                new StyledEditorKit.ForegroundAction("set-foreground", event.getColor()).actionPerformed(
                        new ActionEvent(wysiwygEditorPane, 0, "set-foreground")
                );
                updateSelection();
                wysiwygEditorPane.requestFocusInWindow();
            }
        });

        // View toggle (WYSIWYG -> Preview -> Source, spec §8.2) + density
        // toggle (compact <-> expanded), both flat 24x24.
        viewButton = new JButton();
        makeToolbarButton24(viewButton);
        viewButton.addActionListener(e -> cycleView());
        densityButton = new JButton();
        makeToolbarButton24(densityButton);
        densityButton.addActionListener(e -> toggleExpanded());

        // Edit in dialog button
        editInDialogButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/open-in-new-window.png"));
        makeToolbarButton24(editInDialogButton);
        editInDialogButton.setToolTipText("Edit in dedicated window");
        editInDialogButton.addActionListener(e -> {
            HTMLText result = UIUtils.getHTMLByDialog(getDesktopWorkbench(), this, "Edit", null, new HTMLText(getHTML()));
            if (result != null) {
                setText(result.getHtml());
            }
        });

        // Insert image
        insertImageButton = new JButton("Image", JIPipe.RESOURCES.getIcon16("actions/insert-image.png"));
        makeToolbarButton24(insertImageButton);
        JPopupMenu insertImageMenu = UIUtils.addPopupMenuToButton(insertImageButton);
        {
            JMenuItem insertImageFromFileItem = new JMenuItem("Embed from file ...", JIPipe.RESOURCES.getIcon16("actions/document-open-folder.png"));
            insertImageFromFileItem.addActionListener(e -> insertImageFromFile());
            insertImageMenu.add(insertImageFromFileItem);

            JMenuItem insertImageFromClipboardItem = new JMenuItem("Embed from clipboard", JIPipe.RESOURCES.getIcon16("actions/edit-paste.png"));
            insertImageFromClipboardItem.addActionListener(e -> insertImageFromClipboard());
            insertImageMenu.add(insertImageFromClipboardItem);

            insertImageMenu.addSeparator();

            JMenuItem insertImageFromURL = new JMenuItem("Link from URL", JIPipe.RESOURCES.getIcon16("actions/edit-link.png"));
            insertImageFromURL.addActionListener(e -> insertImageURL());
            insertImageMenu.add(insertImageFromURL);
        }

        // Insert link
        insertLinkButton = new JButton("Link", JIPipe.RESOURCES.getIcon16("actions/insert-link.png"));
        makeToolbarButton24(insertLinkButton);
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
        if (JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), parameterCollection, null, "Insert link", JIPipeDesktopFormPanel.WITH_SCROLLING)) {
            try {
                int caretPosition = wysiwygEditorPane.getCaretPosition();
                HTMLDocument document = (HTMLDocument) wysiwygEditorPane.getDocument();
                String url = parameterCollection.get("url").get(String.class);
                String text = StringUtils.orElse(parameterCollection.get("text").get(String.class), url);
                wysiwygEditorKit.insertHTML(document, caretPosition, "<a href=\"" + ScriptUtils.escapeString(url) + "\" >" + text + "</a>", 0, 0, HTML.Tag.A);
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
        Path path = JIPipeDesktop.openFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Insert image from file", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_IMAGEIO_IMAGES);
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
                    JIPipe.RESOURCES.getIcon16("apps/jipipe.png"), new Object[]{
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
        button.setIcon(JIPipe.RESOURCES.getIcon16(icon));
        button.setToolTipText(name);
        makeToolbarButton24(button);
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

    /**
     * The three view states (spec §8.2): WYSIWYG (the live editable HTML pane),
     * Preview (a read-only render of the current HTML), and Source (a plain
     * editable text area showing the raw markup). This is an internal view
     * concern, separate from the legacy {@link Mode} (which callers use for
     * toolbar density) and from the compact/expanded density toggle.
     */
    public enum View {
        WYSIWYG,
        PREVIEW,
        SOURCE
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
