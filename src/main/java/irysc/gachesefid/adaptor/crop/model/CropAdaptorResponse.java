package irysc.gachesefid.adaptor.crop.model;

import lombok.Data;

@Data
public class CropAdaptorResponse<T> {
    private final T result;
    private final AdaptorResponse response;
}
