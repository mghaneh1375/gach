package irysc.gachesefid.Dto.Dashboard.Student;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import irysc.gachesefid.Dto.Deserializer.MongoNumberLongDeserializer;
import irysc.gachesefid.Dto.Serializer.ContentPicSerializer;
import irysc.gachesefid.Dto.Serializer.LongDateSerialization;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedContentDto {
    @JsonSerialize(using = ObjectIdSerializer.class)
    private ObjectId id;
    private String title;
    private String slug;
    private List<String> tags;
    private String level;
    private Integer sessionsCount;
    private Integer duration;
    private List<String> teachers;
    private Integer price;
    private Integer rate;
    private Integer buyersCount;
    @JsonSerialize(using = ContentPicSerializer.class)
    private String img;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = OffValueFilter.class)
    private Off off;

    public static class Off {
        private String type;
        private Integer amount;
        @JsonIgnore
        @JsonDeserialize(using = MongoNumberLongDeserializer.class)
        private Long start;
        @JsonIgnore
        @JsonDeserialize(using = MongoNumberLongDeserializer.class)
        private Long expiration;

        public Long getStart() {
            return start;
        }

        public void setStart(Long start) {
            this.start = start;
        }

        public Long getExpiration() {
            return expiration;
        }

        public void setExpiration(Long expiration) {
            this.expiration = expiration;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getAmount() {
            return amount;
        }

        public void setAmount(Integer amount) {
            this.amount = amount;
        }
    }
}