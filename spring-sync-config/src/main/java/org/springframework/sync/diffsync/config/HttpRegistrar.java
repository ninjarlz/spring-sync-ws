package org.springframework.sync.diffsync.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.sync.diffsync.web.JsonPatchHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Configuration adapter for Differential Synchronization HTTP communication in Spring.
 * @author Michał Kuśmidrowicz
 */
@Configuration
public class HttpRegistrar implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new JsonPatchHttpMessageConverter());
        converters.add(new MappingJackson2HttpMessageConverter());
    }
}
