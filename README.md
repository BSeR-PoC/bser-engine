# BSeR-engine

Bidirectional Services e-Referrals (BSeR) Engine is a backend service implementation of BSeR FHIR IG (https://build.fhir.org/ig/HL7/bser/StructureDefinition-referral-servicerequest.html). The BSeR Engine is implemented to show an example of BSeR implementation. BSeR implements local custom interfaces for operations with BSeR UI. And, BSeR engine provides $process-message FHIR API operation/ This API conforms to the FHIR Restful API Operation for messages and handles BSeR FHIR messages. 

BSeR engine is built on HAPI FHIR Server example (https://github.com/hapifhir/hapi-fhir-examples/tree/master/restful-server-example). BSeR engine also implements /jwk endpoint for the SMARTonFHIR communication so that EHR can retrieve JWK set.

```
<host>/$process-message accepts BSeR message or BSeR response message
<host>/$referral-request is local internal operation API UI or any client can request for referrals. BSeR engine will construct BSeR FHIR data based on the information provided from this request.
```