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
import org.hkijena.jipipe.api.service.JIPipeServiceInitializationSettings;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.commons.components.color.JIPipeDesktopColorChooserDialog;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.launcher.commands.screenshotui.ScreenshotRenderer;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.TooltipUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code JIPipeLauncher screenshot-ui --out <dir> [--only id1,id2]} — boots the
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
        // Synthetic: a parameter collection with one JIPipeFontParameter (the
        // font editor has no node consumer until the Task 10 migration).
        SHOWCASE.put("panel-font", "<font-parameter>");
        // Synthetic: a populated file list (8 items > page size 5) so the
        // inline list editor's pagination (+k more) and grip rows are visible
        // (#1339 pass 2, S2).
        SHOWCASE.put("panel-list-populated", "<list-populated>");
        // Synthetic: an empty file list (S2 empty state: hint, no rows, no footer).
        SHOWCASE.put("panel-list-empty", "<list-empty>");
    }

    private static final int PANEL_WIDTH = 480;
    private static final int DIALOG_WIDTH = 380;
    private static final int DIALOG_HEIGHT = 560;

    /**
     * Panel flags for the showcase capture. Deliberately WITHOUT
     * {@code WITH_SCROLLING}: a JScrollPane reports a clipped preferred height
     * (not its content's), which truncates an off-screen capture. The captured
     * panel is the full, unscrolled form — which is what the VLM must see.
     * (Ruling: Task 0.)
     */
    private static final int PANEL_FLAGS = JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION
            | JIPipeDesktopFormPanel.DOCUMENTATION_BELOW
            | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR
            | JIPipeDesktopFormPanel.DOCUMENTATION_EXTERNAL;

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
                renderColorDialog(outDir, only, rendered);
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
                // Synthetic: one JIPipeFontParameter (no node uses it yet — the
                // migration lands in Task 10).
                FontParameterShowcase root = new FontParameterShowcase();
                panel = new JIPipeDesktopParameterFormPanel(workbench, root, null, PANEL_FLAGS);
            } else if ("<list-populated>".equals(nodeId) || "<list-empty>".equals(nodeId)) {
                // Synthetic file lists (#1339 pass 2, S2): 8 items to show the
                // inline editor's pagination + grip rows; empty to show the
                // hint-only empty state.
                ListShowcase root = new ListShowcase("<list-empty>".equals(nodeId) ? 0 : 8);
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
     * A minimal parameter collection exposing a single {@link JIPipeFontParameter}
     * (harness-only, #1339 — no node uses the font parameter until the Task 10
     * migration). {@link JIPipeDynamicParameterCollection} instantiates the
     * registered type automatically.
     */
    private static class FontParameterShowcase extends JIPipeDynamicParameterCollection {
        FontParameterShowcase() {
            addParameter("font", JIPipeFontParameter.class, "Label font", "A font with family, size, style and text color");
        }
    }

    /**
     * A minimal parameter collection exposing a single {@link PathList} with a
     * given number of entries (harness-only, #1339 pass 2 — the inline list
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

    private static void renderSettingsSheet(JIPipeDesktopDummyWorkbench workbench, Path outDir, List<String> only, List<String> rendered) throws Exception {
        if (!only.isEmpty() && !only.contains("settings-appearance"))
            return;
        // A form-panel consumer (non-parameter rows) — the regression baseline.
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
}
