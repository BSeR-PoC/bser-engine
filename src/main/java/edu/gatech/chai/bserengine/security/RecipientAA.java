package edu.gatech.chai.bserengine.security;

import java.time.Instant;

import org.hl7.fhir.exceptions.FHIRException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RecipientAA {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecipientAA.class);

    private String authenticationApiUrl;
    private String authorizationApiUrl;
    private String recipientSite;
    private long authCodeExpiresAt;
    private long accessTokenExpiresAt;
    private String authCode;
    private String accessToken;

    public RecipientAA () {
        setAuthenticationApiUrl(System.getenv("AUTHENTICATION_API_URL"));
        setAuthorizationApiUrl(System.getenv("AUTHORIZATION_API_URL"));
        setRecipientSite(System.getenv("RECIPIENT_SITE"));

        authCodeExpiresAt = Instant.now().getEpochSecond();
        accessTokenExpiresAt = Instant.now().getEpochSecond();
    }

    public RecipientAA(String authenticationApiUrl, String authorizationApiUrl, String recipientSite) {
        setAuthenticationApiUrl(authenticationApiUrl);
        setAuthorizationApiUrl(authorizationApiUrl);
        setRecipientSite(recipientSite);

        authCodeExpiresAt = Instant.now().getEpochSecond();
        accessTokenExpiresAt = Instant.now().getEpochSecond();
    }

    public boolean isReady() {
        boolean ret =false;
        if (getAuthenticationApiUrl() != null && !getAuthenticationApiUrl().isBlank() 
            && getAuthorizationApiUrl() != null && !getAuthorizationApiUrl().isBlank()
            && getRecipientSite() != null && !getRecipientSite().isBlank()) {
            ret = true;
        } 
        
        return ret;
    }

    private String getYUSAAccessToken (HttpHeaders headers, RestTemplate restTemplate, JSONParser parser, long now) throws ParseException {
        // Get accessCode
        // Construct request body for accessToken request
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = "{\"authCode\": \"" + authCode + "\", \"apiKey\": \"" + System.getenv("apiKey") + "\"}";
        logger.debug("Authorization Request Payload" + jsonBody);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(getAuthorizationApiUrl(), HttpMethod.POST, requestEntity, String.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            String responseBody = responseEntity.getBody();
            JSONObject authorizationRespJson = (JSONObject) parser.parse(responseBody);

            int expiresInMin = ((Long) authorizationRespJson.get("expiresInMin")).intValue();
            accessTokenExpiresAt = now + expiresInMin*60;

            accessToken = (String) authorizationRespJson.get("accessToken");
        }

        return accessToken;
    }

    public String getAccessToken() throws ParseException, RestClientException {
        if (!isReady()) {
            return null;
        }

        long now = Instant.now().getEpochSecond();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        // For YUSA, we need to call Authencate API to get authorization token
        // and use the authorization token to get access token.
        if ("YUSA".equals(recipientSite)) {
            // check if we already have the valid access token.
            if (now < accessTokenExpiresAt-10) {
                // Now expired yet.
                logger.debug("Access Token (not expired): " + accessToken);
                return accessToken;
            }

            JSONParser parser = new JSONParser();

            headers.add("x-client-id", System.getenv("xclientid"));
            headers.add("x-api-sub-key", System.getenv("xapisubkey"));

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // Get authCode
            // if we already valid authCode, then we can access token now.
            if (now < authCodeExpiresAt-10) {
                accessToken = getYUSAAccessToken (headers, restTemplate, parser, now);
            } else {
                ResponseEntity<String> responseEntity = 
                    restTemplate.exchange(getAuthenticationApiUrl(), HttpMethod.GET, requestEntity, String.class);

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    String responseBody = responseEntity.getBody();

                    JSONObject authenticationRespJson = (JSONObject) parser.parse(responseBody);
                    authCode = (String) authenticationRespJson.get("authCode");
                    logger.debug("authCode from Aurhentication call: " + authCode);

                    int expiresInMin = ((Long) authenticationRespJson.get("expiresInMin")).intValue();
                    authCodeExpiresAt = now  + expiresInMin*60;

                    // Get accessCode
                    accessToken = getYUSAAccessToken (headers, restTemplate, parser, now);
                } else {
                    throw new FHIRException("Failed to get access token (" + responseEntity.getStatusCode() + "): " + responseEntity.getBody());
                }
            }    
        }

        logger.debug("Access Token: " + accessToken);
        return accessToken;
    }

    public String getAuthenticationApiUrl () {
        return authenticationApiUrl;
    }

    public void setAuthenticationApiUrl (String authenticationApiUrl) {
        this.authenticationApiUrl = authenticationApiUrl;
    }

    public String getAuthorizationApiUrl () {
        return authorizationApiUrl;
    }

    public void setAuthorizationApiUrl (String authorizationApiUrl) {
        this.authorizationApiUrl = authorizationApiUrl;
    }

    public String getRecipientSite () {
        return this.recipientSite;
    }

    public void setRecipientSite (String recipientSite) {
        this.recipientSite = recipientSite;
    }

    public String submitYusaRR(String targetUrl, String messageBundleJson) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.add("x-client-id", System.getenv("xclientid"));
        headers.add("x-api-sub-key", System.getenv("xapisubkey"));
        headers.setContentType(MediaType.APPLICATION_JSON);

        String myAccessToken = null;
        try {
            myAccessToken = getAccessToken();
        } catch (ParseException e) {
            e.printStackTrace();
            throw new FHIRException("Failed to parse the access token from response");
        }

        if (myAccessToken != null && !myAccessToken.isBlank()) {
            // headers.add("Bearer", myAccessToken);
            headers.setBearerAuth(myAccessToken);
        } else {
            logger.error("Failed to get access token.");
            throw new FHIRException("Failed to getAccessToken.");
        }

        logger.debug("Sending to YUSA (" + targetUrl + ") with AccessToken: " + myAccessToken);

        HttpEntity<String> requestEntity = null;
        ResponseEntity<String> responseEntity = null;
        try {
            requestEntity = new HttpEntity<>(messageBundleJson, headers);
            responseEntity = restTemplate.exchange(targetUrl, HttpMethod.POST, requestEntity, String.class);
        } catch (Exception e) {
            logger.error("Submission to YUSA falied with " + e.getMessage());
            return "FAILED: with an exception - " + e.getMessage();
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new FHIRException("Failed to submit Referral Request - , " + responseEntity.getStatusCode());
        }

        String retString;
        if (HttpStatus.ACCEPTED == responseEntity.getStatusCode()) {
            retString = "ACCEPTED: ";
        } else {
            retString = "COMPLETED: ";
        }
        return retString + "(" + responseEntity.getStatusCode() + ")" + responseEntity.getBody()==null?"":responseEntity.getBody();
    }
} 