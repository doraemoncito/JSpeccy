package utilities;

import lombok.extern.slf4j.Slf4j;

import javax.swing.table.AbstractTableModel;
import java.util.ResourceBundle;

@Slf4j
class TapeTableModel extends AbstractTableModel {

    private final Tape tape;
    private final ResourceBundle bundle;

    public TapeTableModel(final Tape tape) {
        this(tape, ResourceBundle.getBundle("gui/Bundle")); // NOI18N
    }

    public TapeTableModel(final Tape tape, final ResourceBundle bundle) {
        this.tape = tape;
        this.bundle = bundle;
    }

    @Override
    public int getRowCount() {
        return tape.getNumBlocks();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(final int row, final int col) {

        log.debug("getValueAt row {}, col {}}", row, col);
        return switch (col) {
            case 0 -> String.format("%4d", row + 1);
            case 1 -> tape.getBlockType(row);
            case 2 -> tape.getBlockInfo(row);
            default -> "NON EXISTENT COLUMN!";
        };
    }

    @Override
    public String getColumnName(final int col) {

        return switch (col) {
            case 0, 1, 2 -> bundle.getString("JSpeccy.tapeCatalog.columnModel.title" + col);
            default -> "COLUMN ERROR!";
        };
    }

}
