package irysc.gachesefid.Dto.DBMeta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    private String firstname; // first_name
    private String lastname; // last_name
    private String sex;
    private String phone; // optional
    private String mail; // optional
    private Boolean teach; // optional - if be a teacher
    private String teachBio; // optional - teach_bio
    private String teachVideoLink; // optional - teach_video_link
    private Boolean advice; // optional - if be an advisor
    private String adviceBio; // optional - advice_bio
    private String adviceVideoLink; // optional - advice_video_link
    private Boolean acceptStd; // optional - accept_std

}
