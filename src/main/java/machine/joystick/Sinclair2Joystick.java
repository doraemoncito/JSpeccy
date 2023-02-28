package machine.joystick;

import java.awt.event.KeyEvent;

import static machine.Keyboard.KEY_PRESSED_BIT0;
import static machine.Keyboard.KEY_PRESSED_BIT1;
import static machine.Keyboard.KEY_PRESSED_BIT2;
import static machine.Keyboard.KEY_PRESSED_BIT3;
import static machine.Keyboard.KEY_PRESSED_BIT4;
import static machine.Keyboard.KEY_RELEASED_BIT0;
import static machine.Keyboard.KEY_RELEASED_BIT1;
import static machine.Keyboard.KEY_RELEASED_BIT2;
import static machine.Keyboard.KEY_RELEASED_BIT3;
import static machine.Keyboard.KEY_RELEASED_BIT4;

public class Sinclair2Joystick implements Joystick {

    public void handleKeyPressed(final int key, final boolean shiftPressed, final int[] rowKey) {
        switch (key) {
            case KeyEvent.VK_LEFT -> rowKey[3] &= KEY_PRESSED_BIT0;                         // 1 -- Left
            case KeyEvent.VK_DOWN -> rowKey[3] &= KEY_PRESSED_BIT2;                         // 3 -- Down
            case KeyEvent.VK_UP -> rowKey[3] &= KEY_PRESSED_BIT3;                           // 4 -- Up
            case KeyEvent.VK_RIGHT -> rowKey[3] &= KEY_PRESSED_BIT1;                        // 2 -- Right
            case KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH -> rowKey[3] &= KEY_PRESSED_BIT4;  // 5 -- Fire
        }
    }

    public void handleKeyReleased(final int key, final boolean shiftPressed, final int[] rowKey) {

        switch (key) {
            case KeyEvent.VK_LEFT -> rowKey[3] |= KEY_RELEASED_BIT0;                        // 1 -- Left
            case KeyEvent.VK_DOWN -> rowKey[3] |= KEY_RELEASED_BIT2;                        // 3 -- Down
            case KeyEvent.VK_UP -> rowKey[3] |= KEY_RELEASED_BIT3;                          // 4 -- Up
            case KeyEvent.VK_RIGHT -> rowKey[3] |= KEY_RELEASED_BIT1;                       // 2 -- Right
            case KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH -> rowKey[3] |= KEY_RELEASED_BIT4; // 5  -- Fire
        }
    }

}
