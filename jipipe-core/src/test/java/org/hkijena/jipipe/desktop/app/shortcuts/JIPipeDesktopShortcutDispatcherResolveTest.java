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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the public resolution helpers of
 * {@link JIPipeDesktopShortcutDispatcher} that the command bar reuses:
 * {@link JIPipeDesktopShortcutDispatcher#scopesContaining} (innermost-first
 * scope ordering for a focus owner) and {@link
 * JIPipeDesktopShortcutDispatcher#resolveAction} (one action id against the
 * containing scopes, with the text-input guard per the action).
 * <p>
 * No dispatcher installation, no key events, headless-safe. The actions must
 * be registered (the guard looks the action up by id) and the resolvers are
 * stubs that return a well-known runnable.
 */
class JIPipeDesktopShortcutDispatcherResolveTest {
    private static final String SAFE_ID = "org.hkijena.jipipe.test.resolve.safe";
    private static final String UNSAFE_ID = "org.hkijena.jipipe.test.resolve.unsafe";

    private static final Runnable safeRun = () -> {
    };
    private static final Runnable unsafeRun = () -> {
    };

    @BeforeEach
    void setUp() {
        // The settings store must exist before the dispatcher constructor
        // runs (it subscribes to the live keymap binding), mirroring the
        // isolation protocol of JIPipeDesktopShortcutDispatcherMapTest.
        JIPipe.ensureInstance();
        JIPipeShortcutSettings.current();
        JIPipeShortcutRegistry.resetInstance();
        JIPipeShortcutRegistry.getInstance().register(
                JIPipeShortcutAction.builder(SAFE_ID, "Safe action", "Test resolve")
                        .textInputSafe(true)
                        .build());
        JIPipeShortcutRegistry.getInstance().register(
                JIPipeShortcutAction.builder(UNSAFE_ID, "Unsafe action", "Test resolve")
                        .build());
        JIPipeDesktopShortcutDispatcher.resetInstance();
    }

    @AfterEach
    void tearDown() {
        JIPipeDesktopShortcutDispatcher.resetInstance();
        JIPipeShortcutRegistry.resetInstance();
    }

    /**
     * Adds the given component as a child of {@code inner}, which is the
     * center of {@code outer}, and returns the component.
     */
    private static <T extends Component> T nestedIn(JPanel outer, JPanel inner, T focus) {
        outer.add(inner, BorderLayout.CENTER);
        inner.add(focus);
        return focus;
    }

    @Test void scopesContainingReturnsInnermostFirst() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel();
        JTextField focus = nestedIn(outerPanel, innerPanel, new JTextField());

        ShortcutScope outer = new ShortcutScope(outerPanel, id -> null);
        ShortcutScope inner = new ShortcutScope(innerPanel, id -> null);
        JIPipeDesktopShortcutDispatcher dispatcher = JIPipeDesktopShortcutDispatcher.getInstance();
        dispatcher.registerScope(outer);
        dispatcher.registerScope(inner);

        List<ShortcutScope> ordered = dispatcher.scopesContaining(focus);
        assertEquals(List.of(inner, outer), ordered,
                "the scopes containing the focus owner, innermost first");

        // A sibling scope that does not contain the focus owner is excluded.
        JPanel sibling = new JPanel();
        outerPanel.add(sibling, BorderLayout.WEST);
        dispatcher.registerScope(new ShortcutScope(sibling, id -> null));
        assertEquals(2, dispatcher.scopesContaining(focus).size());

        // No focus owner: no containing scopes.
        assertEquals(0, dispatcher.scopesContaining(null).size());
    }

    @Test void resolveActionFallsThroughToOuterScope() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel();
        JPanel focus = nestedIn(outerPanel, innerPanel, new JPanel());

        JIPipeDesktopShortcutDispatcher dispatcher = JIPipeDesktopShortcutDispatcher.getInstance();
        // The innermost scope does not answer for the id; the outer one does.
        dispatcher.registerScope(new ShortcutScope(innerPanel, id -> null));
        dispatcher.registerScope(new ShortcutScope(outerPanel, id -> SAFE_ID.equals(id) ? safeRun : null));

        assertSame(safeRun, dispatcher.resolveAction(SAFE_ID, focus),
                "a candidate unknown to the innermost scope falls through to the outer scope");
    }

    @Test void resolveActionInnermostScopeWins() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel();
        JPanel focus = nestedIn(outerPanel, innerPanel, new JPanel());

        JIPipeDesktopShortcutDispatcher dispatcher = JIPipeDesktopShortcutDispatcher.getInstance();
        dispatcher.registerScope(new ShortcutScope(outerPanel, id -> SAFE_ID.equals(id) ? () -> {
        } : null));
        dispatcher.registerScope(new ShortcutScope(innerPanel, id -> SAFE_ID.equals(id) ? safeRun : null));

        assertSame(safeRun, dispatcher.resolveAction(SAFE_ID, focus),
                "the innermost scope that answers for the id wins");
    }

    @Test void resolveActionAppliesTextGuardPerAction() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField focus = new JTextField();
        panel.add(focus, BorderLayout.CENTER);

        // A second scope root whose content is not a text input: the guard
        // must be off there (the panel that hosts the text field must not
        // also be the root of the plain-focus scope, or its text field
        // would be found one level down).
        JPanel plainPanel = new JPanel(new BorderLayout());
        JPanel plainFocus = new JPanel();
        plainPanel.add(plainFocus, BorderLayout.CENTER);

        JIPipeDesktopShortcutDispatcher dispatcher = JIPipeDesktopShortcutDispatcher.getInstance();
        dispatcher.registerScope(new ShortcutScope(panel,
                id -> SAFE_ID.equals(id) ? safeRun : UNSAFE_ID.equals(id) ? unsafeRun : null));
        dispatcher.registerScope(new ShortcutScope(plainPanel,
                id -> SAFE_ID.equals(id) ? safeRun : UNSAFE_ID.equals(id) ? unsafeRun : null));

        // Outside a text input both actions resolve.
        assertSame(safeRun, dispatcher.resolveAction(SAFE_ID, plainFocus));
        assertSame(unsafeRun, dispatcher.resolveAction(UNSAFE_ID, plainFocus),
                "the guard only applies while a text input has the focus");

        // Inside a text input only the text-input-safe action resolves.
        assertSame(safeRun, dispatcher.resolveAction(SAFE_ID, focus),
                "a text-input-safe action resolves in a text field");
        assertNull(dispatcher.resolveAction(UNSAFE_ID, focus),
                "a not-text-input-safe action does not resolve in a text field");
    }

    @Test void resolveActionReturnsNullWhenNothingResolves() {
        JPanel panel = new JPanel();
        JIPipeDesktopShortcutDispatcher dispatcher = JIPipeDesktopShortcutDispatcher.getInstance();
        dispatcher.registerScope(new ShortcutScope(panel, id -> null));

        assertNull(dispatcher.resolveAction(SAFE_ID, panel), "no scope answers");
        assertNull(dispatcher.resolveAction("unknown-id", panel),
                "an unknown id (no guard, no resolver hit) resolves to null");
        assertNull(dispatcher.resolveAction(SAFE_ID, null), "no focus owner: no scopes, no result");
    }
}
