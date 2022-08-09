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
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.MessageHeader.MessageHeaderResponseComponent;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.codesystems.V3EducationLevel;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.dstu3.model.codesystems.ObservationCategory;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import edu.gatech.chai.BSER.model.BSERDiabetesPreventionReferralSupportingInformation;
import edu.gatech.chai.BSER.model.BSEREducationLevel;
import edu.gatech.chai.BSER.model.BSERHA1CObservation;
import edu.gatech.chai.BSER.model.BSERReferralMessageBundle;
import edu.gatech.chai.BSER.model.BSERReferralMessageHeader;
import edu.gatech.chai.BSER.model.BSERReferralRequestComposition;
import edu.gatech.chai.BSER.model.BSERReferralRequestDocumentBundle;
import edu.gatech.chai.BSER.model.BSERReferralTask;
import edu.gatech.chai.BSER.model.ODHEmploymentStatus;
import edu.gatech.chai.SmartOnFhirClient.SmartBackendServices;
import edu.gatech.chai.bserengine.utilities.CodeableConceptUtil;
import edu.gatech.chai.bserengine.utilities.StaticValues;
import edu.gatech.chai.bserengine.utilities.ThrowFHIRExceptions;

public class ServerOperations {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServerOperations.class);

	SmartBackendServices smartBackendServices;

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
	
	public ServerOperations() {
		WebApplicationContext context = ContextLoaderListener.getCurrentWebApplicationContext();
		smartBackendServices = context.getBean(SmartBackendServices.class);
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

		JSONObject accessTokenJson = new JSONObject(accessTokenJsonStr);
		String accessToken = accessTokenJson.getString("access_token");

		// Read test patient
		// IRestfulClientFactory clientFactory = StaticValues.myFhirContext.getRestfulClientFactory();
		BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);

		return authInterceptor;
	}

	private IBaseBundle searchObservation(Reference subject, String category, Coding... codings) {
		BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();

		IGenericClient genericClient = StaticValues.myFhirContext.newRestfulGenericClient(smartBackendServices.getFhirServerUrl());
		genericClient.registerInterceptor(authInterceptor);

		return genericClient.search().forResource(Observation.class)
			.where(Observation.SUBJECT.hasId(subject.getReferenceElement()))
			.where(Observation.CATEGORY.exactly().code(category))
			.where(Observation.CODE.exactly().codings(codings))
			.execute();
	}

	private IBaseResource pullResourceFromEHR(Reference reference) {
		// Read test patient
		// IRestfulClientFactory clientFactory = StaticValues.myFhirContext.getRestfulClientFactory();
		BearerTokenAuthInterceptor authInterceptor = getBearerTokenAuthInterceptor();

		IGenericClient genericClient = StaticValues.myFhirContext.newRestfulGenericClient(smartBackendServices.getFhirServerUrl());
		genericClient.registerInterceptor(authInterceptor);

		// Bundle response = genericClient.search().forResource(Patient.class)
		//     .where(Patient.FAMILY.matches().values("Optime"))
		//     .returnBundle(Bundle.class)
		//     .execute();

		String resourceType = reference.getReferenceElement().getResourceType();
		String resourceId = reference.getReferenceElement().getIdPart();
		System.out.println("resourceType:"+resourceType+" and resourceId:"+resourceId);
		IBaseResource response = genericClient.read().resource(resourceType).withId(resourceId).prettyPrint().execute();

		IParser parser = StaticValues.myFhirContext.newJsonParser();
		String responseString = parser.encodeResourceToString(response);
		System.out.println("Response: \n" + responseString);
		
		return response;
	}	
	
	@Operation(name="$referral-request")
	public Parameters processReferral(
		@OperationParam(name="referral") ServiceRequest theServiceRequest,
		@OperationParam(name="serviceType") CodeType theServiceType,
		@OperationParam(name="educationLevel") CodeType theEducationLevel,
		@OperationParam(name="employmentStatus") CodeType theEmploymentStatus,
		@OperationParam(name="allergies") Bundle theAllergies,
		@OperationParam(name="bloodPressure") List<ParametersParameterComponent> theBloodPressure,
		@OperationParam(name="bodyHeight") IBase theBodyHeight,
		@OperationParam(name="bodyWeight") IBase theBodyWeight,
		@OperationParam(name="bmi") IBase theBmi,
		@OperationParam(name="diagnosis") IBase theDiagnosis,
		@OperationParam(name="isBabyLatching") BooleanType theIsBabyLatching,
		@OperationParam(name="momsConcerns") StringType theMomsConcerns,
		@OperationParam(name="nippleShieldUse") BooleanType theNippleShieldUse,
		@OperationParam(name="ha1cObservation") List<ParametersParameterComponent> theHa1cObservation,
		@OperationParam(name="medications") Bundle theMedications,
		@OperationParam(name="nrtAuthorizationStatus") CodeType theNrtAuthorizationStatus,
		@OperationParam(name="child")  List<ParametersParameterComponent> theChild,
		@OperationParam(name="smokingStatus") CodeType theSmokingStatus,
		@OperationParam(name="communicationPreferences") List<ParametersParameterComponent> theCommunicationPreferences
	) {

		Reference sourceReference = null;
		Reference targetReference = null;
		Reference subjectReference = null;
		Patient patient = null;
		PractitionerRole sourcePractitionerRole = null;
		PractitionerRole targetPractitionerRole = null;
		Organization sourceOrganization = null;
		Organization targetOrganization = null;

		if (theServiceRequest != null) {
			if (theServiceRequest.getStatus() != ServiceRequestStatus.ACTIVE) {
				// Set the status to ACTIVE before we submit the referral
				theServiceRequest.setStatus(ServiceRequestStatus.ACTIVE);
			}
			// Get source and target practitioners.
			sourceReference = theServiceRequest.getRequester();
			sourcePractitionerRole = (PractitionerRole) pullResourceFromEHR(sourceReference);

			Reference reference = sourcePractitionerRole.getOrganization();
			if (reference != null && !reference.isEmpty()) {
				sourceOrganization = (Organization) pullResourceFromEHR(reference);
			} else {
				sourceOrganization = new Organization();
				sourceOrganization.setId(new IdType(sourceOrganization.fhirType(), UUID.randomUUID().toString()));
			}

			targetReference = theServiceRequest.getPerformerFirstRep();
			targetPractitionerRole = (PractitionerRole) pullResourceFromEHR(targetReference);

			reference = targetPractitionerRole.getOrganization();
			if (reference != null && !reference.isEmpty()) {
				targetOrganization = (Organization) pullResourceFromEHR(reference);
			} else {
				targetOrganization = new Organization();
				targetOrganization.setId(new IdType(targetOrganization.fhirType(), UUID.randomUUID().toString()));
			}

			subjectReference = theServiceRequest.getSubject();
			if ("Patient".equals(subjectReference.getReferenceElement().getResourceType())) {
				patient = (Patient) pullResourceFromEHR(subjectReference);;
			} else {
				OperationOutcome oo = new OperationOutcome();
				oo.setId(UUID.randomUUID().toString());
				OperationOutcomeIssueComponent ooic = new OperationOutcomeIssueComponent();
				ooic.setSeverity(IssueSeverity.ERROR);
				ooic.setCode(IssueType.REQUIRED);
				ooic.addExpression("ServiceRequest.subject");
				oo.addIssue(ooic);
				throw new InternalErrorException("Subject must be Patient", oo);	
			}
		} else {
			OperationOutcome oo = new OperationOutcome();
			oo.setId(UUID.randomUUID().toString());
			OperationOutcomeIssueComponent ooic = new OperationOutcomeIssueComponent();
			ooic.setSeverity(IssueSeverity.ERROR);
			ooic.setCode(IssueType.REQUIRED);
			ooic.addExpression("Parameters.parameter.where(name='referral').empty()");
			oo.addIssue(ooic);
			throw new InternalErrorException("Referral is missing", oo);
		}
		
		BSEREducationLevel educationLevel = null;
		if (theEducationLevel != null) {
			// Create and save the EducationLevel Observation
			// http://hl7.org/fhir/us/bser/StructureDefinition/BSeR-EducationLevel
			V3EducationLevel v3EducationLevel = V3EducationLevel.fromCode(theEducationLevel.getCode());
			educationLevel = new BSEREducationLevel(new CodeableConcept(new Coding(v3EducationLevel.getSystem(), v3EducationLevel.toCode(), v3EducationLevel.getDisplay())));
			educationLevel.setId(new IdType(educationLevel.fhirType(), UUID.randomUUID().toString()));
		}

		ODHEmploymentStatus odhEmploymentStatus =  null;
		if (theEmploymentStatus != null) {
			// http://hl7.org/fhir/us/odh/StructureDefinition/odh-EmploymentStatus
			odhEmploymentStatus = new ODHEmploymentStatus(subjectReference);
			odhEmploymentStatus.setId(new IdType(odhEmploymentStatus.fhirType(), UUID.randomUUID().toString()));
		}

		if (theAllergies != null) {
			// AllergyTolerance resources in a bundle

		}

		Observation bloodPressureObservation = null;
		if (theBloodPressure != null) {
			for (ParametersParameterComponent bloodPressure : theBloodPressure) {
				if ("valueReference".equals(bloodPressure.getName())) {
					// This is reference to the BP observation. Pull this resource
					// from EHR
					bloodPressureObservation = (Observation) pullResourceFromEHR((Reference)bloodPressure.getValue());
				}
			}
		}

		IBaseBundle supportingInfo = null;
		if (theServiceType != null) {
			if (ServiceType.DIABETES_PREVENTION.is(theServiceType.getCode())) {
				// This is Diabetes Prevention referral service request.
				if (bloodPressureObservation != null) {
					supportingInfo = new BSERDiabetesPreventionReferralSupportingInformation(bloodPressureObservation);
				} else {
					supportingInfo = new BSERDiabetesPreventionReferralSupportingInformation();
				}

				supportingInfo.setId(new IdType(supportingInfo.fhirType(), UUID.randomUUID().toString()));
			}
		} else {
			OperationOutcome oo = new OperationOutcome();
			oo.setId(UUID.randomUUID().toString());
			OperationOutcomeIssueComponent ooic = new OperationOutcomeIssueComponent();
			ooic.setSeverity(IssueSeverity.ERROR);
			ooic.setCode(IssueType.REQUIRED);
			oo.addIssue(ooic);
			throw new InternalErrorException("ServiceType is missing", oo);
		}

		Reference supportingInfoReference = new Reference(supportingInfo.getIdElement());
		System.out.println("Supporting Info Reference: " + supportingInfoReference.getReference());

		// Referral Request Document Bundle
		BSERReferralRequestComposition bSERReferralRequestComposition= new BSERReferralRequestComposition(
			CompositionStatus.FINAL, 
			new CodeableConcept(new Coding("http://loinc.org", "57133-1", "Referral note")), 
			subjectReference, 
			new Date(), 
			sourceReference, 
			"Referral request", 
			supportingInfoReference
		);
		bSERReferralRequestComposition.setId(new IdType(bSERReferralRequestComposition.fhirType(), UUID.randomUUID().toString()));

		// Adding supporting information for ServiceRequest
		BSERReferralRequestDocumentBundle bSERReferralRequestDocumentBundle = new BSERReferralRequestDocumentBundle(bSERReferralRequestComposition);
		bSERReferralRequestDocumentBundle.setId(new IdType(bSERReferralRequestDocumentBundle.fhirType(), UUID.randomUUID().toString()));

		// add the supporting resources to the supporting info bundle slices
		Bundle ha1cObservations = (Bundle) searchObservation(subjectReference, ObservationCategory.LABORATORY.toCode(), new Coding("http://loinc.org", "4548-4", "Hemoglobin A1c/Hemoglobin.total in Blood"));

		// get the first observation for now.
		BundleEntryComponent entry = ha1cObservations.getEntryFirstRep();
		Observation ha1cObservation = (Observation) entry.getResource();

		// Turn this to ha1d observation profile.
		BSERHA1CObservation bserHAicOb = new BSERHA1CObservation();
		ha1cObservation.copyValues(bserHAicOb);
		bserHAicOb.setStatus(ObservationStatus.FINAL);

		((Bundle)supportingInfo).addEntry(new BundleEntryComponent().setFullUrl(bserHAicOb.getId()).setResource(bserHAicOb));
		bSERReferralRequestDocumentBundle.addEntry(new BundleEntryComponent().setFullUrl(((Bundle)supportingInfo).getIdElement().toVersionless().getValue()).setResource((Bundle)supportingInfo));

		theServiceRequest.addSupportingInfo(new Reference(bSERReferralRequestDocumentBundle.getIdElement()));		

		// Create a task
		BSERReferralTask bserReferralTask = new BSERReferralTask(
			sourcePractitionerRole.getId(), 
			targetPractitionerRole.getId(), 
			new Reference(sourceOrganization.getIdElement()), 
			TaskStatus.REQUESTED, 
			new CodeableConcept(new Coding("http://hl7.org/fhir/us/bser/CodeSystem/TaskBusinessStatusCS", "7.0", "Service Request Fulfillment Completed")), 
			new Reference(theServiceRequest.getIdElement()), 
			new Date(), 
			sourceReference, 
			targetReference);
		bserReferralTask.setId(new IdType(bserReferralTask.fhirType(), UUID.randomUUID().toString()));

		// Focus in BSeR Referral Message Bundle
		BSERReferralMessageHeader bserReferralMessageHeader = new BSERReferralMessageHeader(targetReference, sourceReference, new Reference(bserReferralTask.getIdElement()));
		bserReferralMessageHeader.setId(new IdType(bserReferralMessageHeader.fhirType(), UUID.randomUUID().toString()));

		BSERReferralMessageBundle messageBundle = new BSERReferralMessageBundle(bserReferralMessageHeader);
		messageBundle.setId(new IdType(messageBundle.fhirType(), UUID.randomUUID().toString()));
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(bserReferralTask.getIdElement().toVersionless().getValue()).setResource(bserReferralTask));

		// Add optional resources to MessageBundle entry
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(theServiceRequest.getIdElement().toVersionless().getValue()).setResource(theServiceRequest));
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(bSERReferralRequestDocumentBundle.getIdElement().toVersionless().getValue()).setResource(bSERReferralRequestDocumentBundle));

		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(patient.getIdElement().toVersionless().getValue()).setResource(patient));

		// Add all the supporting resources to MessageBundle entry.
		if (odhEmploymentStatus != null) {
			messageBundle.addEmploymentStatus(odhEmploymentStatus);
		}
		if (educationLevel != null) {
			messageBundle.addEducationLevel(educationLevel);
		}

		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourcePractitionerRole.getId()).setResource(sourcePractitionerRole));
		if (sourceOrganization != null)
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(sourceOrganization.getId()).setResource(sourceOrganization));
		
		messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetPractitionerRole.getId()).setResource(targetPractitionerRole));
		if (targetOrganization != null)
			messageBundle.addEntry(new BundleEntryComponent().setFullUrl(targetOrganization.getId()).setResource(targetOrganization));
		
		// Send message bundle to target
		// First, find the endpoint for thie target.
		String targetURL = null;
		if (targetOrganization != null) {
			Reference endpointReference = targetOrganization.getEndpointFirstRep();
			if (endpointReference != null && !endpointReference.isEmpty()) {
				Endpoint targetEndpoint = (Endpoint) pullResourceFromEHR(targetOrganization.getEndpointFirstRep());
				if (targetEndpoint != null && !targetEndpoint.isEmpty()) {
					targetURL = targetEndpoint.getAddress();
				}
			}
		}
		
		// Submit to target $process-message operation
		FhirContext ctx = StaticValues.myFhirContext; 
		IParser parser = ctx.newJsonParser();
		String messageBundleJson = parser.encodeResourceToString(messageBundle);
		logger.info("SENDING TO " + targetURL + ":\n" + messageBundleJson);

		// IGenericClient client = ctx.newRestfulGenericClient(targetURL);
		// IBaseResource response = client
		// 	.operation()
		// 	.processMessage() // New operation for sending messages
		// 	.setMessageBundle(messageBundle)
		// 	.asynchronous(Bundle.class)
		// 	.execute();
		
		// if (response instanceof OperationOutcome) {
		// 	throw new InternalErrorException("Submitting to " + targetURL + " failed", (IBaseOperationOutcome) response);
		// }

		// return anything if needed in Parameters
		Parameters returnParameters = new Parameters();
		return returnParameters;
	}

	@Operation(name="$process-message")
	public Bundle processMessageOperation(
		@OperationParam(name="content") Bundle theContent,
		@OperationParam(name="async") BooleanType theAsync,
		@OperationParam(name="response-url") UriType theUri			
	) {
		Bundle retVal = new Bundle();
		MessageHeader messageHeader = null;
		List<Resource> resources = new ArrayList<Resource>();
		
		if (theContent.getType() == BundleType.MESSAGE) {
			List<BundleEntryComponent> entries = theContent.getEntry();
			// Evaluate the first entry, which must be MessageHeader
//			BundleEntryComponent entry1 = theContent.getEntryFirstRep();
//			Resource resource = entry1.getResource();
			if (entries != null && entries.size() > 0 && 
					entries.get(0).getResource() != null &&
					entries.get(0).getResource().getResourceType() == ResourceType.MessageHeader) {
				messageHeader = (MessageHeader) entries.get(0).getResource();
				// We handle observation-type.
				// TODO: Add other types later.
				Coding event = messageHeader.getEventCoding();
				Coding obsprovided = new Coding("http://hl7.org/fhir/message-events", "observation-provide", "Provide a simple observation or update a previously provided simple observation.");
				if (CodeableConceptUtil.compareCodings(event, obsprovided) == 0) {
					// This is lab report. they are all to be added to the server.
					for (int i=1; i<entries.size(); i++) {
						resources.add(entries.get(i).getResource());
					}
				} else {
					ThrowFHIRExceptions.unprocessableEntityException(
							"We currently support only observation-provided Message event");
				}
			}
		} else {
			ThrowFHIRExceptions.unprocessableEntityException(
					"The bundle must be a MESSAGE type");
		}
		MessageHeaderResponseComponent messageHeaderResponse = new MessageHeaderResponseComponent();
		messageHeaderResponse.setId(messageHeader.getId());

		List<BundleEntryComponent> resultEntries = null;
		try {
			// resultEntries = myMapper.createEntries(resources);
			// TODO: We need to call OMOPonFHIR to create entries here
			messageHeaderResponse.setCode(ResponseType.OK);
		} catch (FHIRException e) {
			e.printStackTrace();
			messageHeaderResponse.setCode(ResponseType.OK);
			OperationOutcome outcome = new OperationOutcome();
			CodeableConcept detailCode = new CodeableConcept();
			detailCode.setText(e.getMessage());
			outcome.addIssue().setSeverity(IssueSeverity.ERROR).setDetails(detailCode);
			messageHeaderResponse.setDetailsTarget(outcome);
		}
		
		messageHeader.setResponse(messageHeaderResponse);
		BundleEntryComponent responseMessageEntry = new BundleEntryComponent();
		UUID uuid = UUID.randomUUID();
		responseMessageEntry.setFullUrl("urn:uuid:"+uuid.toString());
		responseMessageEntry.setResource(messageHeader);
		
		if (resultEntries == null) resultEntries = new ArrayList<BundleEntryComponent>();
		
		resultEntries.add(0, responseMessageEntry);
		retVal.setEntry(resultEntries);
		
		return retVal;
	}
}
