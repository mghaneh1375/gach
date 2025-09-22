package irysc.gachesefid.Dto.DBMeta.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Grade {
    private ObjectId id; // _id
    private String name;
}
