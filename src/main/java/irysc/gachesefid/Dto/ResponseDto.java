package irysc.gachesefid.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResponseDto<T> {
    @Builder.Default
    private String status = "ok";
    private T data;
    public static <T> ResponseDtoBuilder<T> builder(Class<T> type) {
        return new ResponseDtoBuilder<T>();
    }
    public static <T> ResponseDtoBuilder<List<T>> builderList(Class<T> type) {
        return new ResponseDtoBuilder<List<T>>();
    }
}
