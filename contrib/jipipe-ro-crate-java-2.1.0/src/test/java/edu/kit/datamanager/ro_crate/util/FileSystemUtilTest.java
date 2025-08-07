package edu.kit.datamanager.ro_crate.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemUtilTest {

    @ValueSource(strings = {
            "test",
            "test/",
            "test/test",
            "test/test/",
            "test/test/test",
            "test/test/test/"
    })
    @ParameterizedTest
    void ensureTrailingSlash(String value) {
        String result = FileSystemUtil.ensureTrailingSlash(value);
        assertTrue(result.endsWith("/"), "The result should end with a trailing slash.");
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void ensureTrailingSlashNull() {
        String result = FileSystemUtil.ensureTrailingSlash(null);
        assertNull(result, "The result should be null.");
    }
}