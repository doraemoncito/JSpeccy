package machine.joystick;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

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

public class KempstonJoystick implements Joystick {

    @Getter
    @Setter
    private int kempston = 0;

    public void handleKeyPressed(final int key, final boolean shiftPressed, final int[] rowKey) {

        switch (key) {
            case KeyEvent.VK_LEFT -> kempston |= KEY_RELEASED_BIT1;
            case KeyEvent.VK_DOWN -> kempston |= KEY_RELEASED_BIT2;
            case KeyEvent.VK_UP -> kempston |= KEY_RELEASED_BIT3;
            case KeyEvent.VK_RIGHT -> kempston |= KEY_RELEASED_BIT0;
            case KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH -> kempston |= KEY_RELEASED_BIT4;
        }
    }

    public void handleKeyReleased(final int key, final boolean shiftPressed, final int[] rowKey) {

        switch (key) {
            case KeyEvent.VK_LEFT -> kempston &= KEY_PRESSED_BIT1;
            case KeyEvent.VK_DOWN -> kempston &= KEY_PRESSED_BIT2;
            case KeyEvent.VK_UP -> kempston &= KEY_PRESSED_BIT3;
            case KeyEvent.VK_RIGHT -> kempston &= KEY_PRESSED_BIT0;
            case KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH -> kempston &= KEY_PRESSED_BIT4;
        }
    }

}
