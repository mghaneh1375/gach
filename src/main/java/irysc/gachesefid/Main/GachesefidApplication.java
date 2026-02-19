package irysc.gachesefid.Main;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import irysc.gachesefid.Controllers.Jobs;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Dto.Deserializer.ObjectIdDeserializer;
import irysc.gachesefid.Dto.Serializer.ObjectIdSerializer;
import irysc.gachesefid.Models.NewAlert;
import irysc.gachesefid.Service.GeneralCacheService;
import irysc.gachesefid.Service.ReportService;
import org.bson.types.ObjectId;
import org.springdoc.core.SpringDocUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.TimeZone;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Utility.Utility.printException;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@ComponentScan({"irysc.gachesefid.Routes", "irysc.gachesefid.Validator",
        "irysc.gachesefid.Security", "irysc.gachesefid.Service",
        "irysc.gachesefid.Controllers", "irysc.gachesefid.adaptor",
        "irysc.gachesefid.schedule"
})
@EntityScan("irysc.gachesefid.Service")
@Configuration
@EnableScheduling
@EnableCaching
public class GachesefidApplication implements WebMvcConfigurer {

    @Autowired
    private GeneralCacheService generalCacheService;

    @Value("${custom.mongodb.url}")
    private String mongodbUrl;

    public static MongoDatabase mongoDatabase;

    public static AccessRequestRepository accessRequestRepository;
    public static ActivationRepository activationRepository;
    public static AdminNotifRepository adminNotifRepository;
    public static AdviceReportRepository adviceReportRepository;
    public static AdviseExamTagRepository adviseExamTagRepository;
    public static AdviceTagReportRepository adviceTagReportRepository;
    public static AdvisorFinanceOfferRepository advisorFinanceOfferRepository;
    public static AdvisorMeetingRepository advisorMeetingRepository;
    public static AdviseTagRepository adviseTagRepository;
    public static AdvisorRequestsRepository advisorRequestsRepository;
    public static AuthorRepository authorRepository;
    public static AvatarRepository avatarRepository;
    public static BadgeRepository badgeRepository;
    public static BranchRepository branchRepository;
    public static CertificateRepository certificateRepository;
    public static CityRepository cityRepository;
    public static CustomQuizRepository customQuizRepository;
    public static CoinHistoryRepository coinHistoryRepository;
    public static ConfigDashboardRepository configDashboardRepository;
    public static CourseIntroductionRepository courseIntroductionRepository;
    public static ConfigRepository configRepository;
    public static ContentConfigRepository contentConfigRepository;
    public static ContentRepository contentRepository;
    public static CommentRepository commentRepository;
    public static ContentQuizRepository contentQuizRepository;
    public static CreditRepository creditRepository;
    public static DailyAdvRepository dailyAdvRepository;

    public static EscapeQuizQuestionRepository escapeQuizQuestionRepository;
    public static EscapeQuizRepository escapeQuizRepository;
    public static ExchangeRepository exchangeRepository;

    public static GiftRepository giftRepository;
    public static GradeRepository gradeRepository;
    public static HWRepository hwRepository;
    public static LevelRepository levelRepository;
    public static LifeScheduleRepository lifeScheduleRepository;
    public static LifeStyleTagRepository lifeStyleTagRepository;
    public static OffcodeRepository offcodeRepository;
    public static OnlineStandQuizRepository onlineStandQuizRepository;
    public static OpenQuizRepository openQuizRepository;
    public static PackageRepository packageRepository;
    public static PackageLevelRepository packageLevelRepository;
    public static PayLinkRepository payLinkRepository;
    public static PointRepository pointRepository;
    public static ProfileConfigRepository profileConfigRepository;
    public static IRYSCQuizRepository iryscQuizRepository;
    public static QuestionRepository questionRepository;
    public static QuestionReportRepository questionReportRepository;
    public static QuestionTagRepository questionTagRepository;
    public static RequestRepository requestRepository;
    public static RSSRepository rssRepository;
    public static SchoolQuestionRepository schoolQuestionRepository;
    public static ScheduleRepository scheduleRepository;
    public static SchoolQuizRepository schoolQuizRepository;
    public static SchoolRepository schoolRepository;
    public static SeoRepository seoRepository;
    public static SettlementRequestRepository settlementRequestRepository;
    public static SMSQueueRepository smsQueueRepository;
    public static StateRepository stateRepository;
    public static SubjectRepository subjectRepository;
    public static TarazRepository tarazRepository;
    public static TeachScheduleRepository teachScheduleRepository;
    public static TeachReportRepository teachReportRepository;
    public static TeachRateRepository teachRateRepository;
    public static StdRateRepository stdRateRepository;
    public static TeachTagReportRepository teachTagReportRepository;
    public static TeacherBioRepository teacherBioRepository;
    public static TicketRepository ticketRepository;
    public static TransactionRepository transactionRepository;
    public static UserGiftRepository userGiftRepository;

    public static UserRepository userRepository;
    public static UserBadgeRepository userBadgeRepository;
    public static UserLevelRepository userLevelRepository;
    public static UserPointRepository userPointRepository;
    public static MailRepository mailRepository;
    public static MailQueueRepository mailQueueRepository;
    public static MissedChunkRepository missedChunkRepository;
    public static NotifRepository notifRepository;

    public static HashMap<String, Integer> newThingsCache = new HashMap<>();

    private static void setupDB(ConnectionString connectionString) {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .retryWrites(true)
                    .build();
            MongoClient mongoClient = MongoClients.create(settings);
            mongoDatabase = mongoClient.getDatabase("gachesefid");

            accessRequestRepository = new AccessRequestRepository();
            activationRepository = new ActivationRepository();
            advisorFinanceOfferRepository = new AdvisorFinanceOfferRepository();
            adminNotifRepository = new AdminNotifRepository();
            adviceReportRepository = new AdviceReportRepository();
            adviseExamTagRepository = new AdviseExamTagRepository();
            adviceTagReportRepository = new AdviceTagReportRepository();
            advisorMeetingRepository = new AdvisorMeetingRepository();
            adviseTagRepository = new AdviseTagRepository();
            advisorRequestsRepository = new AdvisorRequestsRepository();
            authorRepository = new AuthorRepository();
            avatarRepository = new AvatarRepository();
            badgeRepository = new BadgeRepository();
            branchRepository = new BranchRepository();
            certificateRepository = new CertificateRepository();
            cityRepository = new CityRepository();
            customQuizRepository = new CustomQuizRepository();
            coinHistoryRepository = new CoinHistoryRepository();
            configDashboardRepository = new ConfigDashboardRepository();
            courseIntroductionRepository = new CourseIntroductionRepository();
            configRepository = new ConfigRepository();
            contentConfigRepository = new ContentConfigRepository();
            commentRepository = new CommentRepository();
            contentRepository = new ContentRepository();
            contentQuizRepository = new ContentQuizRepository();
            creditRepository = new CreditRepository();
            dailyAdvRepository = new DailyAdvRepository();
            escapeQuizRepository = new EscapeQuizRepository();
            escapeQuizQuestionRepository = new EscapeQuizQuestionRepository();
            exchangeRepository = new ExchangeRepository();
            giftRepository = new GiftRepository();
            gradeRepository = new GradeRepository();
            hwRepository = new HWRepository();
            levelRepository = new LevelRepository();
            lifeScheduleRepository = new LifeScheduleRepository();
            lifeStyleTagRepository = new LifeStyleTagRepository();
            mailRepository = new MailRepository();
            mailQueueRepository = new MailQueueRepository();
            offcodeRepository = new OffcodeRepository();
            onlineStandQuizRepository = new OnlineStandQuizRepository();
            openQuizRepository = new OpenQuizRepository();
            packageRepository = new PackageRepository();
            packageLevelRepository = new PackageLevelRepository();
            payLinkRepository = new PayLinkRepository();
            profileConfigRepository = new ProfileConfigRepository();
            pointRepository = new PointRepository();
            iryscQuizRepository = new IRYSCQuizRepository();
            questionRepository = new QuestionRepository();
            questionTagRepository = new QuestionTagRepository();
            questionReportRepository = new QuestionReportRepository();
            requestRepository = new RequestRepository();
            rssRepository = new RSSRepository();
            schoolQuestionRepository = new SchoolQuestionRepository();
            schoolQuizRepository = new SchoolQuizRepository();
            schoolRepository = new SchoolRepository();
            seoRepository = new SeoRepository();
            settlementRequestRepository = new SettlementRequestRepository();
            smsQueueRepository = new SMSQueueRepository();
            stateRepository = new StateRepository();
            subjectRepository = new SubjectRepository();
            scheduleRepository = new ScheduleRepository();
            tarazRepository = new TarazRepository();
            ticketRepository = new TicketRepository();
            transactionRepository = new TransactionRepository();
            teachScheduleRepository = new TeachScheduleRepository();
            teacherBioRepository = new TeacherBioRepository();
            teachReportRepository = new TeachReportRepository();
            teachRateRepository = new TeachRateRepository();
            stdRateRepository = new StdRateRepository();
            teachTagReportRepository = new TeachTagReportRepository();
            userGiftRepository = new UserGiftRepository();
            userRepository = new UserRepository();
            userBadgeRepository = new UserBadgeRepository();
            userLevelRepository = new UserLevelRepository();
            userPointRepository = new UserPointRepository();
            notifRepository = new NotifRepository();
            missedChunkRepository = new MissedChunkRepository();
        } catch (Exception x) {
            printException(x);
        }
    }

    private static void setupNewThingsCache() {

        newThingsCache.put(NewAlert.NEW_TICKETS.getName(), ticketRepository.count(
                and(
                        eq("status", "pending"),
                        exists("chats.2", false)
                )
        ));

        newThingsCache.put(NewAlert.OPEN_TICKETS_WAIT_FOR_ADMIN.getName(), ticketRepository.count(
                and(
                        eq("status", "pending"),
                        exists("chats.1", true)
                )
        ));

    }

    public static ObjectMapper objectMapper;

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Iran"));
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
        module.addSerializer(ObjectId.class, new ObjectIdSerializer());

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new SimpleModule().addSerializer(ObjectId.class, new ObjectIdSerializer()));
        objectMapper.registerModule(new SimpleModule().addDeserializer(ObjectId.class, new ObjectIdDeserializer()));

        new SpringApplicationBuilder(GachesefidApplication.class)
                .run(args);
    }

    @PostConstruct
    private void setupDb() {
        setupDB(new ConnectionString(mongodbUrl));
        setupNewThingsCache();
        new Thread(new Jobs()).start();
        generalCacheService.getInfo();
    }

    @Autowired
    ReportService reportService;

    @Bean(name = "cropRT")
    public RestTemplate initializeCropRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

//        HttpClientConnectionManager connectionManager = (HttpClientConnectionManager) PoolingHttpClientConnectionManagerBuilder.create()
//                .setMaxConnTotal(200)
//                .setMaxConnPerRoute(20)
//                .build();
//
//        CloseableHttpClient httpClient = HttpClients.custom()
//                .setConnectionManager(connectionManager)
//                .build();

//        HttpComponentsClientHttpRequestFactory requestFactory =
//                new HttpComponentsClientHttpRequestFactory(httpClient);
//        requestFactory.setConnectTimeout(100000);
//        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }

    static {
        SpringDocUtils.getConfig().replaceWithSchema(ObjectId.class, new StringSchema());
    }

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI().addSecurityItem(new SecurityRequirement().
                        addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes
                        ("Bearer Authentication", createAPIKeyScheme()));
    }
}
