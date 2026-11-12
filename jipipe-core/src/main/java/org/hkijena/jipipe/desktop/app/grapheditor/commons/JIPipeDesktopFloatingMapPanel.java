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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.MapLayer;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUIUpdatedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUIUpdatedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopMapState.AnchorMode;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The floating map of the graph editor: a small, lockable panel that floats
 * over the canvas (the editor overlay's {@link JLayeredPane#DRAG_LAYER}) and
 * shows a minimap of the whole graph. It succeeds the former "Navigator" dock
 * panel and the docked minimap.
 * <p>
 * The panel is driven by a {@link JIPipeDesktopMapState} that is persisted in
 * the application settings (see
 * {@link org.hkijena.jipipe.plugins.settings.SettingsKeys#PIPELINE_MAP_STATE}
 * and {@link org.hkijena.jipipe.plugins.settings.SettingsKeys#COMPARTMENTS_MAP_STATE}).
 * External states enter via {@link #applyState(JIPipeDesktopMapState)}, which
 * adopts a deep copy: the settings service hands stored copies to readers,
 * but for an absent setting it returns its single shared default instance,
 * so a state received from the binding must never be mutated directly. The
 * panel's own state leaves via {@link #getState()}, which also returns a
 * copy; the editor writes it through to the binding after every user change.
 * <p>
 * Visual language follows the approved prototype
 * {@code docs/superpowers/mockups/2026-11-05-navigator-overhaul/04-floating-map.html}:
 * a rounded panel with a soft drop shadow, a drag header ("Map") with
 * layer/lock/hide controls, and a resize handle at the bottom-right corner.
 */
public class JIPipeDesktopFloatingMapPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeDesktopGraphCanvasUIUpdatedEventListener {

    /**
     * The smallest map width the user can resize to.
     */
    public static final int MAP_MIN_WIDTH = 180;

    /**
     * The smallest map height the user can resize to.
     */
    public static final int MAP_MIN_HEIGHT = 130;

    /**
     * The distance the map keeps from the canvas edges.
     */
    public static final int MAP_EDGE_MARGIN = 8;

    /**
     * The distance at which a fresh (never moved) map anchors itself from the
     * canvas top-right corner.
     */
    public static final int MAP_DEFAULT_OFFSET = 16;

    /**
     * The anchor modes in the order they appear in the anchor menu: the nine
     * snapping points in reading order ({@link AnchorMode#Auto} is a separate
     * entry above them).
     */
    private static final AnchorMode[] ANCHOR_MENU_ORDER = {
            AnchorMode.TopLeft, AnchorMode.TopCenter, AnchorMode.TopRight,
            AnchorMode.CenterLeft, AnchorMode.CenterCenter, AnchorMode.CenterRight,
            AnchorMode.BottomLeft, AnchorMode.BottomCenter, AnchorMode.BottomRight
    };

    /**
     * Side length of the bottom-right resize handle (the only resize
     * affordance, per prototype 04).
     */
    private static final int MAP_RESIZE_HANDLE_SIZE = 14;

    /**
     * Corner radius of the panel chrome.
     */
    private static final int MAP_BORDER_ARC = 8;

    /**
     * Spread of the soft drop shadow painted around the panel box.
     */
    private static final int MAP_SHADOW_SIZE = 6;

    /**
     * Number of concentric translucent rectangles used to soften the shadow.
     */
    private static final int MAP_SHADOW_LAYERS = 6;

    /**
     * Alpha of the innermost shadow layer; outer layers fade toward zero.
     */
    private static final int MAP_SHADOW_ALPHA = 16;

    /**
     * Stroke width of the viewport rectangle.
     */
    private static final int MAP_VIEWPORT_STROKE_WIDTH = 2;

    /**
     * Fill color of the viewport rectangle (same as the former minimap).
     */
    private static final Color AREA_FILL_COLOR = new Color(0x3365a4e3, true);

    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final MapBody mapBody;
    private final JToolBar header;
    private final JToggleButton lockButton;
    private final JButton anchorButton;
    private final Map<AnchorMode, JRadioButtonMenuItem> anchorMenuItems = new EnumMap<>(AnchorMode.class);
    private final JComponent resizeHandle;
    private final JCheckBoxMenuItem nodesLayerItem;
    private final JCheckBoxMenuItem edgesLayerItem;
    private final JCheckBoxMenuItem notesLayerItem;
    private JIPipeDesktopMapState state;
    private boolean headerDragging;
    private boolean resizeDragging;
    private boolean anchored;

    /**
     * The effective anchor of an {@link AnchorMode#Auto} map: the snapping
     * point detected at the first layout or the last drag release. Always
     * {@code null} while the state carries a concrete anchor.
     */
    private AnchorMode resolvedAnchor;

    /**
     * @param graphEditorUI the graph editor that displays this map
     */
    public JIPipeDesktopFloatingMapPanel(JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        // The panel's own working state: defaultState() creates a fresh
        // instance, so the panel never aliases the settings service's shared
        // default (applyState copies on adoption as well).
        this.state = JIPipeDesktopMapState.defaultState();
        this.nodesLayerItem = new JCheckBoxMenuItem("Nodes");
        this.edgesLayerItem = new JCheckBoxMenuItem("Edges");
        this.notesLayerItem = new JCheckBoxMenuItem("Canvas notes");
        this.lockButton = createLockButton();
        this.anchorButton = createAnchorButton();
        this.resizeHandle = createResizeHandle();
        this.mapBody = new MapBody();
        this.header = createHeader();
        initialize();
        applyStateToUI();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setOpaque(false);
        // Space for the painted drop shadow plus the 1 px outline
        setBorder(BorderFactory.createEmptyBorder(MAP_SHADOW_SIZE + 1,
                MAP_SHADOW_SIZE + 1,
                MAP_SHADOW_SIZE + 1,
                MAP_SHADOW_SIZE + 1));

        add(header, BorderLayout.NORTH);
        add(mapBody, BorderLayout.CENTER);

        // Repaint triggers: the map contents follow the canvas and its scroll
        // position. Cheap repaints only; the geometry is recomputed per paint.
        graphEditorUI.getScrollPane().getHorizontalScrollBar().addAdjustmentListener(e -> mapBody.repaint());
        graphEditorUI.getScrollPane().getVerticalScrollBar().addAdjustmentListener(e -> mapBody.repaint());
        graphEditorUI.getCanvasUI().getGraphCanvasUpdatedEventEmitter().subscribeWeak(this);
        graphEditorUI.getCanvasUI().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // The canvas fills the editor overlay: re-place the map at
                // its anchor and clamp it into the resized overlay
                reclampToOverlay();
            }
        });
    }

    private JToolBar createHeader() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JLabel gripLabel = new JLabel("\u22EE\u22EE");
        gripLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
        toolBar.add(gripLabel);
        toolBar.add(Box.createHorizontalStrut(4));

        JLabel titleLabel = new JLabel("Map");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        toolBar.add(titleLabel);
        toolBar.add(Box.createHorizontalGlue());

        JButton layersButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/dialog-layers.png"));
        UIUtils.makeButtonFlatWithSize(layersButton, 20, 2);
        layersButton.setToolTipText("Layers");
        toolBar.add(layersButton);
        toolBar.add(Box.createHorizontalStrut(2));
        JPopupMenu layersMenu = UIUtils.addPopupMenuToButton(layersButton);
        nodesLayerItem.addActionListener(e -> {
            state.setLayerNodes(nodesLayerItem.isSelected());
            mapBody.repaint();
            graphEditorUI.saveMapStateToSettings();
        });
        edgesLayerItem.addActionListener(e -> {
            state.setLayerEdges(edgesLayerItem.isSelected());
            mapBody.repaint();
            graphEditorUI.saveMapStateToSettings();
        });
        notesLayerItem.addActionListener(e -> {
            state.setLayerNotes(notesLayerItem.isSelected());
            mapBody.repaint();
            graphEditorUI.saveMapStateToSettings();
        });
        layersMenu.add(nodesLayerItem);
        layersMenu.add(edgesLayerItem);
        layersMenu.add(notesLayerItem);

        toolBar.add(anchorButton);
        toolBar.add(Box.createHorizontalStrut(2));

        toolBar.add(lockButton);
        toolBar.add(Box.createHorizontalStrut(2));

        JButton hideButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/dialog-close.png"));
        UIUtils.makeButtonFlatWithSize(hideButton, 20, 2);
        hideButton.setToolTipText("Hide map");
        hideButton.addActionListener(e -> graphEditorUI.setMapVisible(false));
        toolBar.add(hideButton);

        // The whole header starts drags (the buttons consume their own
        // presses); locked headers do not
        HeaderDragController dragController = new HeaderDragController();
        toolBar.addMouseListener(dragController);
        toolBar.addMouseMotionListener(dragController);
        return toolBar;
    }

    private JToggleButton createLockButton() {
        JToggleButton button = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/lock.png"));
        UIUtils.makeButtonFlatWithSize(button, 20, 2);
        button.setToolTipText("Lock position");
        button.addActionListener(e -> {
            state.setLocked(button.isSelected());
            applyLock();
            graphEditorUI.saveMapStateToSettings();
        });
        return button;
    }

    /**
     * Creates the anchor configuration button: a reloadable popup menu (see
     * UIUtils) with the Auto mode and the nine snapping points, each with its
     * anchor icon.
     */
    private JButton createAnchorButton() {
        JButton button = new JButton(JIPipe.RESOURCES.getIcon16("actions/anchor.png"));
        UIUtils.makeButtonFlatWithSize(button, 20, 2);
        button.setToolTipText("Map anchor");

        JPopupMenu menu = new JPopupMenu();
        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem autoItem = new JRadioButtonMenuItem("Auto", JIPipe.RESOURCES.getIcon16("actions/anchor.png"));
        autoItem.addActionListener(e -> applyAnchorChoice(AnchorMode.Auto));
        group.add(autoItem);
        anchorMenuItems.put(AnchorMode.Auto, autoItem);
        menu.add(autoItem);
        menu.add(new JSeparator());
        for (AnchorMode mode : ANCHOR_MENU_ORDER) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(anchorLabel(mode), JIPipe.RESOURCES.getIcon16(anchorIcon(mode)));
            item.addActionListener(e -> applyAnchorChoice(mode));
            group.add(item);
            anchorMenuItems.put(mode, item);
            menu.add(item);
        }
        UIUtils.addReloadablePopupMenuToButton(button, menu, this::reloadAnchorMenu);
        return button;
    }

    /**
     * Syncs the anchor menu items with the current state before the popup is
     * shown.
     */
    private void reloadAnchorMenu() {
        AnchorMode selected = state.getAnchor();
        for (Map.Entry<AnchorMode, JRadioButtonMenuItem> entry : anchorMenuItems.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == selected);
        }
    }

    /**
     * Applies an anchor choice from the menu. Auto only switches the mode:
     * the snapping point is detected again at the next drag release. A
     * concrete anchor snaps the map flush to the snapping point (offset =
     * the edge margin; centered axes ignore it) and stays selected until
     * changed again.
     *
     * @param mode the chosen anchor mode
     */
    private void applyAnchorChoice(AnchorMode mode) {
        state.setAnchor(mode);
        if (mode != AnchorMode.Auto) {
            resolvedAnchor = mode;
            state.setAnchorOffset(MAP_EDGE_MARGIN, MAP_EDGE_MARGIN);
            placeIntoOverlay();
            applyBounds();
            mapBody.repaint();
        }
        graphEditorUI.saveMapStateToSettings();
    }

    private static String anchorLabel(AnchorMode mode) {
        return switch (mode) {
            case Auto -> "Auto";
            case TopLeft -> "Top left";
            case TopCenter -> "Top center";
            case TopRight -> "Top right";
            case CenterLeft -> "Center left";
            case CenterCenter -> "Center";
            case CenterRight -> "Center right";
            case BottomLeft -> "Bottom left";
            case BottomCenter -> "Bottom center";
            case BottomRight -> "Bottom right";
        };
    }

    private static String anchorIcon(AnchorMode mode) {
        return switch (mode) {
            case Auto -> "actions/anchor.png";
            case TopLeft -> "actions/anchor-top-left.png";
            case TopCenter -> "actions/anchor-top-center.png";
            case TopRight -> "actions/anchor-top-right.png";
            case CenterLeft -> "actions/anchor-center-left.png";
            case CenterCenter -> "actions/anchor-center-center.png";
            case CenterRight -> "actions/anchor-center-right.png";
            case BottomLeft -> "actions/anchor-bottom-left.png";
            case BottomCenter -> "actions/anchor-bottom-center.png";
            case BottomRight -> "actions/anchor-bottom-right.png";
        };
    }

    private JComponent createResizeHandle() {
        JComponent handle = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Two diagonal strokes, like the corner mark of prototype 04
                Graphics2D graphics2D = (Graphics2D) g.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(ThemeUtils.getCurrentStyle().getTextMuted());
                graphics2D.drawLine(getWidth() - 5, getHeight() - 2, getWidth() - 2, getHeight() - 5);
                graphics2D.drawLine(getWidth() - 8, getHeight() - 2, getWidth() - 2, getHeight() - 8);
                graphics2D.dispose();
            }
        };
        handle.setOpaque(false);
        handle.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        ResizeController resizeController = new ResizeController();
        handle.addMouseListener(resizeController);
        handle.addMouseMotionListener(resizeController);
        return handle;
    }

    /**
     * Adopts a map state.
     * <p>
     * The method is idempotent: a state equal to the current one (for example
     * the echo of our own write) changes nothing. While the user drags the
     * header or the resize handle, position and size of external states are
     * ignored, so a mid-drag write from another editor cannot pull the map
     * out from under the mouse.
     *
     * @param newState the state to apply
     */
    public void applyState(JIPipeDesktopMapState newState) {
        if (newState == null || newState.equals(state)) {
            return;
        }
        // Copy-on-read: never adopt (and later mutate) an object owned by the
        // settings service. Its shared default instance is handed out for an
        // absent setting, and it must not be touched.
        JIPipeDesktopMapState applied = new JIPipeDesktopMapState(newState);
        // A hand-edited settings file can store an unusable size: clamp it to
        // the resize minimum, so the panel never collapses below a clickable
        // size (the position is re-placed from the anchor separately in
        // applyStateToUI)
        applied.setSize(Math.max(MAP_MIN_WIDTH, applied.getWidth()),
                Math.max(MAP_MIN_HEIGHT, applied.getHeight()));
        if (headerDragging || resizeDragging) {
            applied.setPosition(state.getX(), state.getY());
            applied.setSize(state.getWidth(), state.getHeight());
        }
        state = applied;
        applyStateToUI();
    }

    /**
     * @return a copy of the panel's current state; the caller may mutate it
     * freely (the editor writes it through to the settings binding)
     */
    public JIPipeDesktopMapState getState() {
        return new JIPipeDesktopMapState(state);
    }

    /**
     * Shows or hides the map. Called by the editor's toggle and the live
     * state application; persistence is the caller's job.
     *
     * @param visible whether the map is shown
     */
    public void setVisibleInCanvas(boolean visible) {
        state.setVisible(visible);
        applyVisibility();
    }

    @Override
    public void onGraphCanvasUpdated(JIPipeDesktopGraphCanvasUIUpdatedEvent event) {
        if (isDisplayable()) {
            mapBody.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics2D = (Graphics2D) g.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int boxX = MAP_SHADOW_SIZE;
        int boxY = MAP_SHADOW_SIZE;
        int boxWidth = getWidth() - 2 * MAP_SHADOW_SIZE;
        int boxHeight = getHeight() - 2 * MAP_SHADOW_SIZE;

        // Soft drop shadow: concentric translucent rounded rectangles that
        // fade outward
        for (int i = MAP_SHADOW_LAYERS; i >= 1; --i) {
            int alpha = MAP_SHADOW_ALPHA * (MAP_SHADOW_LAYERS - i + 1) / MAP_SHADOW_LAYERS;
            graphics2D.setColor(new Color(0, 0, 0, alpha));
            graphics2D.fillRoundRect(boxX - i, boxY - i, boxWidth + 2 * i, boxHeight + 2 * i, MAP_BORDER_ARC + i, MAP_BORDER_ARC + i);
        }

        // Panel body
        graphics2D.setColor(ThemeUtils.getCurrentStyle().getPanelBackground());
        graphics2D.fillRoundRect(boxX, boxY, boxWidth, boxHeight, MAP_BORDER_ARC, MAP_BORDER_ARC);

        // Header band with rounded top corners: the clip cuts off the bottom
        // rounding of the filled round rect
        Rectangle headerBounds = header.getBounds();
        if (headerBounds.height > 0) {
            int bandHeight = headerBounds.y + headerBounds.height - boxY;
            graphics2D.setClip(boxX, boxY, boxWidth, bandHeight);
            graphics2D.setColor(ThemeUtils.getCurrentStyle().getButtonBackground());
            graphics2D.fillRoundRect(boxX, boxY, boxWidth, bandHeight + MAP_BORDER_ARC, MAP_BORDER_ARC, MAP_BORDER_ARC);
            graphics2D.setClip(null);
            graphics2D.drawLine(boxX, boxY + bandHeight, boxX + boxWidth - 1, boxY + bandHeight);
        }

        // Outline
        graphics2D.setColor(ThemeUtils.getCurrentStyle().getBorderColor());
        graphics2D.drawRoundRect(boxX, boxY, boxWidth - 1, boxHeight - 1, MAP_BORDER_ARC, MAP_BORDER_ARC);
        graphics2D.dispose();
    }

    /**
     * Pushes the current state into the UI: bounds, anchor, visibility,
     * lock and layer menu items.
     */
    private void applyStateToUI() {
        ensureAnchored();
        placeIntoOverlay();
        applyBounds();
        applyVisibility();
        applyLock();
        applyLayersToMenuItems();
    }

    private void applyBounds() {
        setBounds(state.getX(), state.getY(), state.getWidth(), state.getHeight());
    }

    private void applyVisibility() {
        setVisible(state.isVisible());
    }

    private void applyLock() {
        lockButton.setSelected(state.isLocked());
        // While locked, drags are rejected by the controllers, the resize
        // affordance is removed and the anchor cannot be changed (it would
        // move the map)
        anchorButton.setEnabled(!state.isLocked());
        resizeHandle.setVisible(!state.isLocked());
        header.setCursor(state.isLocked() ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    private void applyLayersToMenuItems() {
        nodesLayerItem.setState(state.isLayerNodes());
        edgesLayerItem.setState(state.isLayerEdges());
        notesLayerItem.setState(state.isLayerNotes());
    }

    /**
     * Resolves the anchor of a map that has not been laid out yet: a fresh
     * (never moved) map anchors itself to the canvas top-right corner,
     * offset by {@link #MAP_DEFAULT_OFFSET}; an auto map with a stored
     * position detects its snapping point from that position. A concrete
     * anchor needs no resolution: its persisted offset glues the map to the
     * snapping point in the current overlay (applied by
     * {@link #placeIntoOverlay()}). The resolution runs at the first layout
     * that gives the overlay a real size; until then an unsized overlay
     * would anchor to 0.
     */
    private void ensureAnchored() {
        if (anchored) {
            return;
        }
        Container parent = getParent();
        if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0) {
            return;
        }
        anchored = true;
        if (state.getAnchor() != AnchorMode.Auto) {
            return;
        }
        if (state.getX() == 0 && state.getY() == 0) {
            resolvedAnchor = AnchorMode.TopRight;
            state.setAnchorOffset(MAP_DEFAULT_OFFSET, MAP_DEFAULT_OFFSET);
        } else {
            resolvedAnchor = AnchorMode.detect(parent.getWidth(), parent.getHeight(),
                    state.getWidth(), state.getHeight(), state.getX(), state.getY());
            Point offset = resolvedAnchor.captureOffset(parent.getWidth(), parent.getHeight(),
                    state.getWidth(), state.getHeight(), state.getX(), state.getY());
            state.setAnchorOffset(offset.x, offset.y);
        }
    }

    /**
     * Places the map into the overlay according to its anchor and offset,
     * then clamps it as a safety (for example when the map is larger than
     * the overlay). Auto maps use the resolved (detected) anchor; a map
     * whose anchor was never resolved keeps its position (clamp only).
     * Placement is skipped while the user drags, so an external state cannot
     * pull the map out from under the mouse.
     */
    private void placeIntoOverlay() {
        Container parent = getParent();
        if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0) {
            return;
        }
        AnchorMode resolved = state.getAnchor() != AnchorMode.Auto ? state.getAnchor() : resolvedAnchor;
        if (!headerDragging && !resizeDragging && resolved != null) {
            Point position = resolved.place(parent.getWidth(), parent.getHeight(),
                    state.getWidth(), state.getHeight(),
                    state.getAnchorOffset().getX(), state.getAnchorOffset().getY());
            state.setPosition(position.x, position.y);
        }
        clampPosition();
    }

    /**
     * Re-resolves the anchor after a drag or resize: in Auto mode, the
     * snapping point is detected from the current position; a manually
     * chosen anchor is kept. The offset is always re-captured from the
     * current position, so the map keeps its place relative to the anchored
     * sides.
     */
    private void adoptAnchorFromPosition() {
        Container parent = getParent();
        if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0) {
            return;
        }
        AnchorMode anchor = state.getAnchor() != AnchorMode.Auto
                ? state.getAnchor()
                : AnchorMode.detect(parent.getWidth(), parent.getHeight(),
                state.getWidth(), state.getHeight(), state.getX(), state.getY());
        resolvedAnchor = anchor;
        Point offset = anchor.captureOffset(parent.getWidth(), parent.getHeight(),
                state.getWidth(), state.getHeight(), state.getX(), state.getY());
        state.setAnchorOffset(offset.x, offset.y);
    }

    /**
     * Re-applies the anchor and clamps the map into the overlay, then
     * repaints. Called when the overlay (this panel's parent) or the
     * canvas resizes. The canvas preferred size is content-driven, so it does
     * not follow the overlay whenever the content exceeds the viewport
     * (window un-maximize, splitter drag): re-placing must not rely on canvas
     * resize events alone.
     */
    void reclampToOverlay() {
        ensureAnchored();
        placeIntoOverlay();
        applyBounds();
        mapBody.repaint();
    }

    /**
     * Keeps the map inside the overlay, {@link #MAP_EDGE_MARGIN} away from
     * its edges. Skipped while the overlay has no real size yet, so a stored
     * position is not clamped toward 0 before the first layout.
     */
    private void clampPosition() {
        Container parent = getParent();
        if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0) {
            return;
        }
        int maxX = Math.max(MAP_EDGE_MARGIN, parent.getWidth() - state.getWidth() - MAP_EDGE_MARGIN);
        int maxY = Math.max(MAP_EDGE_MARGIN, parent.getHeight() - state.getHeight() - MAP_EDGE_MARGIN);
        state.setPosition(Math.max(MAP_EDGE_MARGIN, Math.min(maxX, state.getX())),
                Math.max(MAP_EDGE_MARGIN, Math.min(maxY, state.getY())));
    }

    /**
     * Pans the canvas so that the clicked map point becomes the center of
     * the view (the former minimap's center-on-point behavior, adapted to
     * canvas zoom: scroll values live in zoomed screen space, the map in
     * world space).
     *
     * @param point the clicked point in map body coordinates
     */
    private void scrollTo(Point point) {
        MapGeometry geometry = computeGeometry();
        if (geometry.scale <= 0) {
            return;
        }
        double zoom = graphEditorUI.getCanvasUI().getZoom();
        if (zoom <= 0) {
            return;
        }
        JScrollBar horizontalScrollBar = graphEditorUI.getScrollPane().getHorizontalScrollBar();
        JScrollBar verticalScrollBar = graphEditorUI.getScrollPane().getVerticalScrollBar();
        int gridX = (int) ((point.x - geometry.viewX) / geometry.scale * zoom);
        int gridY = (int) ((point.y - geometry.viewY) / geometry.scale * zoom);
        horizontalScrollBar.setValue(gridX - horizontalScrollBar.getVisibleAmount() / 2);
        verticalScrollBar.setValue(gridY - verticalScrollBar.getVisibleAmount() / 2);
    }

    /**
     * @return the fit-to-content transform of the canvas into the map body:
     * the canvas is scaled to fit (keeping the aspect ratio) and centered
     */
    private MapGeometry computeGeometry() {
        int canvasWidth = graphEditorUI.getCanvasUI().getWidth();
        int canvasHeight = graphEditorUI.getCanvasUI().getHeight();
        if (canvasWidth <= 0) {
            canvasWidth = mapBody.getWidth();
        }
        if (canvasHeight <= 0) {
            canvasHeight = mapBody.getHeight();
        }
        if (canvasWidth <= 0 || canvasHeight <= 0 || mapBody.getWidth() <= 0 || mapBody.getHeight() <= 0) {
            return new MapGeometry(0, 0, 0);
        }
        double scale = Math.min(1.0 * mapBody.getWidth() / canvasWidth, 1.0 * mapBody.getHeight() / canvasHeight);
        int viewWidth = (int) (canvasWidth * scale);
        int viewHeight = (int) (canvasHeight * scale);
        int viewX = mapBody.getWidth() / 2 - viewWidth / 2;
        int viewY = mapBody.getHeight() / 2 - viewHeight / 2;
        return new MapGeometry(scale, viewX, viewY);
    }

    /**
     * @return the map layers enabled by the current state, as a non-null set
     * for {@code paintMiniMap}
     */
    private Set<MapLayer> stateLayers() {
        Set<MapLayer> layers = EnumSet.noneOf(MapLayer.class);
        if (state.isLayerNodes()) {
            layers.add(MapLayer.NODES);
        }
        if (state.isLayerEdges()) {
            layers.add(MapLayer.EDGES);
        }
        if (state.isLayerNotes()) {
            layers.add(MapLayer.NOTES);
        }
        return layers;
    }

    /**
     * Starts a header drag: the map follows the mouse, clamped into the
     * overlay. The state is written through only on release (never per
     * mouse move).
     */
    private class HeaderDragController extends MouseAdapter {
        private Point pressScreen;
        private Point startPosition;

        @Override
        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e) || state.isLocked()) {
                return;
            }
            pressScreen = e.getLocationOnScreen();
            startPosition = getLocation();
            headerDragging = true;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!headerDragging || pressScreen == null) {
                return;
            }
            Point screen = e.getLocationOnScreen();
            state.setPosition(startPosition.x + screen.x - pressScreen.x,
                    startPosition.y + screen.y - pressScreen.y);
            clampPosition();
            applyBounds();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!headerDragging) {
                return;
            }
            headerDragging = false;
            boolean moved = !getLocation().equals(startPosition);
            pressScreen = null;
            startPosition = null;
            if (moved) {
                adoptAnchorFromPosition();
                graphEditorUI.saveMapStateToSettings();
            }
        }
    }

    /**
     * Resizes the map from its bottom-right corner, clamped into the
     * overlay. The state is written through only on release (never per
     * mouse move).
     */
    private class ResizeController extends MouseAdapter {
        private Point pressScreen;
        private Dimension startSize;

        @Override
        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e) || state.isLocked()) {
                return;
            }
            pressScreen = e.getLocationOnScreen();
            startSize = new Dimension(state.getWidth(), state.getHeight());
            resizeDragging = true;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!resizeDragging || pressScreen == null) {
                return;
            }
            Point screen = e.getLocationOnScreen();
            int width = startSize.width + screen.x - pressScreen.x;
            int height = startSize.height + screen.y - pressScreen.y;
            Container parent = getParent();
            if (parent != null) {
                // Keep the handle inside the overlay
                width = Math.min(width, parent.getWidth() - state.getX() - MAP_EDGE_MARGIN);
                height = Math.min(height, parent.getHeight() - state.getY() - MAP_EDGE_MARGIN);
            }
            state.setSize(Math.max(MAP_MIN_WIDTH, width), Math.max(MAP_MIN_HEIGHT, height));
            applyBounds();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!resizeDragging) {
                return;
            }
            resizeDragging = false;
            boolean resized = startSize != null
                    && (state.getWidth() != startSize.width || state.getHeight() != startSize.height);
            pressScreen = null;
            startSize = null;
            if (resized) {
                // The size changed: re-capture the anchor offset so the next
                // placement keeps the map where it is
                adoptAnchorFromPosition();
                graphEditorUI.saveMapStateToSettings();
            }
        }
    }

    /**
     * The map body: paints the graph minimap and the viewport rectangle, and
     * pans the canvas on click and drag. The background is painted by the
     * panel chrome, so the body itself stays transparent.
     */
    private class MapBody extends JComponent {

        private MapBody() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            add(resizeHandle);
            MouseAdapter panController = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    scrollTo(e.getPoint());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    scrollTo(e.getPoint());
                }
            };
            addMouseListener(panController);
            addMouseMotionListener(panController);
        }

        @Override
        public void doLayout() {
            super.doLayout();
            resizeHandle.setBounds(getWidth() - MAP_RESIZE_HANDLE_SIZE,
                    getHeight() - MAP_RESIZE_HANDLE_SIZE,
                    MAP_RESIZE_HANDLE_SIZE,
                    MAP_RESIZE_HANDLE_SIZE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics2D = (Graphics2D) g.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            MapGeometry geometry = computeGeometry();

            // The graph, scaled to fit and restricted to the enabled layers
            if (geometry.scale > 0) {
                graphEditorUI.getCanvasUI().getPaintManager().paintMiniMap(graphics2D, geometry.scale, geometry.viewX, geometry.viewY, stateLayers());
            }

            // The viewport rectangle. The former minimap used raw scroll
            // values, which is wrong when the canvas is zoomed: scroll values
            // live in zoomed screen space, so they are converted back to
            // world pixels first.
            double zoom = graphEditorUI.getCanvasUI().getZoom();
            if (geometry.scale > 0 && zoom > 0) {
                JScrollBar horizontalScrollBar = graphEditorUI.getScrollPane().getHorizontalScrollBar();
                JScrollBar verticalScrollBar = graphEditorUI.getScrollPane().getVerticalScrollBar();
                int scrollWidth = (int) (geometry.scale * horizontalScrollBar.getVisibleAmount() / zoom);
                int scrollHeight = (int) (geometry.scale * verticalScrollBar.getVisibleAmount() / zoom);
                int scrollX = (int) (geometry.scale * horizontalScrollBar.getValue() / zoom);
                int scrollY = (int) (geometry.scale * verticalScrollBar.getValue() / zoom);
                graphics2D.setColor(AREA_FILL_COLOR);
                graphics2D.fillRect(geometry.viewX + scrollX, geometry.viewY + scrollY, scrollWidth, scrollHeight);
                graphics2D.setColor(ThemeUtils.getCurrentStyle().getPrimaryColor());
                graphics2D.setStroke(new BasicStroke(MAP_VIEWPORT_STROKE_WIDTH));
                graphics2D.drawRect(geometry.viewX + scrollX, geometry.viewY + scrollY, scrollWidth, scrollHeight);
            }
            graphics2D.dispose();
        }
    }

    /**
     * The fit-to-content transform of the canvas into the map body.
     */
    private static final class MapGeometry {
        private final double scale;
        private final int viewX;
        private final int viewY;

        private MapGeometry(double scale, int viewX, int viewY) {
            this.scale = scale;
            this.viewX = viewX;
            this.viewY = viewY;
        }
    }
}
