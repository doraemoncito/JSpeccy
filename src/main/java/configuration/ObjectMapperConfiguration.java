package configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for the Jackson object mapper
 *
 * @author jose.hernandez
 */
@Configuration
@Slf4j
public class ObjectMapperConfiguration {

    @Primary
    @Bean(name = "objectMapper")
    public ObjectMapper objectMapper() {

        log.info("Instantiating custom Jackson object mapper with date-time support");

        return new ObjectMapper()
                /*
                 * Only serialize those fields that are not null
                 */
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                /*
                 * The Java time modele is required to deserialize dates transmitted as arrays of date and time
                 * coordinates. I.e. "timestamp": [ 2020, 7, 17, 9, 33, 36, 686892000 ]
                 *
                 * https://stackoverflow.com/questions/55107588/problem-with-deserialization-of-localdatetime-in-junit-test
                 */
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                /* The ParameterNamesModule is required to allow Jackson to autodetect field names for the Java map to
                 * POJO conversion.
                 */
                .registerModule(new ParameterNamesModule())
                /*
                 * NOTE: The OpenAPI code generator should be annotating classes that use OpenAPI discriminators
                 * with @JsonIgnoreProperties(ignoreUnknown = true) to support inheritance but since the current
                 * implementation of the OpenAPI code generator fails to do add the annotation, we instead instruct
                 * the object mapper globally in this configuration.
                 *
                 *  @JsonIgnoreProperties(ignoreUnknown = true)
                 *  public class FooBarImpl extends BaseFooBar implements FooBar {
                 *
                 * For more information on these issues deserialization of polymorphic OpenAPI types please see:
                 *
                 * - [Deserialization of a polymorphic type based on presence of a property](https://github.com/FasterXML/jackson-databind/issues/1627)
                 * - [Deserializing polymorphic types with Jackson](https://stackoverflow.com/questions/24263904/deserializing-polymorphic-types-with-jackson/55333395#55333395)
                 */
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                /*
                 * We are using fluent accessors in the model objects, which in conjunction with the private fields in
                 * the model objects is preventing Jackson from deserializing into those fields by default. The lines
                 * below make these private fields visible to Jackson. Without this, Jackson is not able to set these
                 * values by default.
                 */
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

}
