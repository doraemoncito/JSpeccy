package utilities;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class TapeTableModelTest {

    private Tape tape;
    private TapeTableModel model;

    @BeforeEach
    void setUp() {
        tape = Mockito.mock(Tape.class);
        model = new TapeTableModel(tape);
    }

    @Test
    void testGetColumnName() {
        final String[] expectedColumnNames = {
                "Block #", "Block Type", "Block information", "COLUMN ERROR!"
        };

        List<String> actualColumnNames = IntStream.range(0, 4)
                .mapToObj(model::getColumnName)
                .toList();

        assertThat("Column name matches expectation", actualColumnNames, contains(expectedColumnNames));
    }

}
