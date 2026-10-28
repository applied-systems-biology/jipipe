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

package org.hkijena.jipipe.api.service;

import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeSettingsServiceComponentTest {

    @TempDir
    Path tempDir;

    @Test void documentsAreScopeSeparated() {
        // Construct the component against a temp user dir override is not possible in a pure unit test;
        // this test uses the injectable file resolver constructor instead.
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        assertNotSame(c.document(SettingsScope.APPLICATION), c.document(SettingsScope.RUNTIME));
        assertEquals(SettingsScope.APPLICATION, c.document(SettingsScope.APPLICATION).scope());
    }

    @Test void registeredKeyIsResolvable() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        JIPipeSettingKey<Boolean> k = JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true);
        c.registerKey(k);
        assertSame(k, c.keyByBlockAndSubPath("registry:x", "a"));
    }

    @Test void bindingRoundTripsThroughSaveAndReload() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        SettingsBinding<Boolean> b = c.bind(JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true));
        b.set(false);
        c.save();
        c.reload();
        assertFalse(c.bind(JIPipeSettingKey.bool("registry:x", "a", Boolean.class).defaultValue(true)).get());
    }

    @Test void availableScopesAlwaysContainAllThree() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        assertEquals(Set.of(SettingsScope.APPLICATION, SettingsScope.PROJECT, SettingsScope.RUNTIME), c.availableScopes());
    }

    @Test void testingFactoryDoesNotTouchTheService() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        // The testing factory constructs without a JIPipeService: the service must be null,
        // and every service-dependent accessor must be null-guarded instead of throwing NPE.
        assertNull(c.getService());
        assertNull(c.getProgressInfo());
        // PROJECT has no file on disk; the in-memory document is still reachable without a service.
        assertNull(c.fileFor(SettingsScope.PROJECT));
        assertNotNull(c.document(SettingsScope.PROJECT));
        // The change event emitters are usable standalone.
        assertNotNull(c.getChangedEventEmitter());
        assertNotNull(c.getLegacyChangedEventEmitter());
    }

    @Test void fileForMapsScopesToProfileFiles() {
        JIPipeSettingsServiceComponent c = JIPipeSettingsServiceComponent.forTesting(tempDir);
        assertEquals(tempDir.resolve("settings.json"), c.fileFor(SettingsScope.APPLICATION));
        assertEquals(tempDir.resolve("runtime-settings.json"), c.fileFor(SettingsScope.RUNTIME));
        assertNull(c.fileFor(SettingsScope.PROJECT));
    }
}
