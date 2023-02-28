/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import joystickinput.JoystickRaw;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import machine.joystick.CursorJoystick;
import machine.joystick.FullerJoystick;
import machine.joystick.Joystick;
import machine.joystick.KempstonJoystick;
import machine.joystick.NoneJoystick;
import machine.joystick.Sinclair1Joystick;
import machine.joystick.Sinclair2Joystick;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

/**
 *
 * @author jsanchez
 */
@Slf4j
public class Keyboard implements KeyListener {

    public enum JoystickModel {

        NONE(new NoneJoystick()),
        KEMPSTON(new KempstonJoystick()),
        SINCLAIR1(new Sinclair1Joystick()),
        SINCLAIR2(new Sinclair2Joystick()),
        CURSOR(new CursorJoystick()),
        FULLER(new FullerJoystick());

        @Getter
        private final Joystick joystick;

        JoystickModel(final Joystick joystick) {
            this.joystick = joystick;
        }

    }

    private final int[] rowKey = new int[8];
    private int sjs1, sjs2;
    private boolean shiftPressed, mapPCKeys;
    private final boolean winBug;
    private final KeyEvent[] keyEventPending = new KeyEvent[8];

    private JoystickModel joystickModel, shadowJoystick;
    private final JoystickRaw joystick1, joystick2;
    private boolean rzxEnabled = false;

    /*
     * Spectrum Keyboard Map
     *
     * PORT  |  BIT 4   3   2   1   0
     * -----------------------------------------
     * 254 (FEh)    V   C   X   Z   CAPS
     * -----------------------------------------
     * 253 (FDh)    G   F   D   S   A
     * -----------------------------------------
     * 251 (FBh)    T   R   E   W   Q
     * -----------------------------------------
     * 247 (F7h)    5   4   3   2   1
     * -----------------------------------------
     * 239 (EFh)    6   7   8   9   0
     * -----------------------------------------
     * 223 (FDh)    Y   U   I   O   P
     * -----------------------------------------
     * 191 (BFh)    H   J   K   L   ENTER
     * -----------------------------------------
     * 127 (7Fh)    B   N   M   SYM BREAK/SPACE
     * -----------------------------------------
     * 
     */
    public static final int KEY_PRESSED_BIT0 = 0xfe;
    public static final int KEY_PRESSED_BIT1 = 0xfd;
    public static final int KEY_PRESSED_BIT2 = 0xfb;
    public static final int KEY_PRESSED_BIT3 = 0xf7;
    public static final int KEY_PRESSED_BIT4 = 0xef;
    public static final int KEY_PRESSED_BIT7 = 0x7f;  // for Fuller fire button
    public static final int KEY_RELEASED_BIT0 = 0x01;
    public static final int KEY_RELEASED_BIT1 = 0x02;
    public static final int KEY_RELEASED_BIT2 = 0x04;
    public static final int KEY_RELEASED_BIT3 = 0x08;
    public static final int KEY_RELEASED_BIT4 = 0x10;
    public static final int KEY_RELEASED_BIT5 = 0x20; // for Kempston fire button 2
    public static final int KEY_RELEASED_BIT6 = 0x40; // for Kempston fire button 3
    public static final int KEY_RELEASED_BIT7 = 0x80; // for Fuller fire button

    public Keyboard(configuration.KeyboardJoystickType config, JoystickRaw joy1, JoystickRaw joy2) {
        reset();
        setJoystickModel(config.getJoystickModel());
        mapPCKeys = config.isMapPCKeys();
        winBug = System.getProperty("os.name").contains("Windows");
        rzxEnabled = config.isRecreatedZX();
        joystick1 = joy1;
//        if (joystick1 != null) {
//            joystick1.addButtonListener(null);
//        }
        joystick2 = joy2;
    }

    public final void reset() {
        Arrays.fill(rowKey, 0xff);
        shiftPressed = false;
        ((KempstonJoystick) JoystickModel.KEMPSTON.getJoystick()).setKempston(0);
        ((FullerJoystick) JoystickModel.FULLER.getJoystick()).setFuller(0xff);
        sjs1 = sjs2 = 0xff;
        Arrays.fill(keyEventPending, null);
    }

    public final JoystickModel getJoystickModel() {
        return joystick1 == null ? joystickModel : shadowJoystick;
    }

    public final void setJoystickModel(JoystickModel model) {
        ((KempstonJoystick) JoystickModel.KEMPSTON.getJoystick()).setKempston(0);
        ((FullerJoystick) JoystickModel.FULLER.getJoystick()).setFuller(0xff);
        sjs1 = sjs2 = 0xff;

        if (joystick1 != null) {
            shadowJoystick = model;

            switch (shadowJoystick) {
                case SINCLAIR1 -> joystickModel = (joystick2 == null) ? JoystickModel.SINCLAIR2 : JoystickModel.NONE;
                case SINCLAIR2 -> joystickModel = (joystick2 == null) ? JoystickModel.SINCLAIR1 : JoystickModel.NONE;
                default -> joystickModel = JoystickModel.NONE;
            }
        } else {
            joystickModel = model;
            shadowJoystick = JoystickModel.NONE;
        }
    }

    public final void setJoystickModel(final int model) {
        switch (model) {
            case 1 -> setJoystickModel(JoystickModel.KEMPSTON);
            case 2 -> setJoystickModel(JoystickModel.SINCLAIR1);
            case 3 -> setJoystickModel(JoystickModel.SINCLAIR2);
            case 4 -> setJoystickModel(JoystickModel.CURSOR);
            case 5 -> setJoystickModel(JoystickModel.FULLER);
            default -> setJoystickModel(JoystickModel.NONE);
        }
    }

    public boolean isMapPCKeys() {
        return mapPCKeys;
    }

    public void setMapPCKeys(boolean state) {
        mapPCKeys = state;
        shiftPressed = false;
        Arrays.fill(keyEventPending, null);
    }

    public boolean isRZXEnabled() {
        return rzxEnabled;
    }

    public void setRZXEnabled(boolean state) {
        rzxEnabled = state;
    }

    public int readKempstonPort() {
        if (joystick1 == null) {
            return ((KempstonJoystick) JoystickModel.KEMPSTON.getJoystick()).getKempston();
        }

        // Standard Kempston Port
//        int state = joystick1.getButtonMask();
//        if (state == 0) {
//            return 0x00;
//        }
//
        int buttons = 0;
//        if ((state & 0x20) != 0) {
//            buttons |= KEY_RELEASED_BIT0; // Kempston Right
//        }
//        if ((state & 0x80) != 0) {
//            buttons |= KEY_RELEASED_BIT1; // Kempston Left
//        }
//        if ((state & 0x40) != 0) {
//            buttons |= KEY_RELEASED_BIT2; // Kempston Down
//        }
//        if ((state & 0x10) != 0) {
//            buttons |= KEY_RELEASED_BIT3; // Kempston Up
//        }
//        if ((state & 0x6000) != 0) {
//            buttons |= KEY_RELEASED_BIT4; // Kempston Fire
//        }
//        if ((state & 0x8000) != 0) {
//            buttons |= KEY_RELEASED_BIT5; // Kempston Fire 2
//        }
//        if ((state & 0x1000) != 0) {
//            buttons |= KEY_RELEASED_BIT6; // Kempston Fire 3
//        }
        int coord = joystick1.getAxisValue(0);
        if (coord > 24575) {
            buttons |= KEY_RELEASED_BIT0; // Kempston Right
        } else if (coord < -24575) {
            buttons |= KEY_RELEASED_BIT1; // Kempston Left
        }

        coord = joystick1.getAxisValue(1);
        if (coord > 24575) {
            buttons |= KEY_RELEASED_BIT2; // Kempston Down
        } else if (coord < -24575) {
            buttons |= KEY_RELEASED_BIT3; // Kempston Up
        }

        int state = joystick1.getButtonMask();
        if (state == 0) {
            return buttons;
        }

        if ((state & 0x6000) != 0) {
            buttons |= KEY_RELEASED_BIT4; // Kempston Fire
        }
        if ((state & 0x8000) != 0) {
            buttons |= KEY_RELEASED_BIT5; // Kempston Fire 2
        }
        if ((state & 0x1000) != 0) {
            buttons |= KEY_RELEASED_BIT6; // Kempston Fire 3
        }
        return buttons;
    }

    public int readFullerPort() {
        if (joystick1 == null) {
            return ((FullerJoystick) JoystickModel.FULLER.getJoystick()).getFuller();
        }

        int state = joystick1.getButtonMask();
        if (state == 0)
            return 0xff;

        int buttons = 0xff;
        if ((state & 0x20) != 0) {
            buttons &= KEY_PRESSED_BIT4; // Fuller Right
        }
        if ((state & 0x80) != 0) {
            buttons &= KEY_PRESSED_BIT2; // Fuller Left
        }
        if ((state & 0x40) != 0) {
            buttons &= KEY_PRESSED_BIT1; // Fuller Down
        }
        if ((state & 0x10) != 0) {
            buttons &= KEY_PRESSED_BIT0; // Fuller Up
        }
        if ((state & 0xF000) != 0) {
            buttons &= KEY_PRESSED_BIT7; // Fuller Fire
        }
        return buttons;
    }

    private void joystickToSJS1(int state) {
        sjs1 = 0xff;

        if (state == 0)
            return;

        if ((state & 0x80) != 0) {
            sjs1 &= KEY_PRESSED_BIT4; // Sinclair 2 Left (6)
        }
        if ((state & 0x20) != 0) {
            sjs1 &= KEY_PRESSED_BIT3; // Sinclair 2 Right (7)
        }
        if ((state & 0x40) != 0) {
            sjs1 &= KEY_PRESSED_BIT2; // Sinclair 2 Down (8)
        }
        if ((state & 0x10) != 0) {
            sjs1 &= KEY_PRESSED_BIT1; // Sinclair 2 Up (9)
        }
        if ((state & 0xF000) != 0) {
            sjs1 &= KEY_PRESSED_BIT0; // Sinclair 2 Fire (0)
        }
    }

    private void joystickToSJS2(int state) {
        sjs2 = 0xff;

        if (state == 0)
            return;

        if ((state & 0x80) != 0) {
            sjs2 &= KEY_PRESSED_BIT0; // Sinclair 1 Left (1)
        }
        if ((state & 0x20) != 0) {
            sjs2 &= KEY_PRESSED_BIT1; // Sinclair 1 Right (2)
        }
        if ((state & 0x40) != 0) {
            sjs2 &= KEY_PRESSED_BIT2; // Sinclair 1 Down (3)
        }
        if ((state & 0x10) != 0) {
            sjs2 &= KEY_PRESSED_BIT3; // Sinclair 1 Up (4)
        }
        if ((state & 0xF000) != 0) {
            sjs2 &= KEY_PRESSED_BIT4; // Sinclair 1 Fire (5)
        }
    }

    private void joystickToCursor() {
        sjs1 = sjs2 = 0xff;

        int state = joystick1.getButtonMask();
        if (state == 0)
            return;

        if ((state & 0x80) != 0) {
            sjs2 &= KEY_PRESSED_BIT4; // Cursor Left (5)
        }
        if ((state & 0x20) != 0) {
            sjs1 &= KEY_PRESSED_BIT2; // Cursor Right (8)
        }
        if ((state & 0x40) != 0) {
            sjs1 &= KEY_PRESSED_BIT4; // Cursor Down (6)
        }
        if ((state & 0x10) != 0) {
            sjs1 &= KEY_PRESSED_BIT3; // Cursor Up (7)
        }
        if ((state & 0xF000) != 0) {
            sjs1 &= KEY_PRESSED_BIT0; // Cursor Fire (0)
        }
    }

    public int readKeyboardPort(int port, boolean mapJoysticks) {
        int keys = 0xff;
        int res = port >>> 8;

        // When a second joystick is present:
        // - If J1 is emulating Kempston, J2 emulates Sinclair1
        // - If J1 is emulating Sinclair1, J2 emulates Sinclair2
        // - If J1 is emulating Sinclair2, J2 emulates Sinclair1
        if (mapJoysticks && joystick1 != null) {
            switch (shadowJoystick) {
                case KEMPSTON -> {
                    if (joystick2 != null && res == 0xef)
                        joystickToSJS1(joystick2.getButtonMask());
                }
                case SINCLAIR1 -> {
                    if (res == 0xef)
                        joystickToSJS1(joystick1.getButtonMask());
                    if (joystick2 != null && res == 0xf7)
                        joystickToSJS2(joystick2.getButtonMask());
                }
                case SINCLAIR2 -> {
                    if (res == 0xf7)
                        joystickToSJS2(joystick1.getButtonMask());
                    if (joystick2 != null && res == 0xef)
                        joystickToSJS1(joystick2.getButtonMask());
                }
                case CURSOR -> joystickToCursor();
            }
        }

//        if (rzxEnabled) {
//            rowKey[0] &= rowKey[0];
//            rowKey[1] &= rowKey[1];
//            rowKey[2] &= rowKey[2];
//            rowKey[3] &= rowKey[3];
//            rowKey[4] &= rowKey[4];
//            rowKey[5] &= rowKey[5];
//            rowKey[6] &= rowKey[6];
//            rowKey[7] &= rowKey[7];
//        }

        log.trace(String.format("readKeyboardPort: %04X, %02x, %02x", port, sjs1, sjs2));

        switch (res) {
            case 0x7f -> { // SPACE to 'B' row
                return rowKey[7];
            }
            case 0xbf -> { // ENTER to 'H' row
                return rowKey[6];
            }
            case 0xdf -> { // 'P' to 'Y' row
                return rowKey[5];
            }
            case 0xef -> { // '0' to '6' row
                return rowKey[4] & sjs1;
            }
            case 0xf7 -> { // '1' to '5' row
                return rowKey[3] & sjs2;
            }
            case 0xfb -> { // 'Q' to 'T' row
                return rowKey[2];
            }
            case 0xfd -> { // 'A' to 'G' row
                return rowKey[1];
            }
            case 0xfe -> { //  'SHIFT' to 'V' row
                return rowKey[0];
            }
            default -> {    // reading more than a row
                res = ~res & 0xff;
                for (int row = 0, mask = 0x01; row < 8; row++, mask <<= 1) {
                    if ((res & mask) != 0) {
                        keys &= rowKey[row];
                    }
                }
                return keys;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent evt) {

        if (rzxEnabled) {
            receiveRecreatedZXKeyEvents(evt);
            return;
        }

        if (mapPCKeys) {
            char keychar = evt.getKeyChar();
            if (keychar != KeyEvent.CHAR_UNDEFINED && !evt.isAltDown()) {
                log.trace("pressed {}", keychar);
                if (pressedKeyChar(keychar)) {
                    for (int key = 0; key < keyEventPending.length; key++) {
                        if (keyEventPending[key] == null) {
                            keyEventPending[key] = evt;
                            log.trace("Key pressed {} ", String.format("#%d: %c", key, keychar));
                            break;
                        }
                    }
                    return;
                }
            }
        }

        int key = evt.getKeyCode();

        log.trace(String.format("Press keyCode = %d, modifiers = %d", key, evt.getModifiersEx()));

        /*
         * Windows no envía el keycode VK_ALT_GRAPH y en su lugar envía dos eventos, Ctrl + Alt, en ese orden.
         * Además, una repetición de tecla consiste en múltiples eventos keyPressed y un solo evento keyReleased.
         * 
         * El Ctrl es una pulsación normal y el Alt lleva activos los modificadores CTRL y ALT.
         * 
         * El problema es que el primer Ctrl nos "presiona" la tecla Symbol-Shift, y hay que quitarla.
         * 
         * En cualquier otro caso, la tecla Alt hay que saltársela para que sigan funcionando los
         * atajos de teclado sin producir pulsaciones espureas en el emulador.
         * 
         * Además, una repetición de tecla consiste en múltiples eventos keyPressed y un solo evento keyReleased.
         * 
         * Algunos teclados de Windows no tienen AltGr sino un Alt derecho. Shit yourself, little parrot!.
         */
        if (winBug && key == KeyEvent.VK_ALT && (evt.getModifiersEx() == (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)
                || evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT)) {
            key = KeyEvent.VK_ALT_GRAPH;
            rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        } else {
            // En caso de ser Windows, si se reciben Alt + Control probablemente lo que se pulsó fue AltGr
            // Gracias a pastbytes por detectar (también) este problema e indicarme la manera de reproducirlo.
            if (evt.isAltDown() && !evt.isControlDown())
                return;
        }

        switch (key) {
            // Row B - Break/Space
            case KeyEvent.VK_SPACE -> rowKey[7] &= KEY_PRESSED_BIT0;    // Break/Space
            case KeyEvent.VK_CONTROL -> rowKey[7] &= KEY_PRESSED_BIT1;  // Symbol Shift
            case KeyEvent.VK_M -> rowKey[7] &= KEY_PRESSED_BIT2;        // M
            case KeyEvent.VK_N -> rowKey[7] &= KEY_PRESSED_BIT3;        // N
            case KeyEvent.VK_B -> rowKey[7] &= KEY_PRESSED_BIT4;        // B

            // Row ENTER - H
            case KeyEvent.VK_ENTER -> rowKey[6] &= KEY_PRESSED_BIT0;    // ENTER
            case KeyEvent.VK_L -> rowKey[6] &= KEY_PRESSED_BIT1;        // L
            case KeyEvent.VK_K -> rowKey[6] &= KEY_PRESSED_BIT2;        // K
            case KeyEvent.VK_J -> rowKey[6] &= KEY_PRESSED_BIT3;        // J
            case KeyEvent.VK_H -> rowKey[6] &= KEY_PRESSED_BIT4;        // H

            // Row P - Y
            case KeyEvent.VK_P -> rowKey[5] &= KEY_PRESSED_BIT0;        // P
            case KeyEvent.VK_O -> rowKey[5] &= KEY_PRESSED_BIT1;        // O
            case KeyEvent.VK_I -> rowKey[5] &= KEY_PRESSED_BIT2;        // I
            case KeyEvent.VK_U -> rowKey[5] &= KEY_PRESSED_BIT3;        // U
            case KeyEvent.VK_Y -> rowKey[5] &= KEY_PRESSED_BIT4;        // Y

            // Row 0 - 6
            case KeyEvent.VK_0 -> rowKey[4] &= KEY_PRESSED_BIT0;        // 0
            case KeyEvent.VK_9 -> rowKey[4] &= KEY_PRESSED_BIT1;        // 9
            case KeyEvent.VK_8 -> rowKey[4] &= KEY_PRESSED_BIT2;        // 8
            case KeyEvent.VK_7 -> rowKey[4] &= KEY_PRESSED_BIT3;        // 7
            case KeyEvent.VK_6 -> rowKey[4] &= KEY_PRESSED_BIT4;        // 6

            // Row 1 - 5
            case KeyEvent.VK_1 -> rowKey[3] &= KEY_PRESSED_BIT0;        // 1
            case KeyEvent.VK_2 -> rowKey[3] &= KEY_PRESSED_BIT1;        // 2
            case KeyEvent.VK_3 -> rowKey[3] &= KEY_PRESSED_BIT2;        // 3
            case KeyEvent.VK_4 -> rowKey[3] &= KEY_PRESSED_BIT3;        // 4
            case KeyEvent.VK_5 -> rowKey[3] &= KEY_PRESSED_BIT4;        // 5

            // Row Q - T
            case KeyEvent.VK_Q -> rowKey[2] &= KEY_PRESSED_BIT0;        // Q
            case KeyEvent.VK_W -> rowKey[2] &= KEY_PRESSED_BIT1;        // W
            case KeyEvent.VK_E -> rowKey[2] &= KEY_PRESSED_BIT2;        // E
            case KeyEvent.VK_R -> rowKey[2] &= KEY_PRESSED_BIT3;        // R
            case KeyEvent.VK_T -> rowKey[2] &= KEY_PRESSED_BIT4;        // T

            // Row A - G
            case KeyEvent.VK_A -> rowKey[1] &= KEY_PRESSED_BIT0;        // A
            case KeyEvent.VK_S -> rowKey[1] &= KEY_PRESSED_BIT1;        // S
            case KeyEvent.VK_D -> rowKey[1] &= KEY_PRESSED_BIT2;        // D
            case KeyEvent.VK_F -> rowKey[1] &= KEY_PRESSED_BIT3;        // F
            case KeyEvent.VK_G -> rowKey[1] &= KEY_PRESSED_BIT4;        // G

            // Row Caps Shift - V
            case KeyEvent.VK_SHIFT -> {
                rowKey[0] &= KEY_PRESSED_BIT0;                          // Caps Shift
                shiftPressed = true;
            }
            case KeyEvent.VK_Z -> rowKey[0] &= KEY_PRESSED_BIT1;        // Z
            case KeyEvent.VK_X -> rowKey[0] &= KEY_PRESSED_BIT2;        // X
            case KeyEvent.VK_C -> rowKey[0] &= KEY_PRESSED_BIT3;        // C
            case KeyEvent.VK_V -> rowKey[0] &= KEY_PRESSED_BIT4;        // V

            // Additional keys
            case KeyEvent.VK_BACK_SPACE -> {
                if (!shiftPressed)
                    rowKey[0] &= KEY_PRESSED_BIT0;                      // CAPS
                rowKey[4] &= KEY_PRESSED_BIT0;                          // 0
            }
            case KeyEvent.VK_COMMA -> rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT3);   // Symbol Shift + N (',')
            case KeyEvent.VK_PERIOD -> rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT2);  // Symbol Shift + M ('.')
            case KeyEvent.VK_MINUS -> {
                rowKey[7] &= KEY_PRESSED_BIT1;                          // Symbol Shift
                rowKey[6] &= KEY_PRESSED_BIT3;                          // J
            }
            case KeyEvent.VK_PLUS -> {
                rowKey[7] &= KEY_PRESSED_BIT1;                          // Symbol Shift
                rowKey[6] &= KEY_PRESSED_BIT2;                          // K
            }
            case KeyEvent.VK_EQUALS -> {                                // UK Keyboard
                rowKey[7] &= KEY_PRESSED_BIT1;                          // Symbol Shift
                rowKey[6] &= KEY_PRESSED_BIT1;                          // L
            }
            case KeyEvent.VK_NUMBER_SIGN -> {                           // UK Keyboard
                rowKey[7] &= KEY_PRESSED_BIT1;                          // Symbol Shift
                rowKey[3] &= KEY_PRESSED_BIT2;                          // 3
            }
            case KeyEvent.VK_SLASH -> {                                 // UK Keyboard
                rowKey[7] &= KEY_PRESSED_BIT1;                          // Symbol Shift
                rowKey[0] &= KEY_PRESSED_BIT4;                          // V
            }
            case KeyEvent.VK_SEMICOLON -> { // UK Keyboard
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[5] &= KEY_PRESSED_BIT1; // O
            }
            case KeyEvent.VK_CAPS_LOCK -> {
                if (!shiftPressed)
                    rowKey[0] &= KEY_PRESSED_BIT0; // CAPS
                rowKey[3] &= KEY_PRESSED_BIT1; // 2  -- Caps Lock
            }
            case KeyEvent.VK_ESCAPE -> {
                if (!shiftPressed)
                    rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[7] &= KEY_PRESSED_BIT0; // Space
            }
            // Joystick emulation
            case KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH ->
                    joystickModel.getJoystick().handleKeyPressed(key, shiftPressed, rowKey);
        }
    }

    @Override
    public void keyReleased(KeyEvent evt) {

        if (rzxEnabled) {
            return;
        }

        if (mapPCKeys) {
            char keychar = evt.getKeyChar();

            if (keychar != KeyEvent.CHAR_UNDEFINED && !evt.isAltDown()) {
                log.trace("released {}", keychar);
                for (int key = 0; key < keyEventPending.length; key++) {
                    if (keyEventPending[key] != null
                            && evt.getKeyCode() == keyEventPending[key].getKeyCode()) {
                        keychar = keyEventPending[key].getKeyChar();
                        keyEventPending[key] = null;
                        log.trace(String.format("Key released #%d: %c\n", key, keychar));
                    }
                }

                if (releasedKeyChar(keychar)) {
                    return;
                }
            }
        }

        int key = evt.getKeyCode();

        log.trace(String.format("Release keyCode = %d, modifiers = %d", key, evt.getModifiersEx()));

        /*
         * Windows no envía el keycode VK_ALT_GRAPH y en su lugar envía dos eventos, Ctrl + Alt, en ese orden.
         * 
         * El Ctrl lleva activo el modificador Alt. El Alt es un evento normal.
         * 
         * La tecla Alt hay que saltársela para que sigan funcionando los atajos de teclado sin
         * producir pulsaciones espureas en el emulador.
         * 
         * Además, una repetición de tecla consiste en múltiples eventos keyPressed y un solo evento keyReleased.
         * 
         * Algunos teclados de Windows no tienen AltGr sino un Alt derecho. Shit yourself, little parrot!.
         */
        if (winBug && ((key == KeyEvent.VK_CONTROL && evt.getModifiersEx() == InputEvent.ALT_DOWN_MASK)
                || (key == KeyEvent.VK_ALT && evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT))) {
            key = KeyEvent.VK_ALT_GRAPH;
        } else {
            if (evt.isAltDown() && !evt.isControlDown())
                return;
        }

        switch (key) {
            // Row Break/Space - B
            case KeyEvent.VK_SPACE -> rowKey[7] |= KEY_RELEASED_BIT0; // Break/Space
            case KeyEvent.VK_CONTROL -> rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
            case KeyEvent.VK_M -> rowKey[7] |= KEY_RELEASED_BIT2; // M
            case KeyEvent.VK_N -> rowKey[7] |= KEY_RELEASED_BIT3; // N
            case KeyEvent.VK_B -> rowKey[7] |= KEY_RELEASED_BIT4; // B

            // Row ENTER - H
            case KeyEvent.VK_ENTER -> rowKey[6] |= KEY_RELEASED_BIT0; // ENTER
            case KeyEvent.VK_L -> rowKey[6] |= KEY_RELEASED_BIT1; // L
            case KeyEvent.VK_K -> rowKey[6] |= KEY_RELEASED_BIT2; // K
            case KeyEvent.VK_J -> rowKey[6] |= KEY_RELEASED_BIT3; // J
            case KeyEvent.VK_H -> rowKey[6] |= KEY_RELEASED_BIT4; // H

            // Row P - Y
            case KeyEvent.VK_P -> rowKey[5] |= KEY_RELEASED_BIT0; // P
            case KeyEvent.VK_O -> rowKey[5] |= KEY_RELEASED_BIT1; // O
            case KeyEvent.VK_I -> rowKey[5] |= KEY_RELEASED_BIT2; // I
            case KeyEvent.VK_U -> rowKey[5] |= KEY_RELEASED_BIT3; // U
            case KeyEvent.VK_Y -> rowKey[5] |= KEY_RELEASED_BIT4; // Y

            // Row 0 - 6
            case KeyEvent.VK_0 -> rowKey[4] |= KEY_RELEASED_BIT0; // 0
            case KeyEvent.VK_9 -> rowKey[4] |= KEY_RELEASED_BIT1; // 9
            case KeyEvent.VK_8 -> rowKey[4] |= KEY_RELEASED_BIT2; // 8
            case KeyEvent.VK_7 -> rowKey[4] |= KEY_RELEASED_BIT3; // 7
            case KeyEvent.VK_6 -> rowKey[4] |= KEY_RELEASED_BIT4; // 6

            // Row 1 - 5
            case KeyEvent.VK_1 -> rowKey[3] |= KEY_RELEASED_BIT0; // 1
            case KeyEvent.VK_2 -> rowKey[3] |= KEY_RELEASED_BIT1; // 2
            case KeyEvent.VK_3 -> rowKey[3] |= KEY_RELEASED_BIT2; // 3
            case KeyEvent.VK_4 -> rowKey[3] |= KEY_RELEASED_BIT3; // 4
            case KeyEvent.VK_5 -> rowKey[3] |= KEY_RELEASED_BIT4; // 5

            // Row Q - T
            case KeyEvent.VK_Q -> rowKey[2] |= KEY_RELEASED_BIT0; // Q
            case KeyEvent.VK_W -> rowKey[2] |= KEY_RELEASED_BIT1; // W
            case KeyEvent.VK_E -> rowKey[2] |= KEY_RELEASED_BIT2; // E
            case KeyEvent.VK_R -> rowKey[2] |= KEY_RELEASED_BIT3; // R
            case KeyEvent.VK_T -> rowKey[2] |= KEY_RELEASED_BIT4; // T

            // Row A - G
            case KeyEvent.VK_A -> rowKey[1] |= KEY_RELEASED_BIT0; // A
            case KeyEvent.VK_S -> rowKey[1] |= KEY_RELEASED_BIT1; // S
            case KeyEvent.VK_D -> rowKey[1] |= KEY_RELEASED_BIT2; // D
            case KeyEvent.VK_F -> rowKey[1] |= KEY_RELEASED_BIT3; // F
            case KeyEvent.VK_G -> rowKey[1] |= KEY_RELEASED_BIT4; // G

            // Row Caps Shift - V
            case KeyEvent.VK_SHIFT -> {
                if (shiftPressed) {
                    rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
                    shiftPressed = false;
                }
            }
            case KeyEvent.VK_Z -> rowKey[0] |= KEY_RELEASED_BIT1; // Z
            case KeyEvent.VK_X -> rowKey[0] |= KEY_RELEASED_BIT2; // X
            case KeyEvent.VK_C -> rowKey[0] |= KEY_RELEASED_BIT3; // C
            case KeyEvent.VK_V -> rowKey[0] |= KEY_RELEASED_BIT4; // V

            // Additional keys
            case KeyEvent.VK_BACK_SPACE -> {
                if (!shiftPressed) {
                    rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
                }
                rowKey[4] |= KEY_RELEASED_BIT0; // 0
            }
            case KeyEvent.VK_COMMA -> rowKey[7] |= (KEY_RELEASED_BIT1 | KEY_RELEASED_BIT3);     // Symbol Shift + N
            case KeyEvent.VK_PERIOD -> rowKey[7] |= (KEY_RELEASED_BIT1 | KEY_RELEASED_BIT2);    // Symbol Shift + M
            case KeyEvent.VK_MINUS -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[6] |= KEY_RELEASED_BIT3; // J
            }
            case KeyEvent.VK_PLUS -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[6] |= KEY_RELEASED_BIT2; // K
            }
            case KeyEvent.VK_EQUALS -> { // UK Keyboard
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[6] |= KEY_RELEASED_BIT1; // L
            }
            case KeyEvent.VK_NUMBER_SIGN -> { // UK Keyboard
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[3] |= KEY_RELEASED_BIT2; // 3
            }
            case KeyEvent.VK_SLASH -> { // UK Keyboard
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[0] |= KEY_RELEASED_BIT4; // V
            }
            case KeyEvent.VK_SEMICOLON -> { // UK Keyboard
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[5] |= KEY_RELEASED_BIT1; // O
            }
            case KeyEvent.VK_CAPS_LOCK -> {
                if (!shiftPressed) {
                    rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
                }
                rowKey[3] |= KEY_RELEASED_BIT1; // 2
            }
            case KeyEvent.VK_ESCAPE -> {
                if (!shiftPressed) {
                    rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
                }
                rowKey[7] |= KEY_RELEASED_BIT0; // Space
            }
            // Joystick emulation
            case KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_META, KeyEvent.VK_ALT_GRAPH ->
                joystickModel.getJoystick().handleKeyReleased(key, shiftPressed, rowKey);
        }
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent evt) {
        // TODO add your handling code here:
    }

    private boolean pressedKeyChar(char keyChar) {
        boolean done = true;

        if (shiftPressed) {
            rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
        }

        switch (keyChar) {
            case '!' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[3] &= KEY_PRESSED_BIT0; // 1
            }
            case '"' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[5] &= KEY_PRESSED_BIT0; // P
            }
            case '#' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[3] &= KEY_PRESSED_BIT2; // 3
            }
            case '$' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[3] &= KEY_PRESSED_BIT3; // 4
            }
            case '%' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[3] &= KEY_PRESSED_BIT4; // 5
            }
            case '&' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[4] &= KEY_PRESSED_BIT4; // 6
            }
            case '\'' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[4] &= KEY_PRESSED_BIT3; // 7
            }
            case '(' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[4] &= KEY_PRESSED_BIT2; // 8
            }
            case ')' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[4] &= KEY_PRESSED_BIT1; // 9
            }
            case '*' -> rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT4); // Symbol Shift + b
            case '+' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[6] &= KEY_PRESSED_BIT2; // K
            }
            case ',' -> rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT3); // Symbol Shift + n
            case '-' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[6] &= KEY_PRESSED_BIT3; // J
            }
            case '.' -> rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT2); // Symbol Shift + m
            case '/' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[0] &= KEY_PRESSED_BIT4; // V
            }
            case '0' -> rowKey[4] &= KEY_PRESSED_BIT0; // 0
            case '1' -> rowKey[3] &= KEY_PRESSED_BIT0; // 1
            case '2' -> rowKey[3] &= KEY_PRESSED_BIT1; // 2
            case '3' -> rowKey[3] &= KEY_PRESSED_BIT2; // 3
            case '4' -> rowKey[3] &= KEY_PRESSED_BIT3; // 4
            case '5' -> rowKey[3] &= KEY_PRESSED_BIT4; // 5
            case '6' -> rowKey[4] &= KEY_PRESSED_BIT4; // 6
            case '7' -> rowKey[4] &= KEY_PRESSED_BIT3; // 7
            case '8' -> rowKey[4] &= KEY_PRESSED_BIT2; // 8
            case '9' -> rowKey[4] &= KEY_PRESSED_BIT1; // 9
            case ':' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[0] &= KEY_PRESSED_BIT1; // Z
            }
            case ';' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[5] &= KEY_PRESSED_BIT1; // O
            }
            case '<' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[2] &= KEY_PRESSED_BIT3; // R
            }
            case '=' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[6] &= KEY_PRESSED_BIT1; // L
            }
            case '>' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[2] &= KEY_PRESSED_BIT4; // T
            }
            case '?' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[0] &= KEY_PRESSED_BIT3; // C
            }
            case '@' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[3] &= KEY_PRESSED_BIT1; // 2
            }
            case 'A' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[1] &= KEY_PRESSED_BIT0; // A
            }
            case 'B' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[7] &= KEY_PRESSED_BIT4; // B
            }
            case 'C' -> rowKey[0] &= (KEY_PRESSED_BIT0 & KEY_PRESSED_BIT3); // Caps Shift + c
            case 'D' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[1] &= KEY_PRESSED_BIT2; // D
            }
            case 'E' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[2] &= KEY_PRESSED_BIT2; // E
            }
            case 'F' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[1] &= KEY_PRESSED_BIT3; // F
            }
            case 'G' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[1] &= KEY_PRESSED_BIT4; // G
            }
            case 'H' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[6] &= KEY_PRESSED_BIT4; // H
            }
            case 'I' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[5] &= KEY_PRESSED_BIT2; // I
            }
            case 'J' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[6] &= KEY_PRESSED_BIT3; // J
            }
            case 'K' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[6] &= KEY_PRESSED_BIT2; // K
            }
            case 'L' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[6] &= KEY_PRESSED_BIT1; // L
            }
            case 'M' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[7] &= KEY_PRESSED_BIT2; // M
            }
            case 'N' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[7] &= KEY_PRESSED_BIT3; // N
            }
            case 'O' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[5] &= KEY_PRESSED_BIT1; // O
            }
            case 'P' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[5] &= KEY_PRESSED_BIT0; // P
            }
            case 'Q' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[2] &= KEY_PRESSED_BIT0; // Q
            }
            case 'R' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[2] &= KEY_PRESSED_BIT3; // R
            }
            case 'S' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[1] &= KEY_PRESSED_BIT1; // S
            }
            case 'T' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[2] &= KEY_PRESSED_BIT4; // T
            }
            case 'U' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[5] &= KEY_PRESSED_BIT3; // U
            }
            case 'V' -> rowKey[0] &= (KEY_PRESSED_BIT0 & KEY_PRESSED_BIT4); // Caps Shift + v
            case 'W' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[2] &= KEY_PRESSED_BIT1; // W
            }
            case 'X' -> rowKey[0] &= (KEY_PRESSED_BIT0 & KEY_PRESSED_BIT2); // Caps Shift + x
            case 'Y' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[5] &= KEY_PRESSED_BIT4; // Y
            }
            case 'Z' -> rowKey[0] &= (KEY_PRESSED_BIT0 & KEY_PRESSED_BIT1); // Caps Shift + z
            case '[' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[5] &= KEY_PRESSED_BIT4; // Y
            }
            case '\\' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[1] &= KEY_PRESSED_BIT2; // D
            }
            case ']' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[5] &= KEY_PRESSED_BIT3; // U
            }
            case '_' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[4] &= KEY_PRESSED_BIT0; // 0
            }
            case 'a' -> rowKey[1] &= KEY_PRESSED_BIT0; // A
            case 'b' -> rowKey[7] &= KEY_PRESSED_BIT4; // B
            case 'c' -> rowKey[0] &= KEY_PRESSED_BIT3; // C
            case 'd' -> rowKey[1] &= KEY_PRESSED_BIT2; // D
            case 'e' -> rowKey[2] &= KEY_PRESSED_BIT2; // E
            case 'f' -> rowKey[1] &= KEY_PRESSED_BIT3; // F
            case 'g' -> rowKey[1] &= KEY_PRESSED_BIT4; // G
            case 'h' -> rowKey[6] &= KEY_PRESSED_BIT4; // H
            case 'i' -> rowKey[5] &= KEY_PRESSED_BIT2; // I
            case 'j' -> rowKey[6] &= KEY_PRESSED_BIT3; // J
            case 'k' -> rowKey[6] &= KEY_PRESSED_BIT2; // K
            case 'l' -> rowKey[6] &= KEY_PRESSED_BIT1; // L
            case 'm' -> rowKey[7] &= KEY_PRESSED_BIT2; // M
            case 'n' -> rowKey[7] &= KEY_PRESSED_BIT3; // N
            case 'o' -> rowKey[5] &= KEY_PRESSED_BIT1; // O
            case 'p' -> rowKey[5] &= KEY_PRESSED_BIT0; // P
            case 'q' -> rowKey[2] &= KEY_PRESSED_BIT0; // Q
            case 'r' -> rowKey[2] &= KEY_PRESSED_BIT3; // R
            case 's' -> rowKey[1] &= KEY_PRESSED_BIT1; // S
            case 't' -> rowKey[2] &= KEY_PRESSED_BIT4; // T
            case 'u' -> rowKey[5] &= KEY_PRESSED_BIT3; // U
            case 'v' -> rowKey[0] &= KEY_PRESSED_BIT4; // V
            case 'w' -> rowKey[2] &= KEY_PRESSED_BIT1; // W
            case 'x' -> rowKey[0] &= KEY_PRESSED_BIT2; // X
            case 'y' -> rowKey[5] &= KEY_PRESSED_BIT4; // Y
            case 'z' -> rowKey[0] &= KEY_PRESSED_BIT1; // Z
            case '{' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[1] &= KEY_PRESSED_BIT3; // F
            }
            case '|', '¦' -> { // Spanish keyboard
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[1] &= KEY_PRESSED_BIT1; // S
            }
            case '}' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[1] &= KEY_PRESSED_BIT4; // G
            }
            case '~' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[1] &= KEY_PRESSED_BIT0; // A
            }
            case '©' -> { // Mac only
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[5] &= KEY_PRESSED_BIT0; // P
            } // PC only
            // Mac only
            case '`', '§', '¡' -> { // Spanish keyboard only
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[3] &= KEY_PRESSED_BIT0; // 1 (EDIT mode)
            } // PC only
            // Mac only
            case '¬', '±', '¿' -> { // Spanish keyboard only
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[4] &= KEY_PRESSED_BIT1; // 9 (GRAPHICS mode)
            }
            case '£' -> {
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
                rowKey[0] &= KEY_PRESSED_BIT2; // x
            }
            case 'º' -> {
                rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift -- Extended Mode
            }
            default -> {
                if (shiftPressed) {
                    rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                }
                done = false;
            }
        }
        return done;
    }

    private boolean releasedKeyChar(char keyChar) {
        boolean done = true;

        switch (keyChar) {
            case '!' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[3] |= KEY_RELEASED_BIT0; // 1
            }
            case '"' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[5] |= KEY_RELEASED_BIT0; // P
            }
            case '#' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[3] |= KEY_RELEASED_BIT2; // 3
            }
            case '$' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[3] |= KEY_RELEASED_BIT3; // 4
            }
            case '%' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[3] |= KEY_RELEASED_BIT4; // 5
            }
            case '&' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[4] |= KEY_RELEASED_BIT4; // 6
            }
            case '\'' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[4] |= KEY_RELEASED_BIT3; // 7
            }
            case '(' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[4] |= KEY_RELEASED_BIT2; // 8
            }
            case ')' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[4] |= KEY_RELEASED_BIT1; // 9
            }
            case '*' -> rowKey[7] |= (KEY_RELEASED_BIT1 | KEY_RELEASED_BIT4); // Symbol Shift + b
            case '+' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[6] |= KEY_RELEASED_BIT2; // K
            }
            case ',' -> rowKey[7] |= (KEY_RELEASED_BIT1 | KEY_RELEASED_BIT3); // Symbol Shift + n
            case '-' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[6] |= KEY_RELEASED_BIT3; // J
            }
            case '.' -> rowKey[7] |= 0x06; // Symbol Shift + M
            case '/' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[0] |= KEY_RELEASED_BIT4; // V
            }
            case '0' -> rowKey[4] |= KEY_RELEASED_BIT0; // 0
            case '1' -> rowKey[3] |= KEY_RELEASED_BIT0; // 1
            case '2' -> rowKey[3] |= KEY_RELEASED_BIT1; // 2
            case '3' -> rowKey[3] |= KEY_RELEASED_BIT2; // 3
            case '4' -> rowKey[3] |= KEY_RELEASED_BIT3; // 4
            case '5' -> rowKey[3] |= KEY_RELEASED_BIT4; // 5
            case '6' -> rowKey[4] |= KEY_RELEASED_BIT4; // 6
            case '7' -> rowKey[4] |= KEY_RELEASED_BIT3; // 7
            case '8' -> rowKey[4] |= KEY_RELEASED_BIT2; // 8
            case '9' -> rowKey[4] |= KEY_RELEASED_BIT1; // 9
            case ':' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[0] |= KEY_RELEASED_BIT1; // Z
            }
            case ';' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[5] |= KEY_RELEASED_BIT1; // O
            }
            case '<' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[2] |= KEY_RELEASED_BIT3; // R
            }
            case '=' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[6] |= KEY_RELEASED_BIT1; // L
            }
            case '>' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[2] |= KEY_RELEASED_BIT4; // T
            }
            case '?' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[0] |= KEY_RELEASED_BIT3; // C
            }
            case '@' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[3] |= KEY_RELEASED_BIT1; // 2
            }
            case 'A' -> rowKey[1] |= KEY_RELEASED_BIT0; // A
            case 'B' -> rowKey[7] |= KEY_RELEASED_BIT4; // B
            case 'C' -> rowKey[0] |= KEY_RELEASED_BIT3; // C
            case 'D' -> rowKey[1] |= KEY_RELEASED_BIT2; // D
            case 'E' -> rowKey[2] |= KEY_RELEASED_BIT2; // E
            case 'F' -> rowKey[1] |= KEY_RELEASED_BIT3; // F
            case 'G' -> rowKey[1] |= KEY_RELEASED_BIT4; // G
            case 'H' -> rowKey[6] |= KEY_RELEASED_BIT4; // H
            case 'I' -> rowKey[5] |= KEY_RELEASED_BIT2; // I
            case 'J' -> rowKey[6] |= KEY_RELEASED_BIT3; // J
            case 'K' -> rowKey[6] |= KEY_RELEASED_BIT2; // K
            case 'L' -> rowKey[6] |= KEY_RELEASED_BIT1; // L
            case 'M' -> rowKey[7] |= KEY_RELEASED_BIT2; // M
            case 'N' -> rowKey[7] |= KEY_RELEASED_BIT3; // N
            case 'O' -> rowKey[5] |= KEY_RELEASED_BIT1; // O
            case 'P' -> rowKey[5] |= KEY_RELEASED_BIT0; // P
            case 'Q' -> rowKey[2] |= KEY_RELEASED_BIT0; // Q
            case 'R' -> rowKey[2] |= KEY_RELEASED_BIT3; // R
            case 'S' -> rowKey[1] |= KEY_RELEASED_BIT1; // S
            case 'T' -> rowKey[2] |= KEY_RELEASED_BIT4; // T
            case 'U' -> rowKey[5] |= KEY_RELEASED_BIT3; // U
            case 'V' -> rowKey[0] |= KEY_RELEASED_BIT4; // V
            case 'W' -> rowKey[2] |= KEY_RELEASED_BIT1; // W
            case 'X' -> rowKey[0] |= KEY_RELEASED_BIT2; // X
            case 'Y' -> rowKey[5] |= KEY_RELEASED_BIT4; // Y
            case 'Z' -> rowKey[0] |= KEY_RELEASED_BIT1; // Z
            case '[' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[5] |= KEY_RELEASED_BIT4; // Y
            }
            case '\\' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[1] |= KEY_RELEASED_BIT2; // D
            }
            case ']' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[5] |= KEY_RELEASED_BIT3; // U
            }
            case '_' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[4] |= KEY_RELEASED_BIT0; // 0
            }
            case 'a' -> rowKey[1] |= KEY_RELEASED_BIT0; // A
            case 'b' -> rowKey[7] |= KEY_RELEASED_BIT4; // B
            case 'c' -> rowKey[0] |= KEY_RELEASED_BIT3; // C
            case 'd' -> rowKey[1] |= KEY_RELEASED_BIT2; // D
            case 'e' -> rowKey[2] |= KEY_RELEASED_BIT2; // E
            case 'f' -> rowKey[1] |= KEY_RELEASED_BIT3; // F
            case 'g' -> rowKey[1] |= KEY_RELEASED_BIT4; // G
            case 'h' -> rowKey[6] |= KEY_RELEASED_BIT4; // H
            case 'i' -> rowKey[5] |= KEY_RELEASED_BIT2; // I
            case 'j' -> rowKey[6] |= KEY_RELEASED_BIT3; // J
            case 'k' -> rowKey[6] |= KEY_RELEASED_BIT2; // K
            case 'l' -> rowKey[6] |= KEY_RELEASED_BIT1; // L
            case 'm' -> rowKey[7] |= KEY_RELEASED_BIT2; // M
            case 'n' -> rowKey[7] |= KEY_RELEASED_BIT3; // N
            case 'o' -> rowKey[5] |= KEY_RELEASED_BIT1; // O
            case 'p' -> rowKey[5] |= KEY_RELEASED_BIT0; // P
            case 'q' -> rowKey[2] |= KEY_RELEASED_BIT0; // Q
            case 'r' -> rowKey[2] |= KEY_RELEASED_BIT3; // R
            case 's' -> rowKey[1] |= KEY_RELEASED_BIT1; // S
            case 't' -> rowKey[2] |= KEY_RELEASED_BIT4; // T
            case 'u' -> rowKey[5] |= KEY_RELEASED_BIT3; // U
            case 'v' -> rowKey[0] |= KEY_RELEASED_BIT4; // V
            case 'w' -> rowKey[2] |= KEY_RELEASED_BIT1; // W
            case 'x' -> rowKey[0] |= KEY_RELEASED_BIT2; // X
            case 'y' -> rowKey[5] |= KEY_RELEASED_BIT4; // Y
            case 'z' -> rowKey[0] |= KEY_RELEASED_BIT1; // Z
            case '{' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[1] |= KEY_RELEASED_BIT3; // F
            }
            case '|', '¦' -> { // Spanish keyboard
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[1] |= KEY_RELEASED_BIT1; // S
            }
            case '}' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[1] |= KEY_RELEASED_BIT4; // G
            }
            case '~' -> {
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[1] |= KEY_RELEASED_BIT0; // A
            }
            case '©' -> { // Mac only
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[5] |= KEY_RELEASED_BIT0; // P
            } // Mac only
            case '`', '§', '¡' -> // Spanish keyboard
//                rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
                    rowKey[3] |= KEY_RELEASED_BIT0; // 1
            // Mac only
            case '¬', '±', '¿' -> // Spanish keyboard
//                rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
                    rowKey[4] |= KEY_RELEASED_BIT1; // G (Graphics mode)
            case '£' -> { // Pound sign
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
                rowKey[0] |= KEY_RELEASED_BIT2; // X
            }
            case 'º' -> { // Spanish keyboard only
                rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
                rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift -- Extended Mode
            }
            default -> {
                if (shiftPressed) {
                    rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
                }
                done = false;
            }
        }
        return done;
    }

    private void receiveRecreatedZXKeyEvents(KeyEvent evt) {

        log.trace("Key Pressed: {}", evt);
        switch (evt.getKeyChar()) {
            case '!' -> rowKey[7] &= KEY_PRESSED_BIT1;  // Symbol Shift
            case '$' -> rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
            case '%' -> rowKey[7] &= KEY_PRESSED_BIT0;  // Break/Space
            case ',' -> rowKey[7] &= KEY_PRESSED_BIT4;  // B
            case '-' -> rowKey[0] &= KEY_PRESSED_BIT2;  // X
            case '.' -> rowKey[7] |= KEY_RELEASED_BIT4; // B
            case '/' -> rowKey[7] &= KEY_PRESSED_BIT3;  // N
            case '0' -> rowKey[6] &= KEY_PRESSED_BIT3;  // J
            case '1' -> rowKey[6] |= KEY_RELEASED_BIT3; // J
            case '2' -> rowKey[6] &= KEY_PRESSED_BIT2;  // K
            case '3' -> rowKey[6] |= KEY_RELEASED_BIT2; // K
            case '4' -> rowKey[6] &= KEY_PRESSED_BIT1;  // L
            case '5' -> rowKey[6] |= KEY_RELEASED_BIT1; // L
            case '6' -> rowKey[6] &= KEY_PRESSED_BIT0;  // ENTER
            case '7' -> rowKey[6] |= KEY_RELEASED_BIT0; // ENTER
            case '8' -> rowKey[0] &= KEY_PRESSED_BIT0;  // Caps Shift
            case '9' -> rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
            case ':' -> rowKey[0] |= KEY_RELEASED_BIT4; // V
            case ';' -> rowKey[0] &= KEY_PRESSED_BIT4;  // V
            case '<' -> rowKey[0] &= KEY_PRESSED_BIT1;  // Z
            case '=' -> rowKey[0] |= KEY_RELEASED_BIT2; // X
            case '>' -> rowKey[0] |= KEY_RELEASED_BIT1; // Z
            case '?' -> rowKey[7] |= KEY_RELEASED_BIT3; // N
            case 'a' -> rowKey[3] &= KEY_PRESSED_BIT0;  // 1
            case 'b' -> rowKey[3] |= KEY_RELEASED_BIT0; // 1
            case 'c' -> rowKey[3] &= KEY_PRESSED_BIT1;  // 2
            case 'd' -> rowKey[3] |= KEY_RELEASED_BIT1; // 2
            case 'e' -> rowKey[3] &= KEY_PRESSED_BIT2;  // 3
            case 'f' -> rowKey[3] |= KEY_RELEASED_BIT2; // 3
            case 'g' -> rowKey[3] &= KEY_PRESSED_BIT3;  // 4
            case 'h' -> rowKey[3] |= KEY_RELEASED_BIT3; // 4
            case 'i' -> rowKey[3] &= KEY_PRESSED_BIT4;  // 5
            case 'j' -> rowKey[3] |= KEY_RELEASED_BIT4; // 5
            case 'k' -> rowKey[4] &= KEY_PRESSED_BIT4;  // 6
            case 'l' -> rowKey[4] |= KEY_RELEASED_BIT4; // 6
            case 'm' -> rowKey[4] &= KEY_PRESSED_BIT3;  // 7
            case 'n' -> rowKey[4] |= KEY_RELEASED_BIT3; // 7
            case 'o' -> rowKey[4] &= KEY_PRESSED_BIT2;  // 8
            case 'p' -> rowKey[4] |= KEY_RELEASED_BIT2; // 8
            case 'q' -> rowKey[4] &= KEY_PRESSED_BIT1;  // 9
            case 'r' -> rowKey[4] |= KEY_RELEASED_BIT1; // 9
            case 's' -> rowKey[4] &= KEY_PRESSED_BIT0;  // 0
            case 't' -> rowKey[4] |= KEY_RELEASED_BIT0; // 0
            case 'u' -> rowKey[2] &= KEY_PRESSED_BIT0;  // Q
            case 'v' -> rowKey[2] |= KEY_RELEASED_BIT0; // Q
            case 'w' -> rowKey[2] &= KEY_PRESSED_BIT1;  // W
            case 'x' -> rowKey[2] |= KEY_RELEASED_BIT1; // W
            case 'y' -> rowKey[2] &= KEY_PRESSED_BIT2;  // E
            case 'z' -> rowKey[2] |= KEY_RELEASED_BIT2; // E
            case 'A' -> rowKey[2] &= KEY_PRESSED_BIT3;  // R
            case 'B' -> rowKey[2] |= KEY_RELEASED_BIT3; // R
            case 'C' -> rowKey[2] &= KEY_PRESSED_BIT4;  // T
            case 'D' -> rowKey[2] |= KEY_RELEASED_BIT4; // T
            case 'E' -> rowKey[5] &= KEY_PRESSED_BIT4;  // Y
            case 'F' -> rowKey[5] |= KEY_RELEASED_BIT4; // Y
            case 'G' -> rowKey[5] &= KEY_PRESSED_BIT3;  // U
            case 'H' -> rowKey[5] |= KEY_RELEASED_BIT3; // U
            case 'I' -> rowKey[5] &= KEY_PRESSED_BIT2;  // I
            case 'J' -> rowKey[5] |= KEY_RELEASED_BIT2; // I
            case 'K' -> rowKey[5] &= KEY_PRESSED_BIT1;  // O
            case 'L' -> rowKey[5] |= KEY_RELEASED_BIT1; // O
            case 'M' -> rowKey[5] &= KEY_PRESSED_BIT0;  // P
            case 'N' -> rowKey[5] |= KEY_RELEASED_BIT0; // P
            case 'O' -> rowKey[1] &= KEY_PRESSED_BIT0;  // A
            case 'P' -> rowKey[1] |= KEY_RELEASED_BIT0; // A
            case 'Q' -> rowKey[1] &= KEY_PRESSED_BIT1;  // S
            case 'R' -> rowKey[1] |= KEY_RELEASED_BIT1; // S
            case 'S' -> rowKey[1] &= KEY_PRESSED_BIT2;  // D
            case 'T' -> rowKey[1] |= KEY_RELEASED_BIT2; // D
            case 'U' -> rowKey[1] &= KEY_PRESSED_BIT3;  // F
            case 'V' -> rowKey[1] |= KEY_RELEASED_BIT3; // F
            case 'W' -> rowKey[1] &= KEY_PRESSED_BIT4;  // G
            case 'X' -> rowKey[1] |= KEY_RELEASED_BIT4; // G
            case 'Y' -> rowKey[6] &= KEY_PRESSED_BIT4;  // H
            case 'Z' -> rowKey[6] |= KEY_RELEASED_BIT4; // H
            case '[' -> rowKey[0] &= KEY_PRESSED_BIT3;  // C
            case ']' -> rowKey[0] |= KEY_RELEASED_BIT3; // C
            case '^' -> rowKey[7] |= KEY_RELEASED_BIT0; // Break/Space
            case '{' -> rowKey[7] &= KEY_PRESSED_BIT2;  // M
            case '}' -> rowKey[7] |= KEY_RELEASED_BIT2; // M
            default -> {
            }
        }
    }
}
