package machine.joystick;


public interface Joystick {

    void handleKeyPressed(final int key, final boolean shiftPressed, final int[] rowKey);

    void handleKeyReleased(final int key, final boolean shiftPressed, final int[] rowKey);

}
