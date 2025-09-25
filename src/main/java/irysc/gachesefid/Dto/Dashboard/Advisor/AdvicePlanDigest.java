package irysc.gachesefid.Dto.Dashboard.Advisor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdvicePlanDigest {
    private String title;
    private Integer price;
}
