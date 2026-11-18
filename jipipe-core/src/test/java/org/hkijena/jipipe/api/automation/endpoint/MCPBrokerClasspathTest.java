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

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link MCPBrokerClasspath}: pure string manipulation, no file system access.
 * <p>
 * The fixtures mirror the R2 defect: a released Fiji install mixes Fiji's base jars with
 * JIPipe's dependency jars in one directory and jaunch's directory glob order is
 * unspecified, so the broker spawn must reorder duplicate artifact families deterministically
 * (newest first) without dropping anything.
 */
public class MCPBrokerClasspathTest {

    private static final String SEP = File.pathSeparator;

    private static String join(String... entries) {
        return String.join(SEP, entries);
    }

    @Test
    public void jacksonAnnotationsNewestVersionWinsRegardlessOfInputOrder() {
        // The R2 defect in miniature: Fiji ships jackson-annotations-2.18.0, JIPipe ships
        // 2.21; the shadowing order must never depend on the host's directory enumeration.
        String oldFirst = join(
                "jars/jackson-annotations-2.18.0.jar",
                "jars/jackson-annotations-2.21.jar");
        assertEquals(join(
                        "jars/jackson-annotations-2.21.jar",
                        "jars/jackson-annotations-2.18.0.jar"),
                MCPBrokerClasspath.curate(oldFirst));
        String newFirst = join(
                "jars/jackson-annotations-2.21.jar",
                "jars/jackson-annotations-2.18.0.jar");
        assertEquals(join(
                        "jars/jackson-annotations-2.21.jar",
                        "jars/jackson-annotations-2.18.0.jar"),
                MCPBrokerClasspath.curate(newFirst));
    }

    @Test
    public void reactiveStreamsNewerPatchVersionWins() {
        String input = join(
                "jars/reactive-streams-1.0.3.jar",
                "jars/reactive-streams-1.0.4.jar");
        assertEquals(join(
                        "jars/reactive-streams-1.0.4.jar",
                        "jars/reactive-streams-1.0.3.jar"),
                MCPBrokerClasspath.curate(input));
    }

    @Test
    public void versionComponentsAreComparedNumerically() {
        // "2.21" beats "2.3" numerically (21 > 3), which a plain string comparison gets
        // wrong ("2.3" > "2.21"). Same rule for 18 > 2 below: the pin documents the
        // numeric rule, not an expected "prefix" behavior.
        String twoPointTwentyOneVersusTwoPointThree = join(
                "jars/jackson-core-2.3.jar",
                "jars/jackson-core-2.21.jar");
        assertEquals(join(
                        "jars/jackson-core-2.21.jar",
                        "jars/jackson-core-2.3.jar"),
                MCPBrokerClasspath.curate(twoPointTwentyOneVersusTwoPointThree));
        String eighteenVersusTwo = join(
                "jars/jackson-databind-2.18.0.jar",
                "jars/jackson-databind-2.2.jar");
        assertEquals(join(
                        "jars/jackson-databind-2.18.0.jar",
                        "jars/jackson-databind-2.2.jar"),
                MCPBrokerClasspath.curate(eighteenVersusTwo));
    }

    @Test
    public void devModeClasspathWithoutDuplicatesIsByteIdentical() {
        // Dev mode: target/classes directories plus single-version ~/.m2 jars, no
        // duplicate groups. The curation must be an exact no-op.
        String devClasspath = join(
                "modules/jipipe-core/target/classes",
                "modules/jipipe-desktop/target/classes",
                "/home/user/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21.jar",
                "/home/user/.m2/repository/io/projectreactor/reactive-streams/1.0.4/reactive-streams-1.0.4.jar");
        assertEquals(devClasspath, MCPBrokerClasspath.curate(devClasspath));
    }

    @Test
    public void directoryEntriesPassThroughAndKeepTheirRelativeOrder() {
        // Directories are never grouped and keep their relative order among themselves.
        // The jar winner is hoisted to the front of the whole curated classpath; the loser
        // stays on the classpath in the second block.
        String input = join(
                "modules/app/target/classes",
                "jars/jackson-annotations-2.18.0.jar",
                "modules/core/target/classes",
                "jars/jackson-annotations-2.21.jar",
                "modules/tables/target/classes");
        assertEquals(join(
                        "jars/jackson-annotations-2.21.jar",
                        "modules/app/target/classes",
                        "jars/jackson-annotations-2.18.0.jar",
                        "modules/core/target/classes",
                        "modules/tables/target/classes"),
                MCPBrokerClasspath.curate(input));
    }

    @Test
    public void crossGroupVersionJarStaysOnTheClasspathBehindItsWinner() {
        // tools.jackson jackson-core 3.1.8 and com.fasterxml jackson-core 2.18.0 share the
        // artifact identity "jackson-core" but live in different packages: both are needed
        // at runtime. The winner only takes first position; nothing is ever dropped.
        String input = join(
                "jars/jackson-core-2.18.0.jar",
                "jars/tools.jackson.core/jackson-core-3.1.8.jar");
        assertEquals(join(
                        "jars/tools.jackson.core/jackson-core-3.1.8.jar",
                        "jars/jackson-core-2.18.0.jar"),
                MCPBrokerClasspath.curate(input));
    }

    @Test
    public void exactDuplicatePathAppearsOnce() {
        String input = join(
                "jars/jackson-annotations-2.21.jar",
                "jars/reactive-streams-1.0.4.jar",
                "jars/jackson-annotations-2.21.jar");
        assertEquals(join(
                        "jars/jackson-annotations-2.21.jar",
                        "jars/reactive-streams-1.0.4.jar"),
                MCPBrokerClasspath.curate(input));
    }

    @Test
    public void jarWithoutVersionSegmentIsASingletonGroup() {
        String input = join(
                "jars/fiji-lib.jar",
                "jars/jackson-annotations-2.18.0.jar",
                "jars/fiji-lib2.jar");
        assertEquals(input, MCPBrokerClasspath.curate(input));
    }

    @Test
    public void emptyEntriesAreDroppedAndEntriesAreTrimmed() {
        String input = join("", " jars/jackson-annotations-2.21.jar ", "", "jars/jackson-annotations-2.21.jar", "");
        assertEquals("jars/jackson-annotations-2.21.jar", MCPBrokerClasspath.curate(input));
    }

    @Test
    public void nullAndEmptyInputYieldEmptyOutput() {
        assertEquals("", MCPBrokerClasspath.curate(null));
        assertEquals("", MCPBrokerClasspath.curate(""));
    }

    @Test
    public void equalVersionDuplicateKeepsFirstOccurrenceAsWinner() {
        // Not our release scenario, but the tie rule must be deterministic: the first
        // occurrence wins and the later twin follows it (a no-op besides the dedupe).
        String input = join(
                "jars/slf4j-api-2.0.17.jar",
                "jars/ext/slf4j-api-2.0.17.jar");
        assertEquals(input, MCPBrokerClasspath.curate(input));
    }

    @Test
    public void releasedFijiLayoutIsReorderedDeterministically() {
        // Integration fixture of the actual conflict families (facts sheet items 5 and 6):
        // annotations and reactive-streams are true version conflicts, the Jackson 2/3
        // cores are not, and the surrounding single-copy jars keep their order.
        String input = join(
                "jars/imglib2-7.1.3.jar",
                "jars/jackson-annotations-2.18.0.jar",
                "jars/jackson-core-2.18.0.jar",
                "jars/tools.jackson.core/jackson-core-3.1.8.jar",
                "jars/reactive-streams-1.0.3.jar",
                "jars/jackson-databind-2.18.0.jar",
                "jars/reactive-streams-1.0.4.jar",
                "jars/jackson-annotations-2.21.jar",
                "jars/slf4j-api-2.0.17.jar");
        String curated = MCPBrokerClasspath.curate(input);
        String[] entries = curated.split(SEP, -1);
        // Every entry survives exactly once.
        assertEquals(9, entries.length);
        for (String entry : input.split(SEP, -1)) {
            long count = java.util.Arrays.stream(entries).filter(entry::equals).count();
            assertEquals(1, count, "entry must appear exactly once: " + entry);
        }
        // The winners take the front of their families: both annotation jars come first
        // among themselves with 2.21 leading, same for reactive-streams.
        int newAnnotations = indexOf(entries, "jars/jackson-annotations-2.21.jar");
        int oldAnnotations = indexOf(entries, "jars/jackson-annotations-2.18.0.jar");
        assertTrue(newAnnotations < oldAnnotations, "2.21 must precede 2.18.0: " + curated);
        int newStreams = indexOf(entries, "jars/reactive-streams-1.0.4.jar");
        int oldStreams = indexOf(entries, "jars/reactive-streams-1.0.3.jar");
        assertTrue(newStreams < oldStreams, "1.0.4 must precede 1.0.3: " + curated);
        int core3 = indexOf(entries, "jars/tools.jackson.core/jackson-core-3.1.8.jar");
        int core2 = indexOf(entries, "jars/jackson-core-2.18.0.jar");
        assertTrue(core3 < core2, "jackson-core 3.1.8 must precede 2.18.0: " + curated);
        // Single-copy jars keep their relative order.
        assertTrue(indexOf(entries, "jars/imglib2-7.1.3.jar") < indexOf(entries, "jars/slf4j-api-2.0.17.jar"));
    }

    private static int indexOf(String[] entries, String entry) {
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].equals(entry)) {
                return i;
            }
        }
        return -1;
    }
}
