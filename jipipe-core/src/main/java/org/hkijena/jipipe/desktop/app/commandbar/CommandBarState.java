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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The persisted recently-fired list of the command bar (issue #1346, Task 8):
 * the action ids of registry commands the user fired through the bar, most
 * recent first. It is stored in the application settings document under the
 * {@value CommandBarSettings#BLOCK_PATH} block, at the {@code state}
 * sub-path, addressed through {@link CommandBarSettings#STATE}.
 * <p>
 * The list is capped at {@value #MAX_RECENT} ids: the oldest fall off the
 * end. An id is recorded at most once (firing it again moves it to the
 * front), so the list is also the recency order the bar uses to tie-break
 * equal search scores.
 * <p>
 * The field accessors use explicit {@code @JsonGetter}/{@code @JsonSetter}
 * annotations because the global {@link org.hkijena.jipipe.utils.json.JsonUtils}
 * mapper disables implicit property visibility (the
 * {@link org.hkijena.jipipe.api.shortcuts.KeymapSettings} convention). An
 * absent field reads back as an empty list (the field initializer), and a
 * {@code null} value in the document is tolerated the same way.
 * <p>
 * <b>Settings rule.</b> An empty state is never materialized into the
 * document: {@link CommandBarSettings#recordFired(String)} writes back only
 * while the list has content, so a profile that never fired a registry
 * command through the bar stays free of this key.
 */
public class CommandBarState {
    /**
     * The maximum number of ids kept in the recently-fired list.
     */
    public static final int MAX_RECENT = 20;

    private List<String> recentCommandIds = new ArrayList<>();

    public CommandBarState() {
    }

    /**
     * @return a defensive copy of the recently-fired ids, most recent first
     */
    @JsonGetter("recentCommandIds")
    public List<String> getRecentCommandIds() {
        return new ArrayList<>(recentCommandIds);
    }

    /**
     * @param recentCommandIds the ids to store, most recent first;
     *                         {@code null} is treated as empty
     */
    @JsonSetter("recentCommandIds")
    public void setRecentCommandIds(List<String> recentCommandIds) {
        this.recentCommandIds = recentCommandIds == null ? new ArrayList<>() : new ArrayList<>(recentCommandIds);
    }

    /**
     * Records a fired action id: inserts it at the front, removes any
     * previous occurrence of the same id, and caps the list at
     * {@value #MAX_RECENT} ids (the oldest fall off the end).
     *
     * @param actionId the id of the fired registry command; {@code null}
     *                 (ephemeral entries carry none) and empty strings are
     *                 not recorded
     */
    public void addRecent(String actionId) {
        if (actionId == null || actionId.isEmpty()) {
            return;
        }
        recentCommandIds.remove(actionId);
        recentCommandIds.add(0, actionId);
        if (recentCommandIds.size() > MAX_RECENT) {
            recentCommandIds.subList(MAX_RECENT, recentCommandIds.size()).clear();
        }
    }

    /**
     * Filters a recent list down to the ids the given predicate recognizes,
     * keeping the order (most recent first).
     *
     * @param recent  the recent list, most recent first, may be
     *                {@code null}
     * @param isKnown the recognizer, e.g. {@code registry::isRegistered}
     * @return the known ids, in the given order; empty for a {@code null}
     *         list
     */
    public static List<String> knownRecent(List<String> recent, Predicate<String> isKnown) {
        if (recent == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>(recent.size());
        for (String id : recent) {
            if (id != null && isKnown.test(id)) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * The pure write guard behind {@link CommandBarSettings#recordFired(String)}:
     * returns the state to persist after firing {@code actionId} on top of
     * {@code current}, or {@code null} when nothing must be written
     * (an unrecordable id). That is the settings rule that an empty state
     * is never materialized into the document: the caller writes back only
     * a non-null result.
     *
     * @param current    the stored state, or {@code null} when absent
     *                   (treated as empty)
     * @param actionId   the id of the fired registry command
     * @return the state to write back, or {@code null} to skip the write
     */
    public static CommandBarState updated(CommandBarState current, String actionId) {
        if (actionId == null || actionId.isEmpty()) {
            return null;
        }
        CommandBarState next = current == null ? new CommandBarState() : new CommandBarState(current.recentCommandIds);
        next.addRecent(actionId);
        return next.recentCommandIds.isEmpty() ? null : next;
    }

    /**
     * Copies the given ids into this state (package-private; the public
     * entry point is {@link #setRecentCommandIds(List)}).
     */
    CommandBarState(List<String> recentCommandIds) {
        this.recentCommandIds = new ArrayList<>(recentCommandIds);
    }
}
