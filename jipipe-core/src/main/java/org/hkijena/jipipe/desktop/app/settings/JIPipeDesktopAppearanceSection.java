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
 * See the LICENSE file provided with the full license.
 */

package org.hkijena.jipipe.desktop.app.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeModernThemeStyleParameter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * One merged section of the settings tab (Appearance, Accessibility or the
 * Keyboard placeholder) — the {@code JIPipeDesktopCustomizerDialog} replacement
 * (modern settings system, Task 19).
 * <p>
 * <b>Header.</b> Name, description, a "Modified" badge and a "Reset section"
 * button. The badge and the reset are driven by {@link AppearanceSectionModel}:
 * the badge is re-evaluated whenever one of the section's blocks changes (the
 * component subscribes to the document and unsubscribes in
 * {@link #dispose()}), and the reset removes exactly the section's owned blocks,
 * so every parameter falls back to its default. The section is then re-created,
 * which also refreshes the editors (the sheet bridge has already re-applied the
 * document into the live sheet).
 * <p>
 * <b>Content.</b> The section shows <em>selected</em> parameters of
 * <em>already registered</em> sheets (see
 * {@link AppearanceSectionModel#parametersBySheet(String)}) inside one
 * {@link JIPipeDesktopFormPanel}, grouped by topic — one Appearance section, not
 * two subsheets. Editors are created over the live sheet instances through
 * their {@link JIPipeParameterTree} node (never a new sheet instance), so an
 * edit fires the sheet's {@code ParameterChangedEvent} → the settings engine's
 * {@code SettingsSheetBridge} → the settings document → the debounced
 * auto-save. The Appearance section additionally hosts the live
 * {@link ThemePreviewPanel}, refreshed from the tree's parameter-changed events
 * and from the scale control.
 * <p>
 * <b>UI scale.</b> Not a settings key (it lives in the jaunch config files): a
 * control that reads the current system scale, rounds to 25% and, when released
 * on a changed value, asks whether to apply the scale to Fiji/ImageJ too, calls
 * {@link UIUtils#saveUIScaleToJaunch(float, boolean)} and reports a failed write
 * with an error dialog — exactly the old dialog's Apply path. A "restart JIPipe
 * to apply the UI scale" hint replaces the old modal restart message.
 */
public class JIPipeDesktopAppearanceSection extends JPanel implements JIPipeDesktopWorkbenchAccess, Disposable {

    private static final String THEME_KEY = "theme-v2";
    private static final String THEME_STYLE_KEY = "theme-style";
    private static final String PAGE_APPEARANCE = AppearanceSectionModel.SECTION_APPEARANCE;
    private static final String PAGE_ACCESSIBILITY = AppearanceSectionModel.SECTION_ACCESSIBILITY;
    private static final String PAGE_KEYBOARD = AppearanceSectionModel.SECTION_KEYBOARD;

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopSettingsTab settingsTab;
    private final String page;
    private final SettingsDocument document;
    private final AppearanceSectionModel model;
    private final JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final ThemePreviewPanel previewPanel = new ThemePreviewPanel();

    /**
     * The composed sheets' parameter trees, keyed by their block path. The trees
     * are kept alive for the editors' lifetime (the editors address the live
     * sheet through the access).
     */
    private final Map<String, JIPipeParameterTree> trees = new LinkedHashMap<>();

    private final JLabel modifiedBadge = new JLabel("Modified");
    private final Consumer<JsonNode> documentListener = node -> refreshModifiedBadge();

    /**
     * The system scale at construction: the reference the scale control is
     * displayed and corrected against (the old dialog's {@code systemScale}).
     */
    private final double systemScale;
    private final JSlider scaleSlider = new JSlider(25, 500, 100);
    private final JLabel scaleLabel = new JLabel("100%");

    private boolean disposed;

    /**
     * @param workbench   the workbench
     * @param settingsTab the hosting tab (re-creates the section after a reset)
     * @param page        the merged section to render ({@link AppearanceSectionModel#SECTION_APPEARANCE}, ...)
     */
    public JIPipeDesktopAppearanceSection(JIPipeDesktopWorkbench workbench,
                                          JIPipeDesktopSettingsTab settingsTab,
                                          String page) {
        this.workbench = workbench;
        this.settingsTab = settingsTab;
        this.page = page;
        this.document = applicationDocument();
        this.model = AppearanceSectionModel.forSection(document, page);
        this.systemScale = UIUtils.getScale(workbench != null ? workbench.getWindow() : null);
        initialize();
        model.subscribe(documentListener);
    }

    /**
     * @return the live APPLICATION settings document (materialized on demand)
     */
    public static SettingsDocument applicationDocument() {
        JIPipeSettingsServiceComponent service = JIPipe.getSettingsService();
        if (service == null) {
            throw new IllegalStateException("The settings service is not available");
        }
        return service.document(SettingsScope.APPLICATION);
    }

    private static int getRoundedScale(int scale) {
        return Math.round(scale / 25f) * 25;
    }

    /**
     * Opens the settings tab on the merged Appearance section — the entry point
     * that replaced the "Customize JIPipe" dialog (Tools menu, welcome panel).
     *
     * @param workbench the workbench (must be a project workbench)
     */
    public static void openAppearance(JIPipeDesktopWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench projectWorkbench) {
            projectWorkbench.openSettingsSection(SettingsScope.APPLICATION,
                    AppearanceSectionModel.section(AppearanceSectionModel.SECTION_APPEARANCE).mergedSheetId());
        }
    }

    /**
     * Registers the merged sections with the settings tab: each registered
     * merged sheet's form is replaced by its merged section, whose parameters
     * come from the general UI and file chooser sheets (see
     * {@link AppearanceSectionModel#parametersBySheet(String)}). Idempotent;
     * called from the plugin registration.
     */
    public static void registerCompositions() {
        for (AppearanceSectionModel.Section section : AppearanceSectionModel.SECTIONS) {
            JIPipeDesktopSettingsTab.registerMergedPage(section.mergedSheetId(), section.id());
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setOpaque(false);
        initializeHeader();
        initializeForm();
        // The form panel already scrolls (WITH_SCROLLING): it is the center.
        add(formPanel, BorderLayout.CENTER);
        refreshModifiedBadge();
        refreshPreview();
    }

    private void initializeHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel titlePanel = new JPanel(new BorderLayout(8, 0));
        titlePanel.setOpaque(false);
        JLabel nameLabel = new JLabel(sectionName(), sectionIcon(), JLabel.LEFT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, nameLabel.getFont().getSize2D() * 1.2f));
        titlePanel.add(nameLabel, BorderLayout.WEST);
        titlePanel.add(modifiedBadge, BorderLayout.CENTER);
        header.add(titlePanel, BorderLayout.NORTH);

        JLabel descriptionLabel = new JLabel(sectionDescription());
        descriptionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        header.add(descriptionLabel, BorderLayout.CENTER);

        modifiedBadge.setIcon(JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png"));
        modifiedBadge.setForeground(UIManager.getColor("Label.disabledForeground"));
        modifiedBadge.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        if (!model.ownedBlockPaths().isEmpty()) {
            JButton resetButton = UIUtils.createButton("Reset section",
                    "Restore every setting of this section to its default",
                    JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"),
                    this::doActionResetSection);
            buttons.add(resetButton);
        }
        header.add(buttons, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    private String sectionName() {
        if (PAGE_APPEARANCE.equals(page)) {
            return "Appearance";
        }
        if (PAGE_ACCESSIBILITY.equals(page)) {
            return "Accessibility";
        }
        return "Keyboard";
    }

    private String sectionDescription() {
        return switch (page) {
            case PAGE_APPEARANCE -> "Theme, UI style and file dialog design";
            case PAGE_ACCESSIBILITY -> "Reduced motion and UI scale";
            default -> "Keyboard shortcuts";
        };
    }

    private Icon sectionIcon() {
        return switch (page) {
            case PAGE_APPEARANCE -> JIPipe.RESOURCES.getIcon16("actions/palette.png");
            case PAGE_ACCESSIBILITY -> JIPipe.RESOURCES.getIcon16("actions/universal-access.png");
            default -> JIPipe.RESOURCES.getIcon16("actions/keyboard.png");
        };
    }

    private void initializeForm() {
        if (PAGE_KEYBOARD.equals(page)) {
            formPanel.addWideToForm(UIUtils.createInfoLabel("No keyboard settings yet",
                    AppearanceSectionModel.KEYBOARD_EMPTY_STATE));
            formPanel.addVerticalGlue();
            return;
        }

        Map<String, List<String>> parametersBySheet = AppearanceSectionModel.parametersBySheet(page);

        // Appearance: live preview on top of the parameter groups.
        if (PAGE_APPEARANCE.equals(page)) {
            Map<String, JIPipeParameterAccess> generalUi = parameterAccessesOf(AppearanceSectionModel.GENERAL_UI_BLOCK);
            if (!generalUi.isEmpty()) {
                trees.values().forEach(tree -> tree.getParameterChangedEventEmitter().subscribeLambda((e, l) -> refreshPreview()));
            }
            previewPanel.setPreferredSize(new Dimension(480, 260));
            formPanel.addWideToForm(UIUtils.wrapInIslandPanelIfNeeded(previewPanel));
            formPanel.addWideToForm(UIUtils.createJLabel("Tip: You can use Tools > Theme manager/editor to create your own style.",
                    JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png")));
        }

        for (Map.Entry<String, List<String>> entry : parametersBySheet.entrySet()) {
            String blockPath = entry.getKey();
            Map<String, JIPipeParameterAccess> accesses = parameterAccessesOf(blockPath);
            String currentGroup = null;
            for (String key : entry.getValue()) {
                JIPipeParameterAccess access = accesses.get(key);
                if (access == null) {
                    continue;
                }
                String group = groupOf(key);
                if (!Objects.equals(group, currentGroup)) {
                    currentGroup = group;
                    formPanel.addGroupHeader(group, JIPipe.RESOURCES.getIcon16("actions/configure.png"));
                    String groupDescription = descriptionOfGroup(key);
                    if (groupDescription != null) {
                        formPanel.addWideToForm(UIUtils.createReadonlyBorderlessTextArea(groupDescription));
                    }
                }
                addParameterRow(access);
            }
        }

        // Accessibility: the UI-scale control (not a settings key).
        if (PAGE_ACCESSIBILITY.equals(page)) {
            formPanel.addGroupHeader("UI scale", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
            initializeScaleControl();
        }

        formPanel.addVerticalGlue();
    }

    private void addParameterRow(JIPipeParameterAccess access) {
        JLabel label = new JLabel(access.getName());
        formPanel.addToForm(createParameterEditor(access), label);
    }

    /**
     * Creates the standard parameter editor for one access of a live sheet,
     * exactly as the plain form does (so the editor writes through
     * {@link JIPipeParameterAccess#set(Object)} and fires the sheet's change
     * event).
     */
    private JComponent createParameterEditor(JIPipeParameterAccess access) {
        return JIPipe.getParameterTypes().createEditorInstance(access, workbench, parameterTreeOf(access), formPanel);
    }

    /**
     * @param blockPath the sheet's block path
     * @return the parameter accesses of the live sheet keyed by the sheet key,
     * or an empty map if the sheet is not registered
     */
    private Map<String, JIPipeParameterAccess> parameterAccessesOf(String blockPath) {
        JIPipeParameterTree tree = trees.get(blockPath);
        if (tree == null) {
            String sheetId = sheetIdOf(blockPath);
            JIPipeApplicationSettingsSheet sheet = sheetId == null ? null
                    : JIPipe.getSettings().getById(sheetId, JIPipeApplicationSettingsSheet.class);
            if (sheet == null) {
                return Map.of();
            }
            tree = new JIPipeParameterTree(sheet);
            trees.put(blockPath, tree);
        }
        return tree.getParameters();
    }

    /**
     * @param access a parameter access of a composed sheet
     * @return the tree the access belongs to (the one it was read from)
     */
    private JIPipeParameterTree parameterTreeOf(JIPipeParameterAccess access) {
        for (JIPipeParameterTree tree : trees.values()) {
            for (JIPipeParameterAccess candidate : tree.getParameters().values()) {
                if (candidate == access) {
                    return tree;
                }
            }
        }
        return null;
    }

    private static String sheetIdOf(String blockPath) {
        if (AppearanceSectionModel.GENERAL_UI_BLOCK.equals(blockPath)) {
            return AppearanceSectionModel.GENERAL_UI_SHEET_ID;
        }
        if (AppearanceSectionModel.FILE_CHOOSER_BLOCK.equals(blockPath)) {
            return AppearanceSectionModel.FILE_CHOOSER_SHEET_ID;
        }
        return blockPath.startsWith("sheet:") ? blockPath.substring("sheet:".length()) : null;
    }

    private static String groupOf(String key) {
        return switch (key) {
            case THEME_KEY, THEME_STYLE_KEY -> "Theme";
            case "reduced-motion" -> "Motion";
            default -> "File chooser";
        };
    }

    private static String descriptionOfGroup(String key) {
        return switch (key) {
            case "file-chooser-type-v2" -> "You can change the UI JIPipe uses when you open/save files.";
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // Live preview
    // ------------------------------------------------------------------

    /**
     * Reads the current theme, theme style and scale from the live sheets and
     * the scale control, and updates the preview panel.
     */
    private void refreshPreview() {
        if (disposed || !PAGE_APPEARANCE.equals(page)) {
            return;
        }
        Map<String, JIPipeParameterAccess> generalUi = parameterAccessesOf(AppearanceSectionModel.GENERAL_UI_BLOCK);
        JIPipeDesktopUITheme theme = JIPipeDesktopUITheme.Modern;
        JIPipeParameterAccess themeAccess = generalUi.get(THEME_KEY);
        if (themeAccess != null && themeAccess.get(Object.class) instanceof JIPipeDesktopUITheme value) {
            theme = value;
        }
        String styleId = ThemeUtils.DEFAULT_STYLE_ID;
        JIPipeParameterAccess styleAccess = generalUi.get(THEME_STYLE_KEY);
        if (styleAccess != null && styleAccess.get(Object.class) instanceof JIPipeModernThemeStyleParameter style
                && !StringUtils.isNullOrEmpty(style.getValue())
                && ThemeUtils.getAvailableStyleIds().contains(style.getValue())) {
            styleId = style.getValue();
        }
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getStyleFromId(styleId);
        previewPanel.setTheme(theme);
        previewPanel.setThemeStyle(style);
        previewPanel.setScale(scaleSlider.getValue() * 1.0f / getRoundedScale((int) (systemScale * 100)));
    }

    // ------------------------------------------------------------------
    // UI scale (not a settings key)
    // ------------------------------------------------------------------

    private void initializeScaleControl() {
        formPanel.addWideToForm(UIUtils.createReadonlyBorderlessTextArea(
                "If the interface is too large or too small, you can change the UI scale."));

        scaleSlider.setSnapToTicks(true);
        scaleSlider.setMajorTickSpacing(100);
        scaleSlider.setMinorTickSpacing(25);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(false);
        scaleLabel.setMinimumSize(new Dimension(50, 20));
        int scalePercent = (int) (systemScale * 100);
        scaleSlider.setValue(getRoundedScale(scalePercent));
        scaleLabel.setText(getRoundedScale(scalePercent) + "%");

        // Live label/preview on drag; the file write on release (a drag must not
        // trigger one confirm + jaunch write per tick).
        scaleSlider.addChangeListener(e -> {
            int roundedScale = getRoundedScale(scaleSlider.getValue());
            scaleLabel.setText(roundedScale + "%");
            refreshPreview();
        });
        scaleSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                applyScale();
            }
        });
        scaleSlider.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyScale();
            }
        });

        formPanel.addWideToForm(UIUtils.borderNSEWC(null, null, scaleLabel, null, scaleSlider));
        formPanel.addWideToForm(UIUtils.createJLabel("The preview might be slightly inaccurate",
                JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png")));
        formPanel.addWideToForm(UIUtils.createJLabel("Non-integral scales may not correctly work",
                JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png")));
        if (SystemUtils.IS_OS_MAC_OSX) {
            formPanel.addWideToForm(UIUtils.createJLabel("May not work on macOS",
                    JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png")));
        }
        formPanel.addWideToForm(UIUtils.createJLabel("Restart JIPipe to apply the UI scale",
                JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png")));
    }

    /**
     * The old customizer dialog's Apply path for the scale (it is stored in the
     * jaunch config files, not in a settings block): the "apply to Fiji/ImageJ
     * too?" confirmation only on an actual change, then the write, then the
     * error dialog if the write failed.
     */
    private void applyScale() {
        if (disposed) {
            return;
        }
        int newRoundedScale = getRoundedScale(scaleSlider.getValue());
        int originalRoundedScale = getRoundedScale((int) (systemScale * 100));
        if (newRoundedScale == originalRoundedScale) {
            return;
        }
        boolean alsoImageJ = JOptionPane.showConfirmDialog(this,
                "Do you want to apply your scale settings also to Fiji/ImageJ?",
                "Apply",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        if (!UIUtils.saveUIScaleToJaunch(newRoundedScale / 100f, alsoImageJ)) {
            JOptionPane.showMessageDialog(workbench != null ? workbench.getWindow() : this,
                    "Your UI scale setting of " + newRoundedScale + "% could not be saved.",
                    "Appearance",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    // Modified badge + reset
    // ------------------------------------------------------------------

    private void refreshModifiedBadge() {
        if (disposed) {
            return;
        }
        boolean modified = model.isModified();
        modifiedBadge.setVisible(modified);
        modifiedBadge.setToolTipText(modified
                ? "This section contains settings that differ from the defaults" : null);
    }

    private void doActionResetSection() {
        model.resetAll();
        // The bridge has re-applied the (now absent) blocks into the live sheets;
        // re-create the section so the editors show the defaults.
        refreshModifiedBadge();
        if (settingsTab != null) {
            settingsTab.rebuildRightPane();
        }
    }

    @Override
    public JIPipeDesktopWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        model.unsubscribe(documentListener);
        trees.clear();
        formPanel.clear();
    }
}