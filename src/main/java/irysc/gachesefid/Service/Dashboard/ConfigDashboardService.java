package irysc.gachesefid.Service.Dashboard;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOptions;
import irysc.gachesefid.Dto.Dashboard.Admin.AdminDashboardConfig;
import irysc.gachesefid.Dto.Dashboard.Advisor.AdvisorDashboardConfig;
import irysc.gachesefid.Dto.Dashboard.ConfigDto;
import irysc.gachesefid.Dto.Dashboard.Student.StudentDashboardConfig;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Service.MyService;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.configDashboardRepository;

@Service
public class ConfigDashboardService extends MyService {

    public void setAdminConfig(ObjectId userId, AdminDashboardConfig configDto) {
        Document configDoc = configDashboardRepository.findBySecKey(userId);
        AdminDashboardConfig adminDashboardConfig;
        if (configDoc == null)
            adminDashboardConfig = AdminDashboardConfig
                    .builder()
                    .userId(userId)
                    .build();
        else
            adminDashboardConfig = mapper.convertValue(configDoc, AdminDashboardConfig.class);

        adminDashboardConfig.setShowLastSettleRequest(configDto.getShowLastSettleRequest());
        adminDashboardConfig.setShowIncomingRequestsForTeach(configDto.getShowIncomingRequestsForTeach());
        adminDashboardConfig.setShowIncomingRequestsForAdvice(configDto.getShowIncomingRequestsForAdvice());
        adminDashboardConfig.setShowMeetings(configDto.getShowMeetings());
        adminDashboardConfig.setShowTopAdvisors(configDto.getShowTopAdvisors());
        adminDashboardConfig.setShowTopTeachers(configDto.getShowTopTeachers());
        adminDashboardConfig.setShowTopLastWeekBestSellerContents(configDto.getShowTopLastWeekBestSellerContents());

        adminDashboardConfig.setShowLastTickets(configDto.getShowLastTickets());
        adminDashboardConfig.setShowLastNotifs(configDto.getShowLastNotifs());
        adminDashboardConfig.setShowDashboard(configDto.getShowDashboard());

        save(configDoc, userId, adminDashboardConfig);
    }

    public void setAdvisorConfig(ObjectId userId, AdvisorDashboardConfig configDto) {
        Document configDoc = configDashboardRepository.findBySecKey(userId);
        AdvisorDashboardConfig advisorDashboardConfig;
        if (configDoc == null)
            advisorDashboardConfig = AdvisorDashboardConfig
                    .builder()
                    .userId(userId)
                    .build();
        else
            advisorDashboardConfig = mapper.convertValue(configDoc, AdvisorDashboardConfig.class);

        advisorDashboardConfig.setShowFilledKarbargs(configDto.getShowFilledKarbargs());
        advisorDashboardConfig.setShowMyLastComments(configDto.getShowMyLastComments());
        advisorDashboardConfig.setShowLastSettleRequest(configDto.getShowLastSettleRequest());
        advisorDashboardConfig.setShowInProgressKarbargs(configDto.getShowInProgressKarbargs());
        advisorDashboardConfig.setShowIncomingRequestsForTeach(configDto.getShowIncomingRequestsForTeach());
        advisorDashboardConfig.setShowIncomingRequestsForAdvice(configDto.getShowIncomingRequestsForAdvice());
        advisorDashboardConfig.setShowMeeting(configDto.getShowMeeting());
        advisorDashboardConfig.setShowMyCurrStudents(configDto.getShowMyCurrStudents());
        advisorDashboardConfig.setShowMyFutureTeaches(configDto.getShowMyFutureTeaches());

        advisorDashboardConfig.setShowLastTickets(configDto.getShowLastTickets());
        advisorDashboardConfig.setShowLastNotifs(configDto.getShowLastNotifs());
        advisorDashboardConfig.setShowDashboard(configDto.getShowDashboard());

        save(configDoc, userId, advisorDashboardConfig);
    }

    public void setStudentConfig(ObjectId userId, StudentDashboardConfig configDto) {
        Document configDoc = configDashboardRepository.findBySecKey(userId);
        StudentDashboardConfig studentDashboardConfig;
        if (configDoc == null)
            studentDashboardConfig = StudentDashboardConfig
                    .builder()
                    .userId(userId)
                    .build();
        else
            studentDashboardConfig = mapper.convertValue(configDoc, StudentDashboardConfig.class);

        studentDashboardConfig.setShowCurrentKarbargs(configDto.getShowCurrentKarbargs());
        studentDashboardConfig.setShowMeeting(configDto.getShowMeeting());
        studentDashboardConfig.setShowRequestsStatusForAdvice(configDto.getShowRequestsStatusForAdvice());
        studentDashboardConfig.setShowRequestsStatusForTeach(configDto.getShowRequestsStatusForTeach());
        studentDashboardConfig.setShowFutureQuiz(configDto.getShowFutureQuiz());
        studentDashboardConfig.setShowMyAdvisor(configDto.getShowMyAdvisor());
        studentDashboardConfig.setShowSuggestionForQuiz(configDto.getShowSuggestionForQuiz());
        studentDashboardConfig.setShowSuggestionForContent(configDto.getShowSuggestionForContent());

        studentDashboardConfig.setShowLastTickets(configDto.getShowLastTickets());
        studentDashboardConfig.setShowLastNotifs(configDto.getShowLastNotifs());
        studentDashboardConfig.setShowDashboard(configDto.getShowDashboard());

        save(configDoc, userId, studentDashboardConfig);
    }

    private void save(Document configDoc, ObjectId userId, ConfigDto configDto) {
        Document doc = new Document(mapper.convertValue(configDto, Document.class));
        doc.remove("id");
        doc.put("_id", configDoc == null ? new ObjectId() : configDoc.getObjectId("_id"));
        doc.put("user_id", userId);
        configDashboardRepository.updateOne(
                eq("user_id", userId),
                new BasicDBObject("$set", doc),
                new UpdateOptions().upsert(true)
        );
        configDashboardRepository.clearFromCache(userId);
    }

    public <T extends ConfigDto> ResponseEntity getConfig(
            ObjectId userId, Class clazz
    ) {
        Document config = configDashboardRepository.findBySecKey(userId);
        return new ResponseEntity<>(
                ResponseDto.builder(clazz)
                        .data(config == null
                                ? clazz.getName().equals(AdvisorDashboardConfig.class.getName())
                                ? new AdvisorDashboardConfig()
                                : clazz.getName().equals(AdminDashboardConfig.class.getName())
                                ? new AdminDashboardConfig()
                                : new StudentDashboardConfig()
                                : simpleMapper.convertValue(config, clazz))
                        .build(),
                HttpStatus.OK
        );
    }
}
