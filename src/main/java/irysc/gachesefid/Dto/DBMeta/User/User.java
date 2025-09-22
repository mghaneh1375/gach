package irysc.gachesefid.Dto.DBMeta.User;

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
public class User {
    private ObjectId id; // _id
    private Double coin;
    private Double money;
    private String firstname; // first_name
    private String lastname; // last_name
    private String pic;
    private String NID;
    private String invitationCode; // optional - invitation_code
    private City city; // optional
    private State state; // optional
    private School school; // optional
    private Grade grade; // optional
    private List<Branch> branches; // optional
    private String sex;
    private String phone; // optional
    private String mail; // optional
    private Boolean teach; // optional - if be a teacher
    private String teachBio; // optional - teach_bio
    private String teachVideoLink; // optional - teach_video_link
    private Integer defaultTeachPrice; // optional - default_teach_price
    private Boolean advice; // optional - if be an advisor
    private String adviceBio; // optional - advice_bio
    private String adviceVideoLink; // optional - advice_video_link
    private Boolean acceptStd; // optional - accept_std

}
