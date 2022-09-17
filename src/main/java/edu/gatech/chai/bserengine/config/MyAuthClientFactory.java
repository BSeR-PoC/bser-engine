package edu.gatech.chai.bserengine.config;

import javax.servlet.http.HttpServletRequest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.server.util.ITestingUiClientFactory;

public class MyAuthClientFactory implements ITestingUiClientFactory {

	@Override
	public IGenericClient newClient(FhirContext theFhirContext, HttpServletRequest theRequest,
			String theServerBaseUrl) {
		// Create a client
		IGenericClient client = theFhirContext.newRestfulGenericClient(theServerBaseUrl);

		String authBasic = System.getenv("AUTH_BASIC");
		String authBearer = System.getenv("AUTH_BEARER");
		if (authBasic != null && !authBasic.isEmpty()) {
			String[] basicCredential = authBasic.split(":");
			if (basicCredential.length == 2) {
				// Bust have two parameters
				String username = basicCredential[0];
				String password = basicCredential[1];

				client.registerInterceptor(new BasicAuthInterceptor(username, password));
			}
		} else if (authBearer != null && !authBearer.isEmpty()) {
			client.registerInterceptor(new BearerTokenAuthInterceptor(authBearer));
		}

//		theFhirContext.getRestfulClientFactory().setConnectionRequestTimeout(600000);
		theFhirContext.getRestfulClientFactory().setSocketTimeout(600000);

		return client;
	}

}
