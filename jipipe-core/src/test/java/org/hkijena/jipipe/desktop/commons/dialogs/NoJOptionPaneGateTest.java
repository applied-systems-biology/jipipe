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

package org.hkijena.jipipe.desktop.commons.dialogs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Build gate for the migration to the modern dialog API (#1341): the token
 * {@code JOptionPane} must not appear anywhere in main code. Import lines
 * count as violations too - the goal is zero references, so the check is a
 * plain substring match per line (no comment/import filtering).
 * <p>
 * Scope: {@code jipipe-core/src/main/java}, every plugin module's
 * {@code src/main/java} (below {@code plugins/}),
 * and the remaining top-level modules' {@code src/main/java} trees when they
 * are present in the checkout ({@code jipipe-desktop}, {@code jipipe-cli},
 * {@code jipipe-launcher}, and the submodules of {@code contrib}). Test trees
 * ({@code src/test}) are never scanned.
 * <p>
 * The gate is fail-closed: if the repository layout cannot be verified (no
 * ancestor of the test class directory contains {@code jipipe-core}, or an
 * expected main source tree is missing), the test fails with a clear message
 * instead of silently passing. An unreadable source file is also a failure.
 * <p>
 * Intentionally red until the migration waves complete (Tasks 8-10 and the
 * sweep in Task 11); run it only via targeted {@code -Dtest=} invocations
 * until then.
 */
class NoJOptionPaneGateTest {

    /**
     * Top-level directories beyond this module and {@code plugins} that are
     * part of main code. Entries missing from a checkout are skipped; for a
     * present entry, {@code <dir>/src/main/java} is scanned, or - if the
     * entry is a multi-module aggregator (e.g. {@code contrib}) - every
     * submodule's {@code src/main/java} below it.
     */
    private static final List<String> OTHER_MODULE_DIRS =
            List.of("jipipe-desktop", "jipipe-cli", "jipipe-launcher", "contrib");

    private static final String TOKEN = "JOptionPane";

    /**
     * The failure message lists the first N violations (each as file:line)
     * plus the total count, so huge red runs stay readable.
     */
    private static final int MAX_LISTED_VIOLATIONS = 100;

    /**
     * One token occurrence: main-source file (repo-root-relative) and the
     * 1-based line number.
     */
    private record Hit(String file, int line) {
    }

    @Test
    void noJOptionPaneInMainCode() throws IOException {
        Path repoRoot = repoRoot();
        List<Path> roots = scanRoots(repoRoot);
        ScanResult result = scan(repoRoot, roots);
        if (!result.hits().isEmpty()) {
            fail(message(repoRoot, roots, result));
        }
    }

    /**
     * Resolves the repository root from this test class' location
     * ({@code <root>/jipipe-core/target/test-classes}) by walking up to the
     * first directory that contains {@code jipipe-core}. This works
     * regardless of the working directory surefire uses.
     */
    private static Path repoRoot() {
        Path start;
        try {
            start = Paths.get(NoJOptionPaneGateTest.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("[NoJOptionPaneGate] fail-closed: cannot resolve the " +
                    "test class location: " + e, e);
        }
        Path dir = Files.isDirectory(start) ? start : start.getParent();
        for (Path p = dir; p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("jipipe-core"))) {
                return p;
            }
        }
        throw new IllegalStateException("[NoJOptionPaneGate] fail-closed: no ancestor of " + start +
                " contains a 'jipipe-core' directory - cannot locate the repository root.");
    }

    /**
     * Collects the {@code src/main/java} roots to scan. The required roots
     * (this module and {@code plugins}) must resolve, otherwise the gate
     * fails instead of scanning an empty/partial set.
     */
    private static List<Path> scanRoots(Path repoRoot) {
        List<Path> roots = new ArrayList<>();
        Path core = repoRoot.resolve("jipipe-core/src/main/java");
        if (!Files.isDirectory(core)) {
            throw new IllegalStateException("[NoJOptionPaneGate] fail-closed: missing main source tree " + core);
        }
        roots.add(core);

        Path plugins = repoRoot.resolve("plugins");
        if (!Files.isDirectory(plugins)) {
            throw new IllegalStateException("[NoJOptionPaneGate] fail-closed: missing 'plugins' directory " + plugins);
        }
        List<Path> pluginRoots = mainJavaRoots(plugins);
        if (pluginRoots.isEmpty()) {
            throw new IllegalStateException("[NoJOptionPaneGate] fail-closed: no plugin main source tree found under " + plugins);
        }
        roots.addAll(pluginRoots);

        for (String name : OTHER_MODULE_DIRS) {
            Path module = repoRoot.resolve(name);
            if (!Files.isDirectory(module)) {
                continue; // module not part of this checkout
            }
            Path direct = module.resolve("src/main/java");
            if (Files.isDirectory(direct)) {
                roots.add(direct);
            } else {
                roots.addAll(mainJavaRoots(module));
            }
        }
        roots.sort(Comparator.comparing(Path::toString));
        return roots;
    }

    /**
     * All {@code <child>/src/main/java} directories below {@code dir},
     * sorted for deterministic output.
     */
    private static List<Path> mainJavaRoots(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isDirectory)
                    .map(child -> child.resolve("src/main/java"))
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("[NoJOptionPaneGate] fail-closed: cannot list " + dir, e);
        }
    }

    /**
     * Walks every root and collects {@code JOptionPane} occurrences.
     * Roots are {@code src/main/java} trees, so {@code target/}, {@code .git},
     * and test sources are never touched.
     */
    private static ScanResult scan(Path repoRoot, List<Path> roots) throws IOException {
        List<Hit> hits = new ArrayList<>();
        int files = 0;
        for (Path root : roots) {
            try (Stream<Path> stream = Files.walk(root)) {
                List<Path> javaFiles = stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".java"))
                        .sorted()
                        .toList();
                files += javaFiles.size();
                for (Path file : javaFiles) {
                    collect(repoRoot, file, hits);
                }
            }
        }
        hits.sort(Comparator.comparing(Hit::file).thenComparingInt(Hit::line));
        return new ScanResult(files, hits);
    }

    private static void collect(Path repoRoot, Path file, List<Hit> hits) {
        List<String> lines;
        try {
            // ISO-8859-1 decodes every byte sequence without failure; the
            // token is ASCII, so byte-preserving decoding is sufficient.
            lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("[NoJOptionPaneGate] fail-closed: cannot read " + file, e);
        }
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(TOKEN)) {
                hits.add(new Hit(repoRoot.relativize(file).toString(), i + 1));
            }
        }
    }

    private static String message(Path repoRoot, List<Path> roots, ScanResult result) {
        List<Hit> hits = result.hits();
        StringBuilder sb = new StringBuilder();
        sb.append("[NoJOptionPaneGate] the token 'JOptionPane' must not appear in main code ")
                .append("(#1341: migrate call sites to org.hkijena.jipipe.desktop.commons.dialogs).\n");
        sb.append("Scanned ").append(result.files()).append(" .java files under ").append(roots.size())
                .append(" main source trees (repo root: ").append(repoRoot).append(").\n");
        sb.append("Violations: ").append(hits.size()).append(" occurrence(s).\n");
        int listed = Math.min(hits.size(), MAX_LISTED_VIOLATIONS);
        sb.append("First ").append(listed).append(" violation(s):\n");
        for (int i = 0; i < listed; i++) {
            Hit hit = hits.get(i);
            sb.append("  ").append(hit.file()).append(":").append(hit.line()).append('\n');
        }
        if (hits.size() > listed) {
            sb.append("  ... and ").append(hits.size() - listed).append(" more.\n");
        }
        return sb.toString();
    }

    private record ScanResult(int files, List<Hit> hits) {
    }
}
