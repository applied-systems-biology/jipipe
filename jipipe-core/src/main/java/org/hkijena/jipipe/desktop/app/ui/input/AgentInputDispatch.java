package org.hkijena.jipipe.desktop.app.ui.input;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Dispatch-mode input delivery for the agent UI operations (Layer 2, synthetic input):
 * the events are constructed as AWT {@link MouseEvent}, {@link MouseWheelEvent}, and
 * {@link KeyEvent} objects and delivered through the target component's
 * {@code dispatchEvent}. This is the deterministic default ({@code input-mode: "dispatch"});
 * it works without a display and is unaffected by display-server specifics. Its limitation:
 * components that poll the physical input state (native peers, the coming 3D viewers) do not
 * see these events; those surfaces need {@link AgentInputSystem} (Robot input).
 * <p>
 * Delivery is not effect: a completed method call confirms that the event reached the
 * component, not that the component did what the caller expected. The operations re-read
 * state to confirm.
 * <p>
 * All methods must be called on the event dispatch thread (the agent UI operations
 * guarantee this); coordinates are component-local.
 */
public final class AgentInputDispatch {

    private AgentInputDispatch() {
    }

    /**
     * The common contract of the two input modes: {@link AgentInputDispatch.Mode} delivers
     * synthetic AWT events on the event dispatch thread (default), {@link AgentInputSystem.Mode}
     * drives a {@link java.awt.Robot} off the event dispatch thread (opt-in). Declared here
     * instead of a top-level file so that the input package stays within its two planned files.
     */
    public sealed interface AgentInputMode permits AgentInputDispatch.Mode, AgentInputSystem.Mode {

        /**
         * The mode name as the agent UI operations report it: {@code "dispatch"} or {@code "system"}.
         */
        String name();

        /**
         * Clicks at the component-local point. A click is a {@code MOUSE_PRESSED}/{@code MOUSE_RELEASED}/
         * {@code MOUSE_CLICKED} triple; a multi-click is delivered like the real event stream, as one
         * triple per click with growing {@code clickCount} (1, 2, ...). The popup trigger flag of a
         * right click is set on the released and clicked events, not on the pressed event (the X11
         * and Windows convention), so popup checks wired to both press and release fire exactly once.
         *
         * @param target     the component that receives the events; also the event source
         * @param x          the x coordinate, component-local
         * @param y          the y coordinate, component-local
         * @param clickCount the number of clicks (2 = double click)
         * @param button     the button: {@link MouseEvent#BUTTON1}, {@link MouseEvent#BUTTON2},
         *                   or {@link MouseEvent#BUTTON3}
         */
        void click(Component target, int x, int y, int clickCount, int button);

        /**
         * Drags from the component-local {@code from} point to the component-local {@code to} point:
         * a {@code MOUSE_PRESSED} at {@code from}, ten interpolated {@code MOUSE_DRAGGED} events
         * (the last one at {@code to}), and a {@code MOUSE_RELEASED} at {@code to}.
         *
         * @param target the component that receives the events; also the event source
         * @param fromX  the start x coordinate, component-local
         * @param fromY  the start y coordinate, component-local
         * @param toX    the end x coordinate, component-local
         * @param toY    the end y coordinate, component-local
         * @param button the button held during the drag: {@link MouseEvent#BUTTON1},
         *               {@link MouseEvent#BUTTON2}, or {@link MouseEvent#BUTTON3}
         */
        void drag(Component target, int fromX, int fromY, int toX, int toY, int button);

        /**
         * Scrolls at the component-local point by the signed {@code amount} (positive scrolls
         * down/away from the user, negative up/towards the user, like the real wheel rotation).
         *
         * @param target the component that receives the events; also the event source
         * @param x      the x coordinate, component-local
         * @param y      the y coordinate, component-local
         * @param amount the signed scroll amount
         */
        void scroll(Component target, int x, int y, int amount);

        /**
         * Types the text into the keyboard focus owner, character by character. No Enter key
         * exists here: text only (an agent that needs Enter fires an action instead).
         *
         * @param focusOwner the component that receives the key events; the operations resolve
         *                   the keyboard focus owner and refuse to type when there is none
         * @param text       the text to type
         */
        void typeText(Component focusOwner, String text);
    }

    /**
     * The dispatch-mode implementation (its {@link #name()} is {@code "dispatch"}).
     */
    public static final class Mode implements AgentInputMode {

        /**
         * Into how many interpolated {@code MOUSE_DRAGGED} events a drag is split.
         */
        private static final int DRAG_STEPS = 10;

        /**
         * How many units one wheel rotation scrolls ({@code WHEEL_UNIT_SCROLL}). One unit per
         * rotation keeps every reader consistent: {@code getWheelRotation()} (for example the
         * graph canvas zoom reads its sign) and {@code getUnitsToScroll()} (the JDK scroll pane
         * implementation multiplies the units with the rotation) both yield the amount.
         */
        private static final int WHEEL_UNITS_PER_ROTATION = 1;

        /**
         * The old-style button modifier mask for a mouse button id. The {@link MouseEvent}
         * constructor normalizes these into the extended modifier bits (verified for JDK 21
         * and 25), so both {@code getModifiers()} readers and {@code getModifiersEx()} readers
         * (the {@code SwingUtilities.isXMouseButton} checks that JIPipe's listeners use) see
         * the button.
         */
        private static int buttonMask(int button) {
            return switch (button) {
                case MouseEvent.BUTTON1 -> InputEvent.BUTTON1_MASK;
                case MouseEvent.BUTTON2 -> InputEvent.BUTTON2_MASK;
                case MouseEvent.BUTTON3 -> InputEvent.BUTTON3_MASK;
                default -> throw new IllegalArgumentException("unsupported mouse button: " + button);
            };
        }

        Mode() {
        }

        @Override
        public String name() {
            return "dispatch";
        }

        @Override
        public void click(Component target, int x, int y, int clickCount, int button) {
            int mask = buttonMask(button);
            int clicks = Math.max(1, clickCount);
            boolean popupTrigger = button == MouseEvent.BUTTON3;
            for (int i = 1; i <= clicks; i++) {
                long when = System.currentTimeMillis();
                target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, when, mask,
                        x, y, i, false, button));
                target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, when, mask,
                        x, y, i, popupTrigger, button));
                target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_CLICKED, when, mask,
                        x, y, i, popupTrigger, button));
            }
        }

        @Override
        public void drag(Component target, int fromX, int fromY, int toX, int toY, int button) {
            int mask = buttonMask(button);
            target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    mask, fromX, fromY, 1, false, button));
            for (int i = 1; i <= DRAG_STEPS; i++) {
                target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                        mask, fromX + (toX - fromX) * i / DRAG_STEPS, fromY + (toY - fromY) * i / DRAG_STEPS,
                        1, false, MouseEvent.NOBUTTON));
            }
            target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                    mask, toX, toY, 1, false, button));
        }

        @Override
        public void scroll(Component target, int x, int y, int amount) {
            // Deviation from the plan's sketch ("the signed amount as both rotation and units"):
            // with the amount in both fields, getUnitsToScroll() (scroll units x rotation, the
            // distance the JDK scroll pane implementation scrolls by) would return the amount
            // squared. One unit per rotation keeps every reader at the amount.
            target.dispatchEvent(new MouseWheelEvent(target, MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                    0, x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, WHEEL_UNITS_PER_ROTATION, amount));
        }

        @Override
        public void typeText(Component focusOwner, String text) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                long when = System.currentTimeMillis();
                focusOwner.dispatchEvent(new KeyEvent(focusOwner, KeyEvent.KEY_PRESSED, when, 0,
                        KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED));
                focusOwner.dispatchEvent(new KeyEvent(focusOwner, KeyEvent.KEY_TYPED, when, 0,
                        KeyEvent.VK_UNDEFINED, c));
                focusOwner.dispatchEvent(new KeyEvent(focusOwner, KeyEvent.KEY_RELEASED, when, 0,
                        KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED));
            }
        }
    }

    /**
     * The dispatch-mode singleton; the agent UI operations deliver through
     * {@code AgentInputDispatch.MODE}.
     */
    public static final Mode MODE = new Mode();
}
