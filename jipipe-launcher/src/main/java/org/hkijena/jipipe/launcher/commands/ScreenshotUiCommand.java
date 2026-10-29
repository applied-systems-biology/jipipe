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

package org.hkijena.jipipe.launcher.commands;

import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.runtimepartitioning.RuntimePartitionReferenceParameter;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializationSettings;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.color.JIPipeDesktopColorChooserDialog;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditor;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.launcher.commands.screenshotui.ScreenshotRenderer;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.matrix.Matrix2DFloat;
import org.hkijena.jipipe.plugins.parameters.library.primitives.*;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.IntegerList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalBooleanParameter;
import org.hkijena.jipipe.plugins.parameters.library.references.IconRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeNodeInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeParameterTypeInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.roi.FixedMargin;
import org.hkijena.jipipe.plugins.parameters.library.roi.InnerMargin;
import org.hkijena.jipipe.plugins.parameters.ui.api.JIPipeDesktopListEditorDialog;
import org.hkijena.jipipe.plugins.parameters.ui.api.JIPipeListParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.ui.library.JIPipeDesktopFontFamilySizeStyleColorParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.ui.library.JIPipeDesktopFontFamilySizeStyleParameterEditorUI;
import org.hkijena.jipipe.plugins.settings.application.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.TooltipUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * {@code JIPipeLauncher screenshot-ui --out <dir> [--only id1,id2]} - boots the
 * GUI stack in-process and renders a fixed set of showcase parameter panels and
 * dialogs to PNGs for VLM-driven before/after verification (the #1339
 * parameter UX overhaul). Filenames are stable; the per-task VLM prompts key
 * off them.
 * <p>
 * This is a verification harness, not a feature: it exists so an agent can
 * render the real (themed) parameter panel of a real node to an image and ask
 * a VLM whether a given redesign landed.
 */
public class ScreenshotUiCommand {

    /**
     * Stable showcase set: filename → node type id. The settings sheet and the
     * color dialog are rendered separately (they are not nodes).
     */
    private static final Map<String, String> SHOWCASE = new LinkedHashMap<>();
    /**
     * S8 render-out audit (#1339 pass 2): a per-editor showcase set. For every
     * parameter editor without a dedicated node showcase, a synthetic
     * one-parameter collection ({@code <editor-...>} → field class) rendered
     * at the standard panel width. The sweep checks the pass-1 form contract
     * per editor: border present, consistent height family, left-aligned
     * value, options/properties column, no self-drawn name, no clipping.
     */
    private static final Map<String, Class<?>> PER_EDITOR = new LinkedHashMap<>();
    private static final int PANEL_WIDTH = 480;
    private static final int DIALOG_WIDTH = 380;
    private static final int DIALOG_HEIGHT = 560;
    private static final int LIST_DIALOG_WIDTH = 640;
    private static final int LIST_DIALOG_HEIGHT = 520;
    /**
     * Panel flags for the showcase capture. Deliberately WITHOUT
     * {@code WITH_SCROLLING}: a JScrollPane reports a clipped preferred height
     * (not its content's), which truncates an off-screen capture. The captured
     * panel is the full, unscrolled form - which is what the VLM must see.
     * (Ruling: Task 0.)
     */
    private static final int PANEL_FLAGS = JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION
            | JIPipeDesktopFormPanel.DOCUMENTATION_BELOW
            | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR
            | JIPipeDesktopFormPanel.DOCUMENTATION_EXTERNAL;

    static {
        SHOWCASE.put("panel-stack-to-montage", "ij1-dimensions-stack2montage-v2");
        SHOWCASE.put("panel-import-file-list", "import-file-list");
        SHOWCASE.put("panel-roi3d-statistics", "ij3d-measure-roi3d");
        SHOWCASE.put("panel-combo-boolean", "ij1-morph-reconstruct-2d");
        // The generic enum editor in both modes: 42-value ColorMap (BOX) plus a
        // boolean; the montage panel covers the small (<=5) COMBO enums.
        SHOWCASE.put("panel-enum-box", "ij1-set-lut-from-color-map");
        // #1339 font migration: the split font rows are now one "Font" preview row.
        SHOWCASE.put("panel-draw-text-roi", "ij1-roi-draw-text");
        // #1339 font family: the node with the color-less "Label font" row
        // (adaptive editor: family/size/style, no color section) plus the
        // separate optional "Label color" row.
        SHOWCASE.put("panel-convert-2d-roi-to-rgb", "ij1-roi-to-rgb");
        // Synthetic: a parameter collection with one JIPipeFontFamilySizeStyleColorParameter (the
        // font editor has no node consumer until the Task 10 migration).
        SHOWCASE.put("panel-font", "<font-parameter>");
        // Synthetic: a populated file list (8 items > page size 5) so the
        // inline list editor's pagination (+k more) and grip rows are visible
        // (#1339 pass 2, S2).
        SHOWCASE.put("panel-list-populated", "<list-populated>");
        // Synthetic: an empty file list (S2 empty state: hint, no rows, no footer).
        SHOWCASE.put("panel-list-empty", "<list-empty>");
        // Synthetic: a bounded 0-1 double annotated style=SLIDER at runtime
        // (#1339 pass 2, S6) - the number editor renders a slider, not a
        // text field + spinners.
        SHOWCASE.put("panel-number-slider", "<number-slider>");
        // Synthetic: an HTMLText field (#1339 pass 2, S7) - the modernized
        // compact-toolbar HTML widget (B/I/U, color, image, |, view toggle,
        // density toggle, edit-in-dialog).
        SHOWCASE.put("panel-html-text", "<html-text>");
    }

    static {
        PER_EDITOR.put("editor-boolean", Boolean.class);
        PER_EDITOR.put("editor-number-field", Double.class);
        PER_EDITOR.put("editor-integer-list", IntegerList.class);
        PER_EDITOR.put("editor-string", String.class);
        PER_EDITOR.put("editor-string-or-double", StringOrDouble.class);
        PER_EDITOR.put("editor-dynamic-enum", DynamicStringEnumParameter.class);
        PER_EDITOR.put("editor-dynamic-set", DynamicStringSetParameter.class);
        PER_EDITOR.put("editor-path-list", PathList.class);
        PER_EDITOR.put("editor-collection-list", JIPipeParameterCollectionList.class);
        PER_EDITOR.put("editor-matrix", Matrix2DFloat.class);
        PER_EDITOR.put("editor-margin-fixed", FixedMargin.class);
        PER_EDITOR.put("editor-margin-inner", InnerMargin.class);
        PER_EDITOR.put("editor-color", Color.class);
        PER_EDITOR.put("editor-optional-boolean", OptionalBooleanParameter.class);
        PER_EDITOR.put("editor-optional-color", OptionalColorParameter.class);
        PER_EDITOR.put("editor-expression", JIPipeExpressionParameter.class);
        PER_EDITOR.put("editor-runtime-partition-ref", RuntimePartitionReferenceParameter.class);
        // #1339 item 1/6: the ref editors that switched to the searchable
        // RichSelectionBox comboboxes (and the icon ref whose redundant Edit
        // button was removed) - per-editor renders for the VLM loop.
        PER_EDITOR.put("editor-data-info-ref", JIPipeDataInfoRef.class);
        PER_EDITOR.put("editor-node-info-ref", JIPipeNodeInfoRef.class);
        PER_EDITOR.put("editor-parameter-type-info-ref", JIPipeParameterTypeInfoRef.class);
        PER_EDITOR.put("editor-artifact-query", JIPipeArtifactQueryParameter.class);
        PER_EDITOR.put("editor-icon-ref", IconRef.class);
        // #1339 font family: the size-style type - the adaptive editor must
        // render family + size + style with NO color section.
        PER_EDITOR.put("editor-font-family-size-style", JIPipeFontFamilySizeStyleParameter.class);
    }

    private ScreenshotUiCommand() {
    }

    public static void doScreenshotUi(List<String> argsList) {
        Path outDir = parseOut(argsList);
        List<String> only = parseOnly(argsList);

        // Boot the full GUI stack in-process (mirrors GuiCommand / HeadlessRunCommand).
        final ImageJ ij = new ImageJ();
        JIPipeServiceInitializationSettings init = new JIPipeServiceInitializationSettings();
        init.setVerbose(false);
        JIPipe.createInstance(ij.context(), init);
        JIPipeExtensionApplicationSettings ext = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        ext.setSilent(true);
        JIPipe.getInstance().ensureInitialized();

        // Apply the desktop theme before building any Swing component.
        ThemeUtils.applyThemeFromSettings();

        final JIPipeDesktopDummyWorkbench workbench = new JIPipeDesktopDummyWorkbench();
        final List<String> rendered = new ArrayList<>();

        SwingUtilities.invokeLater(() -> {
            try {
                renderNodePanels(workbench, outDir, only, rendered);
                renderPerEditor(workbench, outDir, only, rendered);
                renderFontPopout(workbench, outDir, only, rendered);
                renderHtmlEditorViews(workbench, outDir, only, rendered);
                renderHtmlEditorFull(workbench, outDir, only, rendered);
                renderColorDialog(outDir, only, rendered);
                renderListEditorDialogs(workbench, outDir, only, rendered);
                renderSettingsSheet(workbench, outDir, only, rendered);
                System.out.println("[screenshot-ui] rendered: " + rendered);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                JIPipe.exitLater(0);
            }
        });
    }

    private static void renderNodePanels(JIPipeDesktopDummyWorkbench workbench, Path outDir, List<String> only, List<String> rendered) throws Exception {
        for (Map.Entry<String, String> e : SHOWCASE.entrySet()) {
            String name = e.getKey();
            if (!only.isEmpty() && !only.contains(name))
                continue;
            String nodeId = e.getValue();
            JIPipeDesktopParameterFormPanel panel;
            if ("<font-parameter>".equals(nodeId)) {
                // Synthetic: one JIPipeFontFamilySizeStyleColorParameter (no node uses it yet - the
                // migration lands in Task 10).
                FontParameterShowcase root = new FontParameterShowcase();
                panel = new JIPipeDesktopParameterFormPanel(workbench, root, null, PANEL_FLAGS);
            } else if ("<list-populated>".equals(nodeId) || "<list-empty>".equals(nodeId)) {
                // Synthetic file lists (#1339 pass 2, S2): 8 items to show the
                // inline editor's pagination + grip rows; empty to show the
                // hint-only empty state.
                ListShowcase root = new ListShowcase("<list-empty>".equals(nodeId) ? 0 : 8);
                panel = new JIPipeDesktopParameterFormPanel(workbench, root, null, PANEL_FLAGS);
            } else if ("<number-slider>".equals(nodeId)) {
                // Synthetic bounded 0-1 double annotated style=SLIDER at runtime
                // (#1339 pass 2, S6).
                NumberSliderShowcase root = new NumberSliderShowcase();
                panel = new JIPipeDesktopParameterFormPanel(workbench, root, null, PANEL_FLAGS);
            } else if ("<html-text>".equals(nodeId)) {
                // Synthetic HTMLText field with sample rich content (#1339
                // pass 2, S7) so the modernized widget's toolbar + body are
                // visible in the capture.
                HtmlTextShowcase root = new HtmlTextShowcase();
                panel = new JIPipeDesktopParameterFormPanel(workbench, root, null, PANEL_FLAGS);
            } else {
                JIPipeGraphNode node = JIPipe.createNode(nodeId);
                panel = new JIPipeDesktopParameterFormPanel(
                        workbench, node, TooltipUtils.getAlgorithmDocumentation(node), PANEL_FLAGS);
            }
            ScreenshotRenderer.renderComponent(panel, PANEL_WIDTH, outDir.resolve(name + ".png"));
            rendered.add(name);
        }
    }

    /**
     * S8 render-out audit: renders one synthetic one-parameter collection per
     * parameter editor in {@link #PER_EDITOR} at the standard panel width, so
     * the whole editor family is swept (not just the node showcases).
     */
    private static void renderPerEditor(JIPipeDesktopDummyWorkbench workbench, Path outDir, List<String> only, List<String> rendered) throws Exception {
        for (Map.Entry<String, Class<?>> e : PER_EDITOR.entrySet()) {
            String name = e.getKey();
            if (!only.isEmpty() && !only.contains(name))
                continue;
            JIPipeDynamicParameterCollection root = new JIPipeDynamicParameterCollection();
            root.addParameter("value", e.getValue(), name.substring("editor-".length()).replace('-', ' '), "S8 per-editor audit");
            JIPipeDesktopParameterFormPanel panel = new JIPipeDesktopParameterFormPanel(workbench, root, null, PANEL_FLAGS);
            ScreenshotRenderer.renderComponent(panel, PANEL_WIDTH, outDir.resolve(name + ".png"));
            rendered.add(name);
        }
    }

    /**
     * The HTML editor 3-state view showcase (#1339 pass 2, S7). The view
     * toggle (WYSIWYG / Preview / Source) can't be exercised in a static node
     * panel (it's a click interaction), so the widget is rendered directly -
     * one capture per view state - to verify the brief's claim that Preview
     * renders the HTML read-only and Source shows the raw markup.
     */
    private static void renderHtmlEditorViews(JIPipeDesktopWorkbench workbench, Path outDir, List<String> only, List<String> rendered) throws Exception {
        if (!only.isEmpty() && !only.contains("html-editor-views"))
            return;
        String sample = "<html><body><p>Hello <b>world</b>, this is a <i>rich</i> description.</p></body></html>";
        JIPipeDesktopHTMLEditor editor =
                new JIPipeDesktopHTMLEditor(workbench, JIPipeDesktopHTMLEditor.Mode.Compact, JIPipeDesktopHTMLEditor.WITH_DIALOG_EDITOR_BUTTON);
        editor.setText(sample);
        int width = Math.max(360, editor.getPreferredSize().width);

        // WYSIWYG (default)
        editor.setView(JIPipeDesktopHTMLEditor.View.WYSIWYG);
        ScreenshotRenderer.renderFixed(editor, width, 200, outDir.resolve("html-editor-wysiwyg.png"));
        // Preview (read-only render)
        editor.setView(JIPipeDesktopHTMLEditor.View.PREVIEW);
        ScreenshotRenderer.renderFixed(editor, width, 200, outDir.resolve("html-editor-preview.png"));
        // Source (raw markup)
        editor.setView(JIPipeDesktopHTMLEditor.View.SOURCE);
        ScreenshotRenderer.renderFixed(editor, width, 200, outDir.resolve("html-editor-source.png"));
        rendered.add("html-editor-views");
    }

    /**
     * The HTML editor in FULL (expanded) mode - the state with the two-row
     * toolbar (font/size combos on row 1, formatting/alignment on row 2).
     * This is the state the user's dialog screenshot captured, so it is the
     * one to verify the toolbar left-alignment and the combo selection
     * theming (#1339).
     */
    private static void renderHtmlEditorFull(JIPipeDesktopWorkbench workbench, Path outDir, List<String> only, List<String> rendered) throws Exception {
        if (!only.isEmpty() && !only.contains("html-editor-full"))
            return;
        String sample = "<html><body><p>Hello <b>world</b>, this is a <i>rich</i> description.</p></body></html>";
        JIPipeDesktopHTMLEditor editor =
                new JIPipeDesktopHTMLEditor(workbench, JIPipeDesktopHTMLEditor.Mode.Full, JIPipeDesktopHTMLEditor.WITH_SCROLL_BAR);
        editor.setText(sample);
        ScreenshotRenderer.renderFixed(editor, 720, 260, outDir.resolve("html-editor-full.png"));
        rendered.add("html-editor-full");
    }

    private static void renderColorDialog(Path outDir, List<String> only, List<String> rendered) throws Exception {
        if (!only.isEmpty() && !only.contains("dialog-color") && !only.contains("dialog-color-lab"))
            return;
        // The modern dialog (#1339). Rendered at a fixed size, off-screen (the
        // dialog itself is never shown; its content pane is captured). The
        // alpha-capable capture uses a semi-transparent color so the checkerboard
        // swatch, the α rail, and the 8-digit hex are all visible.
        Color initial = new Color(59, 157, 255, 128);
        if (only.isEmpty() || only.contains("dialog-color")) {
            JIPipeDesktopColorChooserDialog dialog =
                    new JIPipeDesktopColorChooserDialog(null, "Select color", initial, true);
            ScreenshotRenderer.renderFixed(dialog.getContentPane(), DIALOG_WIDTH, DIALOG_HEIGHT, outDir.resolve("dialog-color.png"));
            rendered.add("dialog-color");
        }
        if (only.isEmpty() || only.contains("dialog-color-lab")) {
            JIPipeDesktopColorChooserDialog dialog =
                    new JIPipeDesktopColorChooserDialog(null, "Select color", initial, false);
            dialog.showLabTab();
            ScreenshotRenderer.renderFixed(dialog.getContentPane(), DIALOG_WIDTH, DIALOG_HEIGHT, outDir.resolve("dialog-color-lab.png"));
            rendered.add("dialog-color-lab");
        }
    }

    /**
     * The list editor modal showcases (#1339 pass 2, S2c). The modal is
     * app-modal, so it is never shown - the content pane is constructed and
     * captured off-screen at its initial size (640×520). Each showcase
     * builds a synthetic list, obtains the registered list editor instance
     * for a synthetic parameter access (the same factory call the list
     * editors' rows use internally), and renders
     * {@code new JIPipeDesktopListEditorDialog(null, editor).getContentPane()}.
     */
    private static void renderListEditorDialogs(JIPipeDesktopDummyWorkbench workbench, Path outDir, List<String> only, List<String> rendered) throws Exception {
        if (only.isEmpty() || only.contains("dialog-list-editor")) {
            // A plain file list with 8 items (more than the inline page size of
            // 5) so the modal's full-list body is visible.
            JIPipeDynamicParameterCollection root = new JIPipeDynamicParameterCollection();
            JIPipeMutableParameterAccess access =
                    root.addParameter("files", PathList.class, "Files", "A list of files");
            PathList list = (PathList) JIPipe.getParameterTypes().getInfoByFieldClass(PathList.class).newInstance();
            for (int i = 1; i <= 8; i++) {
                list.add(Paths.get("/data/sample" + i + ".tif"));
            }
            access.set(list);
            ScreenshotRenderer.renderFixed(
                    new JIPipeDesktopListEditorDialog(null, listEditorInstance(workbench, access)).getContentPane(),
                    LIST_DIALOG_WIDTH, LIST_DIALOG_HEIGHT,
                    outDir.resolve("dialog-list-editor.png"));
            rendered.add("dialog-list-editor");
        }
        if (only.isEmpty() || only.contains("dialog-list-collection")) {
            // A parameter collection list with 2 items, each rendered as a
            // nested mini form (Review Focus #5: nested fully-editable rows).
            // Mutate the STORED instance (the one addParameter defaults), not a
            // separately created list: access.set() skips the store when the
            // new value equals the current (empty) one, and the editor reads
            // the stored value. The template carries a text + a number so each
            // item shows real editable fields.
            JIPipeDynamicParameterCollection root = new JIPipeDynamicParameterCollection();
            JIPipeMutableParameterAccess access = root.addParameter("collections",
                    JIPipeParameterCollectionList.class, "Collections", "A list of parameter collections");
            JIPipeParameterCollectionList list =
                    root.getValue("collections", JIPipeParameterCollectionList.class);
            list.getTemplate().addParameter("label", String.class, "Label", "A text label");
            list.getTemplate().addParameter("weight", Double.class, "Weight", "A number");
            list.addNewInstance();
            list.addNewInstance();
            ScreenshotRenderer.renderFixed(
                    new JIPipeDesktopListEditorDialog(null, listEditorInstance(workbench, access)).getContentPane(),
                    LIST_DIALOG_WIDTH, LIST_DIALOG_HEIGHT,
                    outDir.resolve("dialog-list-collection.png"));
            rendered.add("dialog-list-collection");
        }
    }

    /**
     * Creates the registered editor instance for a list parameter access -
     * the same factory call the list editors' rows use internally (a
     * synthetic tree over the access, the dummy workbench, null parent).
     */
    private static JIPipeListParameterEditorUI listEditorInstance(JIPipeDesktopDummyWorkbench workbench, JIPipeMutableParameterAccess access) {
        return (JIPipeListParameterEditorUI) JIPipe.getParameterTypes().createEditorInstance(
                access, workbench, new JIPipeParameterTree(access), null);
    }

    /**
     * The font popout showcase (#1339 pass 2, S4). The popout is a
     * {@link JPopupMenu}, which the harness can't capture while shown - but
     * its content panel can: build a font editor instance and render
     * {@code getPopoutContent()}. The form panel sizes the label column to
     * the widest label automatically; the preview band's fixed preferred
     * width (300px) drives the content column. Render at at least the brief's
     * 320px, widened to the content's natural preferred width so the band
     * (which drives that width) is not itself clipped by the capture.
     */
    private static void renderFontPopout(JIPipeDesktopWorkbench workbench, Path outDir, List<String> only, List<String> rendered) throws Exception {
        if (!only.isEmpty() && !only.contains("popup-font") && !only.contains("popup-font-size-style"))
            return;
        if (only.isEmpty() || only.contains("popup-font")) {
            JIPipeDesktopFontFamilySizeStyleColorParameterEditorUI fontEditor = (JIPipeDesktopFontFamilySizeStyleColorParameterEditorUI)
                    JIPipe.getParameterTypes().createEditorInstanceWithEmbeddedValue(workbench, JIPipeFontFamilySizeStyleColorParameter.class);
            JComponent content = (JComponent) fontEditor.getPopoutContent();
            int width = Math.max(320, content.getPreferredSize().width);
            ScreenshotRenderer.renderComponent(content, width, outDir.resolve("popup-font.png"));
            rendered.add("popup-font");
        }
        if (only.isEmpty() || only.contains("popup-font-size-style")) {
            // The color-less variant (Task 8/9): the same factory mechanism as
            // popup-font, cast to the size-style thin editor - its popout must
            // show family + size + style and NO color section (Review Focus #5).
            JIPipeDesktopFontFamilySizeStyleParameterEditorUI fontEditor = (JIPipeDesktopFontFamilySizeStyleParameterEditorUI)
                    JIPipe.getParameterTypes().createEditorInstanceWithEmbeddedValue(workbench, JIPipeFontFamilySizeStyleParameter.class);
            JComponent content = (JComponent) fontEditor.getPopoutContent();
            int width = Math.max(320, content.getPreferredSize().width);
            ScreenshotRenderer.renderComponent(content, width, outDir.resolve("popup-font-size-style.png"));
            rendered.add("popup-font-size-style");
        }
    }

    private static void renderSettingsSheet(JIPipeDesktopDummyWorkbench workbench, Path outDir, List<String> only, List<String> rendered) throws Exception {
        if (!only.isEmpty() && !only.contains("settings-appearance"))
            return;
        // A form-panel consumer (non-parameter rows) - the regression baseline.
        JIPipeDesktopParameterFormPanel panel = new JIPipeDesktopParameterFormPanel(
                workbench, new JIPipeGeneralUIApplicationSettings(), null,
                JIPipeDesktopFormPanel.NONE);
        ScreenshotRenderer.renderComponent(panel, PANEL_WIDTH, outDir.resolve("settings-appearance.png"));
        rendered.add("settings-appearance");
    }

    private static Path parseOut(List<String> argsList) {
        for (int i = 0; i < argsList.size() - 1; i++) {
            if ("--out".equals(argsList.get(i))) {
                try {
                    Path p = Paths.get(argsList.get(i + 1));
                    Files.createDirectories(p);
                    return p;
                } catch (Exception e) {
                    throw new RuntimeException("Invalid --out directory: " + argsList.get(i + 1), e);
                }
            }
        }
        throw new RuntimeException("Missing --out <dir> argument");
    }

    private static List<String> parseOnly(List<String> argsList) {
        for (int i = 0; i < argsList.size() - 1; i++) {
            if ("--only".equals(argsList.get(i))) {
                return new ArrayList<>(Arrays.asList(argsList.get(i + 1).split(",")));
            }
        }
        return new ArrayList<>();
    }

    /**
     * A minimal parameter collection exposing a single {@link JIPipeFontFamilySizeStyleColorParameter}
     * (harness-only, #1339 - no node uses the font parameter until the Task 10
     * migration). {@link JIPipeDynamicParameterCollection} instantiates the
     * registered type automatically.
     */
    private static class FontParameterShowcase extends JIPipeDynamicParameterCollection {
        FontParameterShowcase() {
            addParameter("font", JIPipeFontFamilySizeStyleColorParameter.class, "Label font", "A font with family, size, style and text color");
            // #1339 item 7: a non-default size proves the preview's trailing
            // number tracks the parameter's size (not a hardcoded "12").
            JIPipeFontFamilySizeStyleColorParameter font = getValue("font", JIPipeFontFamilySizeStyleColorParameter.class);
            font.setSize(24);
        }
    }

    /**
     * A minimal parameter collection exposing a single {@link PathList} with a
     * given number of entries (harness-only, #1339 pass 2 - the inline list
     * editor's pagination/empty state have no populated node showcase).
     */
    private static class ListShowcase extends JIPipeDynamicParameterCollection {
        ListShowcase(int itemCount) {
            addParameter("files", PathList.class, "Files", "A list of files");
            PathList list = getValue("files", PathList.class);
            for (int i = 1; i <= itemCount; i++) {
                list.add(Paths.get("/data/sample" + i + ".tif"));
            }
        }
    }

    /**
     * A minimal parameter collection exposing a bounded (0–1) double annotated
     * {@code style = SLIDER} at runtime (#1339 pass 2, S6). The proven pattern
     * for a runtime annotation instance is {@code addParameter(..., annotations)}.
     * The value is set to 0.5 so the slider thumb is visibly centered - the VLM
     * verifies the thumb position matches the value.
     */
    private static class NumberSliderShowcase extends JIPipeDynamicParameterCollection {
        NumberSliderShowcase() {
            JIPipeMutableParameterAccess access = addParameter("opacity", Double.class, "Opacity", "A 0-1 opacity",
                    new NumberParameterSettings() {
                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return NumberParameterSettings.class;
                        }

                        @Override
                        public double min() {
                            return 0;
                        }

                        @Override
                        public double max() {
                            return 1;
                        }

                        @Override
                        public double step() {
                            return 0.01;
                        }

                        @Override
                        public NumberParameterSettings.Style style() {
                            return NumberParameterSettings.Style.SLIDER;
                        }
                    });
            access.set(0.5);
            // A second, clearly-different value so a VLM can confirm the thumb
            // position tracks the value (0.2 should sit well left of 0.5).
            JIPipeMutableParameterAccess low = addParameter("low", Double.class, "Low", "A 0-1 opacity (verification)",
                    new NumberParameterSettings() {
                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return NumberParameterSettings.class;
                        }

                        @Override
                        public double min() {
                            return 0;
                        }

                        @Override
                        public double max() {
                            return 1;
                        }

                        @Override
                        public double step() {
                            return 0.01;
                        }

                        @Override
                        public NumberParameterSettings.Style style() {
                            return NumberParameterSettings.Style.SLIDER;
                        }
                    });
            low.set(0.2);
        }
    }

    /**
     * A minimal parameter collection exposing a single {@link HTMLText} with
     * sample rich content (#1339 pass 2, S7) - the modernized compact-toolbar
     * HTML widget has no node showcase in the default set, so a synthetic one
     * renders it for the VLM loop.
     */
    private static class HtmlTextShowcase extends JIPipeDynamicParameterCollection {
        HtmlTextShowcase() {
            JIPipeMutableParameterAccess access = addParameter("description", HTMLText.class, "Description", "An HTML description");
            access.set(new HTMLText("<html><body><p>Hello <b>world</b>, this is a <i>rich</i> description.</p></body></html>"));
        }
    }
}
