package irysc.gachesefid.Dto.Deserializer;

import irysc.gachesefid.Models.TeachReportTagMode;

public class TeachReportTagModeDeserializer extends LowerCaseEnumDeserializer<TeachReportTagMode> {
    public TeachReportTagModeDeserializer() {
        super(TeachReportTagMode.class);
    }
}