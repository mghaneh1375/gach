package irysc.gachesefid.Dto.advice;

import irysc.gachesefid.Models.TeachReportTagMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class CreateAdviceTagReportDto {
    @NotNull
    @Size(min = 2, max = 255, message = "عنوان باید بین 2 تا 255 کاراکتر باشد")
    private String label;
    @NotNull
    private TeachReportTagMode mode;
    @NotNull
    @Min(value = 1, message = "اولویت باید حداقل 1 باشد")
    @Max(value = 100, message = "اولویت باید حداکثر 100 باشد")
    private Integer priority;
    @NotNull
    private Boolean visibility;
}
