package machine.joystick;

import java.awt.event.KeyEvent;

import static machine.Keyboard.KEY_PRESSED_BIT0;
import static machine.Keyboard.KEY_PRESSED_BIT2;
import static machine.Keyboard.KEY_PRESSED_BIT3;
import static machine.Keyboard.KEY_PRESSED_BIT4;
import static machine.Keyboard.KEY_RELEASED_BIT0;
import static machine.Keyboard.KEY_RELEASED_BIT2;
import static machine.Keyboard.KEY_RELEASED_BIT3;
import static machine.Keyboard.KEY_RELEASED_BIT4;

public class CursorJoystick implements Joystick {

    public void handleKeyPressed(final int key, final boolean shiftPressed, final int[] rowKey) {

        switch (key) {
            case KeyEvent.VK_LEFT -> rowKey[3] &= KEY_PRESSED_BIT4;                         // 5  -- Left arrow
            case KeyEvent.VK_DOWN -> rowKey[4] &= KEY_PRESSED_BIT4;                         // 6  -- Down arrow
            case KeyEvent.VK_UP -> rowKey[4] &= KEY_PRESSED_BIT3;                           // 7  -- Up arrow
            case KeyEvent.VK_RIGHT -> rowKey[4] &= KEY_PRESSED_BIT2;                        // 8  -- Right arrow
            case KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH -> rowKey[4] &= KEY_PRESSED_BIT0;  // 0 -- Fire
        }
    }

    public void handleKeyReleased(final int key, final boolean shiftPressed, final int[] rowKey) {

        switch (key) {
            case KeyEvent.VK_LEFT -> rowKey[3] |= KEY_RELEASED_BIT4;                        // 5 -- Left arrow
            case KeyEvent.VK_DOWN -> rowKey[4] |= KEY_RELEASED_BIT4;                        // 6 -- Down arrow
            case KeyEvent.VK_UP -> rowKey[4] |= KEY_RELEASED_BIT3;                          // 7 -- Up arrow
            case KeyEvent.VK_RIGHT -> rowKey[4] |= KEY_RELEASED_BIT2;                       // 8 -- Right arrow
            case KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH -> rowKey[4] |= KEY_RELEASED_BIT0; // 0 -- Fire
        }
    }

}
