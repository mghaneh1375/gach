package irysc.gachesefid.Dto.Report.BuyReport;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Deserializer.MongoNumberLongDeserializer;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Dto.UserDigest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuyerInfoDto {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId refId;
    private String title;
    private Integer paid;
    @JsonSerialize(using = LongDateSerialization.class)
    @JsonDeserialize(using = MongoNumberLongDeserializer.class)
    private Long registeredAt;
    private UserDigest user;
}
