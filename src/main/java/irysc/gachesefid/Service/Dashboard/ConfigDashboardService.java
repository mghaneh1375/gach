package irysc.gachesefid.Service.Dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.UpdateOptions;
import irysc.gachesefid.Dto.Dashboard.Advisor.AdvisorDashboardConfig;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.configDashboardRepository;

@Service
public class ConfigDashboardService {
    private final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public void setAdvisorConfig(ObjectId userId, AdvisorDashboardConfig configDto) {
        Document configDoc = configDashboardRepository.findBySecKey(userId);
        AdvisorDashboardConfig advisorDashboardConfig;
        if(configDoc == null)
            advisorDashboardConfig = AdvisorDashboardConfig
                    .builder()
                    .userId(userId)
                    .build();
        else {
            ObjectMapper mapper = new ObjectMapper();
            advisorDashboardConfig = mapper.convertValue(configDoc, AdvisorDashboardConfig.class);
        }

        advisorDashboardConfig.setShowFilledKarbargs(configDto.getShowFilledKarbargs());
        advisorDashboardConfig.setShowDashboard(configDto.getShowDashboard());
        advisorDashboardConfig.setShowLastSettleRequest(configDto.getShowLastSettleRequest());
        advisorDashboardConfig.setShowInProgressKarbargs(configDto.getShowInProgressKarbargs());
        advisorDashboardConfig.setShowIncomingRequestsForTeach(configDto.getShowIncomingRequestsForTeach());
        advisorDashboardConfig.setShowIncomingRequestsForAdvice(configDto.getShowIncomingRequestsForAdvice());
        advisorDashboardConfig.setShowLastTickets(configDto.getShowLastTickets());

        Document doc = new Document(mapper.convertValue(advisorDashboardConfig, Document.class));
        configDashboardRepository.updateOne(
                eq("user_id", userId),
                new BasicDBObject("$set", doc),
                new UpdateOptions().upsert(true)
        );
    }

}
