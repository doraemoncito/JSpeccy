package jspeccy;

import configuration.ObjectMapperConfiguration;
import configuration.ZxInfoClientConfiguration;
import gui.JSpeccy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
// This property source containing build information is generated at runtime by the spring-boot maven plug-in
@PropertySource("classpath:META-INF/build-info.properties")
@Import({
        ObjectMapperConfiguration.class,
        ZxInfoClientConfiguration.class
})
public class JSpeccyApplicationConfiguration {

    @Bean
    public BundlePropertyResolver bundlePropertyResolver() {

        return new BundlePropertyResolver();
    }

    @Bean
    public JSpeccy jSpeccy() {

        return new JSpeccy();
    }

}
