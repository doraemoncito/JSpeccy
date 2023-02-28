package snapshots;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class MemoryStateTest {

    @Test
    @DisplayName("Memory state can be saved and loaded back correctly")
    public void test_memoryStateCanBeSavedAndLoadedBackCorrectly() {
        byte[] expectedRAM = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        MemoryState memoryState = MemoryState.aMemoryState()
                .withPageRam(1, expectedRAM)
                .withMultifaceLocked(true)
                .build();
        log.debug("Memory state: {}", memoryState);

        byte[] actualRAM = memoryState.getPageRam(1);
        assertAll("Memory state can be saved and loaded back correctly",
                () -> assertTrue(memoryState.isMultifaceLocked(), "Multiface Locked flag was saved and loaded correctly"),
                () -> assertArrayEquals(expectedRAM, actualRAM, "RAM page 1 was saved and loaded correctly"));
    }

}
