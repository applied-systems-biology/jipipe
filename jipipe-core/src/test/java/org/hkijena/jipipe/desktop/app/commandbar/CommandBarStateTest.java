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
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CommandBarState}: the bounded, deduplicated
 * recently-fired list of the command bar (issue #1346, Task 8), its JSON
 * round trip, the known-recent filter, and the write guard that keeps an
 * empty state from being materialized into the settings document.
 * <p>
 * All tests except
 * {@link #recordFiredNeverMaterializesAnEmptyState()} are pure; that one
 * boots the live settings service headlessly the same way
 * {@link CommandBarSettingsTest} does: the module's
 * {@code LauncherSessionListener}s isolate the profile into a temp
 * directory and pre-patch ImageJ, so a full {@link JIPipe} instance is
 * hermetic.
 */
class CommandBarStateTest {

    @Test void addRecentDedupesAndCaps() {
        CommandBarState state = new CommandBarState();
        for (int i = 0; i < 25; i++) {
            state.addRecent("id-" + i);
        }
        List<String> ids = state.getRecentCommandIds();
        assertEquals(20, ids.size(), "the list must be capped at 20");
        assertEquals("id-24", ids.get(0), "the most recent id comes first");
        assertEquals("id-5", ids.get(19), "the oldest kept id is the 5th one added");

        // Re-adding an id that is already in the middle of the list moves it
        // to the front without duplicating it.
        state.addRecent("id-10");
        ids = state.getRecentCommandIds();
        assertEquals(20, ids.size());
        assertEquals("id-10", ids.get(0), "a re-added id must move to the front");
        assertEquals(1, ids.stream().filter("id-10"::equals).count(),
                "a re-added id must not be duplicated");
    }

    @Test void jsonRoundTrip() {
        CommandBarState state = new CommandBarState();
        state.addRecent("a");
        state.addRecent("b");
        state.addRecent("c");
        String json = JsonUtils.toJsonString(state);
        CommandBarState back = JsonUtils.readFromString(json, CommandBarState.class);
        assertEquals(List.of("c", "b", "a"), back.getRecentCommandIds(),
                "the round trip must preserve the most-recent-first order");

        // An empty state serializes as an explicit empty list, and an absent
        // field reads back as empty.
        assertEquals("{\"recentCommandIds\":[]}", JsonUtils.toJsonString(new CommandBarState()));
        assertEquals(List.of(),
                JsonUtils.readFromString("{}", CommandBarState.class).getRecentCommandIds());
    }

    @Test void knownRecentFiltersUnknownIds() {
        List<String> known = CommandBarState.knownRecent(
                List.of("a", "b", "c", "d"), id -> id.equals("b") || id.equals("d"));
        assertEquals(List.of("b", "d"), known,
                "unknown ids are dropped, the order is preserved");
        assertTrue(CommandBarState.knownRecent(List.of(), id -> true).isEmpty());
        assertTrue(CommandBarState.knownRecent(null, id -> true).isEmpty());
    }

    @Test void recordFiredSkipsEmptyWrite() {
        // The pure write guard behind CommandBarSettings.recordFired: a fire
        // that carries no recordable id produces nothing to write.
        assertNull(CommandBarState.updated(null, null), "a null id must not be recorded");
        assertNull(CommandBarState.updated(null, ""), "an empty id must not be recorded");
        CommandBarState current = new CommandBarState();
        current.addRecent("a");
        assertNull(CommandBarState.updated(current, null),
                "a null id must not be recorded, even with a populated current state");

        // A real fire produces a writable state with the id moved to the
        // front; an absent current state is read as empty.
        CommandBarState next = CommandBarState.updated(current, "b");
        assertNotNull(next);
        assertEquals(List.of("b", "a"), next.getRecentCommandIds());
        assertEquals(List.of("x"), CommandBarState.updated(null, "x").getRecentCommandIds());
    }

    @Test void recordFiredNeverMaterializesAnEmptyState() {
        JIPipe.ensureInstance();
        CommandBarSettings.stateBinding().reset();
        assertFalse(CommandBarSettings.stateBinding().isModified(),
                "a fresh profile must not hold a state value");
        CommandBarSettings.recordFired(null);
        assertFalse(CommandBarSettings.stateBinding().isModified(),
                "an empty state must never be materialized into the document");
        CommandBarSettings.recordFired("org.hkijena.jipipe.command-bar.test.recent");
        assertTrue(CommandBarSettings.stateBinding().isModified());
        assertEquals(List.of("org.hkijena.jipipe.command-bar.test.recent"),
                CommandBarSettings.stateBinding().get().getRecentCommandIds());
        CommandBarSettings.stateBinding().reset();
    }
}
