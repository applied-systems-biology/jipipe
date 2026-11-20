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

package org.hkijena.jipipe.api.knowledge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Test support: resolves the packaged production KDB corpus root
 * ({@code jipipe-core} resources {@code org/hkijena/jipipe/ai/kdb/entries})
 * from the classpath, so the gates work from any working directory and from
 * both ways the launcher test module can see the resource:
 * <ul>
 * <li>Reactor build ({@code -am}): {@code jipipe-core}'s classes directory is
 * on the classpath and the root is a plain directory.</li>
 * <li>Standalone launcher test run: the dependency resolves from the local
 * repository, the root ships inside the {@code jipipe-core} jar, and the tree
 * is unpacked once into a temporary directory (the loader walks file system
 * paths).</li>
 * </ul>
 */
final class KdbProductionCorpus {

    /**
     * The classpath location of the corpus root (design spec 5.1). A constant
     * string literal: it is the packaged resource path.
     */
    static final String CLASSPATH_LOCATION = "/org/hkijena/jipipe/ai/kdb/entries";

    private static Path unpackedRoot;

    private KdbProductionCorpus() {
    }

    /**
     * The corpus root as a file system path, usable with
     * {@link KdbEntryLoader#loadAll(Path)}.
     *
     * @return the corpus root
     */
    static Path root() {
        URL url = KdbProductionCorpus.class.getResource(CLASSPATH_LOCATION);
        if (url == null) {
            throw new IllegalStateException("The packaged KDB corpus root is missing from the classpath: "
                    + CLASSPATH_LOCATION);
        }
        if ("file".equals(url.getProtocol())) {
            try {
                return Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("The packaged KDB corpus root is not a file system path", e);
            }
        }
        return unpackFromJar(url);
    }

    private static synchronized Path unpackFromJar(URL url) {
        if (unpackedRoot != null) {
            return unpackedRoot;
        }
        try {
            File jarFile = new File(((JarURLConnection) url.openConnection()).getJarFileURL().toURI());
            Path tempRoot = Files.createTempDirectory("jipipe-kdb-corpus");
            tempRoot.toFile().deleteOnExit();
            try (JarFile jar = new JarFile(jarFile)) {
                String prefix = CLASSPATH_LOCATION.substring(1) + "/";
                java.util.Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !name.startsWith(prefix)) {
                        continue;
                    }
                    Path target = tempRoot.resolve(name.substring(prefix.length()));
                    Files.createDirectories(target.getParent());
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, target);
                    }
                }
            }
            Runtime.getRuntime().addShutdownHook(new Thread(KdbProductionCorpus::deleteUnpackedRoot));
            unpackedRoot = tempRoot;
            return tempRoot;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("The packaged KDB corpus root could not be unpacked from " + url, e);
        }
    }

    private static void deleteUnpackedRoot() {
        if (unpackedRoot == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(unpackedRoot)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            // Best effort cleanup of a temporary directory.
        }
    }
}
