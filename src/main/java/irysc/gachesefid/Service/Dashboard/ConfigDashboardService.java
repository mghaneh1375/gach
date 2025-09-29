package irysc.gachesefid.Service.Dashboard;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOptions;
import irysc.gachesefid.Dto.Dashboard.Advisor.AdvisorDashboardConfig;
import irysc.gachesefid.Dto.ResponseDto;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.configDashboardRepository;

@Service
public class ConfigDashboardService {
    private final static ObjectMapper mapper = new ObjectMapper();
    private final static ObjectMapper simpleMapper = new ObjectMapper();

    static {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        simpleMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void setAdvisorConfig(ObjectId userId, AdvisorDashboardConfig configDto) {
        Document configDoc = configDashboardRepository.findBySecKey(userId);
        AdvisorDashboardConfig advisorDashboardConfig;
        if(configDoc == null)
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

        advisorDashboardConfig.setShowLastTickets(configDto.getShowLastTickets());
        advisorDashboardConfig.setShowLastNotifs(configDto.getShowLastNotifs());
        advisorDashboardConfig.setShowDashboard(configDto.getShowDashboard());

        Document doc = new Document(mapper.convertValue(advisorDashboardConfig, Document.class));
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

    public ResponseEntity<ResponseDto<AdvisorDashboardConfig>> getConfig(ObjectId userId) {
        Document config = configDashboardRepository.findBySecKey(userId);
        return new ResponseEntity<>(
                ResponseDto.builder(AdvisorDashboardConfig.class)
                        .data(config == null
                                ? new AdvisorDashboardConfig()
                                : simpleMapper.convertValue(config, AdvisorDashboardConfig.class))
                        .build(),
                HttpStatus.OK
        );
    }

}
