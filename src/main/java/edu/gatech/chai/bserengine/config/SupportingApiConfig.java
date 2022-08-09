package edu.gatech.chai.bserengine.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = { "edu.gatech.chai.SmartOnFhirClient", "edu.gatech.chai.bserengine.servlet", "edu.gatech.chai" })
public class SupportingApiConfig {
    
}
