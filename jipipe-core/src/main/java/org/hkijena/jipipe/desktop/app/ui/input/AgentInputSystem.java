package org.hkijena.jipipe.desktop.app.ui.input;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * System-mode input delivery for the agent UI operations (Layer 2, synthetic input): a
 * {@link Robot} generates the physical OS-level input events. This is the opt-in
 * {@code input-mode: "system"} for surfaces that read the physical input state (the coming
 * 3D viewers, interactive annotation tools); synthetic AWT events delivered through
 * {@link AgentInputDispatch} are invisible to those. Requires a display: the agent UI
 * operations check {@link Mode#isAvailable()} before use and refuse the mode headless.
 * <p>
 * Threading (the {@code captureExecutor} pattern of {@code JIPipeDesktopMCPIntegration}):
 * the methods convert the component-local coordinates to screen coordinates on the calling
 * thread (the operations run on the event dispatch thread), then run the Robot sequence on
 * a private bounded executor with a wait bounded at {@link Mode#INPUT_TIMEOUT_SECONDS}. On
 * Wayland/XWayland hosts the JDK's Robot can block indefinitely (no exception, no timeout)
 * inside the xdg-desktop-portal; the bound turns such a hang into a failed operation
 * instead of a frozen application.
 * <p>
 * Delivery is not effect: a completed method call confirms that the physical input was
 * generated, not that the surface did what the caller expected.
 */
public final class AgentInputSystem {

    private AgentInputSystem() {
    }

    /**
     * The system-mode implementation (its {@link #name()} is {@code "system"}).
     */
    public static final class Mode implements AgentInputDispatch.AgentInputMode {

        /**
         * How long a Robot sequence may take before it is abandoned and reported as an
         * error (the bound of the capture executor pattern). A hung native call can pin its
         * pool thread until the display server responds; the executor bound keeps that leak
         * bounded (at most two hung threads).
         */
        private static final long INPUT_TIMEOUT_SECONDS = 15;

        /**
         * Numbers the {@code jipipe-mcp-input-N} threads.
         */
        private static final AtomicInteger INPUT_THREAD_ID = new AtomicInteger();

        /**
         * How long the shared Robot sleeps after generating an event. A zero-delay burst can
         * be coalesced or dropped by the window system, and a drag without separated motion
         * events would not show its intermediate positions.
         */
        private static final int AUTO_DELAY_MILLIS = 10;

        /**
         * Executes the Robot sequences off the event dispatch thread: a sequence blocked
         * inside the native input portal would otherwise freeze the whole application
         * (observed live for the Robot capture on this host). Daemon threads, two threads
         * (the captureExecutor pattern): when both threads hang, further sequences time out
         * after {@link #INPUT_TIMEOUT_SECONDS} and are reported as errors; the tool degrades,
         * the application never freezes.
         */
        private static final ExecutorService INPUT_EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "jipipe-mcp-input-" + INPUT_THREAD_ID.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });

        /**
         * Serializes the Robot sequences: the Robot synchronizes its individual methods, not
         * a whole sequence, so two concurrent sequences could otherwise interleave their
         * presses and releases.
         */
        private static final Object ROBOT_LOCK = new Object();

        /**
         * The shared Robot, created on first use inside the executor. Not created at class
         * initialization: a Robot cannot be created headless, and the class must stay
         * loadable (the operations decide mode availability through {@link #isAvailable()}).
         */
        private static Robot robot;

        /**
         * Into how many interpolated mouse moves a drag is split (the same count as the
         * dispatch mode).
         */
        private static final int DRAG_STEPS = 10;

        /**
         * The extended button mask for a mouse button id. The Robot requires the
         * {@code *_DOWN_MASK} values; the old-style masks that the dispatch mode pins for the
         * {@link MouseEvent} constructor are not applied by the Robot's native layer.
         */
        private static int robotButtonMask(int button) {
            return switch (button) {
                case MouseEvent.BUTTON1 -> InputEvent.BUTTON1_DOWN_MASK;
                case MouseEvent.BUTTON2 -> InputEvent.BUTTON2_DOWN_MASK;
                case MouseEvent.BUTTON3 -> InputEvent.BUTTON3_DOWN_MASK;
                default -> throw new IllegalArgumentException("unsupported mouse button: " + button);
            };
        }

        /**
         * The absolute screen point for a component-local point. Runs on the calling thread
         * (the Swing phase of the capture pattern) before the Robot sequence is submitted;
         * throws an {@link java.awt.IllegalComponentStateException} when the target is not
         * showing (the operations only resolve showing windows).
         */
        private static Point screenPoint(Component target, int x, int y) {
            Point location = target.getLocationOnScreen();
            location.translate(x, y);
            return location;
        }

        /**
         * The shared Robot, created on first use. Must be called while holding
         * {@link #ROBOT_LOCK} (every sequence does), which also publishes the instance safely.
         */
        private static Robot robot() {
            synchronized (ROBOT_LOCK) {
                if (robot == null) {
                    try {
                        robot = new Robot();
                    } catch (AWTException e) {
                        throw new IllegalStateException("The system input mode could not create a Robot: "
                                + e.getMessage(), e);
                    }
                    robot.setAutoDelay(AUTO_DELAY_MILLIS);
                }
                return robot;
            }
        }

        /**
         * Runs a Robot sequence on {@link #INPUT_EXECUTOR} and waits at most
         * {@link #INPUT_TIMEOUT_SECONDS}. A failure (the Robot refusing to start, the display
         * server blocking the native call, the timeout) surfaces as an
         * {@link IllegalStateException} so the calling operation reports a failed delivery
         * instead of hanging. An interrupted wait restores the interrupt flag.
         */
        private static void runBounded(Runnable sequence) {
            Future<?> future = INPUT_EXECUTOR.submit(sequence);
            try {
                future.get(INPUT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                throw new IllegalStateException("The system input was interrupted while it was delivered", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("The system input failed: " + e.getCause(), e.getCause());
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new IllegalStateException("The system input did not finish within " + INPUT_TIMEOUT_SECONDS
                        + " seconds; the display server may have blocked it, or another sequence is still hung");
            }
        }

        /**
         * A printable ASCII character mapped onto the virtual key code it is typed with, and
         * whether the key must be held with Shift (US layout).
         */
        private record KeyCode(int vk, boolean shift) {
        }

        /**
         * Maps a printable ASCII character onto the key code table: lowercase letters as
         * {@code VK_A} plus the offset, digits as {@code VK_0} plus the offset, space, and the
         * US-layout punctuation (shifted characters type their base key with Shift held).
         * Anything else throws (Enter is never sent: text only).
         */
        private static KeyCode keyCodeFor(char c) {
            if (c >= 'a' && c <= 'z') {
                return new KeyCode(KeyEvent.VK_A + (c - 'a'), false);
            }
            if (c >= 'A' && c <= 'Z') {
                return new KeyCode(KeyEvent.VK_A + (c - 'A'), true);
            }
            if (c >= '0' && c <= '9') {
                return new KeyCode(KeyEvent.VK_0 + (c - '0'), false);
            }
            return switch (c) {
                case ' ' -> new KeyCode(KeyEvent.VK_SPACE, false);
                case '`' -> new KeyCode(KeyEvent.VK_BACK_QUOTE, false);
                case '-' -> new KeyCode(KeyEvent.VK_MINUS, false);
                case '=' -> new KeyCode(KeyEvent.VK_EQUALS, false);
                case '[' -> new KeyCode(KeyEvent.VK_OPEN_BRACKET, false);
                case ']' -> new KeyCode(KeyEvent.VK_CLOSE_BRACKET, false);
                case '\\' -> new KeyCode(KeyEvent.VK_BACK_SLASH, false);
                case ';' -> new KeyCode(KeyEvent.VK_SEMICOLON, false);
                case '\'' -> new KeyCode(KeyEvent.VK_QUOTE, false);
                case ',' -> new KeyCode(KeyEvent.VK_COMMA, false);
                case '.' -> new KeyCode(KeyEvent.VK_PERIOD, false);
                case '/' -> new KeyCode(KeyEvent.VK_SLASH, false);
                case '~' -> new KeyCode(KeyEvent.VK_BACK_QUOTE, true);
                case '!' -> new KeyCode(KeyEvent.VK_1, true);
                case '@' -> new KeyCode(KeyEvent.VK_2, true);
                case '#' -> new KeyCode(KeyEvent.VK_3, true);
                case '$' -> new KeyCode(KeyEvent.VK_4, true);
                case '%' -> new KeyCode(KeyEvent.VK_5, true);
                case '^' -> new KeyCode(KeyEvent.VK_6, true);
                case '&' -> new KeyCode(KeyEvent.VK_7, true);
                case '*' -> new KeyCode(KeyEvent.VK_8, true);
                case '(' -> new KeyCode(KeyEvent.VK_9, true);
                case ')' -> new KeyCode(KeyEvent.VK_0, true);
                case '_' -> new KeyCode(KeyEvent.VK_MINUS, true);
                case '+' -> new KeyCode(KeyEvent.VK_EQUALS, true);
                case '{' -> new KeyCode(KeyEvent.VK_OPEN_BRACKET, true);
                case '}' -> new KeyCode(KeyEvent.VK_CLOSE_BRACKET, true);
                case '|' -> new KeyCode(KeyEvent.VK_BACK_SLASH, true);
                case ':' -> new KeyCode(KeyEvent.VK_SEMICOLON, true);
                case '"' -> new KeyCode(KeyEvent.VK_QUOTE, true);
                case '<' -> new KeyCode(KeyEvent.VK_COMMA, true);
                case '>' -> new KeyCode(KeyEvent.VK_PERIOD, true);
                case '?' -> new KeyCode(KeyEvent.VK_SLASH, true);
                default -> throw new IllegalArgumentException("unsupported character for system input: '" + c + "'");
            };
        }

        Mode() {
        }

        @Override
        public String name() {
            return "system";
        }

        /**
         * Whether the system mode can run: it needs a display (a Robot cannot be created
         * headless). The agent UI operations check this gate before use and refuse with a
         * clear error instead of a bare {@link AWTException}.
         */
        public boolean isAvailable() {
            return !GraphicsEnvironment.isHeadless();
        }

        @Override
        public void click(Component target, int x, int y, int clickCount, int button) {
            int mask = robotButtonMask(button);
            Point screen = screenPoint(target, x, y);
            int clicks = Math.max(1, clickCount);
            runBounded(() -> {
                synchronized (ROBOT_LOCK) {
                    Robot r = robot();
                    for (int i = 0; i < clicks; i++) {
                        r.mouseMove(screen.x, screen.y);
                        r.mousePress(mask);
                        r.mouseRelease(mask);
                    }
                }
            });
        }

        @Override
        public void drag(Component target, int fromX, int fromY, int toX, int toY, int button) {
            int mask = robotButtonMask(button);
            Point from = screenPoint(target, fromX, fromY);
            Point to = screenPoint(target, toX, toY);
            runBounded(() -> {
                synchronized (ROBOT_LOCK) {
                    Robot r = robot();
                    r.mouseMove(from.x, from.y);
                    r.mousePress(mask);
                    for (int i = 1; i <= DRAG_STEPS; i++) {
                        r.mouseMove(from.x + (to.x - from.x) * i / DRAG_STEPS,
                                from.y + (to.y - from.y) * i / DRAG_STEPS);
                    }
                    r.mouseRelease(mask);
                }
            });
        }

        @Override
        public void scroll(Component target, int x, int y, int amount) {
            Point screen = screenPoint(target, x, y);
            runBounded(() -> {
                synchronized (ROBOT_LOCK) {
                    Robot r = robot();
                    r.mouseMove(screen.x, screen.y);
                    r.mouseWheel(amount);
                }
            });
        }

        @Override
        public void typeText(Component focusOwner, String text) {
            // Map the characters on the calling thread, so an unsupported character surfaces
            // here with its exact error before any Robot event is generated. The focus owner
            // itself is not addressed by the Robot (physical key events go to the current
            // keyboard focus); the operations resolve it beforehand to refuse typing into
            // nothing.
            KeyCode[] codes = new KeyCode[text.length()];
            for (int i = 0; i < text.length(); i++) {
                codes[i] = keyCodeFor(text.charAt(i));
            }
            runBounded(() -> {
                synchronized (ROBOT_LOCK) {
                    Robot r = robot();
                    for (KeyCode code : codes) {
                        if (code.shift()) {
                            r.keyPress(KeyEvent.VK_SHIFT);
                        }
                        r.keyPress(code.vk());
                        r.keyRelease(code.vk());
                        if (code.shift()) {
                            r.keyRelease(KeyEvent.VK_SHIFT);
                        }
                    }
                }
            });
        }
    }

    /**
     * The system-mode singleton; the agent UI operations deliver through
     * {@code AgentInputSystem.MODE} after checking {@link Mode#isAvailable()}.
     */
    public static final Mode MODE = new Mode();
}
