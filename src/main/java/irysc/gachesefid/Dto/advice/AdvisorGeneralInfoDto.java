package irysc.gachesefid.Dto.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.CommentDigestDto;
import irysc.gachesefid.Dto.Serializer.UserFormSerializer;
import irysc.gachesefid.Dto.UserDigest;
import irysc.gachesefid.Dto.UserDigestSnake;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AdvisorGeneralInfoDto extends AdvisorDigestInfoDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String adviceBio;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String adviceVideoLink;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> tags;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalStudentsCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer rateCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer meetingCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer reportsCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalSettlements;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalSettledAmount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer schedulesCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<UserDigestSnake> students;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CommentDigestDto> recentComments;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ReportDigestDto> recentReports;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(using = UserFormSerializer.class)
    private List<Document> forms;
}
