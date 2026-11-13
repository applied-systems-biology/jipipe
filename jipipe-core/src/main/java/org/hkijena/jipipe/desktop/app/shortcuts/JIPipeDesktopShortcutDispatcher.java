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

package org.hkijena.jipipe.desktop.app.shortcuts;

import org.hkijena.jipipe.api.shortcuts.JIPipeChordStateMachine;
import org.hkijena.jipipe.api.shortcuts.JIPipeChordStateMachine.EventResult;
import org.hkijena.jipipe.api.shortcuts.JIPipeChordStateMachine.KeyEventResult;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutTextGuard;
import org.hkijena.jipipe.api.shortcuts.KeymapSettings;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The application-wide keyboard shortcut dispatcher.
 * <p>
 * A single {@link KeyEventDispatcher} installed on the current
 * {@link KeyboardFocusManager} turns every {@code KEY_PRESSED} event into an
 * action id: length-1 bindings are looked up directly in
 * {@link #singleStrokeMap}; length-2 bindings go through the
 * {@link JIPipeChordStateMachine} (the prefix press reports
 * {@link EventResult#PREFIX_STARTED}, the second press
 * {@link EventResult#CHORD_COMPLETED} with the full chord). A hit is then
 * resolved against the registered {@link ShortcutScope}s that contain the
 * current focus owner, innermost first ({@link #orderedContainingScopes}),
 * and the first scope whose {@link ShortcutScope.Resolver} returns a
 * non-null runnable fires it on the EDT.
 * <p>
 * The stroke maps and the chord machine are rebuilt from the registry
 * whenever the keymap settings or the action registry change
 * ({@link #rebuildStrokeMaps()}). Rebuilding is guarded against re-entry:
 * the settings and registry change listeners fire synchronously on the
 * calling thread, and a listener may mutate the very thing it observes.
 * <p>
 * Scopes register and unregister themselves (Tasks 6, 8 and 10 install the
 * graph editor, dock panel, and project window scopes); the dispatcher
 * never owns a scope, and on window close or dispose the scope's host calls
 * {@link #unregisterScope}.
 */
public final class JIPipeDesktopShortcutDispatcher implements KeyEventDispatcher {
    private static volatile JIPipeDesktopShortcutDispatcher INSTANCE;

    /**
     * The sink for status bar text. The workbench of the focused window
     * registers itself here (Task 6 onward); until a sink is installed,
     * status text is dropped silently, keeping the dispatcher free of
     * workbench imports.
     */
    private static volatile Consumer<String> statusTextSink = text -> {
    };

    /**
     * Single-stroke (length-1 variant) bindings, stroke to action id.
     * A volatile snapshot: rebuilt as a fresh map and published atomically,
     * so the EDT always reads a consistent map even when a settings change
     * arrives on a background thread.
     */
    private volatile Map<KeyStroke, String> singleStrokeMap = Map.of();
    /**
     * Chord (length-2 variant) bindings, chord to action id. Published as an
     * immutable snapshot like {@link #singleStrokeMap}.
     */
    private volatile Map<List<KeyStroke>, String> chordMap = Map.of();
    /**
     * The pending-chord state machine; driven only from the EDT.
     */
    private final JIPipeChordStateMachine chordMachine = new JIPipeChordStateMachine();
    /**
     * The registered scopes. CopyOnWrite because scopes are registered and
     * unregistered from the EDT (dispose paths) while the same list is read
     * on the EDT during dispatch.
     */
    private final List<ShortcutScope> scopes = new CopyOnWriteArrayList<>();
    /**
     * Guards {@link #rebuildStrokeMaps()} against re-entry. Only meaningful
     * on the thread that performs the actual rebuild (the EDT).
     */
    private boolean rebuilding;
    /**
     * The focus manager the dispatcher is attached to. Used to detect a
     * focus manager replacement (a new AWT event queue) so that
     * {@link #install()} re-attaches. {@link KeyboardFocusManager} does not
     * expose its dispatcher list publicly, so the attachment state is
     * tracked here.
     */
    private volatile KeyboardFocusManager installedOn;

    private JIPipeDesktopShortcutDispatcher() {
        // The settings and registry change listeners fire synchronously on
        // the thread that caused the change: a settings file watcher fires
        // on a background thread, while the UI fires on the EDT. The
        // subscriber marshals the rebuild to the EDT in that case; the
        // machine and the focus manager may only be touched from the EDT.
        JIPipeShortcutSettings.binding().subscribe(settings -> onRebuildRequested());
        JIPipeShortcutRegistry.getInstance().addChangeListener(this::onRebuildRequested);
        if (SwingUtilities.isEventDispatchThread()) {
            rebuildStrokeMaps();
        } else {
            SwingUtilities.invokeLater(this::rebuildStrokeMaps);
        }
    }

    /**
     * The change-listener callback: rebuilds the stroke maps, marshaling to
     * the EDT when the change arrives from a background thread.
     */
    private void onRebuildRequested() {
        if (SwingUtilities.isEventDispatchThread()) {
            rebuildStrokeMaps();
        } else {
            SwingUtilities.invokeLater(this::rebuildStrokeMaps);
        }
    }

    /**
     * @return the application-wide dispatcher singleton, created on first use
     */
    public static JIPipeDesktopShortcutDispatcher getInstance() {
        JIPipeDesktopShortcutDispatcher instance = INSTANCE;
        if (instance == null) {
            synchronized (JIPipeDesktopShortcutDispatcher.class) {
                instance = INSTANCE;
                if (instance == null) {
                    instance = new JIPipeDesktopShortcutDispatcher();
                    INSTANCE = instance;
                }
            }
        }
        return instance;
    }

    /**
     * Installs the dispatcher on the current keyboard focus manager.
     * <p>
     * Idempotent: a second call with the dispatcher already attached is a
     * no-op.
     */
    public static synchronized void install() {
        JIPipeDesktopShortcutDispatcher dispatcher = getInstance();
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (dispatcher.installedOn == kfm) {
            return;
        }
        if (dispatcher.installedOn != null) {
            // A focus manager replacement (a new AWT event queue): detach
            // from the old one and re-attach to the new one.
            dispatcher.installedOn.removeKeyEventDispatcher(dispatcher);
        }
        kfm.addKeyEventDispatcher(dispatcher);
        dispatcher.installedOn = kfm;
    }

    /**
     * Adds a scope to the dispatch.
     *
     * @param scope the scope to register
     * @throws IllegalStateException when a scope with the same root is already registered
     */
    public void registerScope(ShortcutScope scope) {
        Objects.requireNonNull(scope, "scope");
        if (scopes.stream().anyMatch(s -> s.root() == scope.root())) {
            throw new IllegalStateException(
                    "A scope for the component " + scope.root() + " is already registered");
        }
        scopes.add(scope);
    }

    /**
     * Removes a scope from the dispatch. A scope that is not registered is
     * ignored, so dispose paths may call this unconditionally.
     *
     * @param scope the scope to unregister
     */
    public void unregisterScope(ShortcutScope scope) {
        if (scope != null) {
            scopes.remove(scope);
        }
    }

    /**
     * Rebuilds the stroke maps from the registry and reconfigures the chord
     * machine with the current timeout and prefixes.
     * <p>
     * Called automatically (on the EDT) by the settings and registry change
     * listeners, and may be called directly (e.g. after scope registration
     * when the registering feature also registered new actions). The maps
     * are published as fresh immutable snapshots, so a concurrent reader on
     * the EDT always sees a consistent state.
     * <p>
     * Must run on the EDT: it reconfigures the chord machine, which is not
     * thread-safe.
     */
    public void rebuildStrokeMaps() {
        if (rebuilding) {
            return;
        }
        rebuilding = true;
        try {
            JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
            KeymapSettings settings = JIPipeShortcutSettings.current();

            Map<KeyStroke, String> single = new HashMap<>();
            Map<List<KeyStroke>, String> chords = new LinkedHashMap<>();
            for (JIPipeShortcutAction action : registry.all()) {
                for (List<KeyStroke> variant : registry.bindingFor(action.id(), settings).variants()) {
                    if (variant.size() == 1) {
                        single.put(variant.get(0), action.id());
                    } else if (variant.size() == 2) {
                        chords.put(variant, action.id());
                    }
                }
            }
            // Publish the chord map first, then the single-stroke map, and
            // only then the machine configuration: a reader that sees the
            // new single-stroke map already sees every chord the new
            // prefixes could lead to.
            this.chordMap = Map.copyOf(chords);
            this.singleStrokeMap = Map.copyOf(single);

            long timeoutNanos = settings.getChordTimeoutMs() * 1_000_000L;
            Set<List<KeyStroke>> prefixes = registry.chordPrefixes(settings);
            // configure() also cancels any pending chord.
            chordMachine.configure(timeoutNanos, prefixes);
        } finally {
            rebuilding = false;
        }
    }

    /**
     * The global event dispatcher entry point.
     * <p>
     * Only {@code KEY_PRESSED} is considered. The stroke is normalized with
     * {@link KeyStroke#getKeyStrokeForEvent(KeyEvent)} and fed to the chord
     * machine first; depending on the machine's verdict, the event either
     * falls through (single-stroke lookup with no hit), is consumed by a
     * pending chord state, or resolves an action against the scopes.
     *
     * @param e the key event
     * @return true when the event was consumed by a shortcut (or a chord
     * state), false when it must be passed on normally
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
        if (stroke == null) {
            return false;
        }
        KeyEventResult result = chordMachine.keyPressed(stroke, System.nanoTime());
        Component focusOwner = focusOwnerOrNull();
        switch (result.result()) {
            case PASSTHROUGH: {
                String actionId = singleStrokeMap.get(stroke);
                if (actionId == null) {
                    return false;
                }
                return fireResolvedAction(actionId, focusOwner, false);
            }
            case PREFIX_STARTED: {
                List<KeyStroke> prefix = chordMachine.pendingPrefix();
                setStatusText("(" + JIPipeShortcutRegistry.formatChord(prefix)
                        + ") was pressed, waiting for second key of chord");
                return true;
            }
            case CHORD_COMPLETED: {
                String actionId = chordMap.get(result.completedChord());
                if (actionId == null) {
                    // The chord is over and the second key is already
                    // swallowed: report and consume.
                    setStatusText("Chord cancelled");
                    return true;
                }
                return fireResolvedAction(actionId, focusOwner, true);
            }
            case CANCELLED: {
                setStatusText(null);
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Resolves a hit action id against the scopes containing the focus
     * owner (innermost first), applies the text-input guard, and fires the
     * first resolvable scope.
     *
     * @param actionId the resolved action id
     * @param focusOwner the current focus owner, may be null
     * @param keyAlreadyConsumed whether the chord state machine has already
     * swallowed the key (a {@code CHORD_COMPLETED} result): then the event
     * is consumed either way; a {@code PASSTHROUGH} hit must not swallow the
     * key when nothing could be resolved (the user types it normally)
     * @return true when the event must be consumed (action fired, or the key
     * was already swallowed), false to pass the key on
     */
    private boolean fireResolvedAction(String actionId, Component focusOwner, boolean keyAlreadyConsumed) {
        JIPipeShortcutAction action = JIPipeShortcutRegistry.getInstance().byId(actionId);
        if (action != null && JIPipeShortcutTextGuard.isTextInput(focusOwner)
                && !action.isTextInputSafe()) {
            // Not resolvable in a text field: the key is swallowed when the
            // chord state machine already consumed it (second key of a
            // chord), otherwise it is passed on (the single stroke is typed
            // normally).
            return keyAlreadyConsumed;
        }
        for (ShortcutScope scope : orderedContainingScopes(scopes, focusOwner)) {
            Runnable runnable = scope.resolver().resolve(actionId);
            if (runnable != null) {
                SwingUtilities.invokeLater(runnable);
                String name = action == null ? actionId : action.name();
                setStatusText("Executed: " + name);
                return true;
            }
        }
        // No scope could resolve the action: same consume semantics as the
        // text-input guard above.
        return keyAlreadyConsumed;
    }

    /**
     * Orders the given scopes by how deeply their roots contain the focus
     * owner: the innermost scope (deepest root) first. Scopes that do not
     * contain the focus owner are excluded.
     * <p>
     * Extracted as a static helper so the ordering is testable without
     * installing the global dispatcher. Multiple scopes may contain the
     * focus owner at once (e.g. two dock panels in one window); the first
     * scope whose {@link ShortcutScope.Resolver} returns a non-null runnable
     * wins.
     *
     * @param scopes     all registered scopes
     * @param focusOwner the component holding the keyboard focus
     * @return the scopes containing the focus owner, innermost first
     */
    static List<ShortcutScope> orderedContainingScopes(List<ShortcutScope> scopes, Component focusOwner) {
        if (focusOwner == null) {
            return List.of();
        }
        List<ShortcutScope> containing = new ArrayList<>();
        for (ShortcutScope scope : scopes) {
            if (scope.contains(focusOwner)) {
                containing.add(scope);
            }
        }
        containing.sort(Comparator.comparingInt(
                (ShortcutScope s) -> depthOf(s.root())).reversed());
        return containing;
    }

    /**
     * @return the current keyboard focus owner, or null when the AWT toolkit
     * has no focus owner (headless or no windows)
     */
    private static Component focusOwnerOrNull() {
        try {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * The depth of a component in the AWT parent chain (root = 1).
     */
    private static int depthOf(Component c) {
        int depth = 0;
        for (Component p = c; p != null; p = p.getParent()) {
            depth++;
        }
        return depth;
    }

    /**
     * Emits status bar text through the registered sink, if any.
     *
     * @param text the text, or null to clear
     */
    static void setStatusText(String text) {
        Consumer<String> sink = statusTextSink;
        if (sink != null) {
            try {
                sink.accept(text);
            } catch (Exception e) {
                // A failing status sink must not break dispatch.
            }
        }
    }

    /**
     * Registers the sink for status bar text. The workbench of the focused
     * window installs this (Task 6 onward); pass null to remove the sink.
     *
     * @param sink receives the status text, or null to clear it
     */
    public static void setStatusTextSink(Consumer<String> sink) {
        statusTextSink = sink;
    }
}
