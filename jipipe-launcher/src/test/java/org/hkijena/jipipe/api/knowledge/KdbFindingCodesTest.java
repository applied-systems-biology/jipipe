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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The finding-code documentation gate (design spec 5.8, the two-test rule):
 * the {@link KdbFindingCode} list is closed, and the code list and the
 * documentation table cannot drift. Every code must be named in
 * {@code docs/superpowers/kdb-finding-codes.md} and the documented severity
 * must match the code's actual severity, so a code may be added, removed, or
 * re-severitied only together with a row in that table.
 */
class KdbFindingCodesTest {

    /**
     * The documentation table, resolved from the repository root. Surefire
     * runs tests with the module basedir ({@code jipipe-launcher}) as the
     * working directory, so the repository root is one level up; to stay
     * robust against other working directories (for example IDE runners), the
     * search walks up from the current directory until a repository checkout
     * that contains the file is found.
     */
    private static Path findingCodesDoc() {
        Path expected = Paths.get("..", "docs", "superpowers", "kdb-finding-codes.md")
                .toAbsolutePath().normalize();
        if (Files.exists(expected)) {
            return expected;
        }
        Path directory = Paths.get("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("docs").resolve("superpowers").resolve("kdb-finding-codes.md");
            if (Files.exists(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        // Not found: return the surefire-relative expectation so the failure
        // message names the canonical location.
        return expected;
    }

    @Test
    void everyFindingCodeIsNamedInTheDocsTable() throws Exception {
        String doc = Files.readString(findingCodesDoc());
        for (KdbFindingCode code : KdbFindingCode.values()) {
            assertTrue(doc.contains("`" + code.name() + "`"),
                    "Finding code " + code + " is not documented in docs/superpowers/kdb-finding-codes.md");
        }
    }

    @Test
    void documentedSeverityMatchesTheCodeSeverity() throws Exception {
        List<String> lines = Files.readAllLines(findingCodesDoc());
        for (KdbFindingCode code : KdbFindingCode.values()) {
            Optional<String> row = lines.stream()
                    .filter(line -> line.contains("`" + code.name() + "`"))
                    .findFirst();
            assertTrue(row.isPresent(),
                    "Finding code " + code + " is not documented in docs/superpowers/kdb-finding-codes.md");
            assertTrue(row.get().contains(code.getSeverity().name()),
                    "Finding code " + code + " is documented with the wrong severity in row: " + row.get());
        }
    }
}
