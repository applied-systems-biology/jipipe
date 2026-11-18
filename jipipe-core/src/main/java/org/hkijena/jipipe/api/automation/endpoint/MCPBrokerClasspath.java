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

package org.hkijena.jipipe.api.automation.endpoint;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic curation of the classpath handed to the spawned MCP broker JVM.
 * Pure string manipulation: no file system access, no JIPipe context.
 * <p>
 * See {@link MCPBrokerClient} for why the broker needs a curated classpath.
 */
public final class MCPBrokerClasspath {

    private MCPBrokerClasspath() {
    }

    /**
     * Orders a raw classpath deterministically with respect to duplicate artifact families.
     * <p>
     * Jar entries are grouped by artifact identity (basename minus the {@code .jar} suffix
     * minus the trailing version segment). Groups with a single distinct entry, directory
     * entries, and jars without a version segment pass through untouched in their original
     * order. In a group with multiple distinct entries the highest version wins (numeric-aware
     * comparison, ties resolved by first occurrence). The result starts with the winners of
     * all duplicate groups (in their groups' first-occurrence order), followed by every other
     * entry in original order: no entry is ever dropped, a losing entry stays on the classpath
     * behind its winner. Exact duplicate paths appear once. When the input has no duplicate
     * groups (dev mode), the output is byte-identical to the input.
     *
     * @param classpath the raw {@code java.class.path} value (may be null or empty)
     * @return the curated classpath, joined with {@link File#pathSeparator}
     */
    public static String curate(String classpath) {
        if (classpath == null || classpath.isEmpty()) {
            return "";
        }

        // Split, trim, drop empties, dedupe exact duplicate paths (first occurrence kept).
        List<String> entries = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String rawEntry : classpath.split(File.pathSeparator, -1)) {
            String entry = rawEntry.trim();
            if (!entry.isEmpty() && seen.add(entry)) {
                entries.add(entry);
            }
        }

        // Group jar entries by artifact identity; directories (identity null) never group.
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            String identity = artifactIdentity(entries.get(i));
            if (identity != null) {
                groups.computeIfAbsent(identity, key -> new ArrayList<>()).add(i);
            }
        }

        // For every duplicate group (more than one distinct entry): the highest version
        // wins (ties keep the first occurrence). Winners are hoisted to the front of the
        // result so every duplicate family resolves newest-first; the losing entries are
        // never dropped, they follow in the second block.
        Set<Integer> winnerIndices = new LinkedHashSet<>();
        for (List<Integer> group : groups.values()) {
            if (group.size() < 2) {
                continue;
            }
            int winner = group.get(0);
            for (int candidate : group) {
                if (compareVersions(versionOf(entries.get(candidate)), versionOf(entries.get(winner))) > 0) {
                    winner = candidate;
                }
            }
            winnerIndices.add(winner);
        }

        // Winners first (in their groups' first-occurrence order), then all remaining
        // entries in original order: every entry appears exactly once.
        List<String> result = new ArrayList<>(entries.size());
        for (int winner : winnerIndices) {
            result.add(entries.get(winner));
        }
        for (int i = 0; i < entries.size(); i++) {
            if (!winnerIndices.contains(i)) {
                result.add(entries.get(i));
            }
        }
        return String.join(File.pathSeparator, result);
    }

    /**
     * Artifact identity of a classpath entry: the basename (after the last {@code /} or
     * {@code \}) of a {@code .jar} entry, minus the {@code .jar} suffix, minus the trailing
     * version segment (the part after the last dash whose remainder starts with a digit).
     * Returns null for directory entries (no {@code .jar} suffix): they never group.
     */
    static String artifactIdentity(String entry) {
        if (!entry.endsWith(".jar")) {
            return null;
        }
        return splitVersion(stem(entry))[0];
    }

    /**
     * Version segment of a classpath entry: the part after the identity's version dash, or
     * null when the jar has no version segment ({@link #compareVersions} treats null as
     * lowest).
     */
    static String versionOf(String entry) {
        if (!entry.endsWith(".jar")) {
            return null;
        }
        return splitVersion(stem(entry))[1];
    }

    /**
     * Splits an {@code artifact-version} stem at its trailing version segment: the last
     * dash such that everything after it starts with a digit. Returns
     * {@code {identity, version}}, with version null when no such dash exists.
     */
    private static String[] splitVersion(String stem) {
        int dash = stem.lastIndexOf('-');
        while (dash >= 0) {
            if (dash + 1 < stem.length() && Character.isDigit(stem.charAt(dash + 1))) {
                return new String[]{stem.substring(0, dash), stem.substring(dash + 1)};
            }
            dash = stem.lastIndexOf('-', dash - 1);
        }
        return new String[]{stem, null};
    }

    private static String stem(String entry) {
        String basename = entry;
        int slash = Math.max(basename.lastIndexOf('/'), basename.lastIndexOf('\\'));
        if (slash >= 0) {
            basename = basename.substring(slash + 1);
        }
        return basename.substring(0, basename.length() - ".jar".length());
    }

    /**
     * Numeric-aware version comparison: components (split on {@code .}, {@code -}, and
     * {@code _}) compare numerically when both parse, otherwise lexically; a version with
     * fewer components but an equal prefix sorts before its extension; null sorts lowest;
     * equal versions return 0.
     */
    static int compareVersions(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        String[] leftParts = left.split("[._-]");
        String[] rightParts = right.split("[._-]");
        int common = Math.min(leftParts.length, rightParts.length);
        for (int i = 0; i < common; i++) {
            int comparison = compareComponent(leftParts[i], rightParts[i]);
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(leftParts.length, rightParts.length);
    }

    private static int compareComponent(String left, String right) {
        try {
            long leftValue = Long.parseLong(left);
            long rightValue = Long.parseLong(right);
            return Long.compare(leftValue, rightValue);
        } catch (NumberFormatException e) {
            return left.compareTo(right);
        }
    }
}
