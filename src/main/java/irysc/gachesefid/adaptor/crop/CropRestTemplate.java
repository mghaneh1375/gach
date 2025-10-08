package irysc.gachesefid.adaptor.crop;

import irysc.gachesefid.adaptor.crop.model.AdaptorResponse;
import irysc.gachesefid.adaptor.crop.model.CropAdaptorResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
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


    public <T> T postForEntity(
            String url, Object requestBody,
            ParameterizedTypeReference<T> typeRef
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        String authStr = String.format("%s:%s", clientId, clientSecret);
        String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
        headers.add("Authorization", "Basic " + base64Creds);

        String path = serviceBaseUrl + url;
        try {
            return cropRestTemplate.exchange(
                    path, HttpMethod.POST, new HttpEntity<>(requestBody, headers),
                    typeRef
            ).getBody();
        } catch (Exception e) {
            String error = e.getMessage();
            if (e instanceof HttpStatusCodeException) {
                if (((HttpStatusCodeException) e).getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                    error = "Unauthorized";
                }
                try {
                    error = ((HttpStatusCodeException) e).getResponseBodyAsString();
                } catch (Exception ignore) {
                }

//                return new CropAdaptorResponse<>(null, new AdaptorResponse(
//                        HttpStatus.valueOf(((HttpStatusCodeException) e).getStatusCode().value()),
//                        error
//                ));
                return null;
            }
            if (e.getCause() instanceof SocketTimeoutException) {
                return null;
//                return new CropAdaptorResponse<>(
//                        null,
//                        new AdaptorResponse(
//                                HttpStatus.BAD_REQUEST,
//                                "timeout"
//                        )
//                );
            }

            throw e;
        }
    }
}
