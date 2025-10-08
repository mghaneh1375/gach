package irysc.gachesefid.adaptor.crop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CropAdaptorResponse<T> {
    private T result;
    private AdaptorResponse response;
}
