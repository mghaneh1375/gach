package irysc.gachesefid.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseDto {
    @Builder.Default
    private String status = "ok";
    private Object data;
}
