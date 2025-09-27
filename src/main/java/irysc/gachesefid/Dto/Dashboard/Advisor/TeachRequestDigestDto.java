package irysc.gachesefid.Dto.Dashboard.Advisor;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Dto.UserDigest;
import irysc.gachesefid.Models.TeachMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeachRequestDigestDto {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private String title;
    private TeachMode teachMode;
    private Integer price;
    private Integer length;
    private Integer minCap;
    private Integer maxCap;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long startAt;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long startDate;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long endDate;
    @JsonSerialize(using = LongDateSerialization.class)
    private Long endRegistration;
    private List<UserDigest> newRequesters;
    private Integer studentsCount;
}
