import com.fasterxml.jackson.databind.ObjectMapper;
import com.std.projects.api.exception.RestTemplateResponseErrorHandler;
import com.std.projects.api.untils.ApiMapper;
import com.std.projects.api.vo.response.ResponseJsonMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.persistence.MappedSuperclass;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;

/**
 * @author Vannaravuth YO
 * @since 24-May-22, 3:44 PM
 */

@Getter
@Setter
@Service
@MappedSuperclass
@NoArgsConstructor
public abstract class ApiService {
    private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    @Autowired
    private RestTemplateBuilder templateBuilder;
    @Autowired
    private RestTemplate restTemplate;
    private String accessTokenUrl;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;

    public ApiService(String accessTokenUrl, String clientId, String clientSecret, String username, String password) {
        this.accessTokenUrl = accessTokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
    }

    public static void turnOffSslChecking() {
        try {
            final SSLContext sslContext;
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, UNQUESTIONING_TRUST_MANAGER, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static void turnOnSslChecking() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext.getInstance("SSL").init(null, null, null);
    }

    public static HttpHeaders getDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static HttpHeaders getBearerHeaders(String accessToken) {
        HttpHeaders headers = getDefaultHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    public String requestTokenByOAuth2RestTemplate() {
        ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();
        details.setClientAuthenticationScheme(AuthenticationScheme.header);
        details.setAccessTokenUri(accessTokenUrl);
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        details.setUsername(username);
        details.setPassword(password);

        OAuth2RestTemplate oauthTemplate = new OAuth2RestTemplate(details);
        OAuth2RestTemplate configure = templateBuilder
                // Can handle excception
                // .errorHandler(new RestTemplateResponseErrorHandler())
                .configure(oauthTemplate);

        return configure.getAccessToken().getValue();
    }

    public String requestOAuthTokenByRestTemplate() {
        HttpHeaders headers = getDefaultHeaders();
        // Can combine with basic auth
        // headers.setBasicAuth(clientId, clientSecret);

        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("username", username);
        map.add("password", password);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(accessTokenUrl,
                HttpMethod.POST, requestEntity, String.class);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(responseEntity.getBody()).path("access_token").asText();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String postJson(String url, JSONObject jsonBody, String accessToken) {
        HttpHeaders headers = getBearerHeaders(accessToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody.toString(), headers);
        return restTemplate.postForObject(url, requestEntity, String.class);
    }

    /**
     * Request to any REST API via <b>RestTemplate</b> using <b>exchange</b> method
     *
     * @param url           Full rest api end point
     * @param method        Http request method
     * @param requestEntity Http request object
     * @return ResponseEntity<ResponseJsonMap> response
     */
    public ResponseEntity<ResponseJsonMap> request(String url, HttpMethod method, HttpEntity<?> requestEntity) {
        return restTemplate.exchange(url, method, requestEntity, ResponseJsonMap.ParameterizedTypeReference);
    }

    /**
     * Using com.std.projects.api.service.ApiService#request(java.lang.String, org.springframework.http.HttpMethod, org.springframework.http.HttpEntity)
     * for request to JSON REST API <b>RestTemplate</b>
     *
     * @param method     Http request method
     * @param url        Full rest api end point
     * @param jsonBody   Request Body with json
     * @param queryParam Query parameter
     * @return ResponseEntity<ResponseJsonMap> response with json format
     */
    public ResponseEntity<ResponseJsonMap> requestJSON(HttpMethod method, String url,
                                                       Map<String, Object> jsonBody, Map<String, Object> queryParam) {
        String accessToken = requestTokenByOAuth2RestTemplate();
        HttpHeaders headers = getBearerHeaders(accessToken);

        String uriString = queryParam != null ? ApiMapper.toURIString(url, queryParam) : url;
        String jsonString = jsonBody != null ? ApiMapper.toJSONString(jsonBody) : null;
        HttpEntity<?> requestEntity = new HttpEntity<>(jsonString, headers);

        return request(uriString, method, requestEntity);
    }

    /**
     * Alias com.std.projects.api.service.ApiService#request(java.lang.String, org.springframework.http.HttpMethod, org.springframework.http.HttpEntity)
     * with Http GET method for more clean and short code and lazy developer
     *
     * @param url           Full rest api end point
     * @param requestEntity Http request object
     * @return ResponseEntity<ResponseJsonMap> response
     */
    public ResponseEntity<ResponseJsonMap> getHttp(String url, HttpEntity<?> requestEntity) {
        return request(url, HttpMethod.GET, requestEntity);
    }

    /**
     * Alias com.std.projects.api.service.ApiService#request(java.lang.String, org.springframework.http.HttpMethod, org.springframework.http.HttpEntity)
     * with Http POST method for more clean and short code and lazy developer
     *
     * @param url           Full rest api end point
     * @param requestEntity Http request object
     * @return ResponseEntity<ResponseJsonMap> response
     */
    public ResponseEntity<ResponseJsonMap> postHttp(String url, HttpEntity<?> requestEntity) {
        return request(url, HttpMethod.POST, requestEntity);
    }

    /**
     * Alias com.std.projects.api.service.ApiService#requestJSON(org.springframework.http.HttpMethod, java.lang.String, java.util.Map, java.util.Map)
     * with Http GET method for more clean and short code and lazy developer
     *
     * @param url        Full rest api end point
     * @param jsonBody   Request Body with json
     * @param queryParam Query parameter
     * @return ResponseEntity<ResponseJsonMap> response
     */
    public ResponseEntity<ResponseJsonMap> getJSON(String url, Map<String, Object> jsonBody, Map<String, Object> queryParam) {
        return requestJSON(HttpMethod.GET, url, jsonBody, queryParam);
    }

    /**
     * Alias com.std.projects.api.service.ApiService#requestJSON(org.springframework.http.HttpMethod, java.lang.String, java.util.Map, java.util.Map)
     * with Http POST method for more clean and short code and lazy developer
     *
     * @param url        Full rest api end point
     * @param jsonBody   Request Body with json
     * @param queryParam Query parameter
     * @return ResponseEntity<ResponseJsonMap> response
     */
    public ResponseEntity<ResponseJsonMap> postJSON(String url, Map<String, Object> jsonBody, Map<String, Object> queryParam) {
        return requestJSON(HttpMethod.POST, url, jsonBody, queryParam);
    }

    /**
     * Filter body's content from response (ResponseEntity<ResponseJsonMap>) if exist
     *
     * @param response ResponseEntity<ResponseJsonMap> from RestTemplate
     * @return Map<String, Map < String, Object>> response body's content
     */
    public Map<String, Map<String, Object>> getResponseContent(ResponseEntity<ResponseJsonMap> response) {
        if (response != null) {
            ResponseJsonMap body = response.getBody();
            if (body != null) {
                return body.getContent();
            }
        }
        return null;
    }

    /**
     * Request to REST API with <b>GET</b> method via RestTemplate
     *
     * @param url        Full rest api end point
     * @param jsonBody   Request Body with json
     * @param queryParam Query parameter
     * @return Map<String, Map < String, Object>> response body's content instead of response (ResponseEntity<ResponseJsonMap>)
     */
    public Map<String, Map<String, Object>> getJsonContent(String url, Map<String, Object> jsonBody, Map<String, Object> queryParam) {
        ResponseEntity<ResponseJsonMap> response = getJSON(url, jsonBody, queryParam);
        return getResponseContent(response);
    }

    /**
     * Request to REST API with <b>POST</b> method via RestTemplate
     *
     * @param url        Full rest api end point
     * @param jsonBody   Request Body with json
     * @param queryParam Query parameter
     * @return Map<String, Map < String, Object>> response body's content instead of response (ResponseEntity<ResponseJsonMap>)
     */
    public Map<String, Map<String, Object>> postJsonContent(String url, Map<String, Object> jsonBody, Map<String, Object> queryParam) {
        ResponseEntity<ResponseJsonMap> response = postJSON(url, jsonBody, queryParam);
        return getResponseContent(response);
    }


}
