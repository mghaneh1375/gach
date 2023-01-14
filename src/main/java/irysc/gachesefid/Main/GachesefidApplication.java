package irysc.gachesefid.Main;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import irysc.gachesefid.Controllers.Jobs;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.NewAlert;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Utility.Utility.printException;
import static irysc.gachesefid.Utility.StaticValues.SCHOOLS;
import static irysc.gachesefid.Utility.StaticValues.STUDENTS;
import static irysc.gachesefid.Utility.StaticValues.QUESTIONS;
import static java.util.Map.entry;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@ComponentScan({"irysc.gachesefid.Routes", "irysc.gachesefid.Validator",
        "irysc.gachesefid.Security", "irysc.gachesefid.Service"})
@EntityScan("irysc.gachesefid.Service")
@Configuration
public class GachesefidApplication implements WebMvcConfigurer {

    final static private String username = "root";
    final static private String password = "Ghhy@110";
//    final static private String username = "test";
//    final static private String password = "123456";

    final static private String dbName = "gach"; // mydb

    final static private ConnectionString connString = new ConnectionString(
            "mongodb://localhost:27017/gachesefid"
    );
    public static Connection con = null;
    public static Map<String, Integer> commonVals = new HashMap<>();
    public static MongoDatabase mongoDatabase;

    public static AccessRequestRepository accessRequestRepository;
    public static ActivationRepository activationRepository;
    public static AdvisorRequestsRepository advisorRequestsRepository;
    public static AlertsRepository alertsRepository;
    public static AuthorRepository authorRepository;
    public static AvatarRepository avatarRepository;
    public static BranchRepository branchRepository;
    public static CertificateRepository certificateRepository;
    public static CityRepository cityRepository;
    public static CustomQuizRepository customQuizRepository;
    public static CoinHistoryRepository coinHistoryRepository;
    public static ConfigRepository configRepository;
    public static ContentConfigRepository contentConfigRepository;
    public static ContentRepository contentRepository;
    public static ContentQuizRepository contentQuizRepository;
    public static GiftRepository giftRepository;
    public static GradeRepository gradeRepository;
    public static OffcodeRepository offcodeRepository;
    public static OpenQuizRepository openQuizRepository;
    public static PackageRepository packageRepository;
    public static IRYSCQuizRepository iryscQuizRepository;
    public static QuestionRepository questionRepository;
    public static QuestionTagRepository questionTagRepository;
    public static RequestRepository requestRepository;
    public static SchoolQuizRepository schoolQuizRepository;
    public static SchoolRepository schoolRepository;
    public static SeoRepository seoRepository;
    public static SMSQueueRepository smsQueueRepository;
    public static StateRepository stateRepository;
    public static SubjectRepository subjectRepository;
    public static TarazRepository tarazRepository;
    public static TicketRepository ticketRepository;
    public static TransactionRepository transactionRepository;
    public static UserGiftRepository userGiftRepository;
    public static UserRepository userRepository;
    public static MailRepository mailRepository;
    public static MailQueueRepository mailQueueRepository;
    public static NotifRepository notifRepository;

    public static HashMap<String, Integer> newThingsCache = new HashMap<>();

    private static void setupDB() {
        try {

//            Class.forName("com.mysql.jdbc.Driver");
//            con = DriverManager.getConnection("jdbc:mysql://localhost/" + dbName + "?useUnicode=true&characterEncoding=UTF-8", username, password);
//            Statement st = con.createStatement();
//            st.executeUpdate("SET GLOBAL WAIT_TIMEOUT = 315360");
//            st.executeUpdate("SET GLOBAL INTERACTIVE_TIMEOUT = 315360");

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .retryWrites(true)
                    .build();
            MongoClient mongoClient = MongoClients.create(settings);
            mongoDatabase = mongoClient.getDatabase("gachesefid");

            accessRequestRepository = new AccessRequestRepository();
            activationRepository = new ActivationRepository();
            advisorRequestsRepository = new AdvisorRequestsRepository();
            alertsRepository = new AlertsRepository();
            authorRepository = new AuthorRepository();
            avatarRepository = new AvatarRepository();
            branchRepository = new BranchRepository();
            certificateRepository = new CertificateRepository();
            cityRepository = new CityRepository();
            customQuizRepository = new CustomQuizRepository();
            coinHistoryRepository = new CoinHistoryRepository();
            configRepository = new ConfigRepository();
            contentConfigRepository = new ContentConfigRepository();
            contentRepository = new ContentRepository();
            contentQuizRepository = new ContentQuizRepository();
            giftRepository = new GiftRepository();
            gradeRepository = new GradeRepository();
            mailRepository = new MailRepository();
            mailQueueRepository = new MailQueueRepository();
            offcodeRepository = new OffcodeRepository();
            openQuizRepository = new OpenQuizRepository();
            packageRepository = new PackageRepository();
            iryscQuizRepository = new IRYSCQuizRepository();
            questionRepository = new QuestionRepository();
            questionTagRepository = new QuestionTagRepository();
            requestRepository = new RequestRepository();
            schoolQuizRepository = new SchoolQuizRepository();
            schoolRepository = new SchoolRepository();
            seoRepository = new SeoRepository();
            smsQueueRepository = new SMSQueueRepository();
            stateRepository = new StateRepository();
            subjectRepository = new SubjectRepository();
            tarazRepository = new TarazRepository();
            ticketRepository = new TicketRepository();
            transactionRepository = new TransactionRepository();
            userGiftRepository = new UserGiftRepository();
            userRepository = new UserRepository();
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
