/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package snapshots;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jsanchez
 */
/* TODO: replace the @Data annotation with the @Value annotation to make the class immutable once all the code that
 * uses this business object has been refactored to use the supporting builder class.
 */
@Data
@Builder(toBuilder = true, builderMethodName = "aMemoryState", setterPrefix = "with")
public class MemoryState {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private byte[][] ram;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private byte[][] lecRam;

    private byte[] IF2Rom;
    private byte[] multifaceRam;
    private int portFD;
    private boolean IF1RomPaged;
    private boolean IF2RomPaged;
    private boolean multifacePaged;
    private boolean multifaceLocked;
    private boolean mf128on48k;

    public byte[] getPageRam(int page) {
        return ram[page];
    }

    public void setPageRam(int page, byte[] memory) {
        ram[page] = memory;
    }

    // Método de conveniencia para los snapshots Z80
    public byte readByte(int page, int address) {
            return ram[page][address];
    }

    public boolean isLecPaged() {
        return (portFD & 0x80) != 0;
    }

    public byte[] getLecPageRam(int page) {
        return lecRam[page];
    }

    public void setLecPageRam(int page, byte[] ram) {
        lecRam[page] = ram;
    }

    public static class MemoryStateBuilder {

        private byte[][] ram = new byte[8][];

        private byte[][] lecRam = new byte[16][];

        public MemoryStateBuilder withPageRam(int page, byte[] memory) {
            this.ram[page] = memory;
            return this;
        }

        public MemoryStateBuilder withLecPageRam(int page, byte[] ram) {
            this.lecRam[page] = ram;
            return this;
        }

    }

}
