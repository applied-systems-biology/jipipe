package edu.kit.datamanager.ro_crate.special;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class IdentifierUtilsTest {

    // Strings taken from
    // https://www.researchobject.org/ro-crate/1.1/data-entities.html#encoding-file-paths
    static final String FILE_CHINESE = "面试.mp4";
    static final String FILE_CHINESE_ENCODED = "%E9%9D%A2%E8%AF%95.mp4";
    static final String FILE_PATH_SPACES_WINDOWS = "Results and Diagrams\\almost-50%.png";
    static final String FILE_PATH_SPACES_UNIX = "Results and Diagrams/almost-50%.png";
    // note: "\" becomes "/" (unix style):
    static final String FILE_PATH_SPACES_ENCODED = "Results%20and%20Diagrams/almost-50%25.png";

    // taken from
    // https://www.researchobject.org/ro-crate/1.1/appendix/jsonld.html#describing-entities-in-json-ld
    static final String URL_SIMPLE_VALID = "http://example.com/";
    static final String URL_ORCID_VALID = "https://orcid.org/0000-0002-1825-0097";
    static final String ENTITY_REFERENCE_SIMPLE = "#alice";
    static final String ENTITY_REFERENCE_GENERATED = "#ac0bd781-7d91-4cdf-b2ad-7305921c7650";
    static final String ENTITY_BLANK_NODE_ID = "_:alice";

    // Other tests
    static final String URL_WITH_SPACES = "https://example.com/file with spaces";
    static final String URL_WITH_SPACES_ENCODED = "https://example.com/file%20with%20spaces";
    static final String URL_WITH_SPECIAL_CHARS = "https://example.com/break§ stuff + code<>/";
    static final String URL_WITH_SPECIAL_CHARS_ENCODED = "https://example.com/break%C2%A7%20stuff%20%2B%20code%3C%3E/";

    public static Stream<Arguments> encodingExamplesProvider() {
        return Stream.of(
                // before encoding , after encoding
                Arguments.of(FILE_PATH_SPACES_UNIX, FILE_PATH_SPACES_ENCODED),
                Arguments.of(FILE_PATH_SPACES_WINDOWS, FILE_PATH_SPACES_ENCODED),
                Arguments.of(URL_WITH_SPACES, URL_WITH_SPACES_ENCODED),
                Arguments.of(URL_WITH_SPECIAL_CHARS, URL_WITH_SPECIAL_CHARS_ENCODED),
                // some things should stay as they are according to the specification
                Arguments.of(FILE_CHINESE, FILE_CHINESE),
                Arguments.of(URL_SIMPLE_VALID, URL_SIMPLE_VALID),
                Arguments.of(URL_ORCID_VALID, URL_ORCID_VALID),
                Arguments.of(ENTITY_REFERENCE_SIMPLE, ENTITY_REFERENCE_SIMPLE),
                Arguments.of(ENTITY_REFERENCE_GENERATED, ENTITY_REFERENCE_GENERATED),
                Arguments.of(ENTITY_BLANK_NODE_ID, ENTITY_BLANK_NODE_ID)
        );
    }
    
    
    public static Stream<Arguments> decodingExamplesProvider() {
        return Stream.of(
                // after decoding , before decoding
                // Strings which will be decoded (encoded strings)
                Arguments.of(URL_WITH_SPACES, URL_WITH_SPACES_ENCODED),
                Arguments.of(URL_WITH_SPECIAL_CHARS, URL_WITH_SPECIAL_CHARS_ENCODED),
                // Strings which are already decoded
                Arguments.of(FILE_PATH_SPACES_UNIX, FILE_PATH_SPACES_UNIX),
                Arguments.of(FILE_PATH_SPACES_WINDOWS, FILE_PATH_SPACES_WINDOWS),
                Arguments.of(URL_WITH_SPACES, URL_WITH_SPACES),
                Arguments.of(URL_WITH_SPECIAL_CHARS, URL_WITH_SPECIAL_CHARS),
                // some things should stay as they are according to the specification
                Arguments.of(FILE_CHINESE, FILE_CHINESE),
                Arguments.of(URL_SIMPLE_VALID, URL_SIMPLE_VALID),
                Arguments.of(URL_ORCID_VALID, URL_ORCID_VALID),
                Arguments.of(ENTITY_REFERENCE_SIMPLE, ENTITY_REFERENCE_SIMPLE),
                Arguments.of(ENTITY_REFERENCE_GENERATED, ENTITY_REFERENCE_GENERATED),
                Arguments.of(ENTITY_BLANK_NODE_ID, ENTITY_BLANK_NODE_ID)
        );
    }

    /**
     * Test if isValidUri differentiates identifiers between
     * "allowed to use" and "not allowed to use" in a crate.
     */
    @Test
    void testIsValidUriWithRoCrateSpecExamples() {
        // Chinese characters are preferred (readability),
        assertTrue(IdentifierUtils.isValidUri(FILE_CHINESE));
        // But the encoded version is also fine.
        assertTrue(IdentifierUtils.isValidUri(FILE_CHINESE_ENCODED));
        // Spaces are not considered valid,
        assertFalse(IdentifierUtils.isValidUri(FILE_PATH_SPACES_UNIX));
        assertFalse(IdentifierUtils.isValidUri(FILE_PATH_SPACES_WINDOWS));
        assertFalse(IdentifierUtils.isValidUri(URL_WITH_SPACES));
        // so we need to encode them.
        assertTrue(IdentifierUtils.isValidUri(FILE_PATH_SPACES_ENCODED));
        assertTrue(IdentifierUtils.isValidUri(URL_WITH_SPACES_ENCODED));
    }

    /**
     * The examples contains a list of encoded identifiers we should test.
     * 
     * @param exampleUnencoded is maybe not encoded, but maybe it already is.
     * @param exampleEncoded is guaranteed to be encoded
     */
    @ParameterizedTest(name = "testIsValidUriWithEncodingExamples {0} and {1}")
    @MethodSource("edu.kit.datamanager.ro_crate.special.IdentifierUtilsTest#encodingExamplesProvider")
    void testIsValidUriWithEncodingExamples(String exampleUnencoded, String exampleEncoded) {
        assertTrue(IdentifierUtils.isValidUri(exampleEncoded));
        if (exampleUnencoded != exampleEncoded) {
            // If we have examples where the encoding is different than the original,
            // this means the uri was not valid before.
            assertFalse(IdentifierUtils.isValidUri(exampleUnencoded));
        }
    }

    /**
     * Detecting URLs works with the examples from the specification.
     */
    @Test
    void testIsUrlWithRoCrateSpecExamples() {
        assertFalse(IdentifierUtils.isUrl(FILE_CHINESE));
        assertFalse(IdentifierUtils.isUrl(FILE_CHINESE_ENCODED));
        assertFalse(IdentifierUtils.isUrl(FILE_PATH_SPACES_ENCODED));
    }

    /**
     * Detecting paths works with the examples from the specification.
     */
    @Test
    void testIsPathWithRoCrateSpecExamples() {
        assertTrue(IdentifierUtils.isPath(FILE_CHINESE));
        assertTrue(IdentifierUtils.isPath(FILE_CHINESE_ENCODED));
        assertTrue(IdentifierUtils.isPath(FILE_PATH_SPACES_ENCODED));

    }

    /**
     * Detecting and differentiating given URLs from paths works.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            // unencoded:
            URL_WITH_SPACES,
            // encoded:
            URL_SIMPLE_VALID,
            URL_ORCID_VALID,
            URL_WITH_SPACES_ENCODED,
            "https://example.com/file.html",
            "http://example-doesnotexist.com",
    })
    void testIsPathAndisUrlWithUrls(String url) {
        assertFalse(IdentifierUtils.isPath(url));
        assertTrue(IdentifierUtils.isUrl(url));
    }

    /**
     * Detecting and differentiating given paths from URLs works.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            // unencoded:
            FILE_PATH_SPACES_UNIX,
            FILE_PATH_SPACES_WINDOWS,
            // encoded:
            FILE_CHINESE,
            FILE_PATH_SPACES_ENCODED,
            FILE_CHINESE_ENCODED,
            FILE_PATH_SPACES_ENCODED,
            "./file.html",
            "file.html",
    })
    void testIsPathAndisUrlWithPaths(String path) {
        assertTrue(IdentifierUtils.isPath(path));
        assertFalse(IdentifierUtils.isUrl(path));
    }

    /**
     * Tests the encoding function for several "before-after" pairs.
     * 
     * This includes normal tests,
     * checks that double-encoding does not happen,
     * and makes sure that some examples and exceptions of the specification are
     * handled correctly.
     */
    @ParameterizedTest(name = "testEncodeWith {0} and {1}")
    @MethodSource("edu.kit.datamanager.ro_crate.special.IdentifierUtilsTest#encodingExamplesProvider")
    void testEncode(String exampleUnencoded, String exampleEncoded) {
        Optional<String> encoded = IdentifierUtils.encode(exampleUnencoded);
        assertEquals(exampleEncoded, encoded.get());
        assertTrue(IdentifierUtils.isValidUri(encoded.get()));
    }

    /**
     * Same as testEncode, but with the decode function.
     * 
     * Uses the same input examples, but in the reverse direction.
     */
    @ParameterizedTest(name = "testDecodeWith {0} and {1}")
    @MethodSource("edu.kit.datamanager.ro_crate.special.IdentifierUtilsTest#decodingExamplesProvider")
    void testDecode(String exampleUnencoded, String exampleEncoded) {
        Optional<String> decoded = IdentifierUtils.decode(exampleEncoded);
        assertEquals(exampleUnencoded, decoded.get());
    }
}
