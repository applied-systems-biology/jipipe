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

package org.hkijena.jipipe.desktop.commons.components.tabs;

import org.junit.jupiter.api.Test;
import org.scijava.Disposable;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the load-bearing link of the window-close teardown: closing a project
 * window disposes the workbench's tab pane
 * ({@code JIPipeDesktopProjectWindow#dispose} →
 * {@code JIPipeDesktopProjectWorkbench#dispose} →
 * {@link JIPipeDesktopTabPane#dispose()}), and the tab pane is what disposes
 * every tab's {@link Disposable} content — the settings tab (its
 * {@code SettingsChangedEvent} subscription) and the graph editors (their
 * settings bindings). Without this link a closed window pins its whole UI tree
 * on the JVM-lifetime settings document.
 * <p>
 * Not a Swing structure test (AGENTS.md): it asserts the dispose cascade only.
 * The pane is not shown; the {@link javax.swing.Timer}s it starts (tab
 * animation) are left to the JVM, which is why the pane is disposed here.
 */
class JIPipeDesktopTabPaneDisposeTest {

    private static final class DisposableContent extends JPanel implements Disposable {
        private final AtomicInteger disposeCount = new AtomicInteger();

        @Override
        public void dispose() {
            disposeCount.incrementAndGet();
        }
    }

    @Test
    void disposeCallsDisposeOnEveryTabContent() {
        JIPipeDesktopTabPane pane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.Style.Top);
        DisposableContent first = new DisposableContent();
        DisposableContent second = new DisposableContent();
        pane.addTab("First", null, first, JIPipeDesktopTabPane.CloseMode.withSilentCloseButton);
        pane.addTab("Second", null, second, JIPipeDesktopTabPane.CloseMode.withSilentCloseButton);

        pane.dispose();

        assertEquals(1, first.disposeCount.get());
        assertEquals(1, second.disposeCount.get());
    }

    @Test
    void disposeAlsoReachesASingletonTabThatWasClosedEarlierInTheSession() {
        // Singleton tabs are created on demand and disappear from the visible
        // tab list when the user closes them; their content is kept for the
        // re-open path, so the pane-level dispose must still release it
        // (it disposes every registered tab before closing them).
        JIPipeDesktopTabPane pane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.Style.Top);
        DisposableContent content = new DisposableContent();
        pane.registerSingletonTab("SETTINGS", "Settings", null, () -> content,
                JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        pane.selectSingletonTab("SETTINGS");

        pane.dispose();

        assertEquals(1, content.disposeCount.get());
    }
}