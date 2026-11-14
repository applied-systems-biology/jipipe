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

package org.hkijena.jipipe.desktop.app.commandbar;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings;
import org.hkijena.jipipe.api.shortcuts.KeymapSettings;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopKeyboardCaptureDialog;
import org.hkijena.jipipe.desktop.app.shortcuts.JIPipeDesktopShortcutDispatcher;
import org.hkijena.jipipe.desktop.app.shortcuts.ShortcutScope;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.desktop.commons.dialogs.Button;
import org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * The per-window command bar (issue #1346, Task 4): a search overlay shown
 * top-center in the host window that lists the registered shortcut commands
 * (built by {@link JIPipeDesktopCommandBarModel}), the ephemeral window and
 * tab switching commands (built by
 * {@link JIPipeDesktopCommandBarSwitching}), and - in chord mode - the
 * commands whose chord variant continues the pending prefix.
 * <p>
 * <b>Lifecycle.</b> The host window creates the bar once and calls
 * {@link #install(javax.swing.JFrame)} (typically right after the window is
 * realized) and {@link #uninstall()} on dispose. Install adds the bar
 * (invisible) to the window root pane's layered pane, registers a
 * {@link ShortcutScope} rooted at the root pane that answers only the
 * {@link JIPipeShortcutActions#COMMAND_BAR_OPEN} trigger, and installs the
 * dispatcher's chord-prefix hook when no other bar owns it. The hook does
 * NOT open this bar directly: it routes the pending prefix through the
 * statically registered bars ({@link #INSTANCES}) in install order, and the
 * bar whose window owns the keyboard focus accepts it. That makes chord
 * mode work in every window, not only in the first-constructed one.
 * Uninstall mirrors both registrations; the hook is removed only when this
 * is the LAST registered bar AND the dispatcher's slot still holds the
 * hook instance the bars installed (identity check against a class-level
 * record, since the last bar to uninstall is typically not the
 * first-installed owner). With two bars installed, uninstalling one must
 * not drop chord mode for the other.
 * <p>
 * <b>The captureActive invariant (read this before touching close()).</b>
 * While the bar is open, the application-wide shortcut dispatch is suspended
 * ({@link JIPipeDesktopShortcutDispatcher#setCaptureActive(boolean)}): the
 * trigger keystroke itself must reach the bar instead of re-resolving, and
 * typed text must never fire a shortcut. There is no timeout: if an exit
 * path ever skipped {@link #close()}, every keyboard shortcut in the
 * application would stay dead until the app is restarted. Therefore
 * <em>every</em> exit path funnels through {@link #close()}, which is the
 * only method that clears the flag and hides the overlay:
 * <ul>
 * <li>Escape (normal mode, via the search field input map)</li>
 * <li>the trigger keystroke pressed again (a toggle)</li>
 * <li>Enter firing the selected entry</li>
 * <li>focus loss of the search field (e.g. a click anywhere outside)</li>
 * <li>the configure affordance (before the capture dialog opens)</li>
 * <li>any key press while chord mode is open (fired chord, Escape, or an
 * unlisted non-modifier key, via the local {@link KeyEventDispatcher}; a
 * bare modifier press passes through and leaves chord mode open)</li>
 * <li>window focus loss while chord mode is open (the local chord
 * dispatcher is application-wide, so a key pressed in another window must
 * not be swallowed by this bar)</li>
 * <li>{@link #uninstall()} (defensively, so a disposed bar can never leave
 * the flag set)</li>
 * </ul>
 * The flag has a second owner: the capture dialog sets it for its lifetime.
 * The configure path never nests the two owners (it fully closes the bar
 * first), and {@link #close()} clears the flag only while the bar itself set
 * it (the {@code suspendOwned} ownership flag set by the open methods), so a
 * close can never resume the dispatch out from under the dialog's
 * recording.
 * <p>
 * <b>Why configure closes first.</b> The keybinding capture dialog
 * ({@link JIPipeDesktopKeyboardCaptureDialog}) owns the same suspend flag for
 * its lifetime (the recorded chord is often itself a bound shortcut). Nesting
 * the two owners would resume dispatch on the inner close and leave the outer
 * open state untracked, so the bar is fully closed (flag cleared, overlay
 * hidden, focus restored) <em>before</em> the dialog opens. The bar does not
 * reopen afterwards; the conflict warning and the override write mirror the
 * Keyboard settings page's commit path.
 */
public class JIPipeDesktopCommandBar extends JPanel {

    /**
     * The overlay width as a fraction of the root pane width. A layout
     * bound, deliberately not text-derived.
     */
    private static final double OVERLAY_WIDTH_FRACTION = 0.5;
    /**
     * The upper bound of the overlay height as a fraction of the root pane
     * height. A layout bound; a taller list scrolls.
     */
    private static final double OVERLAY_MAX_HEIGHT_FRACTION = 0.6;
    /**
     * The gap between the overlay's top edge and the window's measured top
     * strip (see {@link #topBarHeight()}). A pure gap, not a text-derived
     * size.
     */
    private static final int OVERLAY_TOP_GAP = 8;
    /**
     * The size of the flat icon button convention of
     * {@link UIUtils#makeButtonFlat25x25}: an icon-button size, not a
     * text-derived size.
     */
    private static final int CONFIGURE_BUTTON_SIZE = 25;
    /**
     * The right padding of an entry row (the renderer's border inset).
     */
    private static final int ROW_RIGHT_PADDING = 4;
    /**
     * The width of the clickable configure affordance: the flat icon button
     * plus the row's right padding, i.e. exactly the area the renderer
     * paints the button into.
     */
    private static final int CONFIGURE_HIT_WIDTH = CONFIGURE_BUTTON_SIZE + ROW_RIGHT_PADDING;
    /**
     * The number of recently fired commands listed under the "Recently
     * used" header of the empty-query list (issue #1346, Task 8).
     */
    private static final int RECENT_SECTION_SIZE = 5;

    /**
     * The horizontal padding inside a binding chip (the compound border's
     * empty margin). A pure gap, not a text-derived size.
     */
    private static final int CHIP_PADDING_H = 5;
    /**
     * The vertical padding inside a binding chip. A pure gap.
     */
    private static final int CHIP_PADDING_V = 2;
    /**
     * The wider horizontal gap between the stroke chips of a chord variant
     * (the VS Code-style group separation). A pure gap.
     */
    private static final int CHORD_STROKE_GAP = 8;
    /**
     * The vertical gap between stacked binding variants. A pure gap.
     */
    private static final int VARIANT_ROW_GAP = 2;

    private static final String ACTION_KEY_UP = "command-bar-up";
    private static final String ACTION_KEY_DOWN = "command-bar-down";
    private static final String ACTION_KEY_FIRE = "command-bar-fire";
    private static final String ACTION_KEY_ESCAPE = "command-bar-escape";
    private static final String ACTION_KEY_TRIGGER = "command-bar-trigger";

    /**
     * The "Assign anyway" button of the conflict warning
     * (identity-compared, like the Keyboard settings page's button).
     */
    private static final Button ASSIGN_ANYWAY = Button.custom("Assign anyway");

    /**
     * The bars currently installed into windows, in install order. The
     * dispatcher's chord-prefix hook is a single static slot (first
     * installer wins), but it routes the pending prefix through every bar
     * registered here: each bar's focused-window check makes the bars of
     * the other windows decline cheaply, and the focused window's bar
     * accepts. This is what makes chord mode work in EVERY window, not
     * only the first-constructed one.
     */
    private static final List<JIPipeDesktopCommandBar> INSTANCES = new CopyOnWriteArrayList<>();

    /**
     * The chord-prefix hook instance the bars installed into the
     * dispatcher's static slot, or {@code null} when the slot holds
     * nothing or something foreign. The slot is owned by the first
     * installed bar (first bar wins), but the hook it carries routes
     * through {@link #INSTANCES}. When that first bar uninstalls while
     * other bars remain, it must NOT clear the slot (the remaining bars
     * still need it), and the LAST bar to uninstall is typically not the
     * owner: therefore the clear condition compares the slot with this
     * class-level field by identity when {@link #INSTANCES} becomes
     * empty. That also keeps the bars from clearing a handler that
     * foreign code installed.
     */
    private static volatile Predicate<KeyStroke> installedRoutingHook;

    private final JIPipeDesktopSearchTextField searchField = new JIPipeDesktopSearchTextField();
    private final DefaultListModel<JIPipeDesktopCommandBarModel.Entry> listModel = new DefaultListModel<>();
    private final JList<JIPipeDesktopCommandBarModel.Entry> list = new JList<>(listModel);
    private final JIPipeDesktopShortcutDispatcher dispatcher = JIPipeDesktopShortcutDispatcher.getInstance();
    /**
     * The trigger strokes currently installed in the search field's input
     * map, so a rebind can replace exactly the strokes this bar installed.
     */
    private final List<KeyStroke> installedTriggerStrokes = new ArrayList<>();
    /**
     * Repositions the overlay when the root pane is resized (added in
     * {@link #install}, removed in {@link #uninstall}).
     */
    private final ComponentListener repositionListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            repositionOverlay();
        }
    };

    /**
     * The host window, or {@code null} while not installed.
     */
    private JFrame window;
    /**
     * The host window's root pane (the scope root and the size reference of
     * the overlay), or {@code null} while not installed.
     */
    private JRootPane rootPane;
    /**
     * The constraints the bar was added to the layered pane with (remembered
     * for a clean uninstall).
     */
    private Object layerConstraints;
    /**
     * The trigger scope registered with the dispatcher, or {@code null}
     * while not installed.
     */
    private ShortcutScope scope;
    /**
     * The chord-prefix hook this bar installed, or {@code null}. Kept so
     * {@link #uninstall()} can remove it only while this bar still owns it.
     */
    private Predicate<KeyStroke> chordPrefixHandler;
    /**
     * The local key event dispatcher of chord mode, or {@code null} while
     * chord mode is closed. Removed in every {@link #close()}.
     */
    private KeyEventDispatcher chordDispatcher;
    /**
     * The window focus listener of chord mode (removed again in
     * {@link #close()}): the chord key dispatcher is installed
     * application-wide, so without this listener a key pressed in ANOTHER
     * window while chord mode is open would be swallowed by this bar.
     */
    private WindowFocusListener chordWindowFocusListener;
    /**
     * The entries the list currently displays (the same rows the chord
     * dispatcher of chord mode matches against).
     */
    private List<JIPipeDesktopCommandBarModel.Entry> entries = List.of();
    /**
     * The pending chord prefix while chord mode is open.
     */
    private KeyStroke chordPrefix;
    /**
     * Whether the overlay is in chord mode (openChordMode) rather than
     * normal mode (openNormalMode).
     */
    private boolean chordMode;
    /**
     * The focus owner captured when the bar was opened; restored by
     * {@link #close()}.
     */
    private Component previousFocusOwner;
    /**
     * Whether the bar itself set {@code captureActive} (in
     * {@link #openNormalMode()} or {@link #openChordMode(KeyStroke)}) and
     * therefore owns clearing it: {@link #close()} resumes the dispatch
     * only while this flag is set. Visibility is not an ownership
     * guarantee, so a close of a bar that did not arm the suspension (or
     * whose owner was already handed over, e.g. to the capture dialog)
     * must leave the flag alone.
     */
    private boolean suspendOwned;

    /**
     * Creates the bar. Use {@link #install(JFrame)} to attach it to a window.
     */
    public JIPipeDesktopCommandBar() {
        super(new BorderLayout(0, 4));
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(UIUtils.createControlBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        searchField.setToolTipText("Type to filter commands");
        add(searchField, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // The list must never take the focus: the search field keeps it for
        // the whole open lifetime (its focus loss closes the bar), while
        // mouse clicks still select rows.
        list.setFocusable(false);
        list.setCellRenderer(new EntryCellRenderer());
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        installSearchFieldBehavior();
        installKeyBindings();
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onListClicked(e);
            }
        });

        setVisible(false);
    }

    /**
     * The list mode the bar renders with. Package-private indirection so the
     * read can be changed without touching the call sites.
     * <p>
     * Issue #1346, Task 6: reads the persisted list mode setting
     * ({@link CommandBarSettings#currentMode()}, null/unknown-safe with the
     * {@link JIPipeDesktopCommandBarModel.ListMode#GRAYED} fallback).
     * <p>
     * The mode's window-kind filter (graph-family actions hidden while no
     * graph editor hosts the context, in GRAYED and AVAILABLE_ONLY) is
     * applied by the bar itself (see {@code modeFilteredActions}), not by
     * the model: EVERYTHING is a real mode that passes every registry
     * action through.
     */
    static JIPipeDesktopCommandBarModel.ListMode currentListMode() {
        return CommandBarSettings.currentMode();
    }

    // ------------------------------------------------------------------
    // Lifecycle: install / uninstall
    // ------------------------------------------------------------------

    /**
     * Installs the bar into the given window: adds it (invisible) to the
     * root pane's layered pane, registers the trigger shortcut scope rooted
     * at the root pane, and installs the dispatcher's chord-prefix hook when
     * no other bar owns it. The hook routes every pending prefix through the
     * statically registered bars (see {@link #INSTANCES}); each bar's
     * focused-window check makes only the focused window's bar accept it, so
     * chord mode works in every window, not only the first-constructed one.
     * Installing twice into the same window is a no-op; installing into
     * another window uninstalls first.
     *
     * @param window the host window
     */
    public void install(JFrame window) {
        Objects.requireNonNull(window, "window");
        if (this.window == window) {
            return;
        }
        if (this.window != null) {
            uninstall();
        }
        INSTANCES.add(this);
        this.window = window;
        this.rootPane = window.getRootPane();
        // Above the content pane and menu bar, below popups (tooltips and
        // combobox lists of the bar itself stay visible).
        this.layerConstraints = JLayeredPane.MODAL_LAYER;
        rootPane.getLayeredPane().add(this, layerConstraints);
        setVisible(false);
        rootPane.addComponentListener(repositionListener);

        // The trigger scope: rooted at the window's root pane (the same root
        // resolution as JIPipeDesktopDockPanel) and answering only the open
        // command bar action. openNormalMode ignores a re-open of an already
        // open bar (toggle semantics; the dispatch is suspended while open,
        // so the trigger cannot fire through the scope in that state anyway).
        scope = new ShortcutScope(rootPane, actionId -> {
            if (JIPipeShortcutActions.COMMAND_BAR_OPEN.id().equals(actionId)) {
                return this::openNormalMode;
            }
            return null;
        });
        dispatcher.registerScope(scope);

        // The chord-prefix hook (the dispatcher's PREFIX_STARTED branch is
        // wired in Task 5). Only the first installed bar installs it, but
        // the installed hook routes the prefix through INSTANCES, so every
        // window's bar keeps receiving chord prefixes (see
        // routeChordPrefix): the first bar merely owns the static slot.
        if (JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler() == null) {
            chordPrefixHandler = prefix -> routeChordPrefix(prefix);
            JIPipeDesktopShortcutDispatcher.setChordPrefixHandler(chordPrefixHandler);
            installedRoutingHook = chordPrefixHandler;
        }
    }

    /**
     * Uninstalls the bar: closes it (defensively - a disposed bar must never
     * leave the dispatch suspended), removes the resize listener,
     * unregisters the scope, removes this bar from the static registry, and
     * removes the chord-prefix hook only when this bar still owns it
     * (identity check) AND no other bar remains registered: with two bars
     * installed, uninstalling one must not drop chord mode for the other,
     * and the hook goes away only when the LAST bar uninstalls. The clear
     * compares the slot with the hook instance the bars installed
     * ({@link #installedRoutingHook}), not with this bar's own reference:
     * the bar that uninstalls last is typically NOT the first-installed
     * owner, so a per-bar identity check could never fire for it.
     * Uninstalling a non-installed bar is a no-op.
     */
    public void uninstall() {
        if (window == null) {
            return;
        }
        close();
        rootPane.removeComponentListener(repositionListener);
        rootPane.getLayeredPane().remove(this);
        if (scope != null) {
            dispatcher.unregisterScope(scope);
            scope = null;
        }
        INSTANCES.remove(this);
        if (INSTANCES.isEmpty() && installedRoutingHook != null
                && JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler() == installedRoutingHook) {
            JIPipeDesktopShortcutDispatcher.setChordPrefixHandler(null);
            installedRoutingHook = null;
        }
        chordPrefixHandler = null;
        layerConstraints = null;
        rootPane = null;
        window = null;
    }

    // ------------------------------------------------------------------
    // Open / close
    // ------------------------------------------------------------------

    /**
     * Opens the overlay in normal mode: top-center of the layered pane, the
     * width a fraction of the root pane and the height the content's
     * preferred size (clamped to a fraction of the root pane height),
     * focused into the search field, with the app-wide shortcut dispatch
     * suspended for the open lifetime. See the class javadoc for the
     * captureActive invariant.
     */
    public void openNormalMode() {
        if (window == null || rootPane == null) {
            return;
        }
        if (isVisible()) {
            // Re-entry guard (toggle semantics, symmetric with
            // openChordMode's focus-owner guard): a second open must not
            // overwrite previousFocusOwner, or the focus restore of the
            // original open would be destroyed.
            return;
        }
        chordMode = false;
        chordPrefix = null;
        if (previousFocusOwner == null) {
            previousFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        }
        // A fresh open shows the unfiltered list.
        searchField.setText("");
        rebuildList();
        setVisible(true);
        repositionOverlay();
        JIPipeDesktopShortcutDispatcher.getInstance().setCaptureActive(true);
        suspendOwned = true;
        searchField.getTextField().requestFocusInWindow();
    }

    /**
     * Opens the overlay in chord mode: the list shows the registry commands
     * whose chord variant continues {@code prefix} (available first, no
     * ephemeral switching entries). Called by the dispatcher's chord-prefix
     * hook while a chord prefix is pending; returns {@code false} when the
     * bar's window does not own the keyboard focus (the chord belongs to
     * another window) or the bar is not installed.
     * <p>
     * While open, a local {@link KeyEventDispatcher} observes every key
     * press (the capture dialog's lifecycle pattern): the second key of a
     * listed chord variant fires that command and closes; Escape closes; a
     * bare modifier press passes through untouched and keeps chord mode
     * open (keyboards emit the modifier before the letter of a shifted
     * second key); any other key cancels and closes (the non-modifier
     * presses are all consumed, matching the dispatcher's swallow
     * semantics for a pending chord).
     *
     * @param prefix the first stroke of the pending chord
     * @return whether the bar opened (and consumed the chord)
     */
    public boolean openChordMode(KeyStroke prefix) {
        if (window == null || rootPane == null) {
            return false;
        }
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (kfm.getFocusedWindow() != window) {
            // The chord was pressed in another window: not ours to handle.
            return false;
        }
        chordMode = true;
        chordPrefix = prefix;
        if (previousFocusOwner == null) {
            previousFocusOwner = kfm.getFocusOwner();
        }
        // Chord mode is about bound commands; the search filter is not
        // applied and the field starts empty.
        searchField.setText("");
        rebuildList();
        setVisible(true);
        repositionOverlay();
        // Harmless if the dispatcher handoff already set it (Task 5 wires
        // that handoff).
        JIPipeDesktopShortcutDispatcher.getInstance().setCaptureActive(true);
        suspendOwned = true;
        installChordDispatcher();
        installChordWindowFocusListener();
        return true;
    }

    /**
     * The dispatcher's single chord-prefix hook is a static slot, so it can
     * be owned by at most one bar. This static method is what the owning
     * bar's hook delegates to: it routes the pending prefix through every
     * currently installed bar ({@link #INSTANCES}) in install order and
     * returns {@code true} when the first bar to accept it did so.
     *
     * @param prefix the first stroke of the pending chord
     * @return whether an installed bar opened (and consumed the chord)
     */
    static boolean routeChordPrefix(KeyStroke prefix) {
        for (JIPipeDesktopCommandBar bar : INSTANCES) {
            if (bar.acceptChordPrefix(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The per-bar seam of {@link #routeChordPrefix(KeyStroke)}: the bar
     * accepts the prefix only while it is installed AND its host window
     * owns the keyboard focus (the focused-window check of
     * {@link #openChordMode(KeyStroke)}), so the bars of the other windows
     * decline cheaply and the focused window's bar opens. Exposing this
     * separately from the route makes the acceptance contract testable
     * without faking the keyboard focus manager.
     *
     * @param prefix the first stroke of the pending chord
     * @return whether this bar opened chord mode and consumed the prefix
     */
    boolean acceptChordPrefix(KeyStroke prefix) {
        if (window == null || rootPane == null) {
            return false;
        }
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        return kfm.getFocusedWindow() == window && openChordMode(prefix);
    }

    /**
     * @return the chord-prefix hook this bar installed, or {@code null}
     *         when no bar installed one; exposed for the tests of the
     *         multi-window routing (the clear condition of
     *         {@link #uninstall()} compares the dispatcher's slot with the
     *         class-level {@link #installedRoutingHook} instead)
     */
    Predicate<KeyStroke> chordPrefixHandler() {
        return chordPrefixHandler;
    }

    /**
     * The single exit path of the overlay: removes the local chord key
     * dispatcher and the chord window focus listener, hides the overlay,
     * restores the focus owner captured at open time, and clears the
     * app-wide dispatch suspension - but only when the bar itself armed it
     * ({@code suspendOwned}): visibility is
     * not an ownership guarantee, so a close that reaches a bar whose
     * suspension belongs to the capture dialog must leave the flag alone
     * (hiding an already hidden bar, in contrast, is harmless and stays
     * guarded by visibility alone). Every exit path funnels here; see the
     * class javadoc for why this is the most important invariant of the
     * class.
     */
    public void close() {
        if (chordDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(chordDispatcher);
            chordDispatcher = null;
        }
        if (chordWindowFocusListener != null) {
            if (window != null) {
                window.removeWindowFocusListener(chordWindowFocusListener);
            }
            chordWindowFocusListener = null;
        }
        chordMode = false;
        chordPrefix = null;
        if (isVisible()) {
            setVisible(false);
            restorePreviousFocus();
        }
        if (suspendOwned) {
            JIPipeDesktopShortcutDispatcher.getInstance().setCaptureActive(false);
            suspendOwned = false;
        }
        previousFocusOwner = null;
    }

    private void restorePreviousFocus() {
        Component owner = previousFocusOwner;
        previousFocusOwner = null;
        if (owner != null) {
            owner.requestFocusInWindow();
        }
    }

    /**
     * The component every availability resolution and every fire walks the
     * scope chain against: the focus owner captured at open time (the bar
     * itself is only the fallback for the degenerate no-focus case).
     * <p>
     * The bar lives in the root pane's LAYERED PANE, a sibling of the
     * content pane, so no scope rooted inside the content (e.g. a graph
     * editor) can ever contain the bar component itself: resolving against
     * the bar would make every non-root command unavailable. The captured
     * owner makes the walk identical to the dispatcher's own fire path
     * (design Section 4.1: the scopes containing the focus owner).
     * <p>
     * The text-input guard is honored against this reference (no bypass):
     * {@code dispatcher.resolveAction} applies it, so the bar's
     * availability mirrors exactly what a raw keypress would fire. Opened
     * from a text field, graph commands show grayed (and
     * {@code AVAILABLE_ONLY} hides them); the fallback {@code this} is not
     * a text input, so the degenerate case resolves without the guard.
     */
    private Component referenceComponent() {
        return previousFocusOwner != null ? previousFocusOwner : this;
    }

    // ------------------------------------------------------------------
    // Entry list
    // ------------------------------------------------------------------

    /**
     * Rebuilds the displayed entries: collects the registry commands plus
     * (in normal mode) the ephemeral switching commands, then applies the
     * chord filter (chord mode) or the search query (normal mode), and
     * refreshes the trigger keystroke bindings. Must run on the EDT.
     */
    private void rebuildList() {
        // The window-kind filter of the list modes is computed once per
        // rebuild and applied to the registry list and the recents alike.
        boolean graphHosted = graphHosted();
        List<JIPipeShortcutAction> registryActions = modeFilteredActions(
                JIPipeShortcutRegistry.getInstance().all(), graphHosted);
        List<JIPipeDesktopCommandBarModel.Entry> registryEntries = JIPipeDesktopCommandBarModel.collect(
                registryActions,
                JIPipeShortcutSettings.current(),
                currentListMode(),
                // Availability resolves against the focus owner captured at
                // open (see referenceComponent): the same scope walk a raw
                // keypress would take, text-input guard included.
                id -> dispatcher.resolveAction(id, referenceComponent()),
                chordMode ? List.of() : JIPipeDesktopCommandBarSwitching.all(window));
        List<JIPipeDesktopCommandBarModel.Entry> display;
        if (chordMode) {
            display = JIPipeDesktopCommandBarModel.chordEntries(registryEntries, chordPrefix);
        } else {
            // The known recent ids are read once per rebuild and shared: the
            // tie-break uses the full list, the "Recently used" section the
            // first few of their actions (through the same mode filter as
            // the main list, so a mode never lists a command the main list
            // hides).
            List<String> knownRecentIds = recentCommandIds();
            display = filterByQuery(registryEntries, searchField.getText(),
                    knownRecentIds, modeFilteredActions(recentActions(knownRecentIds), graphHosted));
        }
        entries = display;
        listModel.clear();
        for (JIPipeDesktopCommandBarModel.Entry entry : display) {
            listModel.addElement(entry);
        }
        updateTriggerBindings();
        selectFirstSelectable();
        // The list content changed: the overlay must follow (the open-time
        // height would otherwise linger after a query filtered the list).
        repositionOverlay();
    }

    /**
     * The window-kind filter of the list modes: in
     * {@link JIPipeDesktopCommandBarModel.ListMode#GRAYED} and
     * {@link JIPipeDesktopCommandBarModel.ListMode#AVAILABLE_ONLY},
     * graph-family actions ({@link JIPipeShortcutAction#scopeFamily()}
     * equal to {@link JIPipeShortcutAction#FAMILY_GRAPH}) are hidden while
     * no graph editor hosts the reference component (the captured focus
     * owner); {@link JIPipeDesktopCommandBarModel.ListMode#EVERYTHING} and
     * the graph-hosted case pass the given actions through unchanged, as do
     * project- and dock-family actions in every mode. Ephemeral entries are
     * filtered by their callers' contract (they are never passed here).
     */
    private List<JIPipeShortcutAction> modeFilteredActions(List<JIPipeShortcutAction> actions,
                                                           boolean graphHosted) {
        if (currentListMode() == JIPipeDesktopCommandBarModel.ListMode.EVERYTHING || graphHosted) {
            return actions;
        }
        List<JIPipeShortcutAction> result = new ArrayList<>(actions.size());
        for (JIPipeShortcutAction action : actions) {
            if (!JIPipeShortcutAction.FAMILY_GRAPH.equals(action.scopeFamily())) {
                result.add(action);
            }
        }
        return result;
    }

    /**
     * Whether a graph editor hosts the reference component: some scope
     * containing it is rooted at a {@link JIPipeDesktopGraphEditorUI}
     * (the graph editor registers its shortcut scope with itself as root).
     */
    private boolean graphHosted() {
        Component reference = referenceComponent();
        for (ShortcutScope scope : dispatcher.scopesContaining(reference)) {
            if (scope.root() instanceof JIPipeDesktopGraphEditorUI) {
                return true;
            }
        }
        return false;
    }

    /**
     * The search filter of the normal mode: with a non-empty query an entry
     * is kept when its name fuzzy-matches the query
     * ({@link JIPipeDesktopCommandBarSearch#matches}) or its category
     * contains the query case-insensitively (category filtering keeps the
     * plain substring behavior); a category header is kept only while at
     * least one of its entries matched. With an empty query a "Recently
     * used" section is prepended (issue #1346, Task 8): the header plus up
     * to {@value #RECENT_SECTION_SIZE} recently fired commands that are
     * still registered and resolvable, most recent first - in front of the
     * full list, WITHOUT deduplicating: a command that also appears in its
     * own category below stays there (VS Code behavior).
     * <p>
     * With a non-empty query the Task 7 ranking is applied (fuzzy name
     * matches before category-contains-only matches, descending fuzzy
     * score, registry order for equal scores), and Task 8 breaks EQUAL
     * ranks by recency: entries of the same rank are ordered by their
     * position in the recent list (earlier = better; ids not in the recent
     * list rank last). Recency is a second criterion only, so entries of
     * different ranks keep the Task 7 ordering. The re-ranked list is
     * emitted under a single "Results" header: the ranked list no longer
     * walks the original category groups, so per-category headers would be
     * wrong.
     */
    private List<JIPipeDesktopCommandBarModel.Entry> filterByQuery(
            List<JIPipeDesktopCommandBarModel.Entry> entries, String query,
            List<String> recentIds, List<JIPipeShortcutAction> recentActions) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            // prependRecent returns fullList unchanged when there is
            // nothing recent to show.
            return prependRecent(entries, recentActions);
        }
        Map<String, Integer> recencyIndex = recencyIndex(recentIds);
        List<ScoredEntry> scored = new ArrayList<>(entries.size());
        int position = 0;
        for (JIPipeDesktopCommandBarModel.Entry entry : entries) {
            position++;
            if (entry.isCategoryHeader()) {
                continue;
            }
            int matchType = 0;
            double score = JIPipeDesktopCommandBarSearch.score(q, entry.name());
            if (score > 0.0) {
                matchType = 1;
            } else if (containsIgnoreCase(entry.category(), q)) {
                matchType = 2;
                score = 0.0;
            }
            if (matchType == 0) {
                continue;
            }
            int recency = recencyIndex.getOrDefault(entry.actionId(), recencyIndex.size());
            scored.add(new ScoredEntry(entry, matchType, score, recency, position));
        }
        scored.sort(Comparator
                .comparingInt(ScoredEntry::matchType)
                .thenComparing(ScoredEntry::score, Comparator.reverseOrder())
                .thenComparingInt(ScoredEntry::recency)
                .thenComparingInt(ScoredEntry::position));
        List<JIPipeDesktopCommandBarModel.Entry> result = new ArrayList<>(scored.size() * 2);
        boolean headerless = true;
        for (ScoredEntry s : scored) {
            if (headerless) {
                result.add(JIPipeDesktopCommandBarModel.Entry.categoryHeader("Results"));
                headerless = false;
            }
            result.add(s.entry());
        }
        return result;
    }

    /**
     * Prepends the "Recently used" section to the full (empty-query) list:
     * one category header plus up to {@value #RECENT_SECTION_SIZE} recently
     * fired commands that are still registered and resolvable, most recent
     * first. The recents are resolved through the SAME resolver the rest of
     * the list used ({@link #resolveEntry}), so their availability (and the
     * mode applied to them) is identical to a listing of the same registry
     * commands.
     */
    private List<JIPipeDesktopCommandBarModel.Entry> prependRecent(
            List<JIPipeDesktopCommandBarModel.Entry> fullList,
            List<JIPipeShortcutAction> recentActions) {
        if (recentActions.isEmpty()) {
            return fullList;
        }
        KeymapSettings settings = JIPipeShortcutSettings.current();
        List<JIPipeDesktopCommandBarModel.Entry> recentEntries = JIPipeDesktopCommandBarModel.collectRecent(
                recentActions, settings, currentListMode(), this::resolveEntry);
        if (recentEntries.isEmpty()) {
            // No recent survived the mode (e.g. AVAILABLE_ONLY with every
            // recent unavailable): no orphan "Recently used" header.
            return fullList;
        }
        List<JIPipeDesktopCommandBarModel.Entry> result =
                new ArrayList<>(recentEntries.size() + 1 + fullList.size());
        result.add(JIPipeDesktopCommandBarModel.Entry.categoryHeader("Recently used"));
        result.addAll(recentEntries);
        result.addAll(fullList);
        return result;
    }

    /**
     * The id-to-position index of the recent list (earlier = better), used
     * for the recency tie-break of equal search scores.
     */
    private static Map<String, Integer> recencyIndex(List<String> recentIds) {
        Map<String, Integer> index = new HashMap<>(recentIds.size() * 2);
        for (int i = 0; i < recentIds.size(); i++) {
            index.putIfAbsent(recentIds.get(i), i);
        }
        return index;
    }

    /**
     * Resolves an action id to its runnable in the current window state,
     * or {@code null} when unavailable: the same scope-chain walk the list
     * build uses, against the focus owner captured at open
     * ({@link #referenceComponent()}). The text-input guard is honored (no
     * bypass), so the availability mirrors what a raw keypress would fire.
     */
    private Runnable resolveEntry(String actionId) {
        return dispatcher.resolveAction(actionId, referenceComponent());
    }

    /**
     * The recently fired command ids (most recent first), filtered down to
     * ids that are still registered; empty when nothing is stored.
     */
    private static List<String> recentCommandIds() {
        CommandBarState state = CommandBarSettings.stateBinding().get();
        List<String> recent = state == null
                ? List.of()
                : state.getRecentCommandIds();
        return CommandBarState.knownRecent(recent, JIPipeShortcutRegistry.getInstance()::isRegistered);
    }

    /**
     * The actions of the given known recent ids, in the same order,
     * truncated to {@value #RECENT_SECTION_SIZE}: the rows of the
     * "Recently used" section (the tie-break uses the full id list).
     */
    private static List<JIPipeShortcutAction> recentActions(List<String> knownRecentIds) {
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        List<JIPipeShortcutAction> actions = new ArrayList<>(RECENT_SECTION_SIZE);
        for (String id : knownRecentIds) {
            JIPipeShortcutAction action = registry.byId(id);
            if (action != null) {
                actions.add(action);
            }
            if (actions.size() >= RECENT_SECTION_SIZE) {
                break;
            }
        }
        return actions;
    }

    private static boolean containsIgnoreCase(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    /**
     * One matched entry with its rank components (the tie-break of
     * {@link #filterByQuery}): the match type (a fuzzy name match outranks
     * a category-contains-only match), the fuzzy score, the recency
     * position (unknown ids rank last), and the original list position
     * (keeps the registry order for fully tied entries).
     */
    private record ScoredEntry(JIPipeDesktopCommandBarModel.Entry entry, int matchType,
                               double score, int recency, int position) {
    }

    // ------------------------------------------------------------------
    // Selection and firing
    // ------------------------------------------------------------------

    /**
     * @param entry a row, may be {@code null}
     * @return whether the row can hold the selection (category headers are
     *         skipped in selection movement)
     */
    private static boolean isSelectableRow(JIPipeDesktopCommandBarModel.Entry entry) {
        return entry != null && !entry.isCategoryHeader();
    }

    private void selectFirstSelectable() {
        for (int i = 0; i < listModel.size(); i++) {
            if (isSelectableRow(listModel.get(i))) {
                list.setSelectedIndex(i);
                list.ensureIndexIsVisible(i);
                return;
            }
        }
        list.clearSelection();
    }

    /**
     * Moves the selection by one row in the given direction, skipping
     * category headers, clamping at the list ends (no wrap).
     *
     * @param delta -1 for up, +1 for down
     */
    private void moveSelection(int delta) {
        int size = listModel.size();
        if (size == 0) {
            return;
        }
        int index = list.getSelectedIndex();
        if (index < 0) {
            // No selection yet: the first step lands on the nearest end.
            index = delta > 0 ? -1 : size;
        }
        int next = index;
        while (true) {
            next += delta;
            if (next < 0 || next >= size) {
                return;
            }
            if (isSelectableRow(listModel.get(next))) {
                list.setSelectedIndex(next);
                list.ensureIndexIsVisible(next);
                return;
            }
        }
    }

    /**
     * Fires the selected entry (the Enter path).
     */
    private void fireSelected() {
        fireEntry(list.getSelectedValue());
    }

    /**
     * Fires the given entry: the shared path of Enter and a plain row
     * click. Only available entries with a command fire. Registry commands
     * are re-resolved through the dispatcher at fire time, against the
     * focus owner captured at open ({@link #referenceComponent()}; the
     * resolution is one keystroke old at most, since the availability may
     * have changed since the list was built); a re-resolve that comes up
     * empty leaves the bar open (nothing fired), otherwise the command
     * runs on the EDT and the bar closes (the exit path funnels through
     * close()).
     */
    private void fireEntry(JIPipeDesktopCommandBarModel.Entry entry) {
        if (entry == null || entry.isCategoryHeader() || !entry.isAvailable()
                || entry.command() == null) {
            return;
        }
        Runnable command;
        if (entry.actionId() != null) {
            command = dispatcher.resolveAction(entry.actionId(), referenceComponent());
            if (command == null) {
                // No longer resolvable: do not fire (and do not close).
                return;
            }
            // Record the fire for the "Recently used" section and the
            // recency tie-break (Task 8); only registry commands carry an
            // action id, so ephemeral entries are never recorded.
            CommandBarSettings.recordFired(entry.actionId());
        } else {
            command = entry.command();
        }
        SwingUtilities.invokeLater(command);
        close();
    }

    // ------------------------------------------------------------------
    // Chord mode key handling
    // ------------------------------------------------------------------

    /**
     * Installs the local chord-mode key event dispatcher (the capture
     * dialog's lifecycle pattern: added before the interaction, removed in
     * {@link #close()}). Removing a dispatcher from inside its own callback
     * is safe: the keyboard focus manager iterates a copy of its dispatcher
     * list.
     */
    private void installChordDispatcher() {
        if (chordDispatcher != null) {
            return;
        }
        KeyEventDispatcher localDispatcher = e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
            if (stroke == null) {
                return false;
            }
            if (chordPrefix != null) {
                for (JIPipeDesktopCommandBarModel.Entry entry : entries) {
                    if (!entry.isAvailable() || entry.command() == null) {
                        continue;
                    }
                    for (List<KeyStroke> variant : entry.variants()) {
                        if (variant.size() == 2 && variant.get(0).equals(chordPrefix)
                                && variant.get(1).equals(stroke)) {
                            Runnable command = entry.command();
                            SwingUtilities.invokeLater(command);
                            close();
                            return true;
                        }
                    }
                }
            }
            if (stroke.getKeyCode() == KeyEvent.VK_ESCAPE && stroke.getModifiers() == 0) {
                // Escape: a clean close.
                close();
                return true;
            }
            if (isModifierKeyCode(stroke.getKeyCode())) {
                // A bare modifier press never completes a stroke: pass it
                // through and keep chord mode open (the capture dialog's
                // recorder follows the same rule). Keyboards emit the
                // modifier press BEFORE the letter, so closing here would
                // make every shifted second key (e.g. Ctrl+T then Shift+P)
                // unreachable: the Shift would swallow the chord, and the
                // P would arrive in normal dispatch.
                return false;
            }
            // An unlisted key: cancel and close, matching the dispatcher's
            // swallow semantics for a chord that resolves to nothing.
            close();
            return true;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(localDispatcher);
        chordDispatcher = localDispatcher;
    }

    /**
     * Installs the chord-mode window focus listener (removed again in
     * {@link #close()}): the local chord key dispatcher is
     * application-wide, so with chord mode open in window A, any key
     * pressed in window B would be swallowed by A's bar. Closing on window
     * focus loss ends the chord before the focus can move on. (Normal mode
     * needs no equivalent: its search field closes the bar through its own
     * focusLost, which chord mode does not have - its search field is
     * never focused.)
     */
    private void installChordWindowFocusListener() {
        if (chordWindowFocusListener != null || window == null) {
            return;
        }
        WindowFocusListener listener = new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                close();
            }
        };
        window.addWindowFocusListener(listener);
        chordWindowFocusListener = listener;
    }

    /**
     * @param keyCode a key code
     * @return whether the key is a pure modifier key (Ctrl, Shift, Alt,
     *         Alt Graph or Meta; the key codes of the left/right variants
     *         are the same as the plain ones in AWT)
     */
    private static boolean isModifierKeyCode(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT
                || keyCode == KeyEvent.VK_CONTROL
                || keyCode == KeyEvent.VK_ALT
                || keyCode == KeyEvent.VK_META
                || keyCode == KeyEvent.VK_ALT_GRAPH;
    }

    // ------------------------------------------------------------------
    // Configure keybinding affordance
    // ------------------------------------------------------------------

    /**
     * The click handler of the list: a click into the configure affordance
     * (the rightmost {@link #CONFIGURE_HIT_WIDTH} pixels of a configurable,
     * available row) opens the capture flow for that row; a plain click on
     * an available registry or ephemeral row FIRES it (the same path as
     * Enter: re-resolve, fire, close); a click on a grayed/unavailable row
     * only selects it.
     */
    private void onListClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index < 0) {
            return;
        }
        Rectangle bounds = list.getCellBounds(index, index);
        if (bounds == null || !bounds.contains(e.getPoint())) {
            return;
        }
        JIPipeDesktopCommandBarModel.Entry entry = listModel.get(index);
        if (entry == null || entry.isCategoryHeader()) {
            return;
        }
        if (entry.isConfigurable() && entry.isAvailable() && entry.actionId() != null
                && e.getX() >= bounds.x + bounds.width - CONFIGURE_HIT_WIDTH) {
            configureEntry(entry);
            return;
        }
        // A click selects the row in any case; an available entry is fired
        // through the Enter path (the affordance strip was handled above).
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
        if (entry.isAvailable() && entry.command() != null) {
            fireEntry(entry);
        }
    }

    /**
     * Runs the Keyboard settings page's commit path for the given entry:
     * FIRST closes the bar (the capture dialog owns the dispatch suspension
     * - see the class javadoc), then records a chord with
     * {@link JIPipeDesktopKeyboardCaptureDialog#capture}, resolves conflicts
     * (skipped for a cleared chord) with an identity-checked "Assign anyway"
     * warning, and writes the override through the keymap binding. The bar
     * does not reopen afterwards.
     */
    private void configureEntry(JIPipeDesktopCommandBarModel.Entry entry) {
        JIPipeShortcutAction action = JIPipeShortcutRegistry.getInstance().byId(entry.actionId());
        if (action == null || window == null) {
            return;
        }
        // The capture dialog owns the captureActive switch: the bar must be
        // fully closed before the dialog opens, so the two suspend owners
        // never nest.
        close();
        KeymapSettings current = JIPipeShortcutSettings.current();
        List<List<KeyStroke>> currentVariants = JIPipeShortcutRegistry.getInstance()
                .bindingFor(action.id(), current).variants();
        Optional<List<KeyStroke>> captured =
                JIPipeDesktopKeyboardCaptureDialog.capture(window, action.name(), currentVariants);
        if (captured.isEmpty()) {
            return;
        }
        List<KeyStroke> chord = captured.get();
        if (!chord.isEmpty()) {
            KeymapSettings settings = JIPipeShortcutSettings.current();
            List<String> conflictIds = JIPipeShortcutRegistry.getInstance()
                    .findConflicts(action.id(), List.of(chord), settings);
            if (!conflictIds.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (String id : conflictIds) {
                    JIPipeShortcutAction other = JIPipeShortcutRegistry.getInstance().byId(id);
                    names.add(escapeHtml(other != null ? other.name() : id));
                }
                // An <html> message passes through the dialog chrome verbatim,
                // so the (already escaped) names are safe to embed.
                String message = "<html>The shortcut you recorded is already assigned to:<br>"
                        + String.join(", ", names) + "<br><br>"
                        + "Assigning it here too would bind the same keys to several actions. "
                        + "Assign it anyway?";
                // asDefault() returns a copy; keep that instance for the
                // identity comparison (the chrome records the exact instance).
                Button assignAnyway = ASSIGN_ANYWAY.asDefault();
                Optional<Button> choice = JIPipeDesktopDialogs.choose(window, "Shortcut conflict", message,
                        assignAnyway, Button.CANCEL);
                if (choice.filter(b -> b == assignAnyway).isEmpty()) {
                    return;
                }
            }
        }
        KeymapSettings settings = JIPipeShortcutSettings.current();
        settings.setOverride(action.id(), List.of(chord));
        JIPipeShortcutSettings.binding().set(settings);
    }

    /**
     * Escapes the HTML-significant characters of a plain string, for
     * embedding in the (verbatim-passed) {@code <html>} conflict message.
     */
    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ------------------------------------------------------------------
    // Overlay geometry
    // ------------------------------------------------------------------

    /**
     * Positions the overlay top-center in the layered pane: the width is a
     * fraction of the root pane width (never text-derived); the height is
     * the content's preferred size clamped to a fraction of the root pane
     * height (a clamped list scrolls). The top edge sits below the window's
     * measured top strip (see {@link #topBarHeight()}) plus the top gap, so
     * the overlay never covers the toolbar row.
     */
    private void repositionOverlay() {
        if (rootPane == null || !isVisible()) {
            return;
        }
        int rootWidth = rootPane.getWidth();
        int rootHeight = rootPane.getHeight();
        if (rootWidth <= 0 || rootHeight <= 0) {
            return;
        }
        int width = (int) (rootWidth * OVERLAY_WIDTH_FRACTION);
        int maxHeight = (int) (rootHeight * OVERLAY_MAX_HEIGHT_FRACTION);
        int height = Math.min(getPreferredSize().height, maxHeight);
        int x = (rootWidth - width) / 2;
        setBounds(x, topBarHeight() + OVERLAY_TOP_GAP, width, height);
        doLayout();
    }

    /**
     * The measured height of the window's top strip, in layered-pane
     * coordinates: how far the bar must be pushed down to not cover the
     * toolbar row (the Cache / Tasks / Logs buttons and similar).
     * <p>
     * Measured, never assumed: the strip is the maximum {@code y + height}
     * over the content pane's laid-out children that start at {@code y == 0}
     * (the top strip of the content layout). A child that spans the full
     * content height (a wrapper panel filling the whole pane) is not a
     * strip - it would push the overlay to the window's bottom - so that
     * degenerate case falls back to no strip at all, as does a content pane
     * without any laid-out top children (e.g. before the first layout).
     * <p>
     * The overlay lives in the layered pane, which shares its coordinate
     * space with the root pane (not the content pane): a menu bar is laid
     * out ABOVE the content pane inside the layered pane, so its measured
     * height is added on top.
     */
    private int topBarHeight() {
        Container contentPane = rootPane.getContentPane();
        if (contentPane == null) {
            return 0;
        }
        int stripBottom = 0;
        boolean fullPaneChild = false;
        for (Component child : contentPane.getComponents()) {
            if (child.getY() != 0) {
                continue;
            }
            stripBottom = Math.max(stripBottom, child.getY() + child.getHeight());
            if (child.getHeight() >= contentPane.getHeight() && contentPane.getHeight() > 0) {
                fullPaneChild = true;
            }
        }
        if (fullPaneChild || stripBottom <= 0) {
            return 0;
        }
        JMenuBar menuBar = rootPane.getJMenuBar();
        int menuBarHeight = menuBar != null ? menuBar.getHeight() : 0;
        return menuBarHeight + stripBottom;
    }

    // ------------------------------------------------------------------
    // Search field behavior and key bindings
    // ------------------------------------------------------------------

    /**
     * Wires the search field: typed text re-runs the list build (the query
     * filter), and any focus loss closes the bar (with dispatch suspended,
     * an unnoticed open state would silently kill every shortcut; see the
     * class javadoc).
     */
    private void installSearchFieldBehavior() {
        searchField.getTextField().getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (isVisible() && !chordMode) {
                    rebuildList();
                }
            }
        });
        searchField.getTextField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (isVisible()) {
                    close();
                }
            }
        });
    }

    /**
     * Installs the normal-mode key handling on the search field
     * (WHEN_FOCUSED): up/down move the selection, Enter fires, Escape
     * closes, and the trigger variants close the bar (a toggle).
     */
    private void installKeyBindings() {
        InputMap inputMap = searchField.getTextField().getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = searchField.getTextField().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("pressed UP"), ACTION_KEY_UP);
        inputMap.put(KeyStroke.getKeyStroke("pressed DOWN"), ACTION_KEY_DOWN);
        inputMap.put(KeyStroke.getKeyStroke("pressed ENTER"), ACTION_KEY_FIRE);
        inputMap.put(KeyStroke.getKeyStroke("pressed ESCAPE"), ACTION_KEY_ESCAPE);
        actionMap.put(ACTION_KEY_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelection(-1);
            }
        });
        actionMap.put(ACTION_KEY_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelection(1);
            }
        });
        actionMap.put(ACTION_KEY_FIRE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireSelected();
            }
        });
        actionMap.put(ACTION_KEY_ESCAPE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        actionMap.put(ACTION_KEY_TRIGGER, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // The trigger is a toggle: pressing it while the bar is open
                // closes it.
                close();
            }
        });
    }

    /**
     * Refreshes the trigger keystroke bindings from the current keymap
     * (resolved on every rebuild, so a rebind works without reinstalling).
     * Only length-1 variants can be typed into the focused search field; a
     * chord trigger reaches the bar through the dispatcher instead.
     */
    private void updateTriggerBindings() {
        InputMap inputMap = searchField.getTextField().getInputMap(JComponent.WHEN_FOCUSED);
        for (KeyStroke stroke : installedTriggerStrokes) {
            inputMap.remove(stroke);
        }
        installedTriggerStrokes.clear();
        List<List<KeyStroke>> variants = JIPipeShortcutRegistry.getInstance()
                .bindingFor(JIPipeShortcutActions.COMMAND_BAR_OPEN.id(), JIPipeShortcutSettings.current())
                .variants();
        for (List<KeyStroke> variant : variants) {
            if (variant.size() == 1) {
                KeyStroke stroke = variant.get(0);
                inputMap.put(stroke, ACTION_KEY_TRIGGER);
                installedTriggerStrokes.add(stroke);
            }
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /**
     * The list renderer: category header rows (bold, muted foreground) and
     * entry rows (name left with the row icon, the binding as VS Code-style
     * key chips right in gray - one chip per stroke, chord strokes separated
     * by a wider gap, variants stacked vertically - and the configure
     * affordance on the selected available configurable row; the chips end
     * before the always-reserved configure strip). Unavailable rows are
     * fully muted. Row heights come from the renderer's own preferred sizes
     * (no fixed cell height, no assumed text sizes); the reused row panels
     * are force-revalidated after every configure so the freshly set texts
     * are measured synchronously (the
     * {@code JIPipeDesktopFindPanel.FindTreeCellRenderer} pattern).
     */
    private final class EntryCellRenderer implements ListCellRenderer<JIPipeDesktopCommandBarModel.Entry> {
        private final JPanel headerPanel = new JPanel(new BorderLayout());
        private final JLabel headerLabel = new JLabel();
        private final JPanel entryPanel = new JPanel(new BorderLayout());
        private final JPanel entryEndPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        private final JPanel bindingPanel = new JPanel();
        private final JPanel configureSlot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        private final JLabel nameLabel = new JLabel();
        private final JButton configureButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit.png"));

        EntryCellRenderer() {
            headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
            headerPanel.setOpaque(true);
            headerPanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
            headerPanel.add(headerLabel, BorderLayout.CENTER);

            configureButton.setToolTipText("Change the key binding of this command");
            configureButton.setFocusable(false);
            configureButton.setFocusPainted(false);
            configureButton.setContentAreaFilled(false);
            UIUtils.makeButtonFlat25x25(configureButton);

            // The configure affordance reserves its area in EVERY row (not
            // only while the button is visible): the binding chips end
            // before the strip begins, so a plain click on a chip fires the
            // command, while a click into the strip configures it.
            configureSlot.setOpaque(false);
            configureSlot.setPreferredSize(new Dimension(CONFIGURE_BUTTON_SIZE, CONFIGURE_BUTTON_SIZE));

            nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 0));
            bindingPanel.setOpaque(false);
            bindingPanel.setLayout(new BoxLayout(bindingPanel, BoxLayout.Y_AXIS));
            entryEndPanel.setOpaque(false);
            entryEndPanel.add(bindingPanel);
            entryEndPanel.add(configureSlot);
            entryPanel.setOpaque(true);
            entryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, ROW_RIGHT_PADDING));
            entryPanel.add(nameLabel, BorderLayout.CENTER);
            entryPanel.add(entryEndPanel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeDesktopCommandBarModel.Entry> list,
                                                      JIPipeDesktopCommandBarModel.Entry value, int index,
                                                      boolean selected, boolean cellHasFocus) {
            if (value.isCategoryHeader()) {
                applyRowColors(headerPanel, selected);
                headerLabel.setText(value.name());
                headerLabel.setForeground(selected
                        ? headerPanel.getForeground()
                        : UIManager.getColor("Label.disabledForeground"));
                headerPanel.invalidate();
                headerPanel.validate();
                return headerPanel;
            }
            applyRowColors(entryPanel, selected);
            Color foreground = entryPanel.getForeground();
            Color mutedForeground = selected
                    ? foreground
                    : UIManager.getColor("Label.disabledForeground");
            nameLabel.setText(value.name());
            nameLabel.setIcon(value.icon());
            nameLabel.setForeground(value.isAvailable() ? foreground : mutedForeground);
            rebuildBindingChips(value, mutedForeground);
            configureButton.setVisible(selected && value.isConfigurable()
                    && value.isAvailable() && value.actionId() != null);
            entryPanel.invalidate();
            entryPanel.validate();
            return entryPanel;
        }

        /**
         * Rebuilds the binding chips of the row: one row of per-key chips
         * per binding variant (the strokes of a chord separated by the
         * wider chord gap, variants stacked vertically), right-aligned
         * where the plain binding label used to sit. Chip sizes come from
         * the labels' own preferred sizes (the key text is measured); the
         * chips end before the reserved configure strip, so a chip click
         * always fires instead of configuring. Unavailable rows render
         * their chips in the muted (disabled) foreground color.
         */
        private void rebuildBindingChips(JIPipeDesktopCommandBarModel.Entry value, Color textColor) {
            bindingPanel.removeAll();
            // The muted color comes from the LaF; a LaF without the key would
            // crash the chip borders at paint time, so fall back to the row
            // foreground (never null).
            Color chipColor = textColor != null ? textColor : entryPanel.getForeground();
            for (List<KeyStroke> variant : value.variants()) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, CHORD_STROKE_GAP, 0));
                row.setOpaque(false);
                row.setAlignmentX(Component.RIGHT_ALIGNMENT);
                row.setBorder(BorderFactory.createEmptyBorder(VARIANT_ROW_GAP, 0, 0, 0));
                for (KeyStroke stroke : variant) {
                    JLabel chip = new JLabel(JIPipeShortcutRegistry.formatKeyStroke(stroke));
                    chip.setForeground(chipColor);
                    chip.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(chipColor),
                            BorderFactory.createEmptyBorder(CHIP_PADDING_V, CHIP_PADDING_H,
                                    CHIP_PADDING_V, CHIP_PADDING_H)));
                    row.add(chip);
                }
                bindingPanel.add(row);
            }
        }

        private void applyRowColors(JPanel panel, boolean selected) {
            if (selected) {
                panel.setBackground(JIPipeDesktopCommandBar.this.list.getSelectionBackground());
                panel.setForeground(JIPipeDesktopCommandBar.this.list.getSelectionForeground());
            } else {
                panel.setBackground(JIPipeDesktopCommandBar.this.list.getBackground());
                panel.setForeground(JIPipeDesktopCommandBar.this.list.getForeground());
            }
        }
    }
}
