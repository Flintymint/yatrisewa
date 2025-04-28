package com.example.travel_yatra.travel_yatra.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

@Service
public class KhaltiService {
    private final String khaltiSecretKey;

    public KhaltiService() {
        this.khaltiSecretKey = System.getenv().getOrDefault("KHALTI_SECRET_KEY", "test_secret_key");
    }

    public static class KhaltiVerifyResult {
        public boolean success;
        public String idx;
        public String rawResponse;
        public KhaltiVerifyResult(boolean success, String idx, String rawResponse) {
            this.success = success;
            this.idx = idx;
            this.rawResponse = rawResponse;
        }
    }

    public KhaltiVerifyResult verifyPaymentAndGetDetails(String token, int amount) {
        String url = "https://dev.khalti.com/api/v2/payment/verify/";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Key " + khaltiSecretKey);
        Map<String, Object> body = new HashMap<>();
        body.put("token", token);
        body.put("amount", amount);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Try to extract idx from response JSON
                String idx = null;
                String resp = response.getBody();
                if (resp != null) {
                    int idxIdx = resp.indexOf("\"idx\":");
                    if (idxIdx != -1) {
                        int start = resp.indexOf('"', idxIdx + 6) + 1;
                        int end = resp.indexOf('"', start);
                        idx = resp.substring(start, end);
                    }
                }
                return new KhaltiVerifyResult(true, idx, resp);
            } else {
                return new KhaltiVerifyResult(false, null, response.getBody());
            }
        } catch (Exception e) {
            return new KhaltiVerifyResult(false, null, e.getMessage());
        }
    }

    // Initiate payment (generate pidx)
    @SuppressWarnings("unchecked")
    public Map<String, Object> initiatePayment(Object req) {
        String url = "https://dev.khalti.com/api/v2/epayment/initiate/";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Key " + khaltiSecretKey);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> reqMap = (java.util.Map<String, Object>) mapper.convertValue(req, java.util.Map.class);
            // Always set website_url and return_url to the required values
            reqMap.put("website_url", "https://khaltipay.com");
            reqMap.put("return_url", "https://khaltipay.com/payment-callback");
            String jsonBody = mapper.writeValueAsString(reqMap);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("error", response.getBody());
                return error;
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    // Lookup payment status (after payment)
    public Map<String, Object> lookupPayment(String pidx) {
        String url = "https://dev.khalti.com/api/v2/epayment/lookup/";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Key " + khaltiSecretKey);
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", pidx);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("error", response.getBody());
                return error;
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
}
