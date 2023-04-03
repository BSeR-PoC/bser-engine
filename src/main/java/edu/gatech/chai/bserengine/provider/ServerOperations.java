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
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.HealthcareService;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Meta;
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
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
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
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import edu.gatech.chai.BSER.model.BSERArthritusReferralSupportingInformation;
import edu.gatech.chai.BSER.model.BSERCoverage;
import edu.gatech.chai.BSER.model.BSERDiabetesPreventionReferralSupportingInformation;
import edu.gatech.chai.BSER.model.BSERDiagnosis;
import edu.gatech.chai.BSER.model.BSEREarlyChildhoodNutritionReferralSupportingInformation;
import edu.gatech.chai.BSER.model.BSEREducationLevel;
import edu.gatech.chai.BSER.model.BSERHA1CObservation;
import edu.gatech.chai.BSER.model.BSERHypertensionReferralSupportingInformation;
import edu.gatech.chai.BSER.model.BSERObesityReferralSupportingInformation;
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
import edu.gatech.chai.BSER.model.BSERTobaccoUseCessationReferralSupportingInformation;
import edu.gatech.chai.BSER.model.ODHEmploymentStatus;
import edu.gatech.chai.FHIR.model.BMI;
import edu.gatech.chai.FHIR.model.BloodPressure;
import edu.gatech.chai.FHIR.model.BodyHeight;
import edu.gatech.chai.FHIR.model.BodyWeight;
import edu.gatech.chai.SmartOnFhirClient.SmartBackendServices;
import edu.gatech.chai.USCore.model.USCoreAllergyIntolerance;
import edu.gatech.chai.bserengine.security.RecipientAA;
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
	}

	public static String BserReferralRecipientPractitionerRoleProfile = "http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralRecipientPractitionerRole";
	public static String BserReferralInitiatorPractitionerRoleProfile = "http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralInitiatorPractitionerRole";
	public static String BserReferralServiceRequestProfile = "http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralServiceRequest";
	public static String BserReferralFeedbackDocumentBundleProfile = "http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralFeedbackDocumentBundle";
	
	public ServerOperations() {
		WebApplicationContext context = ContextLoaderListener.getCurrentWebApplicationContext();
		smartBackendServices = context.getBean(SmartBackendServices.class);
		recipientAA = context.getBean(RecipientAA.class);

		fhirStore = System.getenv("FHIRSTORE");
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
		IGenericClient genericClient = StaticValues.myFhirContext.newRestfulGenericClient(fhirStore);
		if (smartBackendServices.setFhirServerUrl(fhirStore).isActive()) {
			BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();
			genericClient.registerInterceptor(authInterceptor);
		}

		MethodOutcome createResponse = genericClient.create().resource(resource).execute();
		IBaseOperationOutcome oo = createResponse.getOperationOutcome();
		if (oo != null) {
			throw new FHIRException("BSeR enginen failed to persist external resource, " + resource.getIdElement().toString());
		}

		resource.setId(createResponse.getId());
	}

	private void updateResource (IBaseResource resource) {
		IGenericClient genericClient = StaticValues.myFhirContext.newRestfulGenericClient(fhirStore);
		if (smartBackendServices.setFhirServerUrl(fhirStore).isActive()) {
			BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();
			genericClient.registerInterceptor(authInterceptor);
		}

		MethodOutcome updateResponse = genericClient.update().resource(resource).execute();
		IBaseOperationOutcome oo = updateResponse.getOperationOutcome();
		if (oo != null) {
			throw new FHIRException("BSeR enginen failed to persist external resource, " + resource.getIdElement().toString());
		}

		resource.setId(updateResponse.getId());
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

	private void sendInternalErrorOO(String fhirPath, String msg) {
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

		// ServiceRequest is required.
		if (theServiceRequest == null) {
			sendInternalErrorOO("Parameters.parameter.where(name='referral').empty()", "Referral is missing");
		}

		// If we received this request, it means we are submitting the referral so it shouldn't be ACTIVE
		if (theServiceRequest.getStatus() == ServiceRequestStatus.ACTIVE) {
			warningMessage = warningMessage.concat("The referral request has its status already set to ACTIVE. ");
		}

		// Create a task and add ServiceRequest to task.focus
		// We may not have the complete service request. This is from UI. Create Service Request and copy current ones to
		// newly created service request. 
		BSERReferralServiceRequest serviceRequest = new BSERReferralServiceRequest();
		theServiceRequest.copyValues(serviceRequest);
		serviceRequest.setStatus(ServiceRequestStatus.ACTIVE);

		if (serviceRequest.getIdElement() == null || serviceRequest.getIdElement().isEmpty()) {
			sendInternalErrorOO("ServiceRequest.id", "ServiceRequest does not have id");
		}

		// Get fhirStore Url.
		if (theBserProviderBaseUrl != null) {
			fhirStore = theBserProviderBaseUrl.getValue();

			// set up SMART on FHIR backend service (if possible).
			smartBackendServices.setFhirServerUrl(fhirStore);
		} else {
			sendInternalErrorOO("Parameters.parameter.where(name='bserProviderBaseUrl').empty()", "bserProviderBaseUrl parameter is missing");
		}

		// Sanity check on the ServiceRequest.subject is patient, and it matches with the received Patient resource.
		subjectReference = serviceRequest.getSubject();
		if (!"Patient".equals(subjectReference.getReferenceElement().getResourceType())) {
			sendInternalErrorOO("ServiceRequest.subject", "Subject must be Patient");
		}

		if (thePatient != null) {
			// Check if the included patient resource matches with the one in the serviceRequest. 
			if (!subjectReference.getReferenceElement().getIdPart().equals(thePatient.getIdElement().getIdPart())) {
				sendInternalErrorOO("ServiceRequest.subject", "Patient ID does not match between ServiceRequest.subject and patient.id");
			}
		} else {
			thePatient = (Patient) pullResourceFromFhirServer(subjectReference);
		}

		// Get the initator PractitionerRole resource. This is ServiceRequest.requester.
		// MDI IG users PractitionerRole. First check the requester if it's practitioner or practitionerRole
		Reference requesterReference = serviceRequest.getRequester();
		if (requesterReference.isEmpty()) {
			sendInternalErrorOO("ServiceRequest.requester", "Requester is NULL");
		}

		if (theRequester == null) {
			if ("Practitioner".equals(requesterReference.getType())) {
				// We have practitioner. Get practitoinerRole from FHIR server.
				sourcePractitioner = (Practitioner) pullResourceFromFhirServer(sourceReference);
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
				// requesterReference may be Practitioner. Ok to use this URL as both practitioner and practitionerRole hasve the same base URL
				requesterReference.getReferenceElement().getBaseUrl(), 
				PractitionerRole.class, 
				PractitionerRole.PRACTITIONER.hasId(sourcePractitioner.getIdElement().getIdPart()),
				PractitionerRole.INCLUDE_ORGANIZATION, PractitionerRole.INCLUDE_ENDPOINT);
		} else {
			sourceReference = requesterReference;
			searchBundle = searchResourceFromFhirServer(
				// requesterReference may be Practitioner. Ok to use this URL as both practitioner and practitionerRole hasve the same base URL
				requesterReference.getReferenceElement().getBaseUrl(), 
				PractitionerRole.class, 
				PractitionerRole.RES_ID.exactly().code(sourcePractitioner.getIdElement().getIdPart()),
				PractitionerRole.INCLUDE_ORGANIZATION, PractitionerRole.INCLUDE_ENDPOINT);
		}

		PractitionerRole sourceEhrPractitionerRole = null;
		Organization sourceEhrOrganization = null;
		for (BundleEntryComponent entry : searchBundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource instanceof PractitionerRole) {
				sourceEhrPractitionerRole = (PractitionerRole) resource;
				if ("Practitioner".equals(requesterReference.getType())) {
					sourceReference = new Reference (new IdType(
						sourceEhrPractitionerRole.getIdElement().getBaseUrl(), 
						sourceEhrPractitionerRole.fhirType(), 
						sourceEhrPractitionerRole.getIdElement().getIdPart(), 
						sourceEhrPractitionerRole.getIdElement().getVersionIdPart()));
				}
			} else if (resource instanceof Organization) {
				sourceEhrOrganization = (Organization) resource;
			} else if (resource instanceof Endpoint) {
				sourceEndpoint = (Endpoint) resource;
			}
		}

		// Create BSER initator PractitionerRole
		sourcePractitionerRole = new BSERReferralInitiatorPractitionerRole();
		if (sourceEhrPractitionerRole != null && !sourceEhrPractitionerRole.isEmpty()) {
			sourceEhrPractitionerRole.copyValues(sourcePractitionerRole);
		} else {
			sourcePractitionerRole.setId(new IdType(sourcePractitionerRole.fhirType(), UUID.randomUUID().toString()));
		}

		if (sourceReference == null || sourceReference.isEmpty()) {
			sourceReference = new Reference(sourcePractitionerRole.getIdElement());
		}

		// BSeR IG requires Organization for InitiatorPractitionerRole
		// http://build.fhir.org/ig/HL7/bser/StructureDefinition-BSeR-ReferralInitiatorPractitionerRole.html
		sourceOrganization = new BSEROrganization(true, "Initiator Organization");
		sourceOrganization.addType(new CodeableConcept(new Coding("http://terminology.hl7.org/CodeSystem/organization-type", "prov", "Healthcare Provider")));
		if (sourceEhrOrganization != null && !sourceEhrOrganization.isEmpty()) {
			sourceEhrOrganization.copyValues(sourceOrganization);
		} else {
			sourceOrganization.setId(new IdType(sourceOrganization.fhirType(), UUID.randomUUID().toString()));

			saveResource(sourceOrganization);
		}

		sourceOrganizationReference = new Reference(sourceOrganization.getIdElement());
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
			sourceEndpoint.setId(new IdType(sourceEndpoint.fhirType(), UUID.randomUUID().toString()));
			sourcePractitionerRole.addEndpoint(new Reference(sourceEndpoint.getIdElement()));
		}
		
		// Get target (or recipient) practitioner resource. include practitioner, organization, endpoint, and healthService resources.
		targetReference = serviceRequest.getPerformerFirstRep();
		searchBundle = searchResourceFromFhirServer(
			targetReference.getReferenceElement().getBaseUrl(), 
			PractitionerRole.class, 
			PractitionerRole.RES_ID.exactly().code(targetReference.getReferenceElement().getIdPart()),
			PractitionerRole.INCLUDE_PRACTITIONER, PractitionerRole.INCLUDE_ORGANIZATION, PractitionerRole.INCLUDE_ENDPOINT, PractitionerRole.INCLUDE_SERVICE);

		PractitionerRole targetEhrPractitionerRole = null;
		Organization targetEhrOrganization = null;
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
			}
		}

		if (targetEhrPractitionerRole == null || targetEhrPractitionerRole.isEmpty()) {
			sendInternalErrorOO("ServiceRequest.performer", "The ServiceRequest.performer: " + targetReference.getReference() + " does not seem to exist.");
		}

		if (targetEhrOrganization == null || targetEhrOrganization.isEmpty()) {
			targetOrganization = null;
		} else {
			targetOrganization = new BSEROrganization(true, "Recipient Organzation");
			targetOrganization.addType(new CodeableConcept(new Coding("http://terminology.hl7.org/CodeSystem/organization-type", "bus", "Non-Healthcare Business or Corporation")));
			targetEhrOrganization.copyValues(targetOrganization);
		}

		// Send message bundle to target
		// First, find the endpoint for thie target.
		if (targetEndpoint != null && !targetEndpoint.isEmpty() && targetEndpoint.getAddress() != null && !targetEndpoint.getAddress().isEmpty()) {
			targetEndpointUrl = targetEndpoint.getAddress();
		} else {
			throw new FHIRException("RecipientPractitionerRole.Endpoint and Endpoint.address cannot be null or empty");
		}

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

		IBaseBundle supportingInfo = null;
		if (theServiceType != null) {
			// Construct the supporting information bundle for each service type.
			if (ServiceType.ARTHRITIS.is(theServiceType.getCode())) {
				supportingInfo = new BSERArthritusReferralSupportingInformation();
				// supportingInfo.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ArthritisReferralSupportingInformation");
			} else if (ServiceType.DIABETES_PREVENTION.is(theServiceType.getCode())) {
				supportingInfo = new BSERDiabetesPreventionReferralSupportingInformation();
				// supportingInfo.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-DiabetesPreventionReferralSupportingInformation");
			} else if (ServiceType.EARLY_CHILDHOOD_NUTRITION.is(theServiceType.getCode())) {
				supportingInfo = new BSEREarlyChildhoodNutritionReferralSupportingInformation();
				// supportingInfo.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-EarlyChildhoodNutritionReferralSupportingInformation");
			} else if (ServiceType.HYPERTENSION.is(theServiceType.getCode())) {
				supportingInfo = new BSERHypertensionReferralSupportingInformation();
				// supportingInfo.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-HypertensionReferralSupportingInformation");
			} else if (ServiceType.OBESITY.is(theServiceType.getCode())) {
				supportingInfo = new BSERObesityReferralSupportingInformation();
				// supportingInfo.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ObesityReferralSupportingInformation");
			} else if (ServiceType.TOBACCO_USE_CESSATION.is(theServiceType.getCode())) {
				supportingInfo = new BSERTobaccoUseCessationReferralSupportingInformation();
				// supportingInfo.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-TobaccoUseCessationReferralSupportingInformation");
			}

			// Construct allergies in USCore. We are adding as we are constructing the allergy in order to save time and memroy
			if (theAllergies != null) {
				if (ServiceType.ARTHRITIS.is(theServiceType.getCode()) || ServiceType.OBESITY.is(theServiceType.getCode()) ) {
					for (BundleEntryComponent allergy : theAllergies.getEntry()) {
						USCoreAllergyIntolerance usCoreAllergyIntolerance = new USCoreAllergyIntolerance();
						AllergyIntolerance allergyIntolerance = (AllergyIntolerance) allergy.getResource();
						allergyIntolerance.copyValues(usCoreAllergyIntolerance);
						// Sanity check on allergy received for the patient reference. If the allergy has no baseUrl, we only check resource type and id.
						if (!isEqualReference(subjectReference, usCoreAllergyIntolerance.getPatient(), subjectReference.getReferenceElement().getBaseUrl())) {
							sendInternalErrorOO("AllergyIntolerance.patient", "the Patient reference does not match with ServiceRequest.subject");
						}

						// Add Allergy
						((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(usCoreAllergyIntolerance.getIdElement().toVersionless().getValue()).setResource(usCoreAllergyIntolerance));
					}
				}
			}

			BloodPressure bloodPressureObservation = new BloodPressure();
			if (theBloodPressure != null) {
				boolean referenced = false;
				for (ParametersParameterComponent bpParam : theBloodPressure.getPart()) {
					if ("reference".equals(bpParam.getName())) {
						// This is reference to the BP observation. Pull this resource
						// from EHR
						bloodPressureObservation = (BloodPressure) pullResourceFromFhirServer((Reference)bpParam.getValue());
						if (!isEqualReference(subjectReference, bloodPressureObservation.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
							sendInternalErrorOO("bloodPressure.subject", "the Subject reference does not match with ServiceRequest.subject");
						}
						referenced = true;
						break;
					} else { 
						if ("date".equals(bpParam.getName())) {
							bloodPressureObservation.setEffective((DateTimeType)bpParam.getValue());
						} else if ("diastolic".equals(bpParam.getName())) {
							bloodPressureObservation.setDiastolic((Quantity) bpParam.getValue());
						} else if ("systolic".equals(bpParam.getName())) {
							bloodPressureObservation.setSystolic((Quantity) bpParam.getValue());
						}
					}
				}

				if (!referenced) {
					bloodPressureObservation.setSubject(subjectReference);
					bloodPressureObservation.setStatus(ObservationStatus.FINAL);

					// write to fhirStore.
					saveResource(bloodPressureObservation);
				}
			}

			// Observation Body Height Profile (http://hl7.org/fhir/StructureDefinition/bodyheight)
			BodyHeight bodyHeightObservation = null;
			if (theBodyHeight != null) {
				Type theBodyHeightValue = theBodyHeight.getValue();
				if (theBodyHeightValue instanceof Reference) {
					StringType referenceString = (StringType) theBodyHeightValue;	
					Observation bodyHeightEhrObservation = (Observation) pullResourceFromFhirServer(new Reference(referenceString.asStringValue()));
					if (!isEqualReference(subjectReference, bodyHeightEhrObservation.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
						sendInternalErrorOO("bodyHeight.subject", "the Subject reference does not match with ServiceRequest.subject");
					}

					bodyHeightObservation = new BodyHeight();
					bodyHeightEhrObservation.copyValues(bodyHeightObservation);
				} else if (theBodyHeightValue instanceof Quantity) {
					bodyHeightObservation = new BodyHeight((Quantity) theBodyHeightValue);
					bodyHeightObservation.setSubject(subjectReference);
					bodyHeightObservation.setStatus(ObservationStatus.FINAL);
					
					// write to fhirStore.
					saveResource(bodyHeightObservation);
				} 
			}

			// Observation Body Weight Profile (http://hl7.org/fhir/StructureDefinition/bodyweight)
			BodyWeight bodyWeightObservation = null;
			if (theBodyWeight != null) {
				Type theBodyWeightValue = theBodyWeight.getValue();
				if (theBodyWeightValue instanceof Reference) {
					Observation bodyWeightEhrObservation = (Observation) pullResourceFromFhirServer((Reference)theBodyWeightValue);
					if (!isEqualReference(subjectReference, bodyWeightEhrObservation.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
						sendInternalErrorOO("bodyWeight.subject", "the Subject reference does not match with ServiceRequest.subject");
					}

					bodyWeightObservation = new BodyWeight();
					bodyWeightEhrObservation.copyValues(bodyWeightObservation);
				} else if (theBodyWeightValue instanceof Quantity) {
					bodyWeightObservation = new BodyWeight((Quantity) theBodyWeightValue);
					bodyWeightObservation.setSubject(subjectReference);
					bodyWeightObservation.setStatus(ObservationStatus.FINAL);
					
					// write to fhirStore.
					saveResource(bodyWeightObservation);
				}
			}

			// Observation Body Mass Index Profile (http://hl7.org/fhir/StructureDefinition/bmi)
			BMI bmiObservation = null;
			if (theBmi != null) {
				Type theBmiValue = theBmi.getValue();
				if (theBmiValue instanceof Reference) {
					Observation bmiEhrObservation = (Observation) pullResourceFromFhirServer((Reference)theBmiValue);
					if (!isEqualReference(subjectReference, bmiEhrObservation.getSubject(), subjectReference.getReferenceElement().getBaseUrl())) {
						sendInternalErrorOO("bmi.subject", "the Subject reference does not match with ServiceRequest.subject");
					}

					bmiObservation = new BMI();
					bmiEhrObservation.copyValues(bmiObservation);
				} else if (theBmiValue instanceof Quantity) {
					bmiObservation = new BMI((Quantity) theBmiValue);
					bmiObservation.setSubject(subjectReference);
					bmiObservation.setStatus(ObservationStatus.FINAL);
					
					// write to fhirStore.
					saveResource(bmiObservation);
				}
			}
			
			// BSeR HA1C Observation (http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-HA1C-Observation)
			BSERHA1CObservation ha1cObservation = null;
			if (theHa1cObservation != null) {
				Type theHa1cObservationValue = theHa1cObservation.getValue();
				if (theHa1cObservationValue instanceof Reference) {
					Reference ha1cObservationReference = (Reference)theHa1cObservationValue;
					Observation ha1cEhrObservation = (Observation) pullResourceFromFhirServer(ha1cObservationReference);
					Reference ha1cSubjectReference = ha1cEhrObservation.getSubject();
					if (!isEqualReference(subjectReference, ha1cSubjectReference, ha1cObservationReference.getReferenceElement().getBaseUrl())) {
						sendInternalErrorOO("ha1cObservation.subject", "the Subject reference does not match with ServiceRequest.subject");
					}

					ha1cObservation = new BSERHA1CObservation();
					ha1cEhrObservation.copyValues(ha1cObservation);
					ha1cObservation.setStatus(ObservationStatus.FINAL);
				} else if (theHa1cObservationValue instanceof Quantity) {
					ha1cObservation = new BSERHA1CObservation(
						new CodeableConcept(new Coding("http://loinc.org", "4548-4", "Hemoglobin A1c/Hemoglobin.total in Blood")), 
						(Quantity) theHa1cObservationValue);
					ha1cObservation.setSubject(subjectReference);
					ha1cObservation.setStatus(ObservationStatus.FINAL);

					// write to fhirStore.
					saveResource(ha1cObservation);
				} else {
					sendInternalErrorOO("ha1cObservation", "ha1cObservation must be either Referece or Quantity");
				}
			}

			// BSeR Diagnosis
			if (theDiagnosis != null) {
				for (ParametersParameterComponent diagnosis : theDiagnosis) {
					BSERDiagnosis diagnosisCondition = new BSERDiagnosis();
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
						saveResource(ha1cObservation);
					}

					if (ServiceType.HYPERTENSION.is(theServiceType.getCode()) 
						&& diagnosisCondition != null && !diagnosisCondition.isEmpty()) {
						((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(diagnosisCondition.getIdElement().toVersionless().getValue()).setResource(diagnosisCondition));
					}		
				}
			}

			// Add Blood Pressure.
			if (ServiceType.ARTHRITIS.is(theServiceType.getCode())
				|| ServiceType.DIABETES_PREVENTION.is(theServiceType.getCode())
				|| ServiceType.EARLY_CHILDHOOD_NUTRITION.is(theServiceType.getCode())
				|| ServiceType.HYPERTENSION.is(theServiceType.getCode())
				|| ServiceType.OBESITY.is(theServiceType.getCode()) ) {

				if (bloodPressureObservation != null && !bloodPressureObservation.isEmpty()) {
					((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(bloodPressureObservation.getIdElement().toVersionless().getValue()).setResource(bloodPressureObservation));
				}
			}

			// Add Body Height and Weight
			if (ServiceType.ARTHRITIS.is(theServiceType.getCode())
				|| ServiceType.DIABETES_PREVENTION.is(theServiceType.getCode())
				|| ServiceType.EARLY_CHILDHOOD_NUTRITION.is(theServiceType.getCode())
				|| ServiceType.HYPERTENSION.is(theServiceType.getCode())
				|| ServiceType.OBESITY.is(theServiceType.getCode()) ) {

				if (bodyHeightObservation != null && !bodyHeightObservation.isEmpty()) {
					((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(bodyHeightObservation.getIdElement().toVersionless().getValue()).setResource(bodyHeightObservation));	
				}

				if (bodyWeightObservation != null && !bodyWeightObservation.isEmpty()) {
					((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(bodyWeightObservation.getIdElement().toVersionless().getValue()).setResource(bodyWeightObservation));
				}
			}

			// Add BMI
			if (ServiceType.ARTHRITIS.is(theServiceType.getCode())
				|| ServiceType.DIABETES_PREVENTION.is(theServiceType.getCode())
				|| ServiceType.HYPERTENSION.is(theServiceType.getCode())
				|| ServiceType.OBESITY.is(theServiceType.getCode()) ) {

				if (bmiObservation != null && !bmiObservation.isEmpty()) {
					((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(bmiObservation.getIdElement().toVersionless().getValue()).setResource(bmiObservation));
				}

			}

			if (ServiceType.DIABETES_PREVENTION.is(theServiceType.getCode())) {
				if (ha1cObservation != null && !ha1cObservation.isEmpty()) {
					((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(ha1cObservation.getIdElement().toVersionless().getValue()).setResource(ha1cObservation));
				}
			} 

			if (ServiceType.EARLY_CHILDHOOD_NUTRITION.is(theServiceType.getCode())) {
				if (theChild != null) {
					String lastName = "";
					String firstName = "";
					CodeType genderCode = new CodeType();
					BodyWeight childWeightObservation = null;
					BodyHeight childHeightObservation =  null;
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
								childHeightObservation = new BodyHeight();
								childHeightEhrObservation.copyValues(childHeightObservation);
							} else if (theBodyHeightValue instanceof Quantity) {
								BodyHeight childHeightEhrObservation = new BodyHeight();
								childHeightEhrObservation.setValue((Quantity) theBodyHeightValue);
								childHeightEhrObservation.setId(new IdType(childWeightObservation.fhirType(), UUID.randomUUID().toString()));
							}
						} else if ("weight".equals(child.getName())) {
							Type theBodyWeightValue = theChild.getValue();
							if (theBodyWeightValue instanceof Reference) {
								Observation childWeightEhrObservation = (Observation) pullResourceFromFhirServer((Reference)theBodyWeightValue);
								childWeightObservation = new BodyWeight();
								childWeightEhrObservation.copyValues(childWeightObservation);
							} else if (theBodyWeightValue instanceof Quantity) {
								childWeightObservation = new BodyWeight();
								childWeightObservation.setValue((Quantity) theBodyWeightValue);
								childWeightObservation.setId(new IdType(childWeightObservation.fhirType(), UUID.randomUUID().toString()));

								// write to fhirStore.
								saveResource(childWeightObservation);
							}
						}
					}

					Patient childPatient = new Patient();
					childPatient.setId(new IdType(UUID.randomUUID().toString()));
					childPatient.addName(new HumanName().setFamily(lastName).addGiven(firstName));
					childPatient.setGender(AdministrativeGender.fromCode(genderCode.getCode()));

					((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(childPatient.getIdElement().toVersionless().getValue()).setResource(childPatient));

					if (childWeightObservation != null && !childWeightObservation.isEmpty()) {
						childWeightObservation.setSubject(new Reference(childPatient.getIdElement()));
						// write to fhirStore.
						saveResource(childWeightObservation);

						((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(childWeightObservation.getIdElement().toVersionless().getValue()).setResource(childWeightObservation));
					}

					if (childHeightObservation != null && !childHeightObservation.isEmpty()) {
						childHeightObservation.setSubject(new Reference(childPatient.getIdElement()));
						// write to fhirStore.
						saveResource(childHeightObservation);

						((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(childHeightObservation.getIdElement().toVersionless().getValue()).setResource(childHeightObservation));
					}
				}
			}
			
		} else {
			throw new FHIRException("ServiceType is missing.");
		}

		Reference supportingInfoReference = new Reference(supportingInfo.getIdElement());
		logger.debug("Supporting Info Reference: " + supportingInfoReference.getReference());

		// Referral Request Document Bundle
		BSERReferralRequestComposition bserReferralRequestComposition= new BSERReferralRequestComposition(
			CompositionStatus.FINAL, 
			new CodeableConcept(new Coding("http://loinc.org", "57133-1", "Referral note")), 
			subjectReference, 
			new Date(), 
			sourceReference, 
			"Referral request", 
			supportingInfoReference
		);

		// Adding composition
		bserReferralRequestComposition.setId(new IdType(bserReferralRequestComposition.fhirType(), UUID.randomUUID().toString()));
		BSERReferralRequestDocumentBundle bserReferralRequestDocumentBundle = new BSERReferralRequestDocumentBundle(bserReferralRequestComposition);
		bserReferralRequestDocumentBundle.setTimestamp(new Date());

		// Adding supporting information for ServiceRequest
		bserReferralRequestDocumentBundle.setId(new IdType(bserReferralRequestDocumentBundle.fhirType(), UUID.randomUUID().toString()));
		bserReferralRequestDocumentBundle.addEntry(new BundleEntryComponent().setFullUrl(((Bundle)supportingInfo).getIdElement().toVersionless().getValue()).setResource((Bundle)supportingInfo));

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
		// bdl-9 and 10 say that for Bundle.type = document, idetifier.system and .value must exit.
		Identifier identifier = new Identifier();
		identifier.setSystem("urn:bser:request:document");
		identifier.setValue(UUID.randomUUID().toString());
		bserReferralRequestDocumentBundle.setIdentifier(identifier);


		serviceRequest.addSupportingInfo(new Reference(bserReferralRequestDocumentBundle.getIdElement()));		

		// serviceRequest, which is from UI has Practitioner for the requester. Overwrite this with parctitionerRole.
		serviceRequest.setRequester(sourceReference);

		// Add any additional requirement data elements to serviceRequest.
		// Set the initiator and recipient identifier to conform to the IG. This should be the
		// identifier that recipient can use to identify this request. And, Recipient can
		// echo this for us to use to search our fhirStore.
		String uniqueIdentifierValue = UUID.randomUUID().toString();
		String identifierSystem = "urn:bser:request:id";
		serviceRequest.setInitiatorIdentifier(identifierSystem, uniqueIdentifierValue, sourceOrganizationReference);
		Reference targetOrganizationReference = (targetOrganization == null || targetOrganization.isEmpty())?null:new Reference(targetOrganization.getIdElement());

		// Set the recipient identifier. This is an optional. If we have organization information, set this identifier.
		if (targetOrganizationReference != null && !targetOrganizationReference.isEmpty()) {
			serviceRequest.setRecipientIdentifier(identifierSystem, uniqueIdentifierValue, targetOrganizationReference);
		}

		serviceRequest.setOccurrence(new DateTimeType(new Date()));

		updateResource(serviceRequest);

		BSERReferralTask bserReferralTask = new BSERReferralTask(
			identifierSystem, uniqueIdentifierValue,
			identifierSystem, uniqueIdentifierValue,
			sourceOrganizationReference, 
			targetOrganizationReference,
			TaskStatus.REQUESTED, 
			new CodeableConcept(new Coding("http://hl7.org/fhir/us/bser/CodeSystem/TaskBusinessStatusCS", "2.0", "Service Request Created")), 
			new Reference(serviceRequest.getIdElement()), 
			new Date(), 
			sourceReference, 
			targetReference);

		bserReferralTask.setIntent(TaskIntent.ORDER);

		// create Task resource in the fhirStore. id will be assigned.
		// bserReferralTask.setId(new IdType(bserReferralTask.fhirType(), UUID.randomUUID().toString()));
		saveResource(bserReferralTask);

		// BSeR Referral Message Header focus Task
		BSERReferralMessageHeader bserReferralMessageHeader = new BSERReferralMessageHeader(targetReference, sourceReference, 
			new Reference(bserReferralTask.getIdElement()), bserEndpointProcessMessageUrl, targetEndpointUrl);
		bserReferralMessageHeader.setId(new IdType(bserReferralMessageHeader.fhirType(), UUID.randomUUID().toString()));

		/*** 
		 * NOW All the resources are ready. Create referral package. Most of them are bundles....
		 * 
		 ***/

		// Create Message Bundle. Pass the message header as an argument to add the header in the first entry.
		BSERReferralMessageBundle messageBundle = new BSERReferralMessageBundle(bserReferralMessageHeader);
		messageBundle.setId(new IdType(bserEndpointUrl, messageBundle.fhirType(), UUID.randomUUID().toString(), null));

		// Now add all the referenced resources at this message bundle level. These are (except message header),
		// task and service request (task.focuse = service request). Service Request also has references. They will all
		// be included in this message bundle entry.

		// Task Resource (messageHeader.focus)
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(bserReferralTask.getIdElement().toVersionless().getValue()).setResource(bserReferralTask));

		// Service Request (task.focus)
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(serviceRequest.getIdElement().toVersionless().getValue()).setResource(serviceRequest));

		// Referral Request Document Bundle (serviceRequest.supprotingInfo).
		// The refferral request document bundle includes resources as well. Those are taken care of when this bundle is created above.
		// Profile is not generated when bundle is set to dataelement. So, set it manually.
		// bserReferralRequestDocumentBundle.getMeta().addProfile("http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-ReferralRequestDocumentBundle");
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(bserReferralRequestDocumentBundle.getIdElement().toVersionless().getValue()).setResource(bserReferralRequestDocumentBundle));

		// Patient resource. This is referenced by almost all resources.
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(thePatient.getIdElement().toVersionless().getValue()).setResource(thePatient));

		// Task and Service Request resources have initiator and recipient practitionerRole. And, it may include practitioner. 
		// Add these resources here.
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourcePractitionerRole.getIdElement().toVersionless().getValue()).setResource(sourcePractitionerRole));
		if (sourcePractitioner != null && !sourcePractitioner.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourcePractitioner.getIdElement().toVersionless().getValue()).setResource(sourcePractitioner));
		}
		if (sourceOrganization != null && !sourceOrganization.isEmpty())
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceOrganization.getIdElement().toVersionless().getValue()).setResource(sourceOrganization));
		
		if (sourceEndpoint != null && !sourceEndpoint.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceEndpoint.getIdElement().toVersionless().getValue()).setResource(sourceEndpoint));
		}
	
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetPractitionerRole.getIdElement().toVersionless().getValue()).setResource(targetPractitionerRole));
		if (targetPractitioner != null && !targetPractitioner.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetPractitioner.getIdElement().toVersionless().getValue()).setResource(targetPractitioner));
		}
		if (targetOrganization != null && !targetOrganization.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetOrganization.getIdElement().toVersionless().getValue()).setResource(targetOrganization));
		}

		if (targetEndpoint != null && !targetEndpoint.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetEndpoint.getIdElement().toVersionless().getValue()).setResource(targetEndpoint));
		}

		if (targetHealthService != null && !targetHealthService.isEmpty()) {
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetHealthService.getIdElement().toVersionless().getValue()).setResource(targetHealthService));
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
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(coverage.getIdElement().toVersionless().getValue()).setResource(coverage));
		}

		// Submit to target $process-message operation
		FhirContext ctx = StaticValues.myFhirContext; 
		ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

		if (!"true".equalsIgnoreCase(System.getenv("RECIPIENT_NOT_READY"))) {
			IGenericClient client;
			if (targetEndpointUrl != null && !targetEndpointUrl.isBlank()) {
				IParser parser = ctx.newJsonParser();
				String messageBundleJson = parser.encodeResourceToString(messageBundle);
				logger.debug("SENDING MessageBundle TO " + targetEndpointUrl + ":\n" + messageBundleJson);

				IBaseResource response = null;
				if ("YUSA".equals(recipientAA.getRecipientSite())) {
					// This is YUSA endpoint, which does not have FHIR messaging operation name.
					String respYusa = recipientAA.submitYusaRR(targetEndpointUrl, messageBundleJson);
					warningMessage = warningMessage.concat("Submitted to YUSA in Restful POST and recieved response(s) = " + respYusa + "\n");
				} else {
					String accessToken = null;
					try {
						accessToken = recipientAA.getAccessToken();
					} catch (ParseException e) {
						e.printStackTrace();
					}

					client = ctx.newRestfulGenericClient(targetEndpointUrl);
					if (accessToken != null && !accessToken.isBlank()) {
						BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);
						client.registerInterceptor(authInterceptor);
					}

					response = client
						.operation()
						.processMessage() // New operation for sending messages
						.setMessageBundle(messageBundle)
						.asynchronous(Bundle.class)
						.execute();	
				}

				if (response != null) {
					if (!response.isEmpty() && response instanceof OperationOutcome) {
						throw new InternalErrorException("Submitting to " + targetEndpointUrl + " failed", (IBaseOperationOutcome) response);
					} else if (!response.isEmpty() && response instanceof Resource){
						logger.warn("Message has a resource payload: " + response.fhirType());
					} else {
						logger.debug("Messaging response is null");
					}
				} 

			}
		} else {
			warningMessage = warningMessage.concat("Referral Request has NOT been submitted because submission is disabled. Enable it by setting 'RECIPIENT_NOT_READY' to false. ");
		}

		// Save Message before submission.
		saveResource(messageBundle);
				
		// return anything if needed in Parameters
		Parameters returnParameters = new Parameters();
		returnParameters.addParameter("referral_request", new Reference(messageBundle.getIdElement()));
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

	/***
	 * processMessageOperation
	 * @param theContent
	 * @param theAsync
	 * @param theUri
	 * @return
	 */
	@Operation(name="$process-message")
	public Bundle processMessageOperation(
		@OperationParam(name="content") Bundle theContent,
		@OperationParam(name="async") BooleanType theAsync,
		@OperationParam(name="response-url") UriType theUri			
	) {
		// Feedback would also be async as there can be another message.
		if (theAsync == null) {
			throw new FHIRException("async parameter must exist");
		}
		if (!theAsync.booleanValue()) {
			throw new FHIRException("BSERReferralFeedback Messageing must have async set to true.");
		}

		// String recipientEndpointUrl;
		// if (theUri != null && !theUri.isEmpty()) {
		// 	recipientEndpointUrl = theUri.asStringValue();
		// }

		BSERReferralMessageHeader messageHeader = null;

		Reference initiatorPractitionerRoleReference = null;

		BSERReferralTask bserReferralTask = null;
		Reference referralTaskReference = null;

		if (theContent == null || theContent.isEmpty()) {
			// content cannot be null or empty.
			throw new FHIRException("content is either null or empty");
		}

		if (theContent.getType() == BundleType.MESSAGE) {
			List<BundleEntryComponent> entries = theContent.getEntry();

			// Evaluate the first entry, which must be MessageHeader
			BundleEntryComponent entry1 = theContent.getEntryFirstRep();
			Resource resource = entry1.getResource();
			if (resource instanceof MessageHeader) {
				// MessageHeader is like metadata for a message. Capture all the
				// necessary information.
				messageHeader = (BSERReferralMessageHeader) resource;
				if (!BSERReferralMessageHeader.isBSERReferralMessageHeader(messageHeader)) {
					throw new FHIRException("Received message is NOT REF/RRI - Patient referral");
				}
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
						if (entry.getFullUrl().contains(referralTaskReference.getId())) {
							bserReferralTask = (BSERReferralTask) entry.getResource();
							break;
						}
	
					} 						
				}

				if (bserReferralTask == null || !bserReferralTask.isEmpty()) {
					throw new FHIRException("BSERReferralTask cannot be found from the MessageBundle entries.");
				}

			} else {
				throw new FHIRFormatError("The bundle must be a MESSAGE type");
			}

			TaskOutputComponent outputFromRecipient = bserReferralTask.getOutputFirstRep();
			if (outputFromRecipient == null || outputFromRecipient.isEmpty()) {
				throw new FHIRFormatError("BSERReferralTask.output is null or Empty");
			} 

			Reference bserFeedbackDocumentReference = (Reference) outputFromRecipient.getValue();
			if (bserFeedbackDocumentReference == null || bserFeedbackDocumentReference.isEmpty()) {
				throw new FHIRException("BSERReferralTask.output.valueReference cannot be null or empty.");
			}
			BSERReferralFeedbackDocument bserReferralFeedbackDocument = null;
			for (BundleEntryComponent entry : entries) {
				resource = entry.getResource();
				if (resource instanceof Bundle) {
					if (entry.getFullUrl().contains(bserFeedbackDocumentReference.getId())) {
						bserReferralFeedbackDocument = (BSERReferralFeedbackDocument) entry.getResource();
						break;
					}
				} 
			}

			if (bserReferralFeedbackDocument == null || bserReferralFeedbackDocument.isEmpty()) {
				throw new FHIRException("BSERReferralTask.output.valueReference cannot be found in the entry resources.");
			}

			saveResource(bserReferralFeedbackDocument);

			BSERReferralTask bserReferralTaskFromFhirStore = (BSERReferralTask) pullResourceFromFhirServer(referralTaskReference);
			bserReferralTask.copyValues(bserReferralTaskFromFhirStore);

			// We just stored BSER Referral Feedback Document. Relink this
			bserReferralTaskFromFhirStore.getOutputFirstRep().setValue(new Reference(bserReferralTask.getIdElement()));
			updateResource(bserReferralTaskFromFhirStore);

			// We also need to update ServiceRequest
			Reference serviceRequestReference = bserReferralTaskFromFhirStore.getFocus();
			BSERReferralServiceRequest referralServiceRequestInFeedback = null;
			for (BundleEntryComponent entry : entries) {
				resource = entry.getResource();
				if (resource instanceof ServiceRequest) {
					if (entry.getFullUrl().contains(serviceRequestReference.getId())) {
						referralServiceRequestInFeedback = (BSERReferralServiceRequest) resource;
					}
				}
			}

			if (referralServiceRequestInFeedback == null || referralServiceRequestInFeedback.isEmpty()) {
				throw new FHIRException("Referral ServiceRequest is empty or does not exit.");
			}

			// Now obtain the ServiceRequest from fhirStore.
			BSERReferralServiceRequest bserReferralServiceRequestFromFhirStore = (BSERReferralServiceRequest) pullResourceFromFhirServer(serviceRequestReference);
			referralServiceRequestInFeedback.copyValues(bserReferralServiceRequestFromFhirStore);

			updateResource(bserReferralServiceRequestFromFhirStore);			
		} else {
			throw new FHIRException("The bundle must be a MESSAGE type");
		}
		
		// This is async messaging. So, we do not return Bundle.
		return null;
	}
}
