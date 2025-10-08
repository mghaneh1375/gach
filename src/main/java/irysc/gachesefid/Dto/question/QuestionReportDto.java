package irysc.gachesefid.Dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionReportDto {
    private String label;
    private Integer reportsCount;
    private String question;
    private Integer unseenReportsCount;
}
