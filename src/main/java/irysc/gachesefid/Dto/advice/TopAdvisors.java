package irysc.gachesefid.Dto.advice;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TopAdvisors extends AdvisorDigestInfoDto {}
