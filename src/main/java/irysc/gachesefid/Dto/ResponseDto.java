package irysc.gachesefid.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseDto<T> {
    @Builder.Default
    private String status = "ok";
    private T data;
    public static <T> ResponseDtoBuilder<T> builder(Class<T> type) {
        return new ResponseDtoBuilder<T>();
    }
}
