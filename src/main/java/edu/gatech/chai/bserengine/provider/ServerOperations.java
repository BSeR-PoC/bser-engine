/*******************************************************************************
 * Copyright (c) 2019 Georgia Tech Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package edu.gatech.chai.bserengine.provider;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.HealthcareService;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.MessageHeader.MessageDestinationComponent;
import org.hl7.fhir.r4.model.MessageHeader.MessageHeaderResponseComponent;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.codesystems.V3EducationLevel;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatusEnumFactory;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UrlType;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import edu.gatech.chai.BSER.model.BSERCoverage;
import edu.gatech.chai.BSER.model.BSERDiagnosis;
import edu.gatech.chai.BSER.model.BSEREducationLevel;
import edu.gatech.chai.BSER.model.BSERHA1CObservation;
import edu.gatech.chai.BSER.model.BSERMedicationStatement;
import edu.gatech.chai.BSER.model.BSERNRTAuthorizationStatus;
import edu.gatech.chai.BSER.model.BSEROrganization;
import edu.gatech.chai.BSER.model.BSERReferralFeedbackDocument;
import edu.gatech.chai.BSER.model.BSERReferralInitiatorPractitionerRole;
import edu.gatech.chai.BSER.model.BSERReferralMessageBundle;
import edu.gatech.chai.BSER.model.BSERReferralMessageHeader;
import edu.gatech.chai.BSER.model.BSERReferralRecipientPractitionerRole;
import edu.gatech.chai.BSER.model.BSERReferralRequestComposition;
import edu.gatech.chai.BSER.model.BSERReferralRequestDocumentBundle;
import edu.gatech.chai.BSER.model.BSERReferralServiceRequest;
import edu.gatech.chai.BSER.model.BSERReferralTask;
import edu.gatech.chai.BSER.model.BSERTelcomCommunicationPreferences;
import edu.gatech.chai.BSER.model.BSEREarlyChildhoodNutritionObservation;
import edu.gatech.chai.BSER.model.ODHEmploymentStatus;
import edu.gatech.chai.BSER.model.util.BSEREarlyChildhoodNutritionObservationUtil;
import edu.gatech.chai.BSER.model.util.BSERNRTAuthorizationStatusUtil;
import edu.gatech.chai.BSER.model.util.BSeRTelcomCommunicationPreferencesUtil;
import edu.gatech.chai.BSER.model.util.CommonUtil;
import edu.gatech.chai.SmartOnFhirClient.SmartBackendServices;
import edu.gatech.chai.USCore.model.USCoreBloodPressure;
import edu.gatech.chai.USCore.model.USCoreBodyHeight;
import edu.gatech.chai.USCore.model.USCoreBMI;
import edu.gatech.chai.USCore.model.USCoreBodyWeight;
import edu.gatech.chai.USCore.model.USCoreConditionProblemsAndHealthConcerns;
import edu.gatech.chai.USCore.model.USCoreSmokingStatusObservation;
import edu.gatech.chai.USCore.model.util.USCoreSmokingStatusObservationUtil;
import edu.gatech.chai.USCore.model.USCoreAllergyIntolerance;
import edu.gatech.chai.bserengine.security.RecipientAA;
import edu.gatech.chai.bserengine.utilities.BserTaskBusinessStatus;
import edu.gatech.chai.bserengine.utilities.StaticValues;

public class ServerOperations {
	private static final Logger logger = LoggerFactory.getLogger(ServerOperations.class);

	SmartBackendServices smartBackendServices;
	RecipientAA recipientAA;
	String fhirStore = null;
	String bserEndpointUrl = null;

	public static enum ServiceType {
		ARTHRITIS ("arthritis", "Arthritis"),
		DIABETES_PREVENTION ("diabetes-prevention", "Diabetes Prevention"),
		EARLY_CHILDHOOD_NUTRITION ("early-childhood-nutrition", "Early Childhood Nutrition"),
		HYPERTENSION ("hypertension", "Hypertension"),
		OBESITY ("obesity", "Obesity"),
		TOBACCO_USE_CESSATION ("tobacco-use-cessation", "Arthritis");

		private final String code;
		private final String display;

		ServiceType(String code, String display) {
			this.code = code;
			this.display = display;
		}

		public String getCode() {
			return this.code;
		}

		public String getDisplay() {
			return this.display;
		}

		public boolean is(String code) {
			return this.code.equals(code);
		}

		public CodeableConcept getCodeableConcept() {
			return new CodeableConcept(new Coding(null, getCode(), getDisplay())).setText(getDisplay());
		}
	}

	public static String BserReferralRecipientPractitionerRoleProfile = "http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralRecipientPractitionerRole";
	public static String BserReferralInitiatorPractitionerRoleProfile = "http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralInitiatorPractitionerRole";
	public static String BserReferralServiceRequestProfile = "http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralServiceRequest";
	public static String BserReferralFeedbackDocumentBundleProfile = "http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralFeedbackDocumentBundle";
	
	public ServerOperations() {
		WebApplicationContext context = ContextLoaderListener.getCurrentWebApplicationContext();
		smartBackendServices = context.getBean(SmartBackendServices.class);
		recipientAA = context.getBean(RecipientAA.class);

		fhirStore = System.getenv("FHIRSTORE_URL");
		bserEndpointUrl = System.getenv("BSERENDPOINTE_URL");
		if (bserEndpointUrl == null || bserEndpointUrl.isBlank()) {
			logger.error("BSER Endpoint MUST set in the environment variable.");
			System.exit(-1);
		}
	}

	BearerTokenAuthInterceptor getBearerTokenAuthInterceptor() {
		String accessTokenJsonStr;
		try {
			accessTokenJsonStr = smartBackendServices.getAccessToken(null);
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException
				| IOException e) {
			e.printStackTrace();
			return null;
		}

		if (accessTokenJsonStr == null) {
			// Access token is null. 
			logger.debug("Access Token is NULL.");
			return null;
		}

		JSONObject accessTokenJson = new JSONObject(accessTokenJsonStr);
		String accessToken = accessTokenJson.getString("access_token");

		// IRestfulClientFactory clientFactory = StaticValues.myFhirContext.getRestfulClientFactory();
		BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);

		return authInterceptor;
	}

	private IBaseResource pullResourceFromFhirServer(Reference reference) {
		IBaseResource response = null;
		IGenericClient genericClient;
		String fhirBaseUrl = null;

		// check if this is local or full. If local, we are talking to the 
		if (reference.getReferenceElement().isLocal()) {
			fhirBaseUrl = fhirStore;
		} else {
			fhirBaseUrl = reference.getReferenceElement().getBaseUrl();
		}

		genericClient = StaticValues.myFhirContext.newRestfulGenericClient(fhirBaseUrl);
		if (smartBackendServices.setFhirServerUrl(fhirBaseUrl).isActive()) {
			BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();
			genericClient.registerInterceptor(authInterceptor);
		}

		String resourceType = reference.getReferenceElement().getResourceType();
		String resourceId = reference.getReferenceElement().getIdPart();
		System.out.println("resourceType:"+resourceType+" and resourceId:"+resourceId);
		response = genericClient.read().resource(resourceType).withId(resourceId).prettyPrint().execute();

		IParser parser = StaticValues.myFhirContext.newJsonParser();
		String responseString = parser.encodeResourceToString(response);
		System.out.println("Response: \n" + responseString);
		
		return response;
	}	

	private Bundle searchResourceFromFhirServer(String fhirServerUrl, Class<? extends IBaseResource> resourceClass, ICriterion<?> theCriterion, Include... includes) {
		IGenericClient genericClient;
		String fhirServerBaseUrl = fhirStore;

		if (fhirServerUrl != null && !fhirServerUrl.isBlank()) {
			// Check if the fhirServer is the same as the authBackendServices server.
			// If not, we need to setup this service.
			if (!fhirServerUrl.equals(fhirServerBaseUrl)) {
				smartBackendServices.setFhirServerUrl(fhirServerUrl);
				fhirServerBaseUrl = fhirServerUrl;
			}
		}

		genericClient = StaticValues.myFhirContext.newRestfulGenericClient(fhirServerBaseUrl);
		if (smartBackendServices.isActive()) {
			BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();
			genericClient.registerInterceptor(authInterceptor);
		}

		IQuery<IBaseBundle> searchWhere = genericClient.search().forResource(resourceClass).where(theCriterion);
		if (includes.length > 0) {
			for (Include include : includes) {
				searchWhere = searchWhere.include(include);
			}
		}

		return searchWhere.returnBundle(Bundle.class).execute();
	}

	private void saveResource (IBaseResource resource) {
		if (fhirStore == null || fhirStore.isBlank() || resource == null) {
			return;
		}

		IGenericClient genericClient = StaticValues.myFhirContext.newRestfulGenericClient(fhirStore);
		if (smartBackendServices.setFhirServerUrl(fhirStore).isActive()) {
			BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();
			genericClient.registerInterceptor(authInterceptor);
		}

		MethodOutcome createResponse = genericClient.create().resource(resource).execute();
		if (!createResponse.getCreated()) {
			OperationOutcome oo = (OperationOutcome) createResponse.getOperationOutcome();
			if (oo != null && !oo.isEmpty()) {
				boolean errorOccurred = false;
				for (OperationOutcomeIssueComponent ooIssue : oo.getIssue()) {
					if (IssueSeverity.ERROR == ooIssue.getSeverity() || IssueSeverity.FATAL == ooIssue.getSeverity()) {
						errorOccurred = true;
						break;
					}
				}

				if (errorOccurred) {
					String msg = (oo.getText()==null||oo.getText().isEmpty())?"":", "+oo.getText().getDivAsString();
					throw new FHIRException("FHIR store failed to persist, " + resource.getIdElement().toString() + msg);
				}
			} else {
				throw new FHIRException("FHIR store failed to persist, " + resource.getIdElement().toString());
			}
		}

		// we may have versioned Id from server. However, we will always deal with the latest version.
		// Thus, we only capture baseUrl, resourceType, and IdPart.
		String myBaseUrl = createResponse.getId().getBaseUrl();
		String myResourceType = createResponse.getId().getResourceType();
		String myIdPart = createResponse.getId().getIdPart();
		IdType myIdType = new IdType(myBaseUrl, myResourceType, myIdPart, null);

		resource.setId(myIdType);
	}

	private void updateResource (IBaseResource resource) {
		if (fhirStore == null || fhirStore.isBlank() || resource == null) {
			return;
		}

		IGenericClient genericClient = StaticValues.myFhirContext.newRestfulGenericClient(fhirStore);
		if (smartBackendServices.setFhirServerUrl(fhirStore).isActive()) {
			BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();
			genericClient.registerInterceptor(authInterceptor);
		}

		IdType myIdType = (IdType) resource.getIdElement();
		String myBaseUrl = myIdType.getBaseUrl();
		String myIdPart = myIdType.getIdPart();
		String myResourceType = myIdType.getResourceType();
		IdType noHistoryIdType = new IdType(myBaseUrl, myResourceType, myIdPart, null);
		resource.setId(noHistoryIdType);

		MethodOutcome updateResponse = genericClient.update().resource(resource).execute();
		IBaseOperationOutcome oo = updateResponse.getOperationOutcome();
		if (oo != null) {
			throw new FHIRException("BSeR enginen failed to persist external resource, " + resource.getIdElement().toString());
		}

		resource.setId(updateResponse.getId());
	}

	private OperationOutcome deleteResource(IBaseResource resource) {
		if (fhirStore == null || fhirStore.isBlank() || resource == null) {
			return null;
		}

		IGenericClient genericClient = StaticValues.myFhirContext.newRestfulGenericClient(fhirStore);
		if (smartBackendServices.setFhirServerUrl(fhirStore).isActive()) {
			BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();
			genericClient.registerInterceptor(authInterceptor);
		}

		MethodOutcome createResponse = genericClient.delete().resource(resource).execute();
		return (OperationOutcome) createResponse.getOperationOutcome();
	}

	private OperationOutcome constructErrorOO(String fhirPath, String message) {
		OperationOutcome oo = new OperationOutcome();
		oo.setId(UUID.randomUUID().toString());
		OperationOutcomeIssueComponent ooic = new OperationOutcomeIssueComponent();
		ooic.setSeverity(IssueSeverity.ERROR);
		ooic.setCode(IssueType.REQUIRED);
		if (fhirPath != null && !fhirPath.isBlank()) {
			ooic.addExpression(fhirPath);
		}
		ooic.setDiagnostics(message);
		oo.addIssue(ooic);

		return oo;
	}

	private void sendInternalErrorOO(String fhirPath, String msg) throws  InternalErrorException {
		OperationOutcome oo = constructErrorOO(fhirPath, msg);
		throw new InternalErrorException(msg, oo);
	}

	private boolean isEqualReference(Reference reference, Reference otherReference, String otherBaseUrl) {
		// We compare baseUrl, Resource Type, and Resource ID part. Also version if exists
		// We do not compare display. 
		if (reference == null && otherReference == null) return true;
		if (reference == null) return false;
		if (otherReference == null) return false;

		if (reference.isEmpty() && otherReference.isEmpty()) return true;		
		if (reference.isEmpty()) return false;
		if (otherReference.isEmpty()) return false;

		IdType referenceIdType = (IdType) reference.getReferenceElement();
		IdType otherReferenceType = (IdType) otherReference.getReferenceElement();

		// value contains full url. So, if these match, we return true.
		if (referenceIdType.getValue().equalsIgnoreCase(otherReferenceType.getValue())) return true;

		if (otherReference.getReferenceElement().getBaseUrl() != null) {
			otherBaseUrl = otherReference.getReferenceElement().getBaseUrl();
		}
		if (referenceIdType.getBaseUrl() != null) {
			if (!referenceIdType.getBaseUrl().equalsIgnoreCase(otherBaseUrl)) {
				return false;
			}
		} else if (otherBaseUrl != null) {
			return false;
		}

		if (referenceIdType.getResourceType() != null) {
			if (!referenceIdType.getResourceType().equals(otherReferenceType.getResourceType())) {
				return false;
			}
		} else if (otherReferenceType.getResourceType() != null) {
			return false;
		}

		if (referenceIdType.getIdPart() != null) {
			if (!referenceIdType.getIdPart().equalsIgnoreCase(otherReferenceType.getIdPart())) {
				return false;
			}
		} else if (otherReferenceType.getIdPart() != null) {
			return false;
		}

		if (referenceIdType.getVersionIdPart() != null) {
			if (!referenceIdType.getVersionIdPart().equalsIgnoreCase(otherReferenceType.getVersionIdPart())) {
				return false;
			}
		} else if (otherReferenceType.getVersionIdPart() != null) {
			return false;
		}

		return true;
	}

	Patient searchPatientFromFhirStore(Patient patient) {
		Patient patientFound = null;

		for (Identifier patientIdentifier : patient.getIdentifier()) {
			String system = patientIdentifier.getSystem();
			String code = patientIdentifier.getValue();
			ICriterion<TokenClientParam> criteria;
			if (system != null && !system.isBlank()) {
				criteria = Patient.IDENTIFIER.exactly().systemAndCode(system, code);
			} else {
				criteria = Patient.IDENTIFIER.exactly().code(code);
			}

			Bundle searchBundle = searchResourceFromFhirServer(
				fhirStore, 
				Patient.class, 
				criteria);
			
			if (searchBundle != null && !searchBundle.isEmpty()) {
				List<BundleEntryComponent> searchBundleEntry = searchBundle.getEntry();
				if (searchBundleEntry.size() > 0) {
					patientFound = (Patient) searchBundle.getEntryFirstRep().getResource();
					break;
				}
			}
		}

		return patientFound;
	}

	/***
	 * processReferral - processes the Referral Request.
	 * @param theServiceRequest
	 * @param thePatient
	 * @param thePractitioner
	 * @param theCoverage
	 * @param theBserProviderBaseUrl
	 * @param theServiceType
	 * @param theEducationLevel
	 * @param theEmploymentStatus
	 * @param theAllergies
	 * @param theBloodPressure
	 * @param theBodyHeight
	 * @param theBodyWeight
	 * @param theBmi
	 * @param theDiagnosis
	 * @param theIsBabyLatching
	 * @param theMomsConcerns
	 * @param theNippleShieldUse
	 * @param theHa1cObservation
	 * @param theMedications
	 * @param theNrtAuthorizationStatus
	 * @param theChild
	 * @param theSmokingStatus
	 * @param theCommunicationPreferences
	 * @return
	 */
	@Operation(name="$referral-request")
	public Parameters processReferral(
		@OperationParam(name="referral") ServiceRequest theServiceRequest,
		@OperationParam(name="patient") Patient thePatient,
		@OperationParam(name="requester") Practitioner theRequester,
		@OperationParam(name="coverage") Coverage theCoverage,
		@OperationParam(name="bserProviderBaseUrl") StringType theBserProviderBaseUrl,
		@OperationParam(name="serviceType") CodeType theServiceType,
		@OperationParam(name="educationLevel") CodeType theEducationLevel,
		@OperationParam(name="employmentStatus") CodeType theEmploymentStatus,
		@OperationParam(name="allergies") Bundle theAllergies,
		@OperationParam(name="bloodPressure") ParametersParameterComponent theBloodPressure,
		@OperationParam(name="bodyHeight") ParametersParameterComponent theBodyHeight,
		@OperationParam(name="bodyWeight") ParametersParameterComponent theBodyWeight,
		@OperationParam(name="bmi") ParametersParameterComponent theBmi,
		@OperationParam(name="diagnosis") List<ParametersParameterComponent> theDiagnosis,
		@OperationParam(name="isBabyLatching") BooleanType theIsBabyLatching,
		@OperationParam(name="momsConcerns") StringType theMomsConcerns,
		@OperationParam(name="nippleShieldUse") BooleanType theNippleShieldUse,
		@OperationParam(name="ha1cObservation") ParametersParameterComponent theHa1cObservation,
		@OperationParam(name="medications") Bundle theMedications,
		@OperationParam(name="nrtAuthorizationStatus") CodeType theNrtAuthorizationStatus,
		@OperationParam(name="child")  ParametersParameterComponent theChild,
		@OperationParam(name="smokingStatus") CodeType theSmokingStatus,
		@OperationParam(name="communicationPreferences") ParametersParameterComponent theCommunicationPreferences
	) {
		Reference sourceReference = null;
		Reference targetReference = null;
		Reference subjectReference = null;
		Practitioner sourcePractitioner = null;
		Practitioner targetPractitioner = null;
		PractitionerRole sourcePractitionerRole = null;
		PractitionerRole targetPractitionerRole = null;
		BSEROrganization sourceOrganization = null;
		BSEROrganization targetOrganization = null;
		Endpoint sourceEndpoint = null;
		Endpoint targetEndpoint = null;
		Reference sourceOrganizationReference = null;
		HealthcareService targetHealthService = null;
		String targetEndpointUrl = null;
		String warningMessage = new String();

		/*
		 * When referral request is received, capture the following information.
		 * fhirStore: any resources created or pulled from EHR will be persisted here.
		 * fhirEhr: patient and patient's clinical data are managed.
		 * 
		 * fhirStore: this URL can be obtained from Parameters, bserProviderBaseUrl, or ServiceRequest.performer.
		 * fhirEhr: this URL can be obtained from smartBackendServices.getFhirServerUrl(). If SMARTonFHIR backend services
		 *          is not configured, then ServiceRequest.requester will be used to get the FHIR server URL.
		 */

		boolean recipientReady = false;
		if (!"true".equalsIgnoreCase(System.getenv("RECIPIENT_NOT_READY"))) {
			recipientReady = true;
		}

		// ServiceRequest is required.
		if (theServiceRequest == null) {
			sendInternalErrorOO("Parameters.parameter.where(name='referral').empty()", "Referral is missing");
			return null;
		}

		// If we received this request, it means we are submitting the referral so it shouldn't be ACTIVE
		if (theServiceRequest.getStatus() == ServiceRequestStatus.ACTIVE) {
			if (!warningMessage.isBlank())
				warningMessage = warningMessage.concat(" The referral request has its status already set to ACTIVE.");
			else
				warningMessage = warningMessage.concat("The referral request has its status already set to ACTIVE.");
		}

		// Get fhirStore Url. This will be the FHIR server that will store BSeR resources
		if (theBserProviderBaseUrl != null) {
			fhirStore = theBserProviderBaseUrl.getValue();

			// set up SMART on FHIR backend service (if possible).
			smartBackendServices.setFhirServerUrl(fhirStore);
		} else {
			// sendInternalErrorOO("Parameters.parameter.where(name='bserProviderBaseUrl').empty()", "bserProviderBaseUrl parameter is missing");
			if (!warningMessage.isBlank())
				warningMessage = warningMessage.concat(" bserProviderBaseUrl is missing. ");
			else
				warningMessage = warningMessage.concat("bserProviderBaseUrl is missing. ");
		}

		// Create a ServiceRequest as this will be a main resource for UI and BSeR engine.
		// We may not have the complete service request as it is from UI. Create Service Request and copy current ones to
		// newly created service request. 
		BSERReferralServiceRequest serviceRequest = new BSERReferralServiceRequest();
		theServiceRequest.copyValues(serviceRequest);
		serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);

		// Check if the serviceRequest has a proper ID.
		if (serviceRequest.getIdElement() == null || serviceRequest.getIdElement().isEmpty()) {
			sendInternalErrorOO("ServiceRequest.id", "ServiceRequest does not have id");
		}

		// Sanity check on the ServiceRequest.subject is patient, and it matches with the received Patient resource.
		subjectReference = serviceRequest.getSubject();
		if (!"Patient".equals(subjectReference.getReferenceElement().getResourceType())) {
			sendInternalErrorOO("ServiceRequest.subject", "Subject must be Patient");
		}

		// Check thePatient parameter
		if (thePatient != null) {
			// Check if the included patient resource matches with the one in the serviceRequest. 
			if (!subjectReference.getReferenceElement().getIdPart().equals(thePatient.getIdElement().getIdPart())) {
				sendInternalErrorOO("ServiceRequest.subject", "Patient ID does not match between ServiceRequest.subject and patient.id");
			}
		} else {
			thePatient = (Patient) pullResourceFromFhirServer(subjectReference);
		}

		// Save this patient and rewrite the subject to serviceRequest.
		Patient patientExt = searchPatientFromFhirStore(thePatient);
		if (patientExt == null) {
			saveResource(thePatient);
		} else {
			thePatient = patientExt;
		}

		String subjectName = thePatient.getNameFirstRep().getGivenAsSingleString() + " " + thePatient.getNameFirstRep().getFamily();
		subjectReference = new Reference("Patient" + "/" + thePatient.getIdPart()).setDisplay(subjectName);
		serviceRequest.setSubject(subjectReference);

		// Get the initator PractitionerRole resource. This is ServiceRequest.requester.
		// MDI IG users PractitionerRole. First check the requester if it's practitioner or practitionerRole
		Reference requesterReference = serviceRequest.getRequester();
		if (requesterReference.isEmpty()) {
			sendInternalErrorOO("ServiceRequest.requester", "Requester is NULL");
		}

		if (theRequester == null) {
			if ("Practitioner".equals(requesterReference.getType())) {
				// We have practitioner. Get practitoinerRole from FHIR server.
				sourcePractitioner = (Practitioner) pullResourceFromFhirServer(requesterReference);
			}

			if (sourcePractitioner == null || sourcePractitioner.isEmpty()) {
				sendInternalErrorOO("", "Initiator could not be obtained from either requester or serviceRequest.requester");
			}
		} else {
			// We have source practitioner resource in the Parameters. 
			sourcePractitioner = theRequester;
		}
		
		// Search (or Get) PractitionerRole resource for initator (requester).
		// Include Organization and Endpoint along with PractitionerRole.
		Bundle searchBundle;
		if ("Practitioner".equals(requesterReference.getType())) {
			searchBundle = searchResourceFromFhirServer(
				// requesterReference may be Practitioner. Ok to use this URL as both practitioner and practitionerRole have the same base URL
				requesterReference.getReferenceElement().getBaseUrl(), 
				PractitionerRole.class, 
				PractitionerRole.PRACTITIONER.hasId(sourcePractitioner.getIdElement().getIdPart()),
				PractitionerRole.INCLUDE_ORGANIZATION, PractitionerRole.INCLUDE_ENDPOINT, PractitionerRole.INCLUDE_LOCATION);
		} else {
			searchBundle = searchResourceFromFhirServer(
				// requesterReference may be Practitioner. Ok to use this URL as both practitioner and practitionerRole have the same base URL
				requesterReference.getReferenceElement().getBaseUrl(), 
				PractitionerRole.class, 
				PractitionerRole.RES_ID.exactly().code(requesterReference.getReferenceElement().getIdPart()),
				PractitionerRole.INCLUDE_ORGANIZATION, PractitionerRole.INCLUDE_ENDPOINT, PractitionerRole.INCLUDE_PRACTITIONER, PractitionerRole.INCLUDE_LOCATION);
		}

		PractitionerRole sourceEhrPractitionerRole = null;
		Organization sourceEhrOrganization = null;
		Location sourceLocation = null;
		for (BundleEntryComponent entry : searchBundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource instanceof PractitionerRole) {
				sourceEhrPractitionerRole = (PractitionerRole) resource;
			} else if (resource instanceof Organization) {
				sourceEhrOrganization = (Organization) resource;
			} else if (resource instanceof Endpoint) {
				sourceEndpoint = (Endpoint) resource;
			} else if (resource instanceof Location) {
				sourceLocation = (Location) resource;
			}
		}

		// Create BSER initator PractitionerRole
		sourcePractitionerRole = new BSERReferralInitiatorPractitionerRole();
		if (sourceEhrPractitionerRole != null && !sourceEhrPractitionerRole.isEmpty()) {
			sourceEhrPractitionerRole.copyValues(sourcePractitionerRole);
		}
		//  else {
		// 	sourcePractitionerRole.setId(new IdType(sourcePractitionerRole.fhirType(), UUID.randomUUID().toString()));
		// }

		// BSeR IG requires Organization for InitiatorPractitionerRole
		// http://build.fhir.org/ig/HL7/bser/StructureDefinition-BSeR-ReferralInitiatorPractitionerRole.html
		sourceOrganization = new BSEROrganization(true, "Initiator Organization");
		sourceOrganization.addType(new CodeableConcept(new Coding("http://terminology.hl7.org/CodeSystem/organization-type", "prov", "Healthcare Provider")));
		if (sourceEhrOrganization != null && !sourceEhrOrganization.isEmpty()) {
			sourceEhrOrganization.copyValues(sourceOrganization);
		}
		//  else {
		// 	sourceOrganization.setId(new IdType(sourceOrganization.fhirType(), UUID.randomUUID().toString()));

		// 	// saveResource(sourceOrganization);
		// }

		saveResource(sourceOrganization);
		
		sourceOrganizationReference = new Reference(sourceOrganization.fhirType() + "/" + sourceOrganization.getIdPart());
		sourcePractitionerRole.setOrganization(sourceOrganizationReference);

		// We must provide Endpoint so that recipient can find the endpoint to send feedback back.
		String bserEndpointProcessMessageUrl = null;
		if (bserEndpointUrl.endsWith("/")) {
			bserEndpointProcessMessageUrl = bserEndpointUrl+"$process-message";
		} else {
			bserEndpointProcessMessageUrl = bserEndpointUrl+"/$process-message";
		}

		if (sourceEndpoint == null || sourceEndpoint.isEmpty()) {
			sourceEndpoint = new Endpoint();
			sourceEndpoint.setStatus(EndpointStatus.ACTIVE);
			sourceEndpoint.setConnectionType(new Coding("http://terminology.hl7.org/CodeSystem/endpoint-connection-type", "hl7-fhir-msg", null));
			sourceEndpoint.addPayloadType(new CodeableConcept().setText("BSeR Referral Request Message"));
			sourceEndpoint.setAddress(bserEndpointProcessMessageUrl);
			// sourceEndpoint.setId(new IdType(sourceEndpoint.fhirType(), UUID.randomUUID().toString()));
		}

		saveResource(sourceEndpoint);
		Reference sourceEndpointReference = new Reference(sourceEndpoint.fhirType() + "/" + sourceEndpoint.getIdPart());
		sourcePractitionerRole.setEndpoint(new ArrayList<Reference>(Arrays.asList(sourceEndpointReference)));

		saveResource(sourcePractitioner);
		Reference sourcePractitionerReference = new Reference(sourcePractitioner.fhirType() + "/" + sourcePractitioner.getIdPart());
		sourcePractitionerRole.setPractitioner(sourcePractitionerReference);
		
		saveResource(sourcePractitionerRole);
		sourceReference = new Reference(sourcePractitionerRole.fhirType() + "/" + sourcePractitionerRole.getIdPart());

		// set the srouceReference with the final practitionerRole.
		// sourceReference = new Reference (new IdType(
		// 	sourcePractitionerRole.getIdElement().getBaseUrl(), 
		// 	sourcePractitionerRole.fhirType(), 
		// 	sourcePractitionerRole.getIdElement().getIdPart(), 
		// 	sourcePractitionerRole.getIdElement().getVersionIdPart()));
		
		// Get target (or recipient) practitioner resource. include practitioner, organization, endpoint, and healthService resources.
		targetReference = serviceRequest.getPerformerFirstRep();
		searchBundle = searchResourceFromFhirServer(
			targetReference.getReferenceElement().getBaseUrl(), 
			PractitionerRole.class, 
			PractitionerRole.RES_ID.exactly().code(targetReference.getReferenceElement().getIdPart()),
			PractitionerRole.INCLUDE_PRACTITIONER, PractitionerRole.INCLUDE_ORGANIZATION, PractitionerRole.INCLUDE_ENDPOINT, PractitionerRole.INCLUDE_SERVICE, PractitionerRole.INCLUDE_LOCATION);

		PractitionerRole targetEhrPractitionerRole = null;
		Organization targetEhrOrganization = null;
		Location targetLocation = null;
		for (BundleEntryComponent entry : searchBundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource instanceof PractitionerRole) {
				targetEhrPractitionerRole = (PractitionerRole) resource;
			} else if (resource instanceof Practitioner) {
				targetPractitioner = (Practitioner) resource;
			} else if (resource instanceof Organization) {
				targetEhrOrganization = (Organization) resource;
			} else if (resource instanceof Endpoint) {
				targetEndpoint = (Endpoint) resource;
			} else if (resource instanceof HealthcareService) {
				targetHealthService = (HealthcareService) resource;
			} else if (resource instanceof Location) {
				targetLocation = (Location) resource;
			}
		}

		if (targetEhrPractitionerRole == null || targetEhrPractitionerRole.isEmpty()) {
			sendInternalErrorOO("ServiceRequest.performer", "The ServiceRequest.performer: " + targetReference.getReference() + " does not seem to exist.");
		}

		targetOrganization = new BSEROrganization(true, "Recipient Organzation");
		if (targetEhrOrganization == null || targetEhrOrganization.isEmpty()) {
			// We were not able to resolve the targetOrganization from EHR. 
			// Construct one as best as you can
			// See if we can get some information from targetEhrPractitionerRole.
			// If we had a reference, we should've been able to get it from include. 
			// See if we have identifier ane/or display
			Reference targetOrgReferenceIn = targetEhrPractitionerRole.getOrganization();
			targetOrganization.addType(new CodeableConcept(new Coding("http://terminology.hl7.org/CodeSystem/organization-type", "bus", "Non-Healthcare Business or Corporation")));
			if (targetOrgReferenceIn != null && !targetOrgReferenceIn.isEmpty()) {
				Identifier targetOrgId = targetOrgReferenceIn.getIdentifier();
				if (targetOrgId != null && !targetOrgId.isEmpty()) {
					targetOrganization.addIdentifier(targetOrgId);
				}

				String targetOrgDisp = targetOrgReferenceIn.getDisplay();
				if (targetOrgDisp != null && !targetOrgDisp.isEmpty()) {
					targetOrganization.setName(targetOrgDisp);
				}
			}
			targetOrganization.setId(new IdType(targetOrganization.fhirType(), UUID.randomUUID().toString()));
		} else {
			targetEhrOrganization.copyValues(targetOrganization);
		}

		// Find the endpoint for thie target.
		if (recipientReady && targetEndpoint != null && !targetEndpoint.isEmpty() && targetEndpoint.getAddress() != null && !targetEndpoint.getAddress().isEmpty()) {
			targetEndpointUrl = targetEndpoint.getAddress();
		} else {
			if (!warningMessage.isBlank()) {
				warningMessage = warningMessage.concat(" Recipient is not ready or target Endpoing is not available");
			} else {
				warningMessage = warningMessage.concat("Recipient is not ready or target Endpoing is not available");
			}
			targetEndpointUrl = "http://recipient.notready.or.test/";
			targetEndpoint = new Endpoint(
				new Enumeration<EndpointStatus>(new EndpointStatusEnumFactory(), EndpointStatus.TEST), 
				new Coding("http://terminology.hl7.org/CodeSystem/endpoint-connection-type", "hl7-fhir-msg", ""), 
				new UrlType(targetEndpointUrl));

			targetEndpoint.addPayloadType(new CodeableConcept().setText("BSeR Referral Request Message"));
			targetEndpoint.setId(new IdType(targetEndpoint.fhirType(), UUID.randomUUID().toString()));
			targetEhrPractitionerRole.addEndpoint(new Reference(targetEndpoint.fhirType()+"/"+targetEndpoint.getIdPart()));
			// throw new FHIRException("RecipientPractitionerRole.Endpoint and Endpoint.address cannot be null or empty");
		}

		Reference targetEndpointReference = new Reference(targetEndpoint.fhirType()+"/"+targetEndpoint.getIdPart());


		// if (!targetEndpointUrl.endsWith("$process-message")) {
		// 	warningMessage = warningMessage.concat("The recipient endpoint URL does not end with $process-message. The $process-message is prepended. ");
		// 	if (targetEndpointUrl.endsWith("/")) {
		// 		targetEndpointUrl = targetEndpointUrl.concat("$process-message");
		// 	} else {
		// 		targetEndpointUrl = targetEndpointUrl.concat("/$process-message");
		// 	}
		// }
		
		// Create BSER recipient PractitionerRole
		targetPractitionerRole = new BSERReferralRecipientPractitionerRole();
		targetEhrPractitionerRole.copyValues(targetPractitionerRole);
		
		BSEREducationLevel educationLevel = null;
		if (theEducationLevel != null) {
			// Create and save the EducationLevel Observation
			// http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-EducationLevel
			V3EducationLevel v3EducationLevel = V3EducationLevel.fromCode(theEducationLevel.getCode());
			educationLevel = new BSEREducationLevel(new CodeableConcept(new Coding(v3EducationLevel.getSystem(), v3EducationLevel.toCode(), v3EducationLevel.getDisplay())));
			educationLevel.setId(new IdType(educationLevel.fhirType(), UUID.randomUUID().toString()));
			educationLevel.setStatus(ObservationStatus.FINAL);
			educationLevel.setSubject(subjectReference);

			saveResource(educationLevel);
		}

		ODHEmploymentStatus odhEmploymentStatus =  null;
		if (theEmploymentStatus != null) {
			// http://hl7.org/fhir/us/odh/StructureDefinition/odh-EmploymentStatus
			odhEmploymentStatus = new ODHEmploymentStatus(subjectReference);
			odhEmploymentStatus.setId(new IdType(odhEmploymentStatus.fhirType(), UUID.randomUUID().toString()));
			odhEmploymentStatus.setStatus(ObservationStatus.FINAL);
			odhEmploymentStatus.setValue(new CodeableConcept(new Coding("http://terminology.hl7.org/CodeSystem/v3-ObservationValue", theEmploymentStatus.getCode(), null)));
			odhEmploymentStatus.setSubject(subjectReference);

			saveResource(odhEmploymentStatus);
		}

		// Now, we should be ready to Referral Request Document Bundle.
		// Create supporting info resources, which their references will be placed in the composition.section

		List<Resource> supportingInfoResources = new ArrayList<Resource>();
		List<Reference> allergyReferences = new ArrayList<Reference>();
		if (theAllergies != null && !theAllergies.isEmpty()) {
			for (BundleEntryComponent allergy : theAllergies.getEntry()) {
				USCoreAllergyIntolerance usCoreAllergyIntolerance = new USCoreAllergyIntolerance();
				AllergyIntolerance allergyIntolerance = (AllergyIntolerance) allergy.getResource();
				allergyIntolerance.copyValues(usCoreAllergyIntolerance);
				// Sanity check on allergy received for the patient reference. If the allergy has no baseUrl, we only check resource type and id.
				if (!isEqualReference(subjectReference, usCoreAllergyIntolerance.getPatient(), subjectReference.getReferenceElement().getBaseUrl())) {
					sendInternalErrorOO("AllergyIntolerance.patient", "the Patient reference does not match with ServiceRequest.subject");
				}

				// Add Allergy to the list
				supportingInfoResources.add(usCoreAllergyIntolerance);
				allergyReferences.add(new Reference(usCoreAllergyIntolerance.fhirType()+"/"+usCoreAllergyIntolerance.getIdPart()));

				saveResource(usCoreAllergyIntolerance);
			}
		}

		List<Reference> medicationReferences = new ArrayList<Reference>();
		if (theMedications != null && !theMedications.isEmpty()) {
			for (BundleEntryComponent med :  theMedications.getEntry()) {
				BSERMedicationStatement bserMedicationStatement = new BSERMedicationStatement();
				MedicationStatement medicationStatement = (MedicationStatement) med.getResource();
				medicationStatement.copyValues(bserMedicationStatement);
				// Sanity check on allergy received for the patient reference. If the allergy has no baseUrl, we only check resource type and id.
				if (!isEqualReference(subjectReference, bserMedicationStatement.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
					sendInternalErrorOO("AllergyIntolerance.patient", "the Patient reference does not match with ServiceRequest.subject");
				}

				// Add Allergy to the list
				supportingInfoResources.add(bserMedicationStatement);
				medicationReferences.add(new Reference(bserMedicationStatement.fhirType()+"/"+bserMedicationStatement.getIdPart()));

				saveResource(bserMedicationStatement);
			}
		}

		Reference bpReference = null;
		if (theBloodPressure != null && !theBloodPressure.isEmpty()) {
			USCoreBloodPressure bpObservation = new USCoreBloodPressure();
			boolean referenced = false;
			for (ParametersParameterComponent bpParam : theBloodPressure.getPart()) {
				if ("reference".equals(bpParam.getName())) {
					// This is reference to the BP observation. Pull this resource
					// from EHR
					bpObservation = (USCoreBloodPressure) pullResourceFromFhirServer((Reference)bpParam.getValue());
					if (!isEqualReference(subjectReference, bpObservation.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
						sendInternalErrorOO("bloodPressure.subject", "the Subject reference does not match with ServiceRequest.subject");
					}
					referenced = true;
					break;
				} else { 
					if ("date".equals(bpParam.getName())) {
						bpObservation.setEffective((DateTimeType)bpParam.getValue());
					} else if ("diastolic".equals(bpParam.getName())) {
						bpObservation.setDiastolic((Quantity) bpParam.getValue());
					} else if ("systolic".equals(bpParam.getName())) {
						bpObservation.setSystolic((Quantity) bpParam.getValue());
					}
				}
			}

			if (!referenced) {
				bpObservation.setSubject(subjectReference);
				bpObservation.setStatus(ObservationStatus.FINAL);

				// write to fhirStore.
				saveResource(bpObservation);
			}
			supportingInfoResources.add(bpObservation);
			bpReference = new Reference(bpObservation.fhirType()+"/"+bpObservation.getIdPart());
		}

		Reference bodyHeightReference = null;
		if (theBodyHeight != null) {
			USCoreBodyHeight bodyHeightObservation = null;
			Type theBodyHeightValue = theBodyHeight.getValue();
			if (theBodyHeightValue instanceof Reference) {
				StringType referenceString = (StringType) theBodyHeightValue;	
				Observation bodyHeightEhrObservation = (Observation) pullResourceFromFhirServer(new Reference(referenceString.asStringValue()));
				if (!isEqualReference(subjectReference, bodyHeightEhrObservation.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
					sendInternalErrorOO("bodyHeight.subject", "the Subject reference does not match with ServiceRequest.subject");
				}

				bodyHeightObservation = new USCoreBodyHeight();
				bodyHeightEhrObservation.copyValues(bodyHeightObservation);
			} else if (theBodyHeightValue instanceof Quantity) {
				bodyHeightObservation = new USCoreBodyHeight((Quantity) theBodyHeightValue);
				bodyHeightObservation.setSubject(subjectReference);
				bodyHeightObservation.setStatus(ObservationStatus.FINAL);
				
				// write to fhirStore.
				saveResource(bodyHeightObservation);
			} 

			supportingInfoResources.add(bodyHeightObservation);
			bodyHeightReference = new Reference(bodyHeightObservation.fhirType()+"/"+bodyHeightObservation.getIdPart());
		}

		Reference bodyWeightReference = null;
		if (theBodyWeight != null) {
			USCoreBodyWeight bodyWeightObservation = null;
			Type theBodyWeightValue = theBodyWeight.getValue();
			if (theBodyWeightValue instanceof Reference) {
				Observation bodyWeightEhrObservation = (Observation) pullResourceFromFhirServer((Reference)theBodyWeightValue);
				if (!isEqualReference(subjectReference, bodyWeightEhrObservation.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
					sendInternalErrorOO("bodyWeight.subject", "the Subject reference does not match with ServiceRequest.subject");
				}

				bodyWeightObservation = new USCoreBodyWeight();
				bodyWeightEhrObservation.copyValues(bodyWeightObservation);
			} else if (theBodyWeightValue instanceof Quantity) {
				bodyWeightObservation = new USCoreBodyWeight((Quantity) theBodyWeightValue);
				bodyWeightObservation.setSubject(subjectReference);
				bodyWeightObservation.setStatus(ObservationStatus.FINAL);
				
				// write to fhirStore.
				saveResource(bodyWeightObservation);
			}
			supportingInfoResources.add(bodyWeightObservation);
			bodyWeightReference = new Reference(bodyWeightObservation.fhirType()+"/"+bodyWeightObservation.getIdPart());
		}

		Reference bmiReference = null;
		if (theBmi != null) {
			USCoreBMI bmiObservation = null;
			Type theBmiValue = theBmi.getValue();
			if (theBmiValue instanceof Reference) {
				Observation bmiEhrObservation = (Observation) pullResourceFromFhirServer((Reference)theBmiValue);
				if (!isEqualReference(subjectReference, bmiEhrObservation.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
					sendInternalErrorOO("bmi.subject", "the Subject reference does not match with ServiceRequest.subject");
				}

				bmiObservation = new USCoreBMI();
				bmiEhrObservation.copyValues(bmiObservation);
			} else if (theBmiValue instanceof Quantity) {
				bmiObservation = new USCoreBMI((Quantity) theBmiValue);
				bmiObservation.setSubject(subjectReference);
				bmiObservation.setStatus(ObservationStatus.FINAL);
				
				// write to fhirStore.
				saveResource(bmiObservation);
			}

			supportingInfoResources.add(bmiObservation);
			bmiReference = new Reference(bmiObservation.fhirType()+"/"+bmiObservation.getIdPart());
		}

		Reference bserHa1cReference = null;
		if (theHa1cObservation != null) {
			BSERHA1CObservation bserHa1cObservation = null;
			Type theHa1cObservationValue = theHa1cObservation.getValue();
			if (theHa1cObservationValue instanceof Reference) {
				Reference ha1cObservationReference = (Reference)theHa1cObservationValue;
				Observation ha1cEhrObservation = (Observation) pullResourceFromFhirServer(ha1cObservationReference);
				Reference ha1cSubjectReference = ha1cEhrObservation.getSubject();
				if (!isEqualReference(subjectReference, ha1cSubjectReference, ha1cObservationReference.getReferenceElement().getBaseUrl())) {
					sendInternalErrorOO("ha1cObservation.subject", "the Subject reference does not match with ServiceRequest.subject");
				}

				bserHa1cObservation = new BSERHA1CObservation();
				ha1cEhrObservation.copyValues(bserHa1cObservation);
				bserHa1cObservation.setStatus(ObservationStatus.FINAL);
				bserHa1cObservation.setEffective(new DateTimeType(new Date()));
			} else if (theHa1cObservationValue instanceof Quantity) {
				bserHa1cObservation = new BSERHA1CObservation(
					new CodeableConcept(new Coding("http://loinc.org", "4548-4", "Hemoglobin A1c/Hemoglobin.total in Blood")), 
					(Quantity) theHa1cObservationValue);
				bserHa1cObservation.setSubject(subjectReference);
				bserHa1cObservation.setStatus(ObservationStatus.FINAL);
				bserHa1cObservation.setEffective(new DateTimeType(new Date()));

				// write to fhirStore.
				saveResource(bserHa1cObservation);
			} else {
				sendInternalErrorOO("ha1cObservation", "ha1cObservation must be either Referece or Quantity");
			}

			supportingInfoResources.add(bserHa1cObservation);
			bserHa1cReference = new Reference(bserHa1cObservation.fhirType()+"/"+bserHa1cObservation.getIdPart());
		}

		List<Reference> earlyChildhoodNutritionObReferences = new ArrayList<Reference> ();
		Reference childWeightObservationReference = null;
		Reference childHeightObservationReference = null;

		if (theIsBabyLatching != null) {
			BSEREarlyChildhoodNutritionObservation earlyChildNutritionObs = new BSEREarlyChildhoodNutritionObservation(BSEREarlyChildhoodNutritionObservationUtil.ableToLatch, theIsBabyLatching);
			supportingInfoResources.add(earlyChildNutritionObs);
			earlyChildhoodNutritionObReferences.add(new Reference(earlyChildNutritionObs.fhirType()+"/"+earlyChildNutritionObs.getIdPart()));

			saveResource(earlyChildNutritionObs);
		}

		if (theMomsConcerns != null) {
			BSEREarlyChildhoodNutritionObservation earlyChildNutritionObs = new BSEREarlyChildhoodNutritionObservation(BSEREarlyChildhoodNutritionObservationUtil.maternalConcern, theMomsConcerns);
			supportingInfoResources.add(earlyChildNutritionObs);
			earlyChildhoodNutritionObReferences.add(new Reference(earlyChildNutritionObs.fhirType()+"/"+earlyChildNutritionObs.getIdPart()));

			saveResource(earlyChildNutritionObs);
		}

		if (theNippleShieldUse != null) {
			BSEREarlyChildhoodNutritionObservation earlyChildNutritionObs = new BSEREarlyChildhoodNutritionObservation(BSEREarlyChildhoodNutritionObservationUtil.nippleShield, theNippleShieldUse);
			supportingInfoResources.add(earlyChildNutritionObs);
			earlyChildhoodNutritionObReferences.add(new Reference(earlyChildNutritionObs.fhirType()+"/"+earlyChildNutritionObs.getIdPart()));

			saveResource(earlyChildNutritionObs);
		}

		if (theChild != null) {
			String lastName = "";
			String firstName = "";
			CodeType genderCode = new CodeType();
			USCoreBodyWeight childWeightObservation = null;
			USCoreBodyHeight childHeightObservation =  null;
			for (ParametersParameterComponent child : theChild.getPart()) {
				if ("lastName".equals(child.getName())) {
					lastName = ((StringType) child.getValue()).getValue();
				} else if ("firstName".equals(child.getName())) {
					firstName = ((StringType) child.getValue()).getValue();
				} else if ("gender".equals(child.getName())) {
					genderCode = ((CodeType) child.getValue());
				} else if ("height".equals(child.getName())) {
					Type theBodyHeightValue = theChild.getValue();
					if (theBodyHeightValue instanceof Reference) {
						Observation childHeightEhrObservation = (Observation) pullResourceFromFhirServer((Reference)theBodyHeightValue);
						childHeightObservation = new USCoreBodyHeight();
						childHeightEhrObservation.copyValues(childHeightObservation);
					} else if (theBodyHeightValue instanceof Quantity) {
						childHeightObservation = new USCoreBodyHeight();
						childHeightObservation.setValue((Quantity) theBodyHeightValue);
						childHeightObservation.setId(new IdType(childWeightObservation.fhirType(), UUID.randomUUID().toString()));
					}
				} else if ("weight".equals(child.getName())) {
					Type theBodyWeightValue = theChild.getValue();
					if (theBodyWeightValue instanceof Reference) {
						Observation childWeightEhrObservation = (Observation) pullResourceFromFhirServer((Reference)theBodyWeightValue);
						childWeightObservation = new USCoreBodyWeight();
						childWeightEhrObservation.copyValues(childWeightObservation);
					} else if (theBodyWeightValue instanceof Quantity) {
						childWeightObservation = new USCoreBodyWeight();
						childWeightObservation.setValue((Quantity) theBodyWeightValue);
						childWeightObservation.setId(new IdType(childWeightObservation.fhirType(), UUID.randomUUID().toString()));
					}
				}
			}

			Patient childPatient = new Patient();
			childPatient.setId(new IdType(UUID.randomUUID().toString()));
			childPatient.addName(new HumanName().setFamily(lastName).addGiven(firstName));
			childPatient.setGender(AdministrativeGender.fromCode(genderCode.getCode()));
			supportingInfoResources.add(childPatient);
			saveResource(childPatient);

			if (childWeightObservation != null && !childWeightObservation.isEmpty()) {
				childWeightObservation.setSubject(new Reference(childPatient.fhirType()+"/"+childPatient.getIdPart()));
				// write to fhirStore.
				saveResource(childWeightObservation);

				supportingInfoResources.add(childWeightObservation);
				childWeightObservationReference = new Reference(childWeightObservation.fhirType()+"/"+childWeightObservation.getIdPart());
			}

			if (childHeightObservation != null && !childHeightObservation.isEmpty()) {
				childHeightObservation.setSubject(new Reference(childPatient.fhirType()+"/"+childPatient.getIdPart()));
				// write to fhirStore.
				saveResource(childHeightObservation);

				supportingInfoResources.add(childHeightObservation);
				childHeightObservationReference = new Reference(childHeightObservation.fhirType()+"/"+childHeightObservation.getIdPart());
			}
		}

		// BSeR Diagnosis
		List<Reference> diagnosisConditionReferences = new ArrayList<Reference>();
		if (theDiagnosis != null) {
			for (ParametersParameterComponent diagnosis : theDiagnosis) {
				USCoreConditionProblemsAndHealthConcerns diagnosisCondition = new USCoreConditionProblemsAndHealthConcerns();
				diagnosisCondition.addCategory(CommonUtil.problemListItemCategory());
				Type diagnosisValue = diagnosis.getValue();
				if (diagnosisValue instanceof Reference) {
					if (diagnosisValue.getIdElement() != null) {
						Condition respCondition = (Condition) pullResourceFromFhirServer((Reference) diagnosisValue);
						if (!isEqualReference(subjectReference, respCondition.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
							sendInternalErrorOO("diagnosis.subject", "the Subject reference does not match with ServiceRequest.subject");
						}
		
						respCondition.copyValues(diagnosisCondition);
					}
				} else if (diagnosisValue instanceof Coding) {
					diagnosisCondition.setCode(new CodeableConcept((Coding) diagnosisValue));
					diagnosisCondition.setSubject(subjectReference);
					
					// write to fhirStore.
					saveResource(diagnosisCondition);
				}

				supportingInfoResources.add(diagnosisCondition);
				diagnosisConditionReferences.add(new Reference(diagnosisCondition.fhirType()+"/"+diagnosisCondition.getIdPart()));
			}
		}

		// NRT Authorization Status
		List<Reference> nrtAuthorizationStatusReferences = new ArrayList<Reference>();
		if (theNrtAuthorizationStatus != null) {
			CodeableConcept valueCodeableConcept = null;
			if ("AP".equals(theNrtAuthorizationStatus.getValueAsString())) {
				valueCodeableConcept = BSERNRTAuthorizationStatusUtil.approved;
			} else if ("DE".equals(theNrtAuthorizationStatus.getValueAsString())) {
				valueCodeableConcept = BSERNRTAuthorizationStatusUtil.denied;
			} else if ("PE".equals(theNrtAuthorizationStatus.getValueAsString())) {
				valueCodeableConcept = BSERNRTAuthorizationStatusUtil.pending;
			}

			BSERNRTAuthorizationStatus nrtAuthStatus = new BSERNRTAuthorizationStatus(subjectReference, valueCodeableConcept);

			saveResource(nrtAuthStatus);

			supportingInfoResources.add(nrtAuthStatus);
			nrtAuthorizationStatusReferences.add(new Reference(nrtAuthStatus.getIdElement()));
		}

		Reference smokingStatusReference = null;
		if (theSmokingStatus != null) {
			CodeableConcept smokeStatusValue = null;
			if (theSmokingStatus.getCode().equals("266919005")) {
				smokeStatusValue = USCoreSmokingStatusObservationUtil.neverSmokedTobacco;
			} else if (theSmokingStatus.getCode().equals("266927001")) {
				smokeStatusValue = USCoreSmokingStatusObservationUtil.unknownSmokedTobacco;
			} else if (theSmokingStatus.getCode().equals("428041000124106")) {
				smokeStatusValue = USCoreSmokingStatusObservationUtil.occasionalSmokedTobacco;
			} else if (theSmokingStatus.getCode().equals("428061000124105")) {
				smokeStatusValue = USCoreSmokingStatusObservationUtil.lightSmokedTobacco;
			} else if (theSmokingStatus.getCode().equals("428071000124103")) {
				smokeStatusValue = USCoreSmokingStatusObservationUtil.heavySmokedTobacco;
			} else if (theSmokingStatus.getCode().equals("449868002")) {
				smokeStatusValue = USCoreSmokingStatusObservationUtil.dailySmokedTobacco;
			} else if (theSmokingStatus.getCode().equals("77176002")) {
				smokeStatusValue = USCoreSmokingStatusObservationUtil.smoker;
			} else if (theSmokingStatus.getCode().equals("8517006")) {
				smokeStatusValue = USCoreSmokingStatusObservationUtil.exSmoker;
			}

			USCoreSmokingStatusObservation smokingStatusOb = new USCoreSmokingStatusObservation( 
				ObservationStatus.FINAL, 
				USCoreSmokingStatusObservationUtil.tobaccoSmokingStatusNHIS,
				subjectReference,
				smokeStatusValue);
			
			saveResource(smokingStatusOb);

			supportingInfoResources.add(smokingStatusOb);
			smokingStatusReference = new Reference(smokingStatusOb.fhirType()+"/"+smokingStatusOb.getIdPart());
		}

		List<Reference> communicationPreferencesReferences = new ArrayList<Reference>();
		if (theCommunicationPreferences != null) {

			for (ParametersParameterComponent commPref : theCommunicationPreferences.getPart()) {
				CodeableConcept code = null;
				StringType value = null;
				if ("bestDay".equals(commPref.getName())) {
					code = BSeRTelcomCommunicationPreferencesUtil.bestDay;
				} else if ("bestTime".equals(commPref.getName())) {
					code = BSeRTelcomCommunicationPreferencesUtil.bestTime;
				} else if ("leaveMessage".equals(commPref.getName())) {
					code = BSeRTelcomCommunicationPreferencesUtil.leaveMessageIndicator;
				}
				value = (StringType) commPref.getValue();

				BSERTelcomCommunicationPreferences teleCommPrefOb = new BSERTelcomCommunicationPreferences(code, value);

				saveResource(teleCommPrefOb);

				supportingInfoResources.add(teleCommPrefOb);
				communicationPreferencesReferences.add(new Reference(teleCommPrefOb.fhirType()+"/"+teleCommPrefOb.getIdPart()));
			}
		}

		List<SectionComponent> obesityReferralSupportingInformations = new ArrayList<SectionComponent>();
		List<SectionComponent> arthritisReferralSupportingInformations = new ArrayList<SectionComponent>();
		List<SectionComponent> hypertensionReferralSupportingInformations = new ArrayList<SectionComponent>();
		List<SectionComponent> earlyChildhoodNutritionReferralSupportingInformations = new ArrayList<SectionComponent>();
		List<SectionComponent> diabetesPreventionReferralSupportingInformations = new ArrayList<SectionComponent>();
		List<SectionComponent> tobaccoUseCessationReferralSupportingInformations = new ArrayList<SectionComponent>();
		if (theServiceType != null) {
			// Construct the supporting information section component for BSeR Referral Request Composition.
			if (ServiceType.ARTHRITIS.is(theServiceType.getCode())) {
				arthritisReferralSupportingInformations.add(BSERReferralRequestComposition.createArthritisReferralSupportingInformation(null, 
					allergyReferences, 
					medicationReferences, 
					bpReference, 
					bodyHeightReference, 
					bodyWeightReference, 
					bmiReference));

				serviceRequest.setReasonCode(new ArrayList<CodeableConcept>(Arrays.asList(ServiceType.ARTHRITIS.getCodeableConcept())));
				// supportingInfo.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ArthritisReferralSupportingInformation");
			} else if (ServiceType.DIABETES_PREVENTION.is(theServiceType.getCode())) {
				diabetesPreventionReferralSupportingInformations.add(BSERReferralRequestComposition.createDiabetesPreventionReferralSupportingInformation(null, 
					bserHa1cReference, 
					bpReference, 
					bodyHeightReference, 
					bodyWeightReference, 
					bmiReference));

				serviceRequest.setReasonCode(new ArrayList<CodeableConcept>(Arrays.asList(ServiceType.DIABETES_PREVENTION.getCodeableConcept())));					
			} else if (ServiceType.EARLY_CHILDHOOD_NUTRITION.is(theServiceType.getCode())) {
				earlyChildhoodNutritionReferralSupportingInformations.add(BSERReferralRequestComposition.createEarlyChildhoodNutritionReferralSupportingInformation(null,
					earlyChildhoodNutritionObReferences,
					bpReference, 
					childHeightObservationReference, 
					childWeightObservationReference));

				serviceRequest.setReasonCode(new ArrayList<CodeableConcept>(Arrays.asList(ServiceType.EARLY_CHILDHOOD_NUTRITION.getCodeableConcept())));					
			} else if (ServiceType.HYPERTENSION.is(theServiceType.getCode())) {
				hypertensionReferralSupportingInformations.add(BSERReferralRequestComposition.createHypertensionReferralSupportingInformation(null, 
					diagnosisConditionReferences, 
					bpReference, 
					bodyHeightReference, 
					bodyWeightReference, 
					bmiReference));

				serviceRequest.setReasonCode(new ArrayList<CodeableConcept>(Arrays.asList(ServiceType.HYPERTENSION.getCodeableConcept())));					
			} else if (ServiceType.OBESITY.is(theServiceType.getCode())) {
				obesityReferralSupportingInformations.add(BSERReferralRequestComposition.createObesityReferralSupportingInformation(null, 
					allergyReferences, 
					bpReference, 
					bodyHeightReference, 
					bodyWeightReference, 
					bmiReference));

				serviceRequest.setReasonCode(new ArrayList<CodeableConcept>(Arrays.asList(ServiceType.OBESITY.getCodeableConcept())));					
			} else if (ServiceType.TOBACCO_USE_CESSATION.is(theServiceType.getCode())) {
				tobaccoUseCessationReferralSupportingInformations.add(BSERReferralRequestComposition.createTobaccoUseCessationReferralSupportingInformation(null, 
					nrtAuthorizationStatusReferences, 
					smokingStatusReference, 
					communicationPreferencesReferences));

				serviceRequest.setReasonCode(new ArrayList<CodeableConcept>(Arrays.asList(ServiceType.TOBACCO_USE_CESSATION.getCodeableConcept())));					
			} 
		} else {
			throw new FHIRException("ServiceType is missing.");
		}

		// Referral Request Document Bundle
		BSERReferralRequestComposition bserReferralRequestComposition= new BSERReferralRequestComposition(
			CompositionStatus.FINAL, 
			new CodeableConcept(new Coding("http://loinc.org", "57133-1", "Referral note")), 
			subjectReference, 
			new Date(), 
			sourceReference, 
			"Referral request", 
			obesityReferralSupportingInformations,
			arthritisReferralSupportingInformations,
			hypertensionReferralSupportingInformations,
			earlyChildhoodNutritionReferralSupportingInformations,
			diabetesPreventionReferralSupportingInformations,
			tobaccoUseCessationReferralSupportingInformations
		);

		// Adding composition
		// bserReferralRequestComposition.setId(new IdType(bserReferralRequestComposition.fhirType(), UUID.randomUUID().toString()));
		saveResource(bserReferralRequestComposition);
		BSERReferralRequestDocumentBundle bserReferralRequestDocumentBundle = new BSERReferralRequestDocumentBundle(bserReferralRequestComposition);
		
		bserReferralRequestDocumentBundle.setTimestamp(new Date());

		// Adding supporting information document for ServiceRequest
		bserReferralRequestDocumentBundle.setId(new IdType(bserReferralRequestDocumentBundle.fhirType(), UUID.randomUUID().toString()));

		for (Resource resource : supportingInfoResources) {
			BundleEntryComponent bundleEntryComp = new BundleEntryComponent().setFullUrl(resource.fhirType()+"/"+resource.getIdPart()).setResource(resource);
			bserReferralRequestDocumentBundle.addEntry(bundleEntryComp);
		}

		// bserReferralRequestDocumentBundle.addEntry(new BundleEntryComponent().setFullUrl(((Bundle)supportingInfo).getIdElement().toVersionless().getValue()).setResource((Bundle)supportingInfo));

		// subject:Patient is already present in upper level. But, we need to add the subject in this
		// bundle as well in order for this to get validated.
		// bserReferralRequestDocumentBundle.addEntry(new BundleEntryComponent().setFullUrl(thePatient.getIdElement().toVersionless().getValue()).setResource(thePatient));

		// This is really duplicated resources from nested structure. The initiator PractitionerRole is already present. But, adding here as well
		// for validation.
		// bserReferralRequestDocumentBundle.addEntry(new BundleEntryComponent().setFullUrl(sourcePractitionerRole.getIdElement().toVersionless().getValue()).setResource(sourcePractitionerRole));
		// if (sourcePractitioner != null && !sourcePractitioner.isEmpty()) {
		// 	bserReferralRequestDocumentBundle.addEntry(new BundleEntryComponent().setFullUrl(sourcePractitioner.getIdElement().toVersionless().getValue()).setResource(sourcePractitioner));
		// }
		// if (sourceOrganization != null && !sourceOrganization.isEmpty()) {
		// 	bserReferralRequestDocumentBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceOrganization.getIdElement().toVersionless().getValue()).setResource(sourceOrganization));
		// }
		
		// if (sourceEndpoint != null && !sourceEndpoint.isEmpty()) {
		// 	bserReferralRequestDocumentBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceEndpoint.getIdElement().toVersionless().getValue()).setResource(sourceEndpoint));
		// }

		// According to the BSeR IG http://build.fhir.org/ig/HL7/bser/StructureDefinition-BSeR-ReferralRequestDocumentBundle.html
		// bdl-9 and 10 say that for Bundle.type = document, idetifier.system and .value must exist.
		Identifier identifier = new Identifier();
		identifier.setSystem("urn:bser:request:document");
		identifier.setValue(UUID.randomUUID().toString());
		bserReferralRequestDocumentBundle.setIdentifier(identifier);
		saveResource(bserReferralRequestDocumentBundle);
		Reference bserReferralRequestDocumentBundleReference = new Reference(bserReferralRequestDocumentBundle.fhirType() + "/" + bserReferralRequestDocumentBundle.getIdPart());

		serviceRequest.addSupportingInfo(bserReferralRequestDocumentBundleReference);		

		// serviceRequest, which is from UI has Practitioner for the requester. Overwrite this with parctitionerRole.
		serviceRequest.setRequester(sourceReference);

		// Add any additional requirement data elements to serviceRequest.
		// Set the initiator and recipient identifier to conform to the IG. This should be the
		// identifier that recipient can use to identify this request. And, Recipient can
		// echo this for us to use to search our fhirStore.
		String uniqueIdentifierValue = UUID.randomUUID().toString();
		String identifierSystem = "urn:bser:request:id";
		serviceRequest.setInitiatorIdentifier(identifierSystem, uniqueIdentifierValue, sourceOrganizationReference);
		
		Reference targetOrganizationReference = (targetOrganization == null || targetOrganization.isEmpty())?null:new Reference(targetOrganization.fhirType()+"/"+targetOrganization.getIdPart());

		// Set the recipient identifier. This is an optional. If we have organization information, set this identifier.
		// if (targetOrganizationReference != null && !targetOrganizationReference.isEmpty()) {
		// 	serviceRequest.setRecipientIdentifier(identifierSystem, uniqueIdentifierValue, targetOrganizationReference);
		// }

		serviceRequest.setOccurrence(new DateTimeType(new Date()));

		saveResource(serviceRequest);
		OperationOutcome deleteOO = deleteResource(theServiceRequest);
		if (deleteOO != null) {
			String msg = "DELETE ServiceRequest/" + theServiceRequest.getIdElement().getIdPart() + ": " + 
				deleteOO.getIssueFirstRep().getDetails().getCodingFirstRep().getCode();
			
			if (!warningMessage.isBlank()) {
				warningMessage = warningMessage.concat(" " + msg);
			} else {
				warningMessage = warningMessage.concat(msg);
			}
		}

		Reference serviceRequestReference = new Reference(serviceRequest.fhirType() + "/" + serviceRequest.getIdPart());
		BSERReferralTask bserReferralTask = new BSERReferralTask(
			identifierSystem, uniqueIdentifierValue,
			null, null,
			sourceOrganizationReference, 
			targetOrganizationReference,
			TaskStatus.REQUESTED,
			BserTaskBusinessStatus.SERVICE_REQUEST_CREATED.getCodeableConcept(),
//			new CodeableConcept(new Coding("http://hl7.org/fhir/us/bser/CodeSystem/TaskBusinessStatusCS", "2.0", "Service Request Created")), 
			serviceRequestReference, 
			subjectReference,
			new Date(), 
			sourceReference, 
			targetReference);

		bserReferralTask.setIntent(TaskIntent.ORDER);

		// create Task resource in the fhirStore. id will be assigned.
		saveResource(bserReferralTask);
		if (bserReferralTask.getIdElement().isEmpty()) {
			// we may not able to save this. assign one.
			bserReferralTask.setId(new IdType(bserReferralTask.fhirType(), UUID.randomUUID().toString()));
		}

		Reference bserReferralTaskReference = new Reference(bserReferralTask.fhirType() + "/" + bserReferralTask.getIdPart());

		// BSeR Referral Message Header focus Task
		BSERReferralMessageHeader bserReferralMessageHeader = new BSERReferralMessageHeader(
			targetReference, 
			sourceReference, 
			bserReferralTaskReference, 
			bserEndpointProcessMessageUrl, 
			targetEndpointUrl);

		saveResource(bserReferralMessageHeader);
		// bserReferralMessageHeader.setId(new IdType(bserReferralMessageHeader.fhirType(), UUID.randomUUID().toString()));

		/*** 
		 * NOW All the resources are ready. Create referral package. Most of them are bundles....
		 * 
		 ***/

		// Create Message Bundle. Pass the message header as an argument to add the header in the first entry.
		BSERReferralMessageBundle messageBundle = new BSERReferralMessageBundle(bserReferralMessageHeader);
		// messageBundle.setId(new IdType(bserEndpointUrl, messageBundle.fhirType(), UUID.randomUUID().toString(), null));

		// Now add all the referenced resources at this message bundle level. These are (except message header),
		// task and service request (task.focuse = service request). Service Request also has references. They will all
		// be included in this message bundle entry.

		// Task Resource (messageHeader.focus)
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(bserReferralTaskReference.getReference()).setResource(bserReferralTask));

		// Service Request (task.focus)
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(serviceRequestReference.getReference()).setResource(serviceRequest));

		// Referral Request Document Bundle (serviceRequest.supprotingInfo).
		// The refferral request document bundle includes resources as well. Those are taken care of when this bundle is created above.
		// Profile is not generated when bundle is set to dataelement. So, set it manually.
		// bserReferralRequestDocumentBundle.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralRequestDocumentBundle");
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(bserReferralRequestDocumentBundleReference.getReference()).setResource(bserReferralRequestDocumentBundle));

		// Patient resource. This is referenced by almost all resources.
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(subjectReference.getReference()).setResource(thePatient));

		// Task and Service Request resources have initiator and recipient practitionerRole. And, it may include practitioner. 
		// Add these resources here.
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceReference.getReference()).setResource(sourcePractitionerRole));
		if (sourcePractitioner != null && !sourcePractitioner.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourcePractitioner.fhirType()+"/"+sourcePractitioner.getIdPart()).setResource(sourcePractitioner));
		}
		if (sourceOrganization != null && !sourceOrganization.isEmpty())
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceOrganizationReference.getReference()).setResource(sourceOrganization));
		
		if (sourceEndpoint != null && !sourceEndpoint.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceEndpointReference.getReference()).setResource(sourceEndpoint));
		}

		if (sourceLocation != null && !sourceLocation.isEmpty()) {
			Reference sourceLocationReference = new Reference(sourceLocation.fhirType() + "/" + sourceLocation.getIdPart());
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceLocationReference.getReference()).setResource(sourceLocation));
		}
	
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetPractitionerRole.fhirType()+"/"+targetPractitionerRole.getIdPart()).setResource(targetPractitionerRole));
		if (targetPractitioner != null && !targetPractitioner.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetPractitioner.fhirType()+"/"+targetPractitioner.getIdPart()).setResource(targetPractitioner));
		}
		if (targetOrganization != null && !targetOrganization.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetOrganizationReference.getReference()).setResource(targetOrganization));
		}

		if (targetEndpoint != null && !targetEndpoint.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetEndpointReference.getReference()).setResource(targetEndpoint));
		}

		if (targetHealthService != null && !targetHealthService.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetHealthService.fhirType()+"/"+targetHealthService.getIdPart()).setResource(targetHealthService));
		}

		if (targetLocation != null && !targetLocation.isEmpty()) {
			Reference targetLocationReference = new Reference(targetLocation.fhirType() + "/" + targetLocation.getIdPart());
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetLocationReference.getReference()).setResource(targetLocation));
		}

		// Add all the supporting resources to MessageBundle entry as specified in
		// http://hl7.org/fhir/us/bser/StructureDefinition-BSeR-ReferralMessageBundle.html

		// Adding Employment Status
		if (odhEmploymentStatus != null) {
			messageBundle.addEmploymentStatus(odhEmploymentStatus);
		}

		// Adding Education Level
		if (educationLevel != null) {
			messageBundle.addEducationLevel(educationLevel);
		}

		// TODOP Patient Consent

		// There is another resource defined in IG but not shown on the message bundle profile.
		// * Coverage
		if (theCoverage != null) {
			BSERCoverage coverage = new BSERCoverage();
			theCoverage.copyValues(coverage);
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(coverage.fhirType()+"/"+coverage.getIdPart()).setResource(coverage));
		}

		// Save Message before submission.
		saveResource(messageBundle);
				
		// Submit to target $process-message operation
		FhirContext ctx = StaticValues.myFhirContext; 
		ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

		if (recipientReady) {
			IGenericClient client;
			boolean errorOccurred = false;
			if (targetEndpointUrl != null && !targetEndpointUrl.isBlank()) {
				IParser parser = ctx.newJsonParser();
				String messageBundleJson = parser.encodeResourceToString(messageBundle);
				logger.debug("SENDING MessageBundle TO " + targetEndpointUrl + ":\n" + messageBundleJson);

				IBaseResource response = null;
				if ("YUSA".equals(recipientAA.getRecipientSite())) {
					// This is YUSA endpoint, which does not have FHIR messaging operation name.
					String respYusa = recipientAA.submitYusaRR(targetEndpointUrl, messageBundleJson);
					if (!warningMessage.isBlank()) {
						warningMessage = warningMessage.concat(" Submitted to YUSA in Restful POST and recieved response(s) = " + respYusa + "\n");
					} else {
						warningMessage = warningMessage.concat("Submitted to YUSA in Restful POST and recieved response(s) = " + respYusa + "\n");
					}

					if (respYusa.startsWith("ACCEPTED")) {
						bserReferralTask.setStatus(TaskStatus.REQUESTED);
					} else if (respYusa.startsWith("SUCCESS")) {
						bserReferralTask.setStatus(TaskStatus.REQUESTED);
					} else {
						bserReferralTask.setStatus(TaskStatus.FAILED);
					}					
				} else if ("NO-SUBMISSION".equals(recipientAA.getRecipientSite())) {
					bserReferralTask.setStatus(TaskStatus.REQUESTED);
					if (!warningMessage.isBlank()) {
						warningMessage = warningMessage.concat(" Submission is disabled.\n");
					} else {
						warningMessage = warningMessage.concat("Submission is disabled.\n");
					}
				} else {
					String accessToken = null;
					try {
						accessToken = recipientAA.getAccessToken();
					} catch (ParseException e) {
						if (!warningMessage.isBlank()) {
							warningMessage = warningMessage.concat(" Failed to get an access token: " + e.getMessage() + "\n");
						} else {
							warningMessage = warningMessage.concat("Failed to get an access token: " + e.getMessage() + "\n");
						}
						e.printStackTrace();
					}

					client = ctx.newRestfulGenericClient(targetEndpointUrl);
					if (accessToken != null && !accessToken.isBlank()) {
						BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);
						client.registerInterceptor(authInterceptor);
					}

					try {
						response = client
							.operation()
							.processMessage() // New operation for sending messages
							.setMessageBundle(messageBundle)
							.asynchronous(OperationOutcome.class)
							.execute();	
					} catch (Exception e) {
						if (!warningMessage.isBlank()) {
							warningMessage = warningMessage.concat(" Failed to send a request: " + e.getMessage() + "\n");
						} else {
							warningMessage = warningMessage.concat("Failed to send a request: " + e.getMessage() + "\n");
						}

						errorOccurred = true;
						e.printStackTrace();
					}

					if (targetEndpointUrl.endsWith("/")) {
						targetEndpointUrl += "$process-message";
					} else {
						targetEndpointUrl += "/$process-message";
					}
				}

				if (errorOccurred) {
					bserReferralTask.setStatus(TaskStatus.FAILED);
					OperationOutcome oo = constructErrorOO("Endpoint", "Submitting to " + targetEndpointUrl + " failed. " + warningMessage);
					saveResource(oo);
					setTaskOut(bserReferralTask, oo);
					updateResource(bserReferralTask);

					throw new InternalErrorException("Submitting to " + targetEndpointUrl + " failed. Task.id:" + bserReferralTask.getIdElement().toVersionless() + " " + warningMessage);
				} else {
					if (response != null && !response.isEmpty()) {
						if (response instanceof OperationOutcome) {
							OperationOutcome retOO = (OperationOutcome) response;
							for (OperationOutcomeIssueComponent issue : retOO.getIssue()) {
								if (IssueSeverity.ERROR == issue.getSeverity() || IssueSeverity.FATAL == issue.getSeverity()) {
									bserReferralTask.setStatus(TaskStatus.FAILED);
									errorOccurred = true;

									break;
								}
							}
							
							saveResource(retOO);
							setTaskOut(bserReferralTask, retOO);

							if (errorOccurred) {
								updateResource(bserReferralTask);
								throw new InternalErrorException("Submitting to " + targetEndpointUrl + " failed", (IBaseOperationOutcome) response);
							}

							bserReferralTask.setStatus(TaskStatus.REQUESTED);
						} else {
							logger.warn("Message response has a resource other than operation outcome in the payload: " + response.fhirType());
						}
					}

					updateResource(bserReferralTask);

					serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
					updateResource(serviceRequest);

					if (!warningMessage.isBlank()) {
						warningMessage = warningMessage.concat(" Submitted to " + targetEndpointUrl + "\n");
					} else {
						warningMessage = warningMessage.concat("Submitted to " + targetEndpointUrl + "\n");
					}
				}
			}
		} else {
			if (!warningMessage.isBlank())
				warningMessage = warningMessage.concat(" Referral Request has NOT been submitted because submission is disabled. Enable it by setting 'RECIPIENT_NOT_READY' to false. ");
			else
				warningMessage = warningMessage.concat("Referral Request has NOT been submitted because submission is disabled. Enable it by setting 'RECIPIENT_NOT_READY' to false. ");
		}

		// return anything if needed in Parameters
		Parameters returnParameters = new Parameters();
		returnParameters.addParameter("referral_request_reference", new Reference(messageBundle.getIdElement()));
		returnParameters.addParameter().setName("referral_request_resource").setResource(messageBundle);
		ParametersParameterComponent recipientParam = new ParametersParameterComponent(new StringType("recipient_endpoint"));
		recipientParam.setResource(targetEndpoint);
		returnParameters.addParameter(recipientParam);
		if (warningMessage != null && !warningMessage.isBlank()) {
			returnParameters.addParameter("warning", warningMessage);
		}
	
		return returnParameters;
	}

	boolean isProfile(IBaseResource resource, String myProfile) {
		Meta meta = (Meta) resource.getMeta();
		if (meta != null && !meta.isEmpty()) {
			List<CanonicalType> profiles = meta.getProfile();
			for (CanonicalType profile : profiles) {
				return myProfile.equals(profile);
			}
		}

		return false;
	}

	private void setTaskOut (Task task, OperationOutcome oo) {
		CodeableConcept taskOutTypeCodeable = new CodeableConcept();
		taskOutTypeCodeable.setText("ServiceRequest Task Details");
		TaskOutputComponent taskOutputComponent = new TaskOutputComponent(taskOutTypeCodeable, new Reference(oo.getId()));
		List<TaskOutputComponent> taskOutComps = new ArrayList<TaskOutputComponent>();
		taskOutComps.add(taskOutputComponent);
		task.setOutput(taskOutComps);
	}

	private Identifier getIdentifierByType(Task task, Coding codingParam) {
		for (Identifier taskIdentifier : task.getIdentifier()) {
			CodeableConcept type = taskIdentifier.getType();
			for (Coding coding : type.getCoding()) {
				if (codingParam.getSystem().equals(coding.getSystem())
					&& codingParam.getCode().equals(coding.getCode())) {
						return taskIdentifier;
				}
			}
		}

		return null;
	}

	/***
	 * processMessageOperation
	 * @param theContent
	 * @param theAsync
	 * @param theUri
	 * @return
	 */
	@Operation(name="$process-message", manualResponse = true)
	public void processMessageOperation(
		@OperationParam(name="content") Bundle theContent,
		@OperationParam(name="async") BooleanType theAsync,
		@OperationParam(name="response-url") UriType theUri			
	) {
		// Feedback would also be async as there can be another message.
		// if (theAsync == null) {
		// 	throw new FHIRException("async parameter must exist");
		// }
		// if (!theAsync.booleanValue()) {
		// 	throw new FHIRException("BSERReferralFeedback Messageing must have async set to true.");
		// }

		// String recipientEndpointUrl;
		// if (theUri != null && !theUri.isEmpty()) {
		// 	recipientEndpointUrl = theUri.asStringValue();
		// }

		// Bundle retBundle = new Bundle();

		MessageHeader messageHeader = null;

		Reference initiatorPractitionerRoleReference = null;

		Task bserReferralTask = null;
		Reference referralTaskReference = null;

		if (theContent == null || theContent.isEmpty()) {
			// content cannot be null or empty.
			throw new FHIRException("content is either null or empty");
		}

		if (theContent.getType() == BundleType.MESSAGE) {
			IParser parser = StaticValues.myFhirContext.newJsonParser();
			String responseString = parser.encodeResourceToString(theContent);	
			logger.debug("Received Feedback Message Bundle " + responseString);

			List<BundleEntryComponent> entries = theContent.getEntry();

			// Evaluate the first entry, which must be MessageHeader
			BundleEntryComponent entry1 = theContent.getEntryFirstRep();
			Resource resource = entry1.getResource();
			if (resource instanceof MessageHeader) {
				// MessageHeader is like metadata for a message. Capture all the
				// necessary information.
				messageHeader = (MessageHeader) resource;

				if (!BSERReferralMessageHeader.isBSERReferralMessageHeader(messageHeader)) {
					throw new FHIRException("Received message is NOT REF/RRI - Patient referral");
				}

				// save the orignial message bundle.
				saveResource(theContent);

				// This could be async message response. Check here.
				MessageHeaderResponseComponent response = messageHeader.getResponse();
				if (!response.isEmpty()) {
					// This is message response.
					String originalMessageId = response.getIdentifier();

					// See if we have operation outcome.
					OperationOutcome oo = null;
					Reference details = response.getDetails();
					if (details != null && !details.isEmpty()) {
						// get the oo.
						String ooRef = details.getReference();

						for (BundleEntryComponent ooEntry : theContent.getEntry()) {
							Resource ooResource = ooEntry.getResource();
							if (ooResource instanceof OperationOutcome && ooEntry.getFullUrl().contains(ooRef)) {
								oo = (OperationOutcome) ooResource;
							}
						}
					}

					Bundle orgingalMessageBundles = searchResourceFromFhirServer(fhirStore, Bundle.class, Bundle.MESSAGE.hasId(originalMessageId));
					if (orgingalMessageBundles == null || orgingalMessageBundles.isEmpty()) {
						throw new FHIRException("Failed to find an original message for the response message. Original Message ID = " + originalMessageId);
					}

					Task task = null;
					ServiceRequest serviceRequest = null;

					if (orgingalMessageBundles.getTotal() <= 0) {
						// couldn't fine the messageBundle. This could be because the server does not support
						// this. Try anotehr way.

						Bundle messageBundles = searchResourceFromFhirServer(fhirStore, Bundle.class, Bundle.TYPE.exactly().code("message"));
						if (messageBundles == null || messageBundles.isEmpty()) {
							throw new FHIRException("Failed to get message bundles.");
						}

						if (messageBundles.getTotal() <= 0) {
							throw new FHIRException("Couldn't find any message bundles");
						}

						for (BundleEntryComponent mBundleEntry : messageBundles.getEntry()) {
							Bundle mBundle = (Bundle) mBundleEntry.getResource();
							MessageHeader mh = (MessageHeader) mBundle.getEntryFirstRep().getResource();
							if (originalMessageId.equals(mh.getIdPart())) {
								// This is the one we are looking for.
								for (BundleEntryComponent entry : mBundle.getEntry()) {
									resource = entry.getResource();
									if (resource instanceof Task) {									
										task = (Task) resource;
									}

									if (resource instanceof ServiceRequest) {	
										serviceRequest = (ServiceRequest) resource;
									}							
								}

								break;
							}
						}
					} else {
						// We supposed to get only one. If more, we just choose the first one.
						Bundle mb = (Bundle) orgingalMessageBundles.getEntryFirstRep().getResource();
						for (BundleEntryComponent entry : mb.getEntry()) {
							resource = entry.getResource();
							if (resource instanceof Task) {									
								task = (Task) resource;
							}

							if (resource instanceof ServiceRequest) {	
								serviceRequest = (ServiceRequest) resource;
							}
						}
					}

					if (task == null || task.isEmpty()) {
						throw new FHIRException("Couldn't locate related Task for the MessageHeader/" + originalMessageId);
					}

					if (serviceRequest == null || serviceRequest.isEmpty()) {
						throw new FHIRException("Couldn't locate related ServiceRequest for the MessageHeader/" + originalMessageId);
					}

					if (ResponseType.FATALERROR == response.getCode() || ResponseType.TRANSIENTERROR == response.getCode()) {
						task.setStatus(TaskStatus.FAILED);					
						serviceRequest.setStatus(ServiceRequestStatus.REVOKED);
					} else {
						task.setStatus(TaskStatus.RECEIVED);					
						serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);
					}

					if (oo != null && !oo.isEmpty()) {
						// reduce the size of oo.issue[].diagnostics
						for (OperationOutcomeIssueComponent ooIssue : oo.getIssue()) {
							String diag = ooIssue.getDiagnostics();
							if (diag != null && !diag.isEmpty()) {
								diag = StringEscapeUtils.escapeHtml4(diag);
								if (diag.length() > 300) {
									diag = diag.substring(0, 300);
									
									ooIssue.setDiagnostics(diag);
								}
							}
						}

						// Clear the narrative.
						oo.setText(new Narrative());

						saveResource(oo);
						setTaskOut(task, oo);
					}

					updateResource(task);
					updateResource(serviceRequest);

					// We do not respond to the response message.
					return;
				} else {
					// If this message is not a response. Then, this must be a feedback message.

					// capture sender information. This should be recipient. But,
					// the BSeR IG fixed this to be initiator. 
					// For reverse direction (Recipient --> Initiator), sender and destination need to be swapped.
					// For now, we just follow the IG
					initiatorPractitionerRoleReference = messageHeader.getSender();
					if (initiatorPractitionerRoleReference == null || initiatorPractitionerRoleReference.isEmpty()) {
						throw new FHIRException("MessageHeader.sender is empty or does not exist");
					}

					MessageDestinationComponent destination = messageHeader.getDestinationFirstRep();
					if (destination == null || destination.isEmpty()) {
						throw new FHIRException("MessageHeader.destination is empty or does not exist");
					}

					referralTaskReference = messageHeader.getFocusFirstRep();
					if (referralTaskReference == null || referralTaskReference.isEmpty()) {
						throw new FHIRException("MessageHader.focus[0] is empty or does not exist.");
					}

					for (BundleEntryComponent entry : entries) {
						resource = entry.getResource();
						if (resource instanceof Task) {
							if (entry.getFullUrl().contains(referralTaskReference.getReferenceElement().getValue())) {
								bserReferralTask = (Task) entry.getResource();
								break;
							}
		
						} 						
					}

					if (bserReferralTask == null || bserReferralTask.isEmpty()) {
						throw new FHIRException("BSERReferralTask cannot be found from the MessageBundle entries.");
					}

					// Get and Update Business Status in the Task in the FHIR Store
					// Get PLAC identifier.value.
					String PLACvalue = null;
					// for (Identifier taskIdentifier : bserReferralTask.getIdentifier()) {
					// 	CodeableConcept type = taskIdentifier.getType();
					// 	for (Coding coding : type.getCoding()) {
					// 		if ("http://terminology.hl7.org/CodeSystem/v2-0203".equals(coding.getSystem())
					// 			&& "PLAC".equals(coding.getCode())) {
					// 				PLACvalue = taskIdentifier.getValue();
					// 				break;
					// 		}
					// 	}

					// 	if (PLACvalue != null && !PLACvalue.isEmpty()) {
					// 		break;
					// 	}
					// }

					Identifier placIdentifier = getIdentifierByType(bserReferralTask, new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "PLAC", null));
					if (placIdentifier != null) {
						PLACvalue = placIdentifier.getValue();
					}

					if (PLACvalue == null || PLACvalue.isEmpty()) {
						throw new FHIRException("BSERReferralTask must have PLAC's value.");
					}

					CodeableConcept businessStatus = bserReferralTask.getBusinessStatus();

					Bundle searchBundle = searchResourceFromFhirServer(
						fhirStore, 
						Task.class, 
						Task.IDENTIFIER.exactly().code(PLACvalue),
						Task.INCLUDE_SUBJECT,
						Task.INCLUDE_FOCUS);
		
					if (searchBundle.getTotal() == 0) {
						throw new FHIRException("NO Matching Task Found.");
					} 

					// loop through the search result entry and get task and patient
					Patient myPatient = null;
					Task myTask = null;
					ServiceRequest myServiceRequest = null;
					for (BundleEntryComponent TaskEntry : searchBundle.getEntry()) {
						if (TaskEntry.getResource() instanceof Task) {
							myTask = (Task) TaskEntry.getResource();
						} else if (TaskEntry.getResource() instanceof Patient) {
							myPatient = (Patient) TaskEntry.getResource();
						} else if (TaskEntry.getResource() instanceof ServiceRequest) {
							myServiceRequest = (ServiceRequest) TaskEntry.getResource();
						}
					}

					if (myTask == null) {
						throw new FHIRException("No task found in the search.entry");
					}

					if (myPatient == null || myPatient.isEmpty()) {
						throw new FHIRException("Searched Task (" + myTask.getIdPart() +") has no subject as patient");
					}

					// Get the business status from recipient bserReferralStatus and update the task.
					myTask.setBusinessStatus(businessStatus);
					myTask.setStatus(BserTaskBusinessStatus.taskStatusFromCodeableConcept(businessStatus));

					if (myServiceRequest != null) {
						myServiceRequest.setStatus(BserTaskBusinessStatus.serviceRequestStatusFromCodeableConcept(businessStatus));
					}

					// Set FILL information.
					Identifier filllIdentifier = getIdentifierByType(bserReferralTask, new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "FILL", null));

					if (filllIdentifier != null) {
						// add or update fill identifier.
						Identifier existingFillIdentifier = getIdentifierByType(myTask, new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "FILL", null));
						if (existingFillIdentifier != null) {
							filllIdentifier.copyValues(existingFillIdentifier);
						} else {
							myTask.addIdentifier(filllIdentifier);
						}
					}

					// See if we have something in the output
					for (TaskOutputComponent outputFromRecipient : bserReferralTask.getOutput()) {
						Reference bserFeedbackDocumentReference = (Reference) outputFromRecipient.getValue();
						if (bserFeedbackDocumentReference == null || bserFeedbackDocumentReference.isEmpty()) {
							throw new FHIRException("BSERReferralTask.output.valueReference cannot be null or empty.");
						}

						// From message entries, find out the actual resource of bser feedback document reference.
						for (BundleEntryComponent entry : entries) {
							resource = entry.getResource();
							if (resource instanceof Bundle) {
								if (entry.getFullUrl().contains(bserFeedbackDocumentReference.getReferenceElement().getValue())) {
									// This bundle document has patient data entries for the usecase.
									Bundle recvFeedbackDocument = (Bundle) entry.getResource();

									// We have the document. This 
									BSERReferralFeedbackDocument bserReferralFeedbackDocument = new BSERReferralFeedbackDocument();
									recvFeedbackDocument.copyValues(bserReferralFeedbackDocument);

									// From ths document, grab composition.
									Composition bserReferralFeedbacDocComposition = (Composition) bserReferralFeedbackDocument.getEntryFirstRep().getResource();
									for (SectionComponent section : bserReferralFeedbacDocComposition.getSection()) {
										// run through the section.entry to capture the reference.
										for (Reference sectionEntryReference : section.getEntry()) {
											// Find this reference from the document entry.
											for (BundleEntryComponent bserReferralFeedbackDocEntry : bserReferralFeedbackDocument.getEntry()) {
												if (bserReferralFeedbackDocEntry.getFullUrl().contains(sectionEntryReference.getReferenceElement().getValue())) {
													Resource supportInfoResource = bserReferralFeedbackDocEntry.getResource();

													// substitute the patient
													if (supportInfoResource instanceof Observation) {
														// String supportResourceSubjectRef = myPatient.getIdElement().toVersionless().getId();
														String subjectName = myPatient.getNameFirstRep().getGivenAsSingleString() + " " + myPatient.getNameFirstRep().getFamily();
														Reference subjectReference = new Reference("Patient" + "/" + myPatient.getIdPart()).setDisplay(subjectName);
														((Observation)supportInfoResource).setSubject(subjectReference);
													} 
													
													saveResource(supportInfoResource);
													// sectionEntryReference.setResource(supportInfoResource);
													// resource.fhirType()+"/"+resource.getIdPart()
													sectionEntryReference.setReferenceElement(supportInfoResource.getIdElement());
													bserReferralFeedbackDocEntry.setFullUrl(supportInfoResource.fhirType()+"/"+supportInfoResource.getIdPart());
												}
											}
										}
									}

									
									saveResource(bserReferralFeedbacDocComposition);
									// update composition id in document.
									bserReferralFeedbackDocument.getEntryFirstRep().setFullUrl("Composition/" + bserReferralFeedbacDocComposition.getIdPart());
									bserReferralFeedbackDocument.getEntryFirstRep().getResource().setId(bserReferralFeedbacDocComposition.getIdPart());
									saveResource(bserReferralFeedbackDocument);
									TaskOutputComponent myOutputFromRecipient = new TaskOutputComponent(outputFromRecipient.getType(), new Reference(bserReferralFeedbackDocument.getIdElement()));
									myTask.addOutput(myOutputFromRecipient);

									break;
								}
							} 
						}
					}
					if (myServiceRequest != null) {
						updateResource(myServiceRequest);
					}
					updateResource(myTask);
				}	
			} else {
				throw new FHIRFormatError("The bundle must have MessageHeader first in the entry");
			}
		} else {
			throw new FHIRException("The bundle must be a MESSAGE type");
		}
		
		return;
	}
}
