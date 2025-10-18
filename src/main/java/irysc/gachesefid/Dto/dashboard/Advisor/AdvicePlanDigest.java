package irysc.gachesefid.Dto.dashboard.Advisor;

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
