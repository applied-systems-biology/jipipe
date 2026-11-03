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
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopSegmentedButtonGroup;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeModernThemeStyleParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * auto-save. The Appearance and the Accessibility sections each host a live
 * {@link ThemePreviewPanel}, refreshed from the tree's parameter-changed
 * events and from the scale control (the Appearance preview shows the theme,
 * the Accessibility preview shows the chosen scale).
 * <p>
 * <b>UI scale.</b> Not a settings key (it lives in the jaunch config files): a
 * segmented control offering the integral scales the bundled JDK applies on
 * this platform (100/200/300% — the X11 AWT scale is an {@code int}, so
 * fractional values are silently truncated or ignored, see
 * {@link UIUtils#OFFERED_UI_SCALES}). The initial selection reads the
 * <em>stored</em> value's effective scale (auto → the nearest offered one);
 * selecting a different segment asks whether to apply the scale to
 * Fiji/ImageJ too, calls {@link UIUtils#saveUIScaleToJaunch(float, boolean)}
 * and reports a failed write with an error dialog — the old dialog's Apply
 * path. The Accessibility page also hosts the {@link ThemePreviewPanel}
 * scaled relative to the stored effective scale, so the chosen scale can be
 * seen before the restart it requires.
 */
public class JIPipeDesktopAppearanceSection extends JPanel implements JIPipeDesktopWorkbenchAccess, Disposable {

    private static final Logger logger = LoggerFactory.getLogger(JIPipeDesktopAppearanceSection.class);

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
    /**
     * The default value of every displayed key, read from throwaway fresh sheet
     * instances (see {@link #freshSheetDefaults()}).
     */
    private final Map<String, JsonNode> defaults;
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
     * The system (automatic) scale at construction: the fallback the stored
     * jaunch value is corrected against (the old dialog's {@code systemScale}).
     */
    private final double systemScale;
    /**
     * The scale the STORED jaunch value actually applies (auto markers → the
     * system scale; fractional values → the integer the JVM truncates to, see
     * {@link UIUtils#effectiveUiScaleForStoredValue}). Read once at
     * construction: it is the reference the control is displayed and corrected
     * against, and the preview is scaled relative to it (1.0 = "looks like
     * now").
     */
    private final double storedEffectiveScale;
    private final JIPipeDesktopSegmentedButtonGroup scaleGroup = new JIPipeDesktopSegmentedButtonGroup();

    /**
     * The scale segment that was selected when the section was opened: the one
     * the running JVM applies. A different selection means the stored scale is
     * waiting for a restart (see {@link ThemeUtils#setUiScaleRestartReason(String)}).
     */
    private int appliedScaleSegment;

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
        this.defaults = freshSheetDefaults();
        this.model = AppearanceSectionModel.forSection(document, page, defaults);
        this.systemScale = UIUtils.getScale(workbench != null ? workbench.getWindow() : null);
        this.storedEffectiveScale =
                UIUtils.effectiveUiScaleForStoredValue(UIUtils.parseStoredUiScaleProperty(), systemScale);
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

    /**
     * Reads the default value of every displayed key of the merged sections.
     * <p>
     * The source is one <em>throwaway</em> fresh instance per composed sheet type
     * ({@code new JIPipeGeneralUIApplicationSettings()},
     * {@code new JIPipeFileChooserApplicationSettings()}): settings sheets are
     * plain parameter containers with no constructor side effects, and a fresh
     * instance holds exactly the defaults. The instances are read only — never
     * registered, never attached to a bridge, never kept — so no second sheet
     * can exist for a block.
     *
     * @return {@code blockPath + "/" + key} → the default value as JSON
     */
    public static Map<String, JsonNode> freshSheetDefaults() {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        // One throwaway instance per composed sheet TYPE, shared by every section
        // that displays keys of that sheet (the Appearance and the Accessibility
        // sections both read general-ui defaults), so a block never has two
        // default sources. The cache lives only for this call: the instances are
        // read and then dropped.
        Map<Class<?>, JIPipeApplicationSettingsSheet> throwaways = new LinkedHashMap<>();
        for (AppearanceSectionModel.Section section : AppearanceSectionModel.SECTIONS) {
            Map<String, List<String>> bySheet = AppearanceSectionModel.parametersBySheet(section.id());
            for (Map.Entry<String, List<String>> entry : bySheet.entrySet()) {
                JIPipeApplicationSettingsSheet throwaway = throwaways.computeIfAbsent(
                        sheetTypeOf(entry.getKey()), type -> newThrowaway(entry.getKey()));
                if (throwaway != null) {
                    collectDefaults(result, throwaway, entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * @param blockPath a composed sheet's block path
     * @return the sheet class whose fresh instance holds that sheet's defaults
     */
    private static Class<?> sheetTypeOf(String blockPath) {
        if (AppearanceSectionModel.GENERAL_UI_BLOCK.equals(blockPath)) {
            return JIPipeGeneralUIApplicationSettings.class;
        }
        if (AppearanceSectionModel.FILE_CHOOSER_BLOCK.equals(blockPath)) {
            return JIPipeFileChooserApplicationSettings.class;
        }
        throw new IllegalArgumentException("No fresh-default source for " + blockPath);
    }

    /**
     * @param blockPath a composed sheet's block path
     * @return a fresh, unregistered, unbridged instance of that sheet type
     */
    private static JIPipeApplicationSettingsSheet newThrowaway(String blockPath) {
        Class<?> type = sheetTypeOf(blockPath);
        if (type == JIPipeGeneralUIApplicationSettings.class) {
            return new JIPipeGeneralUIApplicationSettings();
        }
        if (type == JIPipeFileChooserApplicationSettings.class) {
            return new JIPipeFileChooserApplicationSettings();
        }
        throw new IllegalArgumentException("No fresh-default source for " + blockPath);
    }

    private static void collectDefaults(Map<String, JsonNode> target,
                                        JIPipeApplicationSettingsSheet throwawaySheet,
                                        String blockPath,
                                        List<String> keys) {
        Map<String, JIPipeParameterAccess> accesses = new JIPipeParameterTree(throwawaySheet).getParameters();
        for (String key : keys) {
            JIPipeParameterAccess access = accesses.get(key);
            if (access != null) {
                target.put(blockPath + "/" + key, JsonUtils.getObjectMapper().valueToTree(access.get(Object.class)));
            }
        }
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
            // The merged sections own the parameters of their composed sheets, so
            // those plain tree entries would show every knob a second time.
            for (String composedSheetId : section.sheetIds()) {
                JIPipeDesktopSettingsTab.registerAbsorbedSheet(composedSheetId);
            }
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

        // Accessibility: the UI-scale control (not a settings key) and a
        // preview, so the chosen scale can be seen (the running app itself
        // only re-scales on restart).
        if (PAGE_ACCESSIBILITY.equals(page)) {
            formPanel.addGroupHeader("UI scale", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
            initializeScaleControl();
            previewPanel.setPreferredSize(new Dimension(480, 260));
            formPanel.addWideToForm(UIUtils.wrapInIslandPanelIfNeeded(previewPanel));
            formPanel.addWideToForm(UIUtils.createReadonlyBorderlessTextArea(
                    "The preview shows how the interface will look after the next restart. "
                            + "On this platform the JVM only supports integral scales, "
                            + "so 100%, 200% and 300% are available."));
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
     * the scale control, and updates the preview panel. The Appearance page
     * previews the theme and style; the Accessibility page previews the
     * chosen scale (1.0 = the current look, 2.0 = everything twice as large).
     */
    private void refreshPreview() {
        if (disposed || (!PAGE_APPEARANCE.equals(page) && !PAGE_ACCESSIBILITY.equals(page))) {
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
        previewPanel.setThemeStyle(style != null ? style : new JIPipeDesktopModernThemeStyle());
        // The preview scale is RELATIVE to the stored effective scale (1.0 =
        // "looks like now"). The old slider-derived formula was inaccurate:
        // the slider's value was unattached to the stored setting until the
        // file was written, and the slider's 25% steps did not match what the
        // JVM applies.
        previewPanel.setScale(scaleGroup.getSelectedIndex() < 0
                ? 1.0f
                : (float) (UIUtils.OFFERED_UI_SCALES.get(scaleGroup.getSelectedIndex()) / storedEffectiveScale));
    }

    // ------------------------------------------------------------------
    // UI scale (not a settings key)
    // ------------------------------------------------------------------

    private void initializeScaleControl() {
        formPanel.addWideToForm(UIUtils.createReadonlyBorderlessTextArea(
                "If the interface is too large or too small, you can change the UI scale."));

        for (double offered : UIUtils.OFFERED_UI_SCALES) {
            scaleGroup.addSegment((int) Math.round(offered * 100) + "%");
        }
        // The segment matching the STORED effective scale (not the running
        // one, which only changes on restart): auto → nearest offered,
        // fractional → the integer the JVM truncates to.
        scaleGroup.setSelectedIndex(scaleSegmentOf(storedEffectiveScale));
        appliedScaleSegment = scaleGroup.getSelectedIndex();
        // A click both updates the preview and is the Apply point (no drag
        // exists to defer the write to).
        scaleGroup.addSelectionListener(idx -> {
            refreshPreview();
            applyScale(idx);
        });

        formPanel.addWideToForm(scaleGroup);
        formPanel.addWideToForm(UIUtils.createJLabel("Restart JIPipe to apply the UI scale",
                JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png")));
    }

    /**
     * @param effectiveScale an effective scale (see
     *                       {@link UIUtils#effectiveUiScaleForStoredValue})
     * @return the index of the offered scale segment it renders as
     */
    private static int scaleSegmentOf(double effectiveScale) {
        int percent = UIUtils.nearestOfferedUiScalePercent(effectiveScale);
        for (int i = 0; i < UIUtils.OFFERED_UI_SCALES.size(); i++) {
            if ((int) Math.round(UIUtils.OFFERED_UI_SCALES.get(i) * 100) == percent) {
                return i;
            }
        }
        return 0;
    }

    /**
     * The old customizer dialog's Apply path for the scale (it is stored in the
     * jaunch config files, not in a settings block): the "apply to Fiji/ImageJ
     * too?" confirmation only on an actual change, then the write, then the
     * error dialog if the write failed.
     *
     * @param segmentIndex the scale segment the user selected
     */
    private void applyScale(int segmentIndex) {
        if (disposed || segmentIndex < 0 || segmentIndex >= UIUtils.OFFERED_UI_SCALES.size()) {
            return;
        }
        double newScale = UIUtils.OFFERED_UI_SCALES.get(segmentIndex);
        // The header hint follows the user's choice immediately (§1351): a
        // scale other than the one the running JVM applies needs a restart.
        updateScaleRestartReason(segmentIndex);
        // No write when the stored value already applies as the chosen scale
        // (e.g. a stored 2.5 and a 200% selection both truncate to 2).
        if (newScale == storedEffectiveScale) {
            return;
        }
        boolean alsoImageJ = JOptionPane.showConfirmDialog(this,
                "Do you want to apply your scale settings also to Fiji/ImageJ?",
                "Apply",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        if (!UIUtils.saveUIScaleToJaunch((float) newScale, alsoImageJ)) {
            JOptionPane.showMessageDialog(workbench != null ? workbench.getWindow() : this,
                    "Your UI scale setting of " + (int) Math.round(newScale * 100) + "% could not be saved.",
                    "Appearance",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Reports the chosen scale to the restart-needed state of the settings tab
     * (the scale is not a settings key, so nothing else can see it).
     *
     * @param segmentIndex the selected segment
     */
    private void updateScaleRestartReason(int segmentIndex) {
        ThemeUtils.setUiScaleRestartReason(segmentIndex == appliedScaleSegment ? null : "UI scale");
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

    /**
     * Resets the section to its defaults.
     * <p>
     * The document removal alone is cosmetic: the sheet bridge applies only keys
     * that are <em>present</em> in a block, and the next save re-flushes the
     * whole sheet with its current (unchanged) values. The authoritative reset
     * therefore drives every displayed parameter of the <em>live</em> sheet to
     * the default read from a throwaway fresh instance: each {@code set} fires
     * the sheet's parameter change event → the bridge flushes it into the
     * document (the engine auto-saves it). Afterwards the model removes the
     * displayed keys from the document (per key, and only those that still
     * deviate) so the stored profile carries no stale value, and the section is
     * re-created so the editors show the defaults.
     */
    private void doActionResetSection() {
        for (Map.Entry<String, List<String>> entry : model.displayedKeys().entrySet()) {
            Map<String, JIPipeParameterAccess> liveAccesses = parameterAccessesOf(entry.getKey());
            for (String key : entry.getValue()) {
                driveToDefault(entry.getKey(), key, liveAccesses.get(key));
            }
        }
        model.resetAll();
        refreshModifiedBadge();
        if (settingsTab != null) {
            settingsTab.rebuildRightPane();
        }
    }

    /**
     * Sets one live sheet parameter to its default (the value the throwaway
     * fresh sheet holds). Unchanged values and parameters without a recorded
     * default are left untouched, so the method fires nothing for them.
     */
    private void driveToDefault(String blockPath, String key, JIPipeParameterAccess access) {
        JsonNode defaultValue = model.defaultOf(blockPath, key);
        if (access == null || defaultValue == null) {
            return;
        }
        try {
            JsonNode current = JsonUtils.getObjectMapper().valueToTree(access.get(Object.class));
            if (current.equals(defaultValue)) {
                return;
            }
            Object value = JsonUtils.getObjectMapper().readerFor(access.getFieldClass()).readValue(defaultValue);
            access.set(value);
        } catch (Exception | Error e) {
            logger.warn("Could not reset parameter '{}' of {} to its default", key, blockPath, e);
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
        // Leave the restart hint of the header: it is this section's, and the
        // header must not keep claiming a restart after the user moved away.
        if (PAGE_ACCESSIBILITY.equals(page)) {
            ThemeUtils.setUiScaleRestartReason(null);
        }
        model.unsubscribe(documentListener);
        trees.clear();
        formPanel.clear();
    }
}