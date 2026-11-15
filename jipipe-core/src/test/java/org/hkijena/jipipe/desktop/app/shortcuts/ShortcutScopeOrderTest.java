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

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the pure dispatch logic of
 * {@link JIPipeDesktopShortcutDispatcher}: {@link
 * JIPipeDesktopShortcutDispatcher#orderedContainingScopes} (scope ordering)
 * and {@link JIPipeDesktopShortcutDispatcher#resolveFirstAvailable}
 * (candidate ids of a shared stroke).
 * <p>
 * No dispatcher installation, no key events, headless-safe. The resolvers
 * are stubs that return a well-known runnable (or null) so a walk can be
 * verified without firing anything.
 */
class ShortcutScopeOrderTest {
    /**
     * A stub resolver that returns the given runnable for the given action
     * id and null for everything else.
     */
    private static ShortcutScope.Resolver resolves(String actionId, Runnable r) {
        return id -> actionId.equals(id) ? r : null;
    }

    /**
     * Well-known runnables standing in for the two actions behind the
     * shared DELETE stroke (node delete / edge control-point delete).
     */
    private static final Runnable nodeRun = () -> {
    };
    private static final Runnable edgeRun = () -> {
    };

    @Test void innermostScopeWins() {
        // Outer panel P with resolver A, child panel C (in P) with resolver B;
        // scope list in outer-first order.
        JPanel p = new JPanel(new BorderLayout());
        JPanel c = new JPanel();
        p.add(c, BorderLayout.CENTER);
        JLabel focus = new JLabel("focus");
        c.add(focus);

        Runnable outerRun = () -> {
        };
        Runnable innerRun = () -> {
        };
        ShortcutScope outer = new ShortcutScope(p, resolves("shared", outerRun));
        ShortcutScope inner = new ShortcutScope(c, resolves("shared", innerRun));

        List<ShortcutScope> ordered =
                JIPipeDesktopShortcutDispatcher.orderedContainingScopes(List.of(outer, inner), focus);
        // Innermost-first: the scope whose root is deepest in the tree comes first.
        assertSame(inner, ordered.get(0));
        assertSame(outer, ordered.get(1));

        // First non-null resolve wins: walking the ordered scopes and taking
        // the first non-null runnable yields the inner scope's runnable.
        Runnable fired = null;
        for (ShortcutScope scope : ordered) {
            fired = scope.resolver().resolve("shared");
            if (fired != null) {
                break;
            }
        }
        assertSame(innerRun, fired);
    }

    @Test void actionUnknownToInnermostFallsThrough() {
        // The inner resolver knows nothing about the id (returns null): the
        // dispatcher must continue with the next containing scope, not stop.
        JPanel p = new JPanel(new BorderLayout());
        JPanel c = new JPanel();
        p.add(c, BorderLayout.CENTER);
        JLabel focus = new JLabel("focus");
        c.add(focus);

        Runnable outerRun = () -> {
        };
        ShortcutScope outer = new ShortcutScope(p, resolves("only-outer-knows-this", outerRun));
        ShortcutScope inner = new ShortcutScope(c, id -> null);

        List<ShortcutScope> ordered =
                JIPipeDesktopShortcutDispatcher.orderedContainingScopes(List.of(outer, inner), focus);
        assertSame(inner, ordered.get(0));
        assertSame(outer, ordered.get(1));
        assertNull(ordered.get(0).resolver().resolve("only-outer-knows-this"));
        assertSame(outerRun, ordered.get(1).resolver().resolve("only-outer-knows-this"));
    }

    @Test void scopeNotContainingFocusIsIgnored() {
        // A sibling panel scope is excluded even though it is registered.
        JPanel p = new JPanel(new BorderLayout());
        JPanel sibling = new JPanel();
        p.add(sibling, BorderLayout.WEST);
        JPanel c = new JPanel();
        p.add(c, BorderLayout.CENTER);
        JLabel focus = new JLabel("focus");
        c.add(focus);

        Runnable outerRun = () -> {
        };
        Runnable siblingRun = () -> {
        };
        ShortcutScope outer = new ShortcutScope(p, resolves("x", outerRun));
        ShortcutScope siblingScope = new ShortcutScope(sibling, resolves("x", siblingRun));
        ShortcutScope inner = new ShortcutScope(c, resolves("x", () -> {
        }));

        List<ShortcutScope> ordered =
                JIPipeDesktopShortcutDispatcher.orderedContainingScopes(List.of(outer, siblingScope, inner), focus);
        assertEquals(2, ordered.size());
        assertSame(inner, ordered.get(0));
        assertSame(outer, ordered.get(1));
    }

    @Test void focusOnScopeRootIsContained() {
        // The root itself counts as focus owner for its own scope.
        JPanel p = new JPanel();
        ShortcutScope scope = new ShortcutScope(p, resolves("self", () -> {
        }));
        List<ShortcutScope> ordered =
                JIPipeDesktopShortcutDispatcher.orderedContainingScopes(List.of(scope), p);
        assertSame(scope, ordered.get(0));
    }

    // ---------- shared-stroke candidate resolution ----------
    //
    // Several actions may share one stroke (e.g. DELETE is bound to both the
    // node delete and the edge control-point delete; their availability is
    // disjoint by selection). The dispatcher must try the candidate ids in
    // registry order and let the first one that resolves in some scope win.

    @Test void sharedStrokeTriesBothIdsInOrderFirstResolvableWins() {
        // Registry order: node delete first, edge delete second (same shape
        // as NODE_DELETE / EDGE_DELETE_CONTROL_POINT behind one DELETE stroke).
        String nodeId = "org.hkijena.jipipe.graph.node.delete";
        String edgeId = "org.hkijena.jipipe.graph.edge.delete-control-point";
        JPanel p = new JPanel();
        ShortcutScope scope = new ShortcutScope(p, id -> id.equals(nodeId) ? nodeRun : id.equals(edgeId) ? edgeRun : null);
        List<ShortcutScope> ordered = JIPipeDesktopShortcutDispatcher.orderedContainingScopes(List.of(scope), p);

        assertEquals(nodeId,
                JIPipeDesktopShortcutDispatcher.resolveFirstAvailable(List.of(nodeId, edgeId), ordered),
                "both resolve, the earlier registry-order candidate must win");
        assertEquals(edgeId,
                JIPipeDesktopShortcutDispatcher.resolveFirstAvailable(List.of(edgeId, nodeId), ordered),
                "the candidate order (not the action's) decides the winner");
    }

    @Test void sharedStrokeFirstCandidateDeadSecondResolves() {
        // The node delete is unavailable (a control point is selected, not a
        // node): its resolver returns null, the walk must continue and fire
        // the edge delete. This is the exact composition the single-id map
        // got wrong: only the last-registered id was even tried.
        String nodeId = "org.hkijena.jipipe.graph.node.delete";
        String edgeId = "org.hkijena.jipipe.graph.edge.delete-control-point";
        JPanel p = new JPanel();
        ShortcutScope scope = new ShortcutScope(p, id -> id.equals(edgeId) ? edgeRun : null);
        List<ShortcutScope> ordered = JIPipeDesktopShortcutDispatcher.orderedContainingScopes(List.of(scope), p);

        assertEquals(edgeId,
                JIPipeDesktopShortcutDispatcher.resolveFirstAvailable(List.of(nodeId, edgeId), ordered),
                "the first candidate that resolves must win, not the first in the list");
    }

    @Test void candidateUnknownToInnermostFallsThroughToOuter() {
        // The innermost scope (e.g. a dock panel) does not know either
        // candidate; the outer graph scope resolves the second one. The walk
        // restarts at the innermost scope per candidate.
        String nodeId = "org.hkijena.jipipe.graph.node.delete";
        String edgeId = "org.hkijena.jipipe.graph.edge.delete-control-point";
        JPanel outer = new JPanel(new BorderLayout());
        JPanel inner = new JPanel();
        outer.add(inner, BorderLayout.CENTER);
        ShortcutScope outerScope = new ShortcutScope(outer, id -> id.equals(edgeId) ? edgeRun : null);
        ShortcutScope innerScope = new ShortcutScope(inner, id -> null);

        List<ShortcutScope> ordered =
                JIPipeDesktopShortcutDispatcher.orderedContainingScopes(List.of(outerScope, innerScope), inner);
        assertEquals(edgeId,
                JIPipeDesktopShortcutDispatcher.resolveFirstAvailable(List.of(nodeId, edgeId), ordered));
    }

    @Test void noCandidateResolvesReturnsNull() {
        String nodeId = "org.hkijena.jipipe.graph.node.delete";
        String edgeId = "org.hkijena.jipipe.graph.edge.delete-control-point";
        JPanel p = new JPanel();
        ShortcutScope scope = new ShortcutScope(p, id -> null);
        List<ShortcutScope> ordered =
                JIPipeDesktopShortcutDispatcher.orderedContainingScopes(List.of(scope), p);

        assertNull(JIPipeDesktopShortcutDispatcher.resolveFirstAvailable(List.of(nodeId, edgeId), ordered));
        // An empty candidate list resolves to nothing as well (the map
        // lookup miss short-circuits before this helper is reached in
        // production, but the helper itself must be total).
        assertNull(JIPipeDesktopShortcutDispatcher.resolveFirstAvailable(List.of(), ordered));
    }
}
