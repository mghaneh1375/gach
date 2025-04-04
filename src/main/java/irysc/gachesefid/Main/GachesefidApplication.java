package irysc.gachesefid.Main;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import irysc.gachesefid.Controllers.Jobs;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Models.NewAlert;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Utility.Utility.printException;
import static irysc.gachesefid.Utility.StaticValues.SCHOOLS;
import static irysc.gachesefid.Utility.StaticValues.STUDENTS;
import static irysc.gachesefid.Utility.StaticValues.QUESTIONS;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@ComponentScan({"irysc.gachesefid.Routes", "irysc.gachesefid.Validator",
        "irysc.gachesefid.Security", "irysc.gachesefid.Service"})
@EntityScan("irysc.gachesefid.Service")
@Configuration
@EnableScheduling
public class GachesefidApplication implements WebMvcConfigurer {
    final static private ConnectionString connString = new ConnectionString(
            "mongodb://localhost:27017/gachesefid"

    );
    public static MongoDatabase mongoDatabase;

    public static AccessRequestRepository accessRequestRepository;
    public static ActivationRepository activationRepository;
    public static AdminNotifRepository adminNotifRepository;
    public static AdviseExamTagRepository adviseExamTagRepository;
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
    public static NotifRepository notifRepository;

    public static HashMap<String, Integer> newThingsCache = new HashMap<>();

    private static void setupDB() {
        try {

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .retryWrites(true)
                    .build();
            MongoClient mongoClient = MongoClients.create(settings);
            mongoDatabase = mongoClient.getDatabase("gachesefid");

            accessRequestRepository = new AccessRequestRepository();
            activationRepository = new ActivationRepository();
            advisorFinanceOfferRepository = new AdvisorFinanceOfferRepository();
            adminNotifRepository = new AdminNotifRepository();
            adviseExamTagRepository = new AdviseExamTagRepository();
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

//            SCHOOLS = schoolRepository.count(exists("user_id"));
            SCHOOLS = schoolRepository.count(null);
            QUESTIONS = questionRepository.count(null);
            STUDENTS = userRepository.count(eq("level", false));

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

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Iran"));
        setupDB();
//        Enc.init();
        setupNewThingsCache();
        new Thread(new Jobs()).start();
        new SpringApplicationBuilder(GachesefidApplication.class)
                .run(args);
    }

}
