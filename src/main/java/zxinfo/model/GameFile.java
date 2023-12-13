package zxinfo.model;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.net.URL;

/**
 * @author jose.hernandez
 */
@Value
@Builder(builderMethodName = "aGameFile", setterPrefix = "with", toBuilder = true)
public class GameFile {

    // E.g. "/pub/sinclair/games/a/ABC.tzx.zip"
    String path;

    // Like the patch but includes the protocol and the hostname
    URL url;

    // E.g. "Perfect tape (TZX)"
    String format;

    int size;

    public String toString() {

        return String.format("%s, [%s]", getFormat(), getSize());
    }

}
