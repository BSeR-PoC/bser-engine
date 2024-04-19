package edu.gatech.chai.bserengine.utilities;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Task.TaskStatus;

public enum BserTaskBusinessStatus {
    SERVICE_REQUEST_CREATED("Service Request Created", "2.0", TaskStatus.REQUESTED, ServiceRequestStatus.ACTIVE),
    SERVICE_REQUEST_ACCEPTED("Service Request Accepted", "3.0", TaskStatus.ACCEPTED, ServiceRequestStatus.ACTIVE),
    SERVICE_REQUEST_DECLINED("Service Request Declined", "4.0", TaskStatus.REJECTED, ServiceRequestStatus.REVOKED),
    SERVICE_REQUEST_EVENT_SCHEDULED("Service Request Event Scheduled", "5.1.1", TaskStatus.INPROGRESS, ServiceRequestStatus.ACTIVE),
    SCHEDULED_SERVICE_REQUEST_EVENT_UNATTENDED("Scheduled Service Request Event Unattended", "5.1.2", TaskStatus.INPROGRESS, ServiceRequestStatus.ACTIVE),
    SCHEDULED_SERVICE_REQUEST_EVENT_CANCELLED("Scheduled Service Request Event Cancelled", "5.1.3", TaskStatus.CANCELLED, ServiceRequestStatus.REVOKED),
    SERVICE_REQUEST_EVENT_COMPLETED("Service Request Event Completed", "5.1.4", TaskStatus.COMPLETED, ServiceRequestStatus.COMPLETED),
    SERVICE_REQUEST_CANCELLATION_REQUESTED("Service Request Cancellation Requested", "5.2", TaskStatus.INPROGRESS, ServiceRequestStatus.ACTIVE),
    SERVICE_REQUEST_FULFILLMENT_CANCELLED("Service Request Fulfillment Cancelled", "6.0", TaskStatus.CANCELLED, ServiceRequestStatus.REVOKED),
    SERVICE_REQUEST_FULFILLMENT_COMPLETED("Service Request Fulfillment Completed", "7.0", TaskStatus.COMPLETED, ServiceRequestStatus.COMPLETED);
    
    public static String SYSTEM = "http://hl7.org/fhir/us/bser/CodeSystem/TaskBusinessStatusCS";

    private String display;
    private String code;
    private TaskStatus taskStatus;
    private ServiceRequestStatus serviceRequestStatus;

    public CodeableConcept getCodeableConcept() {
        return new CodeableConcept(new Coding(SYSTEM, code, display)); 
    }

    public static TaskStatus taskStatusFromCode(String code) {
        for (BserTaskBusinessStatus businessStatus : BserTaskBusinessStatus.values()) {
            if (businessStatus.code.equals(code)) {
                return businessStatus.taskStatus;
            }
        }

        return TaskStatus.NULL;
    }

    public static ServiceRequestStatus serviceRequestStatusFromCode(String code) {
        for (BserTaskBusinessStatus businessStatus : BserTaskBusinessStatus.values()) {
            if (businessStatus.code.equals(code)) {
                return businessStatus.serviceRequestStatus;
            }
        }

        return ServiceRequestStatus.NULL;
    }

    public static ServiceRequestStatus serviceRequestStatusFromCodeableConcept(CodeableConcept codeableConcept) {
        for (Coding coding : codeableConcept.getCoding()) {
            ServiceRequestStatus status = serviceRequestStatusFromCode(coding.getCode());
            if (status != ServiceRequestStatus.NULL) {
                return status;
            }
        }

        return ServiceRequestStatus.NULL;
    }

    public static TaskStatus taskStatusFromCodeableConcept(CodeableConcept codeableConcept) {
        for (Coding coding : codeableConcept.getCoding()) {
            TaskStatus status = taskStatusFromCode(coding.getCode());
            if (status != TaskStatus.NULL) {
                return status;
            }
        }

        return TaskStatus.NULL;
    }

    private BserTaskBusinessStatus(String display, String code, TaskStatus taskStatus, ServiceRequestStatus serviceRequestStatus) {
        this.display = display;
        this.code = code;
        this.taskStatus = taskStatus;
        this.serviceRequestStatus = serviceRequestStatus;
    }
}
