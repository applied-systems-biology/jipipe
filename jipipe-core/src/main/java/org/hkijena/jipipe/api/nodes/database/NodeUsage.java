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

package org.hkijena.jipipe.api.nodes.database;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The node usage signal of the enhanced search (#1404): how often a node
 * type is actually used. Two sources add up:
 * <ul>
 * <li>the <b>shipped prior</b> - mined from the real-world example-workflow
 * corpus by {@code utils/search-usage/generate-usage-prior.py} and committed
 * as {@code org/hkijena/jipipe/search/usage-prior.json} (community usage,
 * available on every fresh install);</li>
 * <li>the <b>recorded user counts</b> - incremented at the search UIs'
 * insert funnels and persisted in the settings registry (personal usage,
 * local to this installation).</li>
 * </ul>
 * Both are "times used". The prior supplier and the persister are
 * injectable so tests never touch the shipped resource or the real
 * settings (the {@code NodeEntryKind} persistence-helper precedent).
 * <p>
 * Unknown node ids are inert: reading them yields 0, recording them is
 * allowed (they may exist in another installation's plugin set).
 */
public final class NodeUsage {

    private static final String SETTINGS_DATABASE = "node-db";
    private static final JIPipePathMetadataStore.Key SETTINGS_KEY =
            JIPipePathMetadataStore.key("node-usage-counts");

    /**
     * The shipped prior; cached after the first load (the resource never
     * changes within a run).
     */
    private static volatile Map<String, Integer> prior;
    private static volatile Supplier<Map<String, Integer>> priorSupplier = NodeUsage::loadPrior;
    /**
     * The per-user counts; a concurrent map: {@link #record} is called from
     * the EDT but must not lose updates against other callers.
     */
    private static final ConcurrentHashMap<String, Integer> userCounts = new ConcurrentHashMap<>();
    private static volatile boolean userCountsLoaded;
    private static volatile Consumer<Map<String, Integer>> userCountsPersister = NodeUsage::persistUserCounts;

    private NodeUsage() {
    }

    /**
     * The effective usage of the given node id: prior + recorded user
     * count; 0 for {@code null} or an id with no signal.
     */
    public static int usage(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return 0;
        }
        Map<String, Integer> priorMap = prior();
        int total = priorMap.getOrDefault(nodeId, 0);
        total += userCounts().getOrDefault(nodeId, 0);
        return total;
    }

    /**
     * Records one use of the given node id (no-op for {@code null}/blank)
     * and persists the counts write-through.
     */
    public static void record(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        userCounts().merge(nodeId, 1, Integer::sum);
        Consumer<Map<String, Integer>> persister = userCountsPersister;
        if (persister != null) {
            persister.accept(new TreeMap<>(userCounts()));
        }
    }

    /**
     * The cached prior map (loaded through the supplier on first use).
     */
    private static Map<String, Integer> prior() {
        Map<String, Integer> map = prior;
        if (map == null) {
            map = priorSupplier.get();
            prior = map;
        }
        return map;
    }

    /**
     * The recorded user counts, lazily loaded from the settings registry
     * once (tolerant of corrupt entries).
     */
    private static Map<String, Integer> userCounts() {
        if (!userCountsLoaded) {
            synchronized (NodeUsage.class) {
                if (!userCountsLoaded) {
                    userCounts.putAll(readPersistedUserCounts());
                    userCountsLoaded = true;
                }
            }
        }
        return userCounts;
    }

    private static Map<String, Integer> readPersistedUserCounts() {
        if (!JIPipe.isInstantiated()) {
            return Collections.emptyMap();
        }
        try {
            JsonNode node = JIPipe.getSettings().getFromRegistry(
                    SETTINGS_DATABASE, SETTINGS_KEY, JsonNode.class, null, false);
            if (node == null || !node.isObject()) {
                return Collections.emptyMap();
            }
            return coerceCounts(JsonUtils.getObjectMapper().convertValue(node,
                    JsonUtils.getObjectMapper().getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)));
        } catch (Exception e) {
            // A missing/unreadable registry must never break ranking.
            return Collections.emptyMap();
        }
    }

    /**
     * Coerces a raw (JSON-derived) map to integer counts: non-numeric and
     * negative values are dropped (corrupt locations reset, never throw).
     */
    static Map<String, Integer> coerceCounts(Map<String, Object> raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number number && number.longValue() >= 0) {
                out.put(entry.getKey(), (int) Math.min(Integer.MAX_VALUE, number.longValue()));
            }
        }
        return out;
    }

    /**
     * Parses the prior resource document into the count map; a malformed
     * document yields an empty map (fail-open: ranking continues without
     * the prior).
     */
    static Map<String, Integer> parsePriorJson(JsonNode document) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (document == null || !document.has("counts") || !document.get("counts").isObject()) {
            return out;
        }
        JsonNode counts = document.get("counts");
        for (Map.Entry<String, JsonNode> entry : counts.properties()) {
            JsonNode value = entry.getValue();
            if (value != null && value.has("instances") && value.get("instances").isInt()
                    && value.get("instances").asInt() > 0) {
                out.put(entry.getKey(), value.get("instances").asInt());
            }
        }
        return out;
    }

    /**
     * Loads the shipped prior from the classpath; an unreadable resource
     * yields an empty map.
     */
    public static Map<String, Integer> loadPrior() {
        try (var stream = NodeUsage.class.getResourceAsStream("/org/hkijena/jipipe/search/usage-prior.json")) {
            if (stream == null) {
                return Collections.emptyMap();
            }
            return parsePriorJson(JsonUtils.getObjectMapper().readTree(stream));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * The production persister: writes the whole counts map into the
     * settings registry (the {@code node-db} block, like the pinned node
     * ids and the kinds selection). Without a running JIPipe instance
     * (very early startup, headless CLI) recording stays in memory for
     * this run - fail-open, never a crash.
     */
    private static void persistUserCounts(Map<String, Integer> counts) {
        if (!JIPipe.isInstantiated()) {
            return;
        }
        JIPipe.getSettings().putIntoRegistry(SETTINGS_DATABASE, SETTINGS_KEY, new TreeMap<>(counts));
        JIPipe.getSettings().save();
    }

    /**
     * Injects a prior supplier (tests); {@code null} restores the default.
     */
    public static void setPriorSupplier(Supplier<Map<String, Integer>> supplier) {
        priorSupplier = supplier != null ? supplier : NodeUsage::loadPrior;
        prior = null; // re-resolve on next use
    }

    /**
     * Injects a persister (tests); {@code null} restores the default. The
     * consumer receives a snapshot of the current counts after each
     * {@link #record}.
     */
    public static void setUserCountsPersister(Consumer<Map<String, Integer>> persister) {
        userCountsPersister = persister != null ? persister : NodeUsage::persistUserCounts;
    }

    /**
     * Clears all state and restores the production defaults (tests).
     */
    public static void resetForTests() {
        priorSupplier = NodeUsage::loadPrior;
        userCountsPersister = NodeUsage::persistUserCounts;
        synchronized (NodeUsage.class) {
            userCounts.clear();
            userCountsLoaded = false;
        }
        prior = null;
    }
}
