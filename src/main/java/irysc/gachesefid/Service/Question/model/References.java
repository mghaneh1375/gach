package irysc.gachesefid.Service.Question.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class References {
    private Map<String, ObjectId> subjects;
    private Map<String, String> authors;
    private Map<Integer, String> tagsByCode;
    private List<String> tagsByKey;
    private List<String> duplicateOrganizationIds;
}
