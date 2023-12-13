package zxinfo;

import client.api.ZxinfoApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import configuration.ObjectMapperConfiguration;
import configuration.ZxInfoClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import zxinfo.model.Game;
import zxinfo.model.GameFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest(
        classes = {
                ObjectMapperConfiguration.class,
                ZxInfoClientConfiguration.class
        },
        properties = {"zx-info.base-path=https://api.zxinfo.dk/v3"}
)
@Slf4j
class ZxinfoTest {

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final int CONNECT_TIMEOUT = 3000;
    public static final int READ_TIMEOUT = 3000;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ZxinfoApi zxinfoApi;

    @Disabled("This test is currently used for manual testing ONLY")
    @Test
    @DisplayName("Get list of games by letter returns a list of games when configured correctly")
    void testGetListOfGamesByLetter_returnsListOfGames_whenConfiguredCorrectly() throws IOException {

        ArrayList<Game> gameList = new ArrayList<>();
        String letter = "a";
        String response = zxinfoApi.getGamesByLetter(letter,
                "SOFTWARE",
                null,
                "compact",
                10,
                0,
                null);

        var root = objectMapper.readValue(response, JsonNode.class);
        log.info("\n{}", gson.toJson(objectMapper.readValue(response, Map.class)));
        int total = root.at("/hits/total").get("value").asInt();
        log.info("The total number of entries for letter '{}' is {}", letter, total);
        root.at("/hits/hits").elements().forEachRemaining(hit -> {

            Game.GameBuilder gameBuilder = Game.aGame()
                    .withId(hit.get("_id").asText())
                    .withTitle(hit.at("/_source").get("title").asText())
                    .withMachineType(hit.at("/_source").get("machineType").asText());

            hit.at("/_source/releases").elements().forEachRemaining(release ->
                    release.at("/files").elements().forEachRemaining(file -> {
                        String format = file.get("format").asText();
                        if (format.equals("Tape (TAP)") || format.equals("Perfect tape (TZX)") || format.equals("Snapshot (Z80)")) {
                            gameBuilder.withGameFile(GameFile.aGameFile()
                                    .withFormat(format)
                                    .withPath(file.get("path").asText())
                                    .withSize(file.get("size").asInt())
                                    .build());
                        }
                    }));

            Game game = gameBuilder.build();
            if (!game.getGameFiles().isEmpty()) {
                gameList.add(game);
            }
        });

        log.trace(gson.toJson(gameList));

        boolean download = false;
        if (download) {
            String path = gameList.get(0).getGameFiles().get(0).getPath();
            var url = new URL("https://worldofspectrum.org" + path);
            var filename = Paths.get(path).getFileName();
            FileUtils.copyURLToFile(url, new File(filename.toString()), CONNECT_TIMEOUT, READ_TIMEOUT);
        }

        log.trace(gson.toJson(objectMapper.readValue(response, Map.class)));
    }

}
