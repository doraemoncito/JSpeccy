package machine.joystick;

import java.awt.event.KeyEvent;

import static machine.Keyboard.KEY_PRESSED_BIT0;

public class NoneJoystick extends CursorJoystick {

    public void handleKeyPressed(final int key, final boolean shiftPressed, final int[] rowKey) {
        if ((!shiftPressed) && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_UP || key == KeyEvent.VK_RIGHT))
            rowKey[0] &= KEY_PRESSED_BIT0;  // Caps

        super.handleKeyPressed(key, shiftPressed, rowKey);
    }

    public void handleKeyReleased(final int key, final boolean shiftPressed, final int[] rowKey) {

        if ((!shiftPressed) && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_UP || key == KeyEvent.VK_RIGHT))
            rowKey[0] |= KEY_PRESSED_BIT0;  // Caps

        super.handleKeyReleased(key, shiftPressed, rowKey);
    }

}
