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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The application-wide keyboard shortcut dispatcher.
 * <p>
 * A single {@link KeyEventDispatcher} installed on the current
 * {@link KeyboardFocusManager} turns every {@code KEY_PRESSED} event into an
 * ordered list of candidate action ids: length-1 bindings are looked up
 * directly in {@link #singleStrokeMap}; length-2 bindings go through the
 * {@link JIPipeChordStateMachine} (the prefix press reports
 * {@link EventResult#PREFIX_STARTED}, the second press
 * {@link EventResult#CHORD_COMPLETED} with the full chord). The catalog
 * intentionally lets different actions share a stroke when their
 * availability is disjoint (e.g. DELETE removes nodes or, when only control
 * points are selected, control points), so a hit yields one candidate per
 * bound action, in registry order.
 * The candidates are tried in registry order against the registered
 * {@link ShortcutScope}s that contain the current focus owner, innermost
 * first ({@link #orderedContainingScopes}); the first candidate for which
 * the first scope whose {@link ShortcutScope.Resolver} returns a non-null
 * runnable is found fires that runnable on the EDT.
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
     * The chord-prefix handler. The per-window command bar installs this
     * (Task 4): when a chord prefix is pressed, the handler receives the
     * pending prefix stroke and decides whether the chord is consumed by it
     * (true) or the normal dispatch continues (false). The
     * {@code PREFIX_STARTED} branch consults it (Task 5 wires the call);
     * until a handler is installed, the field is {@code null} and chord
     * prefixes behave exactly as before. Like {@link #statusTextSink}, this
     * indirection keeps the dispatcher free of command bar imports.
     */
    private static volatile Predicate<KeyStroke> chordPrefixHandler;

    /**
     * Single-stroke (length-1 variant) bindings, stroke to the ordered list
     * of candidate action ids. Several actions may share one stroke (e.g.
     * the node and the edge delete on DELETE), so the list holds every id
     * bound to the stroke in registry order; the fire path tries them in
     * that order. A volatile snapshot: rebuilt as a fresh map and published
     * atomically, so the EDT always reads a consistent map even when a
     * settings change arrives on a background thread. Package-private so
     * tests can read the published snapshot.
     */
    volatile Map<KeyStroke, List<String>> singleStrokeMap = Map.of();
    /**
     * Chord (length-2 variant) bindings, chord to the ordered list of
     * candidate action ids. Same candidate semantics as
     * {@link #singleStrokeMap}; published as an immutable snapshot like it.
     * Package-private so tests can read the published snapshot.
     */
    volatile Map<List<KeyStroke>, List<String>> chordMap = Map.of();
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
     * Whether the keyboard-shortcut capture dialog is recording (Task 9).
     * While capture is active, {@link #dispatchKeyEvent(KeyEvent)} passes
     * every event through unmodified, so the chord being recorded can be
     * pressed without triggering the shortcut currently bound to those
     * strokes (the recorded chord often <em>is</em> an existing binding, e.g.
     * re-assigning "Move nodes" to its own default). Set by the capture
     * dialog on the EDT for the lifetime of the modal dialog. Volatile: the
     * dialog and the event queue are the same thread in practice, but the
     * flag must be visible to every reader regardless.
     */
    private volatile boolean captureActive;
    /**
     * The focus manager the dispatcher is attached to. Used to detect a
     * focus manager replacement (a new AWT event queue) so that
     * {@link #install()} re-attaches. {@link KeyboardFocusManager} does not
     * expose its dispatcher list publicly, so the attachment state is
     * tracked here.
     */
    private volatile KeyboardFocusManager installedOn;

    /**
     * Clears the application-wide singleton so the next
     * {@link #getInstance()} call creates a fresh dispatcher.
     * <p>
     * Intended for tests that need an isolated dispatcher (and thus an
     * isolated settings/registry subscription); production code should
     * always use {@link #getInstance()}.
     */
    static void resetInstance() {
        INSTANCE = null;
    }

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
     * <p>
     * Several scopes may share one root (e.g. two dock panels in the same
     * window both install a scope rooted at the window's root pane; their
     * resolvers only answer for their own panels, and the innermost-first
     * walk with registration-order tie-break keeps the result deterministic).
     *
     * @param scope the scope to register
     * @throws IllegalStateException when the very same scope instance is
     *                               already registered (a wiring bug in the
     *                               scope's host lifecycle)
     */
    public void registerScope(ShortcutScope scope) {
        Objects.requireNonNull(scope, "scope");
        if (scopes.contains(scope)) {
            throw new IllegalStateException(
                    "The scope with root " + scope.root() + " is already registered");
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
     * Suspends (or resumes) the keyboard-shortcut dispatch while a capture
     * dialog records a new binding.
     * <p>
     * The recorder must see the raw key presses it records, even when the
     * pressed strokes already resolve to an existing shortcut (re-assigning
     * an action to its own default is the common case). While suspended,
     * {@link #dispatchKeyEvent(KeyEvent)} passes every event through, so no
     * shortcut fires and the chord state machine is not touched.
     *
     * @param active whether capture is active
     */
    public void setCaptureActive(boolean active) {
        this.captureActive = active;
    }

    /**
     * @return whether the keyboard-shortcut dispatch is suspended for a
     *         capture dialog (see {@link #setCaptureActive(boolean)})
     */
    public boolean isCaptureActive() {
        return captureActive;
    }

    /**
     * The scopes that contain the given focus owner, innermost first
     * (deepest scope root first, registration order as tie-break): the
     * same ordered list the key dispatch walks
     * (see {@link #dispatchKeyEvent(KeyEvent)}).
     * <p>
     * Exposed for consumers that must evaluate command availability like
     * the dispatch does (the command bar, design Section 4.1): a command is
     * resolvable when some scope's resolver returns a runnable.
     *
     * @param focusOwner the component to test, may be {@code null}
     *                   (descending scopes decline a null focus owner;
     *                   window-level scopes compare the currently focused
     *                   window, so they can contain even then)
     * @return the registered scopes containing the focus owner, innermost
     * first
     */
    public List<ShortcutScope> scopesContaining(Component focusOwner) {
        return orderedContainingScopes(scopes, focusOwner);
    }

    /**
     * Resolves one action id against the registered scopes containing the
     * given focus owner, innermost first, applying the text-input guard
     * per the action: when the focus owner is a text input and the action
     * is not {@link JIPipeShortcutAction#isTextInputSafe() text input safe},
     * the id resolves to nothing.
     * <p>
     * This is exactly the per-id work the fire path performs for each
     * candidate (see {@link #fireResolvedAction}); the command bar reuses
     * it to compute live availability (design Section 4.1) instead of
     * duplicating the walk. Note that the text guard does not apply
     * <em>inside</em> the command bar itself (dispatch is suspended while
     * the bar is open); the guard is part of the resolution because the
     * bar is opened with the trigger from a text field and the
     * availability of commands there must match what the dispatch would
     * fire.
     *
     * @param actionId   the action id to resolve, must not be {@code null}
     * @param focusOwner the component to test against the scopes, may be
     *                   {@code null} (descending scopes decline a null
     *                   focus owner; window-level scopes compare the
     *                   currently focused window)
     * @return the runnable the innermost answering scope produced, or
     * {@code null} when no scope answers or the text-input guard applies
     */
    public Runnable resolveAction(String actionId, Component focusOwner) {
        Objects.requireNonNull(actionId, "actionId");
        JIPipeShortcutAction action = JIPipeShortcutRegistry.getInstance().byId(actionId);
        if (action != null && JIPipeShortcutTextGuard.isTextInput(focusOwner)
                && !action.isTextInputSafe()) {
            // Not resolvable in a text field (same guard the fire path
            // applies per candidate).
            return null;
        }
        for (ShortcutScope scope : orderedContainingScopes(scopes, focusOwner)) {
            Runnable runnable = scope.resolver().resolve(actionId);
            if (runnable != null) {
                return runnable;
            }
        }
        return null;
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

            // Registry order (category, then name) defines the candidate
            // order: for a stroke shared by several actions, the candidates
            // are tried in exactly this order.
            Map<KeyStroke, List<String>> single = new LinkedHashMap<>();
            Map<List<KeyStroke>, List<String>> chords = new LinkedHashMap<>();
            for (JIPipeShortcutAction action : registry.all()) {
                for (List<KeyStroke> variant : registry.bindingFor(action.id(), settings).variants()) {
                    if (variant.size() == 1) {
                        single.computeIfAbsent(variant.get(0), k -> new ArrayList<>()).add(action.id());
                    } else if (variant.size() == 2) {
                        chords.computeIfAbsent(variant, k -> new ArrayList<>()).add(action.id());
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
     * Only {@code KEY_PRESSED} is considered. Scope resolution comes first
     * (spec Section 6, step 2): when no registered scope contains the current
     * focus owner (e.g. inside any dialog), the stroke is not fed to the
     * chord machine at all - any pending chord is cancelled quietly and the
     * event passes through. Otherwise the stroke is normalized with
     * {@link KeyStroke#getKeyStrokeForEvent(KeyEvent)} and fed to the chord
     * machine; depending on the machine's verdict, the event either falls
     * through (single-stroke lookup with no hit), is consumed by a pending
     * chord state, or resolves an action against the scopes.
     *
     * @param e the key event
     * @return true when the event was consumed by a shortcut (or a chord
     * state), false when it must be passed on normally
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (captureActive) {
            // A capture dialog is recording a new binding: let every key
            // reach the dialog's recorder instead of resolving it as the
            // shortcut currently bound to those strokes.
            return false;
        }
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
        if (stroke == null) {
            return false;
        }
        Component focusOwner = focusOwnerOrNull();
        // Spec Section 6, step 2: scope resolution comes first. When no
        // registered scope contains the focus owner (e.g. inside any dialog),
        // the chord machine is not fed at all: a chord prefix must not start
        // (or stay) pending there, or the following keys of the dialog would
        // be swallowed. The machine is cancelled quietly (this also cancels a
        // chord left pending when the focus moved into the dialog) and the
        // key reaches the dialog untouched.
        // Window-level scopes (dock panels, command bar) also contain when
        // the focus owner is the window itself or null (the focused window
        // matches): a chord prefix pressed in such a transient state starts
        // a chord and can hand off to the focused window's command bar,
        // whose own focused-window check accepts it.
        List<ShortcutScope> containingScopes = orderedContainingScopes(scopes, focusOwner);
        if (containingScopes.isEmpty()) {
            chordMachine.cancel();
            return false;
        }
        KeyEventResult result = chordMachine.keyPressed(stroke, System.nanoTime());
        switch (result.result()) {
            case PASSTHROUGH: {
                List<String> candidateIds = singleStrokeMap.get(stroke);
                if (candidateIds == null || candidateIds.isEmpty()) {
                    return false;
                }
                return fireResolvedAction(candidateIds, focusOwner, false);
            }
            case PREFIX_STARTED: {
                // Hand the prefix to an installed chord handler (the command
                // bar) before falling back to the status hint. When the
                // handler consumes the prefix, the machine is cancelled and
                // dispatch is suspended; captureActive is set here as well so
                // a handler that forgets to suspend is still covered.
                Predicate<KeyStroke> handler = currentChordPrefixHandler();
                if (handler != null) {
                    KeyStroke prefixStroke = chordMachine.pendingPrefix().isEmpty()
                            ? stroke : chordMachine.pendingPrefix().get(0);
                    if (handler.test(prefixStroke)) {
                        chordMachine.cancel();
                        setCaptureActive(true);
                        // Clear any stale "(prefix) was pressed..." hint so
                        // it does not linger behind the opened bar.
                        setStatusText(null);
                        return true;
                    }
                }
                List<KeyStroke> prefix = chordMachine.pendingPrefix();
                setStatusText("(" + JIPipeShortcutRegistry.formatChord(prefix)
                        + ") was pressed, waiting for second key of chord");
                return true;
            }
            case CHORD_COMPLETED: {
                List<String> candidateIds = chordMap.get(result.completedChord());
                if (candidateIds == null || candidateIds.isEmpty()) {
                    // The chord is over and the second key is already
                    // swallowed: report and consume.
                    setStatusText("Chord cancelled");
                    return true;
                }
                return fireResolvedAction(candidateIds, focusOwner, true);
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
     * Tries the candidate action ids of one hit (in registry order) against
     * the scopes containing the focus owner (innermost first), applies the
     * text-input guard per candidate, and fires the first candidate that
     * resolves.
     * <p>
     * A shared stroke maps to several candidates (e.g. DELETE is bound to
     * both the node and the edge delete): the guard suppresses a candidate
     * that is not text-input-safe while typing, and a candidate none of the
     * scopes can resolve; both simply pass on to the next candidate. The
     * consume semantics of an overall miss are unchanged: the key is
     * swallowed only when the chord state machine already consumed it,
     * otherwise it is passed on (the user types the stroke normally).
     *
     * @param candidateIds the ordered candidate action ids of the hit
     * @param focusOwner the current focus owner, may be null
     * @param keyAlreadyConsumed whether the chord state machine has already
     * swallowed the key (a {@code CHORD_COMPLETED} result): then the event
     * is consumed either way; a {@code PASSTHROUGH} hit must not swallow the
     * key when nothing could be resolved (the user types it normally)
     * @return true when the event must be consumed (action fired, or the key
     * was already swallowed), false to pass the key on
     */
    private boolean fireResolvedAction(List<String> candidateIds, Component focusOwner,
                                       boolean keyAlreadyConsumed) {
        for (String candidateId : candidateIds) {
            // The per-id work (text-input guard per the action, then the
            // innermost-first scope walk) is shared with the command bar
            // through resolveAction; the guard and the walk are re-evaluated
            // identically to before (a scope that does not contain the
            // focus owner simply has no resolver to consult, so the walk
            // over the containing scopes computed by resolveAction agrees
            // with the one the caller used to compute).
            Runnable runnable = resolveAction(candidateId, focusOwner);
            if (runnable != null) {
                JIPipeShortcutAction action = JIPipeShortcutRegistry.getInstance().byId(candidateId);
                SwingUtilities.invokeLater(runnable);
                setStatusText("Executed: " + (action == null ? candidateId : action.name()));
                return true;
            }
            // A candidate the text-input guard skipped or that no scope
            // resolves is a miss: a later, text-input-safe candidate may
            // still fire.
        }
        // No candidate resolved: same consume semantics as the text-input
        // guard above.
        return keyAlreadyConsumed;
    }

    /**
     * Walks the ordered scopes (innermost first) and resolves every
     * candidate id in order against each scope, returning the first
     * candidate id whose resolver produced a non-null runnable, or
     * {@code null} when none resolves.
     * <p>
     * Extracted as a static helper so the candidate walk is testable without
     * installing the global dispatcher. The scope walk restarts for the
     * next candidate only when the current one resolved nowhere, so a
     * candidate unknown to the innermost scope still falls through to the
     * outer scopes, and the first resolvable candidate wins over later ones.
     *
     * @param candidateIds the ordered candidate action ids of one hit
     * @param orderedScopes the scopes containing the focus owner, innermost
     *                      first (e.g. the result of
     *                      {@link #orderedContainingScopes})
     * @return the first candidate id that resolves in some scope, or
     * {@code null}
     */
    static String resolveFirstAvailable(List<String> candidateIds, List<ShortcutScope> orderedScopes) {
        for (String candidateId : candidateIds) {
            for (ShortcutScope scope : orderedScopes) {
                if (scope.resolver().resolve(candidateId) != null) {
                    return candidateId;
                }
            }
        }
        return null;
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
     * @param focusOwner the component holding the keyboard focus, may be
     *                   {@code null} (descending scopes decline a null
     *                   focus owner via {@link ShortcutScope#contains};
     *                   window-level scopes compare the currently focused
     *                   window, so they can contain even then)
     * @return the scopes containing the focus owner, innermost first
     */
    static List<ShortcutScope> orderedContainingScopes(List<ShortcutScope> scopes, Component focusOwner) {
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

    /**
     * @return the currently installed status text sink (the default silent
     * no-op when none is installed), so an owner can remove it only when it
     * still owns it
     */
    public static Consumer<String> currentStatusTextSink() {
        return statusTextSink;
    }

    /**
     * Registers the chord-prefix handler. The per-window command bar class
     * installs a single routing hook here (Task 4): the first bar to install
     * itself puts that hook into the slot, and the hook routes the pending
     * prefix through every registered bar - the bar whose window owns the
     * keyboard focus accepts it and opens chord mode, the rest decline. The
     * hook is NOT owned per bar: the LAST bar to uninstall clears the slot
     * (identity check against the routing hook instance the bars installed,
     * only when no other bar remains), so uninstalling one of several bars
     * never drops chord mode for the others. Pass {@code null} to remove the
     * handler.
     * <p>
     * The dispatcher consults the hook in the {@code PREFIX_STARTED} branch:
     * when {@code handler.test(prefix)} returns {@code true}, the pending
     * chord is cancelled, the dispatch is suspended, and the key is
     * consumed; a {@code false} result falls through to the status hint and
     * the normal chord flow.
     *
     * @param handler receives the pending prefix stroke and returns whether
     *                it consumed the chord, or {@code null} to clear it
     */
    public static void setChordPrefixHandler(Predicate<KeyStroke> handler) {
        chordPrefixHandler = handler;
    }

    /**
     * @return the currently installed chord-prefix handler, or
     *         {@code null} when none is installed, so an owner can remove
     *         it only when it still owns it
     */
    public static Predicate<KeyStroke> currentChordPrefixHandler() {
        return chordPrefixHandler;
    }
}
