package irysc.gachesefid.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Dto.Serializer.PicSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;

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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String school;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> branches;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String grade;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String city;

    public static UserDigest buildFromDoc(Document doc) {
        return UserDigest
                .builder()
                .id(doc.getObjectId("_id"))
                .firstname(doc.getString("first_name"))
                .lastname(doc.getString("last_name"))
                .phone(doc.containsKey("phone")
                        ? doc.getString("phone")
                        : null
                )
                .mail(doc.containsKey("mail")
                        ? doc.getString("mail")
                        : null
                )
                .nid(doc.containsKey("NID")
                        ? doc.getString("NID")
                        : null
                )
                .pic(doc.containsKey("pic")
                        ? doc.getString("pic")
                        : null
                )
                .school(doc.containsKey("school")
                        ? doc.get("school", Document.class).getString("name")
                        : null
                )
                .grade(doc.containsKey("grade")
                        ? doc.get("grade", Document.class).getString("name")
                        : null
                )
                .branches(doc.containsKey("branches")
                        ? doc.getList("branches", Document.class).stream().map(document -> document.getString("name")).collect(Collectors.toList())
                        : null
                )
                .city(
                        doc.containsKey("city")
                                ? doc.get("city", Document.class).getString("name")
                                : null
                )
                .build();
    }

}
