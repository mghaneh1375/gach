package irysc.gachesefid.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Dto.Serializer.PicSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserDigest {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private String firstname;
    private String lastname;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nid;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phone;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String mail;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(using = PicSerializer.class)
    private String pic;
}
