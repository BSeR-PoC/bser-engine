package edu.gatech.chai.bserengine.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan("edu.gatech.chai.SmartOnFhirClient")
public class FhirServerConfig {
}