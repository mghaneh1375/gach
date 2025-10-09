package irysc.gachesefid.Dto.advice;

import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TopAdvisors extends UserDigest {
    private Integer studentsCount;
}
