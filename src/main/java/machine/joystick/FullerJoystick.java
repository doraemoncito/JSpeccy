package machine.joystick;

import lombok.Getter;
import lombok.Setter;

import java.awt.event.KeyEvent;

import static machine.Keyboard.KEY_PRESSED_BIT0;
import static machine.Keyboard.KEY_PRESSED_BIT1;
import static machine.Keyboard.KEY_PRESSED_BIT2;
import static machine.Keyboard.KEY_PRESSED_BIT3;
import static machine.Keyboard.KEY_PRESSED_BIT7;
import static machine.Keyboard.KEY_RELEASED_BIT0;
import static machine.Keyboard.KEY_RELEASED_BIT1;
import static machine.Keyboard.KEY_RELEASED_BIT2;
import static machine.Keyboard.KEY_RELEASED_BIT3;
import static machine.Keyboard.KEY_RELEASED_BIT7;

public class FullerJoystick implements Joystick {

    @Getter
    @Setter
    private int fuller = 0;

    public void handleKeyPressed(final int key, final boolean shiftPressed, final int[] rowKey) {

        switch (key) {
            case KeyEvent.VK_LEFT -> fuller &= KEY_PRESSED_BIT2;
            case KeyEvent.VK_DOWN -> fuller &= KEY_PRESSED_BIT1;
            case KeyEvent.VK_UP -> fuller &= KEY_PRESSED_BIT0;
            case KeyEvent.VK_RIGHT -> fuller &= KEY_PRESSED_BIT3;
            case KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH -> fuller &= KEY_PRESSED_BIT7;
        }
    }

    public void handleKeyReleased(final int key, final boolean shiftPressed, final int[] rowKey) {

        switch (key) {
            case KeyEvent.VK_LEFT -> fuller |= KEY_RELEASED_BIT2;
            case KeyEvent.VK_DOWN -> fuller |= KEY_RELEASED_BIT1;
            case KeyEvent.VK_UP -> fuller |= KEY_RELEASED_BIT0;
            case KeyEvent.VK_RIGHT -> fuller |= KEY_RELEASED_BIT3;
            case KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH -> fuller |= KEY_RELEASED_BIT7;
        }
    }

}
