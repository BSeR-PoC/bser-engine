package edu.gatech.chai.bserengine.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import edu.gatech.chai.bserengine.provider.ServerOperations;
import edu.gatech.chai.bserengine.security.OIDCInterceptor;
import edu.gatech.chai.bserengine.utilities.StaticValues;
import jakarta.servlet.ServletException;

public class RestfulServerWithOpenApi extends RestfulServer {
    private static final long serialVersionUID = 1L;

    public RestfulServerWithOpenApi() {
        super(StaticValues.myFhirContext);
    }
    
    /**
	 * This method is called automatically when the servlet is initializing.
	 */
	@Override
	public void initialize() throws ServletException {
		// Set server name
		setServerName("BSeR v1.0.0 - FHIR R4");

		// If we have system environment variable to hardcode the base URL, do it now.
		String serverBaseUrl = System.getenv("SERVERBASE_URL");
		if (serverBaseUrl != null && !serverBaseUrl.isEmpty() && !serverBaseUrl.trim().equalsIgnoreCase("")) {
			serverBaseUrl = serverBaseUrl.trim();
			if (!serverBaseUrl.startsWith("http://") && !serverBaseUrl.startsWith("https://")) {
				serverBaseUrl = "https://" + serverBaseUrl;
			}

			if (serverBaseUrl.endsWith("/")) {
				serverBaseUrl = serverBaseUrl.substring(0, serverBaseUrl.length() - 1);
			}

			IServerAddressStrategy serverAddressStrategy = new HardcodedServerAddressStrategy(serverBaseUrl);
			setServerAddressStrategy(serverAddressStrategy);
		}

		/*
		 * Set non resource provider.
		 */
		List<Object> plainProviders = new ArrayList<Object>();
		ServerOperations serverOperations = new ServerOperations();

		/*
		 * add system to the plain provider.
		 */
		plainProviders.add(serverOperations);
		registerProviders(plainProviders);

		/*
		 * Add page provider. Use memory based on for now.
		 */
		FifoMemoryPagingProvider pp = new FifoMemoryPagingProvider(5);
		pp.setDefaultPageSize(50);
		pp.setMaximumPageSize(100000);
		setPagingProvider(pp);

		/*
		 * Use a narrative generator. This is a completely optional step, but can be
		 * useful as it causes HAPI to generate narratives for resources which don't
		 * otherwise have one.
		 */
		INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
		getFhirContext().setNarrativeGenerator(narrativeGen);

		/*
		 * Enable CORS
		 */
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedHeader("x-fhir-starter");
		config.addAllowedHeader("Origin");
		config.addAllowedHeader("Accept");
		config.addAllowedHeader("X-Requested-With");
		config.addAllowedHeader("Content-Type");
		config.addAllowedHeader("Authorization");

		config.addAllowedOrigin("*");
		
		config.addExposedHeader("Location");
		config.addExposedHeader("Content-Location");
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

		CorsInterceptor corsInterceptor = new CorsInterceptor(config);
		registerInterceptor(corsInterceptor);

		OpenApiInterceptor openApiInterceptor = new OpenApiInterceptor();
		registerInterceptor(openApiInterceptor);

		/*
		 * This server interceptor causes the server to return nicely formatter and
		 * coloured responses instead of plain JSON/XML if the request is coming from a
		 * browser window. It is optional, but can be nice for testing.
		 */
		registerInterceptor(new ResponseHighlighterInterceptor());

		OIDCInterceptor oIDCInterceptor = new OIDCInterceptor();
		registerInterceptor(oIDCInterceptor);
		
		/*
		 * Tells the server to return pretty-printed responses by default
		 */
		setDefaultPrettyPrint(true);

		/*
		 * Set response encoding.
		 */
		setDefaultResponseEncoding(EncodingEnum.JSON);

	}
}
