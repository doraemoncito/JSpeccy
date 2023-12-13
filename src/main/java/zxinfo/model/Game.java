package zxinfo.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * @author jose.hernandez
 */
@Value
@Builder(builderMethodName = "aGame", setterPrefix = "with", toBuilder = true)
public class Game {

    String id;
    String title;
    String machineType;

    @Singular
    List<GameFile> gameFiles;

}
