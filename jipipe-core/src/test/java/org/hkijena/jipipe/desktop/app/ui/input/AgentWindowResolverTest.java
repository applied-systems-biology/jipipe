package org.hkijena.jipipe.desktop.app.ui.input;

import org.junit.jupiter.api.Test;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgentWindowResolverTest {
    private static final AgentWindowResolver.WindowBounds A =
            new AgentWindowResolver.WindowBounds(new Rectangle(0, 0, 100, 100), true);
    private static final AgentWindowResolver.WindowBounds B =
            new AgentWindowResolver.WindowBounds(new Rectangle(50, 50, 100, 100), true);
    private static final AgentWindowResolver.WindowBounds HIDDEN =
            new AgentWindowResolver.WindowBounds(new Rectangle(0, 0, 1000, 1000), false);

    @Test
    void resolvesTheTopmostShowingWindowUnderThePoint() {
        AgentWindowResolver.WindowBounds hit =
                AgentWindowResolver.resolve(List.of(A, B), new Point(60, 60));
        assertSame(B, hit);
    }

    @Test
    void singleWindowHitWhereOnlyOneContains() {
        assertSame(A, AgentWindowResolver.resolve(List.of(A, B), new Point(10, 10)));
    }

    @Test
    void hiddenWindowsAreNeverResolved() {
        assertNull(AgentWindowResolver.resolve(List.of(HIDDEN), new Point(10, 10)));
    }

    @Test
    void outsideAllWindowsResolvesNothing() {
        assertNull(AgentWindowResolver.resolve(List.of(A, B), new Point(500, 500)));
    }

    @Test
    void pointInsideAWindowButOutsideAnyComponentStillResolvesTheWindow() {
        assertSame(A, AgentWindowResolver.resolve(List.of(A), new Point(2, 98)));
    }

    @Test
    void allowedRectanglesListsShowingWindowsOnly() {
        assertEquals(List.of(A.onScreenBounds(), B.onScreenBounds()),
                AgentWindowResolver.allowedRectangles(List.of(A, HIDDEN, B)));
    }
}
