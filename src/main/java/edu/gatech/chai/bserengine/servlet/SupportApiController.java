package edu.gatech.chai.bserengine.servlet;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import edu.gatech.chai.SmartOnFhirClient.SmartBackendServices;
import edu.gatech.chai.bserengine.utilities.StaticValues;

@Controller
@SessionAttributes("supportapis")
public class SupportApiController {
	final static Logger logger = LoggerFactory.getLogger(SupportApiController.class);

    @Autowired
	SmartBackendServices smartBackendServices;

    @GetMapping(path = "/jwks", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getJWKSet() {
        IParser parser = StaticValues.myFhirContext.newJsonParser();
        String retVal = "";
        HttpStatus retCode = HttpStatus.OK;

        try {
            retVal = smartBackendServices.getJWKS();
        } catch (Exception e) {
            e.printStackTrace();
            OperationOutcome oo = new OperationOutcome();
            Narrative narrative = new Narrative();
            narrative.setDivAsString("Failed to get JWK Set");
            oo.setText(narrative);
            retVal = parser.encodeResourceToString(oo);
            retCode = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<String>(retVal, retCode);
    }

    @GetMapping(path = "/patient", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPatient() {
        IParser parser = StaticValues.myFhirContext.newJsonParser();
        String retVal = "";
        HttpStatus retCode = HttpStatus.OK;

        try {
			String accessTokenJsonStr = smartBackendServices.getAccessToken(null);
            JSONObject accessTokenJson = new JSONObject(accessTokenJsonStr);
            String accessToken = accessTokenJson.getString("access_token");

            // Read test patient
            // IRestfulClientFactory clientFactory = StaticValues.myFhirContext.getRestfulClientFactory();
            BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);

            IGenericClient genericClient = StaticValues.myFhirContext.newRestfulGenericClient(smartBackendServices.getFhirServerUrl());
            genericClient.registerInterceptor(authInterceptor);

            // Bundle response = genericClient.search().forResource(Patient.class)
            //     .where(Patient.FAMILY.matches().values("Optime"))
            //     .returnBundle(Bundle.class)
            //     .execute();

            Patient response = genericClient.read().resource(Patient.class).withId("eXFljJT8WxVd2PjwvPAGR1A3").prettyPrint().execute();
            retVal = parser.encodeResourceToString(response);
			System.out.println("Patient: \n" + retVal);

            IBaseBundle responseBundle = genericClient.search().forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(new IdType("Patient", "eXFljJT8WxVd2PjwvPAGR1A3")))
                .where(Observation.CATEGORY.exactly().code("vital-signs")).execute();
            String retVal1 = parser.encodeResourceToString(responseBundle);
            System.out.println(("Bundle: \n") + retVal1);

            Practitioner practitioner = genericClient.read().resource(Practitioner.class)
                .withId("e69NcQgKKhcP0umjLTabc4w3").execute();
            String retVal2 = parser.encodeResourceToString(practitioner);

            responseBundle = genericClient.search().forResource(PractitionerRole.class)
                .where(PractitionerRole.PRACTITIONER.hasId("e69NcQgKKhcP0umjLTabc4w3")).execute();
            String retVal3 = parser.encodeResourceToString(responseBundle);

            retVal = "[" + retVal + ", " +retVal3 + ", " + retVal2 + ", " + retVal1 + "]";
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException
				| IOException e) {
			retCode = HttpStatus.INTERNAL_SERVER_ERROR;
			e.printStackTrace();
		}

        return new ResponseEntity<String>(retVal, retCode);
    }
}
