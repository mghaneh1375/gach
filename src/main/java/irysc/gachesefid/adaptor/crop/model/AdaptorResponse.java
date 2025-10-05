package irysc.gachesefid.adaptor.crop.model;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class AdaptorResponse {
    private final HttpStatus status;
    private final String error;
}
