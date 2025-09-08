package irysc.gachesefid.Dto.Report.RequestsReport;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.UserDigest;
import irysc.gachesefid.Enums.AdviceRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvisorRequestDto {
    private UserDigest student;
    private String advisor;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long createdAt;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long answerAt;
    private Integer price;
    private Integer videoCalls;
    private Integer maxExam;
    private Integer maxKarbarg;
    private Integer maxChat;
    private Integer paid;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long activeAt;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long settledAt;
    private AdviceRequestStatus status;
}
