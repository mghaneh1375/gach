package irysc.gachesefid.Dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDigest {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private String firstname;
    private String lastname;
    private String nid;
    private String phone;
}
