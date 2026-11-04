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

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headful plumbing test for the real modal path of the modern dialog API
 * (#1341, spec §6.8): unlike the other dialog suites, which install a
 * capturing presenter and never show a dialog, this test exercises the
 * production {@link ModalPresenter} end to end - {@code showAndWait()}
 * blocks a worker thread inside {@code setVisible(true)}, a {@link Timer}
 * (which fires on the event dispatch thread) {@code doClick()}s a button
 * after the dialog was presented, and {@code showAndWait()} returns with the
 * decision.
 * <p>
 * The choreography is ordered, not sleep-based: the click timer is armed by
 * the window-opened event, so the click can only happen once the dialog is
 * actually presented; the delay only simulates a user deciding shortly after
 * it appeared. A watchdog timer releases a wedged {@code showAndWait()} after
 * a timeout, so a broken choreography surfaces as a failed assertion instead
 * of hanging the build. The brief's fallback (asserting {@code isModal()}
 * on a presented dialog instead of driving a real click) was not needed: the
 * real path ran reliably.
 * <p>
 * The presenter is JVM-static state, so the pre-test presenter is saved and
 * restored around every test. Showing a dialog requires a display, so the
 * suite skips in headless environments (mirroring {@code DialogChromeTest}).
 * No robot is used - the click is an event-driven {@code doClick()} on the
 * EDT.
 */
class HeadfulPlumbingTest {

    /**
     * Delay between the dialog being presented and the EDT clicking the
     * decision button. Ordering is guaranteed by arming the timer in the
     * window-opened event; this delay only lets the presented dialog settle.
     */
    private static final int CLICK_DELAY_MS = 250;

    /**
     * How long a presented but undecided dialog is left alone before the
     * watchdog releases the (wedged) {@code showAndWait()} call.
     */
    private static final int WATCHDOG_TIMEOUT_MS = 10_000;

    /**
     * How long the test waits for {@code showAndWait()} to return. Must
     * exceed the watchdog timeout so the watchdog path surfaces as a failed
     * assertion instead of a timed-out latch.
     */
    private static final long AWAIT_SECONDS = 15;

    /**
     * The dialog presented during the current test call (volatile: written
     * on the EDT during construction, read by the test and watchdog threads).
     */
    private volatile JIPipeDesktopDialog dialog;

    /**
     * The watchdog timer armed during the current test call (only touched by
     * the test thread).
     */
    private Timer watchdog;

    /**
     * The presenter installed before the test, restored afterwards
     * (JVM-static state).
     */
    private DialogPresenter savedPresenter;

    @BeforeAll
    static void boot() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                Assumptions.assumeTrue(false, "headless environment");
            }
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "AWT not available: " + t);
        }
        JIPipe.ensureInstance();
    }

    @BeforeEach
    void forceRealPresenter() {
        // Save whatever presenter is installed and force the production one:
        // this test exists to exercise the real modal path.
        savedPresenter = JIPipeDesktopDialog.getPresenter();
        JIPipeDesktopDialog.setPresenter(new ModalPresenter());
    }

    @AfterEach
    void restorePresenterAndCleanup() throws Exception {
        // Unwind the choreography even after a failed assertion: stop the
        // watchdog, hide (which releases a still-blocked showAndWait() on the
        // failure path) and dispose the dialog, and restore the presenter, so
        // no window, timer, or worker thread leaks into the next test.
        if (watchdog != null) {
            watchdog.stop();
            watchdog = null;
        }
        JIPipeDesktopDialog d = dialog;
        dialog = null;
        if (d != null) {
            SwingUtilities.invokeAndWait(() -> {
                d.setVisible(false);
                d.dispose();
            });
        }
        JIPipeDesktopDialog.setPresenter(savedPresenter);
        savedPresenter = null;
    }

    @Test
    void realModalOkClickReturnsValue() throws Exception {
        // Construct the dialog on the EDT. When the EDT reports the dialog as
        // presented (window opened), arm a one-shot EDT timer that decides
        // YES - the click therefore can only happen after presentation.
        SwingUtilities.invokeAndWait(() -> {
            JIPipeDesktopDialog d = new JIPipeDesktopDialog(null, "t");
            d.addMessage("m");
            d.setButtons(Button.YES, Button.NO);
            d.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    Timer click = new Timer(CLICK_DELAY_MS, ev -> d.getActionButtons().get(0).doClick());
                    click.setRepeats(false);
                    click.start();
                }
            });
            dialog = d;
        });

        // Watchdog: if the click never arrives, release the wedged
        // showAndWait() so the test fails on the assertions below instead of
        // hanging the build. Runs on the EDT; checks showing so a fire after
        // a decided (already hidden) dialog is a no-op.
        AtomicBoolean watchdogFired = new AtomicBoolean();
        watchdog = new Timer(WATCHDOG_TIMEOUT_MS, ev -> {
            JIPipeDesktopDialog d = dialog;
            if (d != null && d.isShowing()) {
                watchdogFired.set(true);
                d.setVisible(false);
            }
        });
        watchdog.setRepeats(false);

        // The production path: showAndWait() is called off the EDT, blocking
        // this worker thread inside ModalPresenter.show() until the dialog is
        // decided on the EDT.
        CountDownLatch returned = new CountDownLatch(1);
        AtomicReference<JIPipeDesktopDialog> returnedDialog = new AtomicReference<>();
        AtomicReference<Throwable> workerError = new AtomicReference<>();
        watchdog.start();
        Thread worker = new Thread(() -> {
            try {
                returnedDialog.set(dialog.showAndWait());
            } catch (Throwable t) {
                workerError.set(t);
            } finally {
                returned.countDown();
            }
        }, "headful-plumbing-showAndWait");
        worker.setDaemon(true);
        worker.start();

        assertTrue(returned.await(AWAIT_SECONDS, TimeUnit.SECONDS),
                "showAndWait() must return once the dialog was decided (click timer armed at window-opened; "
                        + "watchdog releases a wedged call after " + WATCHDOG_TIMEOUT_MS + " ms)");
        if (workerError.get() != null) {
            AssertionError error = new AssertionError("showAndWait() threw on the worker thread: " + workerError.get());
            error.initCause(workerError.get());
            throw error;
        }
        assertSame(dialog, returnedDialog.get(), "ModalPresenter must return the same dialog instance");
        assertFalse(watchdogFired.get(),
                "watchdog fired: the dialog was presented but never decided by the click timer");
        Optional<Button> result = dialog.getResult();
        assertTrue(result.isPresent(), "a decided dialog must report a result");
        assertSame(Button.YES, result.get(), "the result must be the exact Button instance that was clicked");
        worker.join(5_000);
        assertFalse(worker.isAlive(), "the worker thread must have finished after showAndWait() returned");
        assertFalse(dialog.isVisible(), "the decision must close (hide) the dialog");
    }

    /**
     * Input dialogs must open with the focus on their content - the text
     * field - instead of the OK button (the button panel is added before the
     * content in the chrome constructor, so container-order focus traversal
     * would pick the button; {@code showAndWait()} redirects the initial
     * focus to the content).
     * <p>
     * Same choreography as {@link #realModalOkClickReturnsValue()}: the
     * worker thread blocks inside the real {@code showAndWait()}, and a timer
     * armed at window-opened records the focus owner on the EDT and dismisses
     * the dialog to release the worker. The check runs well after the queued
     * {@code requestFocusInWindow()} redirect, so the assertion is
     * deterministic, not sleep-based.
     */
    @Test
    void inputDialogGivesInitialFocusToContent() throws Exception {
        AtomicReference<Component> focusOwner = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JIPipeDesktopDialog d = new JIPipeDesktopDialog(null, "t");
            d.addMessage("m");
            d.addContent(new JTextField("initial"));
            d.setCancelable(true);
            d.setButtons(Button.OK.asDefault());
            d.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    Timer check = new Timer(CLICK_DELAY_MS, ev -> {
                        focusOwner.set(d.getFocusOwner());
                        d.dismiss();
                    });
                    check.setRepeats(false);
                    check.start();
                }
            });
            dialog = d;
        });

        // Watchdog: release a wedged showAndWait() so a broken choreography
        // fails the assertions instead of hanging the build.
        watchdog = new Timer(WATCHDOG_TIMEOUT_MS, ev -> {
            JIPipeDesktopDialog d = dialog;
            if (d != null && d.isShowing()) {
                d.setVisible(false);
            }
        });
        watchdog.setRepeats(false);

        CountDownLatch returned = new CountDownLatch(1);
        AtomicReference<Throwable> workerError = new AtomicReference<>();
        watchdog.start();
        Thread worker = new Thread(() -> {
            try {
                dialog.showAndWait();
            } catch (Throwable t) {
                workerError.set(t);
            } finally {
                returned.countDown();
            }
        }, "headful-input-focus-showAndWait");
        worker.setDaemon(true);
        worker.start();

        assertTrue(returned.await(AWAIT_SECONDS, TimeUnit.SECONDS),
                "showAndWait() must return once the dialog was dismissed by the focus check");
        if (workerError.get() != null) {
            AssertionError error = new AssertionError("showAndWait() threw on the worker thread: " + workerError.get());
            error.initCause(workerError.get());
            throw error;
        }
        assertNotNull(focusOwner.get(), "the focus owner must have been recorded while the dialog was showing");
        // The redirect must have been invoked (code contract, environment
        // independent) and the window manager must have granted it (behavior
        // contract). If a CI window manager ever makes the grant flaky, the
        // first assertion is the documented fallback to keep.
        assertTrue(dialog.isContentFocusRequested(),
                "an input dialog must redirect the initial focus to its content (requestFocusInWindow invoked)");
        assertTrue(focusOwner.get() instanceof JTextField,
                "an input dialog must give the initial focus to its content (the text field), not the OK button - but was: "
                        + focusOwner.get());
        worker.join(5_000);
        assertFalse(worker.isAlive(), "the worker thread must have finished after showAndWait() returned");
    }
}
