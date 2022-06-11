package irysc.gachesefid.Main;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import irysc.gachesefid.Controllers.Jobs;
import irysc.gachesefid.DB.*;
import irysc.gachesefid.Models.NewAlert;
import irysc.gachesefid.Utility.Enc;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static irysc.gachesefid.Utility.Utility.printException;
import static java.util.Map.entry;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@ComponentScan({"irysc.gachesefid.Routes", "irysc.gachesefid.Validator", "irysc.gachesefid.Security", "irysc.gachesefid.Service"})
@EntityScan("irysc.gachesefid.Service")
@Configuration
public class GachesefidApplication implements WebMvcConfigurer {

    final static public int MAX_ALLOWED_THREAD = 300;
    public static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(30);
    final static private String username = "test";
    final static private String password = "123456";
    final static private String dbName = "gachesefid"; // mydb

    final static private ConnectionString connString = new ConnectionString(
            "mongodb://localhost:27017/gachesefid"
    );
    public static Connection con = null;
    public static Map<String, Integer> commonVals = new HashMap<>();
    public static MongoDatabase mongoDatabase;
    public static int runThreads = 0;

    public static ActivationRepository activationRepository;
    public static AdvisorRequestsRepository advisorRequestsRepository;
    public static AlertsRepository alertsRepository;
    public static AvatarRepository avatarRepository;
    public static CertificateRepository certificateRepository;
    public static CoinHistoryRepository coinHistoryRepository;
    public static ConfigRepository configRepository;
    public static GradeRepository gradeRepository;
    public static OffcodeRepository offcodeRepository;
    public static IRYSCQuizRepository iryscQuizRepository;
    public static QuestionRepository questionRepository;
    public static RequestRepository requestRepository;
    public static SubjectRepository subjectRepository;
    public static SchoolQuizRepository schoolQuizRepository;
    public static TicketRepository ticketRepository;
    public static TransactionRepository transactionRepository;
    public static UserRepository userRepository;
    public static MailRepository mailRepository;

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

            activationRepository = new ActivationRepository();
            advisorRequestsRepository = new AdvisorRequestsRepository();
            alertsRepository = new AlertsRepository();
            avatarRepository = new AvatarRepository();
            certificateRepository = new CertificateRepository();
            coinHistoryRepository = new CoinHistoryRepository();
            configRepository = new ConfigRepository();
            gradeRepository = new GradeRepository();
            mailRepository = new MailRepository();
            offcodeRepository = new OffcodeRepository();
            iryscQuizRepository = new IRYSCQuizRepository();
            questionRepository = new QuestionRepository();
            requestRepository = new RequestRepository();
            subjectRepository = new SubjectRepository();
            schoolQuizRepository = new SchoolQuizRepository();
            ticketRepository = new TicketRepository();
            transactionRepository = new TransactionRepository();
            userRepository = new UserRepository();

        } catch (Exception x) {
            printException(x);
        }
    }

    private static void fillCommonVals() {

        commonVals = Map.ofEntries(
                entry("money1", 1),
                entry("money2", 2),
                entry("money3", 33),
                entry("invitationTransaction", 1),
                entry("redundant1Transaction", 2),
                entry("initTransaction", 8),
                entry("redundant2Transaction", 3),
                entry("ravanTransaction", 14),
                entry("studentLevel", 1),
                entry("adviserLevel", 2),
                entry("operator1Level", 3),
                entry("schoolLevel", 9),
                entry("operator2Level", 4),
                entry("adminLevel", 5),
                entry("superAdminLevel", 6),
                entry("controllerLevel", 7),
                entry("namayandeLevel", 8)
        );
//				'designerLevel' => 10, "teacherLevel" => 99,
//				'sampadSch' => 1, 'gheyrSch' => 2, 'nemoneSch' => 3, 'shahedSch' => 4, 'sayerSch' => 5, 'HeyatSch' => 6, 'dolatiSch' => 7,
//				'staticOffCode' => 1, 'dynamicOffCode' => 2, 'chargeTransaction' => 4, 'systemQuiz' => 1, 'motevaseteAval' => 0, 'motevaseteDovom' => 1, 'dabestan' => 2, 'quizRankTransaction' => 9,
//				'regularQuiz' => 2, 'questionQuiz' => 3, 'openQuiz' => 4, 'systemQuizTransaction' => 5, 'regularQuizTransaction' => 6, 'regularQuizGroupTransaction' => 7, 'questionBuyTransaction' => 10, 'kooshaExam' => 33,
//				'konkurAdvice' => 1, 'olympiadAdvice' => 2, 'doore1Advice' => 3, 'doore2Advice' => 4, 'baliniAdvice' => 5, 'unknownAdvice' => 6, 'openQuizTransaction' => 17, 'schoolQuizTransaction' => 18,
//				'elementaryAdvice' => 7, 'schoolAccountTransaction' => 20,
//				'diplom' => 1, 'foghDiplom' => 2, 'lisans' => 3, 'foghLisans' => 4, 'phd' => 5, 'unknown' => 6,
//				"standardRaven" => 1, "childRaven" => 2, "proRaven" => 3,
//				"beckDepression" => 1, "beckAnxiety" => 2
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
        setupDB();
        Enc.init();
        setupNewThingsCache();
        new Thread(new Jobs()).start();
        new SpringApplicationBuilder(GachesefidApplication.class)
                .run(args);
    }

//    @Bean
//    public Docket api() {
//        return new Docket(DocumentationType.SWAGGER_2)
//                .select()
//                .apis(RequestHandlerSelectors.any())
//                .paths(PathSelectors.any())
//                .build();
//    }
}
