package irysc.gachesefid.adaptor.crop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdaptorResponse {
    private HttpStatus status;
    private String error;
}
