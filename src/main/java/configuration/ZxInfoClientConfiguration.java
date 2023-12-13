package configuration;

import client.ApiClient;
import client.api.ZxinfoApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zxinfo.ZxinfoService;

import javax.inject.Inject;

@Configuration
@Slf4j
public class ZxInfoClientConfiguration {

    @Value("${zx-info.base-path:https://api.zxinfo.dk/v3}")
    private String zxInfoBasePath = "<please configure the ZX info server base path in the application properties file>";

    @Value("${zx-download.base-path:https://worldofspectrum.org}")
    private String zxDownloadBasePath = "<please configure the ZX download base path in the application properties file>";

    private static final String USER_AGENT = "JSpeccy";

    @Bean
    public ApiClient zxinfoApiClient() {

        final ApiClient apiClient = new ApiClient();

        apiClient.setBasePath(zxInfoBasePath);
        apiClient.setUserAgent(USER_AGENT);
        // Enable OpenAPI debugging if debug logging is enabled.
        apiClient.setDebugging(log.isDebugEnabled());

        return apiClient;
    }

    @Bean
    public ZxinfoApi zxinfoApi() {

        return new ZxinfoApi(zxinfoApiClient());
    }

    @Bean
    public String zxDownloadBasePath() {

        return zxDownloadBasePath;
    }

    @Bean
    @Inject
    public ZxinfoService zxinfoService(final ObjectMapper objectMapper, final ZxinfoApi zxinfoApi, final String zxDownloadBasePath) {

        return new ZxinfoService(objectMapper, zxinfoApi, zxDownloadBasePath);
    }

}
