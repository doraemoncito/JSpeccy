package zxinfo;

import client.api.ZxinfoApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zxinfo.model.Game;
import zxinfo.model.GameFile;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jose.hernandez
 */
@Service
@Slf4j
public class ZxinfoService {

    private static final String JSON_PATH_HITS = "/hits/hits";  // NOI18N
    private static final String JSON_PATH_SOURCE = "/_source";  // NOI18N

    private final ObjectMapper objectMapper;
    private final ZxinfoApi zxinfoApi;
    private final String zxDownloadBasePath;

    @Inject
    public ZxinfoService(final ObjectMapper objectMapper, final ZxinfoApi zxinfoApi, final String zxDownloadBasePath) {

        this.objectMapper = objectMapper;
        this.zxinfoApi = zxinfoApi;
        this.zxDownloadBasePath = zxDownloadBasePath;
    }

    public List<Game> getGamesByLetter(char letter) {

        try {
            int total = countGamesByLetter(letter);
            ArrayList<Game> gameList = new ArrayList<>(total);

            // And now retrieve the complete game list
            String response = zxinfoApi.getGamesByLetter(Character.toString(letter),
                    "SOFTWARE", null,
                    "compact",
                    total,
                    0,
                    null);

            var hits = objectMapper.readValue(response, JsonNode.class);
            hits.at(JSON_PATH_HITS).elements().forEachRemaining(hit -> {

                // We will only list games that are available for download
                boolean isAvailable = hit.at(JSON_PATH_SOURCE).get("availability").asText().equals("Available");
                String machineType = hit.at(JSON_PATH_SOURCE).get("machineType").asText();

                if (isAvailable && isSupportedMachineType(machineType)) {
                    Game.GameBuilder gameBuilder = Game.aGame()
                            .withId(hit.get("_id").asText())
                            .withTitle(hit.at(JSON_PATH_SOURCE).get("title").asText())
                            .withMachineType(machineType);

                    JsonNode releases = hit.at("/_source/releases");
                    releases.elements().forEachRemaining(release -> release.at("/files").elements()
                            .forEachRemaining(file -> {
                                String format = file.get("format").asText();
                                if (isSupportedFormat(format)) {
                                    String path = file.get("path").asText();
                                    String spec = zxDownloadBasePath + path;
                                    try {
                                        URL url = new URL(spec);
                                        gameBuilder.withGameFile(GameFile.aGameFile()
                                                .withFormat(format)
                                                .withPath(path)
                                                .withUrl(url)
                                                .withSize(file.get("size").asInt())
                                                .build());
                                    }
                                    catch (MalformedURLException e) {
                                        log.error("An invalid URL was encountered whilst attempting to create a game file object: '{}'", spec);
                                    }
                                }
                            }));

                    Game game = gameBuilder.build();
                    if (!game.getGameFiles().isEmpty()) {
                        gameList.add(game);
                    }

                }
            });

            return gameList;
        }
        catch (JsonProcessingException e) {
            log.error("An error was encountered whilst attempting to process a request to retrieve a list of games");
        }

        return Collections.emptyList();
    }

    private boolean isSupportedMachineType(final String machineType) {

        return switch (machineType) {
            case "ZX-Spectrum 48K", "ZX-Spectrum 128K", "ZX-Spectrum 48K/128K", "ZX-Spectrum 128 +3" -> true;
            default -> false;
        };
    }

    private boolean isSupportedFormat(final String format) {

        return switch (format) {
            case "Tape (TAP)", "Perfect tape (TZX)", "Snapshot (Z80)" -> true;
            default -> false;
        };
    }

    private int countGamesByLetter(final char letter) throws JsonProcessingException {
        // A remote call to return 0 games by letter will tell us what's the number of game entries available
        var response = zxinfoApi.getGamesByLetter(Character.toString(letter),
                "SOFTWARE", null,
                "compact",
                0,
                0,
                null);

        var root = objectMapper.readValue(response, JsonNode.class);
        int total = root.at("/hits/total").get("value").asInt();
        log.info("The total number of entries for letter '{}' is {}", letter, total);
        return total;
    }

}
