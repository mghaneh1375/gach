package irysc.gachesefid.adaptor.crop;

import irysc.gachesefid.adaptor.crop.model.AdaptorResponse;
import irysc.gachesefid.adaptor.crop.model.CropAdaptorResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.Collections;

@Component
public class CropRestTemplate {

    private final RestTemplate cropRestTemplate;
    private final String serviceBaseUrl;
    private final String clientId;
    private final String clientSecret;

    public CropRestTemplate(
            @Qualifier("cropRT") RestTemplate cropRestTemplate,
            @Value("${crop.base.url}") String serviceBaseUrl,
            @Value("${crop.clientId}") String clientId,
            @Value("${crop.secret}") String clientSecret
    ) {
        this.cropRestTemplate = cropRestTemplate;
        this.serviceBaseUrl = serviceBaseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }


    public CropAdaptorResponse postForEntity(String url, Object requestBody, Class resultType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        String authStr = String.format("%s:%s", clientId, clientSecret);
        String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
        headers.add("Authorization", "Basic " + base64Creds);

        String path = serviceBaseUrl + "/" + url;
        try {
            return new CropAdaptorResponse(cropRestTemplate.postForEntity(path, new HttpEntity<>(requestBody, headers), resultType).getBody(), new AdaptorResponse(HttpStatus.OK, null));
        }
        catch (Exception e) {
            String error = e.getMessage();
            if(e instanceof HttpStatusCodeException) {
                if (((HttpStatusCodeException)e).getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                    error = "Unauthorized";
                }
                try {
                    error = ((HttpStatusCodeException)e).getResponseBodyAsString();
                } catch (Exception ignore) {}

                return new CropAdaptorResponse<>(null, new AdaptorResponse(
                        HttpStatus.valueOf(((HttpStatusCodeException)e).getStatusCode().value()),
                        error
                ));
            }
            if(e.getCause() instanceof SocketTimeoutException) {
                return new CropAdaptorResponse<>(
                        null,
                        new AdaptorResponse(
                                HttpStatus.BAD_REQUEST,
                                "timeout"
                        )
                );
            }

            throw e;
        }
    }
}
