package irysc.gachesefid.Service.Dashboard;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Sorts;
import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Dto.Dashboard.AdminDashboardStatsDto;
import irysc.gachesefid.Dto.Dashboard.Advisor.AdvisorDashboardStatsDto;
import irysc.gachesefid.Dto.Dashboard.DashboardStatsDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.TICKET_PROJECTION;
import static irysc.gachesefid.Utility.StaticValues.USER_DIGEST;
import static irysc.gachesefid.Utility.Utility.getConfig;
import static irysc.gachesefid.Utility.Utility.getPast;

@Service
public class DashboardService {
    private static Long lastAdminDashboardFetchTime = null;
    private static ResponseEntity<ResponseDto<AdminDashboardStatsDto>> lastAdminDashboardFetch = null;
    private final static long FIVE_MIN_MSEC = StaticValues.ONE_MIN_MSEC * 5;

    private Set<String> detectUserBranches(Document user, List<Document> userIRYSCQuizzes, List<Document> userOpenQuizzes) {
        Set<String> userBranches = new HashSet<>();
        if (user.containsKey("branches") && user.getList("branches", Object.class).size() > 0) {
            userBranches.addAll(
                    user.getList("branches", Document.class).stream().map(document -> document.getString("name").replace("المپیاد", "").strip()).collect(Collectors.toList())
            );
        }
        if (!userIRYSCQuizzes.isEmpty()) {
            userBranches.addAll(
                    userIRYSCQuizzes
                            .stream()
                            .flatMap(document -> document.getList("tags", String.class).stream())
                            .filter(s -> s.contains("المپیاد"))
                            .map(s -> s.replace("المپیاد", "").strip())
                            .distinct()
                            .collect(Collectors.toList())
            );
        }
        if (!userOpenQuizzes.isEmpty()) {
            userBranches.addAll(
                    userOpenQuizzes
                            .stream()
                            .flatMap(document -> document.getList("tags", String.class).stream())
                            .filter(s -> s.contains("المپیاد"))
                            .map(s -> s.replace("المپیاد", "").strip())
                            .distinct()
                            .collect(Collectors.toList())
            );
        }

        return userBranches;
    }

    public ResponseEntity<ResponseDto<DashboardStatsDto>> dashboardInfo(Document user) {

        long curr = System.currentTimeMillis();
        Document rank = tarazRepository.findBySecKey(user.getObjectId("_id"));
        Document config = getConfig();
        double exchangeRate = ((Number) config.get("coin_rate_coef")).doubleValue();
        Document generalCache = Repository.isInCache("general", "first");
        ArrayList<Document> userIRYSCQuizzes = iryscQuizRepository.find(
                and(
                        exists("start"),
                        in("students._id", user.getObjectId("_id"))
                ),
                new BasicDBObject("start", 1).append("tags", 1)
                        .append("end", 1)
        );
        List<Document> userOpenQuizzes = openQuizRepository.find(
                in("students._id", user.getObjectId("_id")),
                new BasicDBObject("tags", 1)
        );

        DashboardStatsDto dto = DashboardStatsDto
                .builder()
                .money(user.get("money"))
                .rank(rank == null ? "" : rank.getInteger("rank"))
                .coinToMoneyExchange(exchangeRate)
                .coin(user.get("coin"))
                .activeTeachers(generalCache == null ? 0 : generalCache.getInteger("activeTeachersCount"))
                .activeAdvisors(generalCache == null ? 0 : generalCache.getInteger("activeAdvisorsCount"))
                .tutorialsCount(generalCache == null ? 0 : generalCache.getInteger("tutorialsCount"))
                .gradeRank(rank == null || !rank.containsKey("grade_rank") ? "" : rank.getInteger("grade_rank"))
                .registrableQuizzes(
                        generalCache == null
                                ? 0
                                : (Integer) generalCache.getOrDefault("activeIRYSCQuizzes", 0) +
                                openQuizRepository.count(
                                        nin("students._id", user.getObjectId("_id"))
                                )
                )
                .passedQuizzes(
                        userIRYSCQuizzes.stream().filter(document -> !document.containsKey("end") || document.getLong("end") < curr).count()
                )
                .activeQuizzes(
                        userIRYSCQuizzes.stream().filter(document -> {
                            System.out.println(document);
                            return document.getLong("start") > curr;
                        }).count()
                )
                .userOpenQuizzes(userOpenQuizzes.size())
                .totalQuizzes(userIRYSCQuizzes.size())
                .build();

        Set<String> branches = detectUserBranches(user, userIRYSCQuizzes, userOpenQuizzes);
        if (!branches.isEmpty()) {
            List<Bson> tags = branches.stream().map(s -> regex("tags", Pattern.compile(Pattern.quote(s), Pattern.CASE_INSENSITIVE))).collect(Collectors.toList());
//            dto.setRegistrableQuizzesSuggestion(
//                    List.of(
//                            iryscQuizRepository.find(
//                                    and(
//                                            nin("students._id", user.getObjectId("_id")),
//                                            gt("start", curr),
//                                            or(tags)
//                                    ), null
//                            ),
//                            openQuizRepository.find(
//                                    and(
//                                            nin("students._id", user.getObjectId("_id")),
//                                            or(tags)
//                                    ), null
//                            )
//                    );

            dto.setTutorialsSuggestion(contentRepository.getSuggestion(user.getObjectId("_id"), tags).toString());
        }

        return new ResponseEntity<>(
                ResponseDto
                        .builder(DashboardStatsDto.class)
                        .data(dto)
                        .status("ok")
                        .build(),
                HttpStatus.OK
        );
    }

    public ResponseEntity<ResponseDto<DashboardStatsDto>> getSiteSummary() {
        Document generalCache = Repository.isInCache("general", "first");

        return new ResponseEntity<>(
                ResponseDto
                        .builder(DashboardStatsDto.class)
                        .status("ok")
                        .data(
                                DashboardStatsDto
                                        .builder()
                                        .activeTeachers(generalCache == null ? 0 : generalCache.getInteger("activeTeachersCount"))
                                        .activeAdvisors(generalCache == null ? 0 : generalCache.getInteger("activeAdvisorsCount"))
                                        .tutorialsCount(generalCache == null ? 0 : generalCache.getInteger("tutorialsCount"))
                                        .schools(generalCache == null ? 0 : generalCache.getInteger("schools"))
                                        .students(generalCache == null ? 0 : generalCache.getInteger("students"))
                                        .questions(generalCache == null ? 0 : generalCache.getInteger("questions"))
                                        .registrableQuizzes(
                                                generalCache == null
                                                        ? 0
                                                        : (Integer) generalCache.getOrDefault("activeIRYSCQuizzes", 0) +
                                                        (Integer) generalCache.getOrDefault("openQuizzesCount", 0)
                                        )
                                        .build()
                        )
                        .build(),
                HttpStatus.OK
        );
    }

    public ResponseEntity<ResponseDto<AdminDashboardStatsDto>> adminDashboardInfo() {
        long curr = System.currentTimeMillis();
        if (lastAdminDashboardFetchTime != null &&
                lastAdminDashboardFetch != null &&
                lastAdminDashboardFetchTime >= curr - FIVE_MIN_MSEC
        )
            return lastAdminDashboardFetch;

        long tilLastMonth = curr - StaticValues.ONE_DAY_MIL_SEC * 30;
        ResponseEntity<ResponseDto<AdminDashboardStatsDto>> response = new ResponseEntity<>(
                ResponseDto
                        .builder(AdminDashboardStatsDto.class)
                        .data(
                                AdminDashboardStatsDto
                                        .builder()
                                        .pendingRequestForAdvisorAnswer(
                                                advisorRequestsRepository.count(eq("answer", "pending"))
                                        )
                                        .pendingChunks(
                                                contentRepository.notChunkedCount()
                                        )
                                        .lastMonthContentBuyCount(
                                                contentRepository.countIndividualRegistrationsLastMonth()
                                        )
                                        .lastMonthCustomQuizRegistry(
                                                customQuizRepository.count(
                                                        and(
                                                                or(
                                                                        eq("status", "paid"),
                                                                        eq("status", "finished")
                                                                ),
                                                                gte("created_at", tilLastMonth)
                                                        )
                                                )
                                        )
                                        .lastMonthKarbargs(
                                                scheduleRepository.count(
                                                        gte("week_start_at_int", Integer.parseInt(getPast("", 30)))
                                                )
                                        )
                                        .lastMonthMeetings(
                                                advisorMeetingRepository.count(
                                                        gte("created_at", tilLastMonth)
                                                )
                                        )
                                        .pendingComments(commentRepository.count(and(
                                                eq("status", "pending")
                                        )))
                                        .activeTeachers(0)
                                        .activeAdvisors(0)
                                        .lastMonthOpenQuizRegistry(
                                                openQuizRepository.countIndividualRegistrationsLastMonth()
                                        )
                                        .lastMonthSettled(
                                                settlementRequestRepository.count(
                                                        and(
                                                                eq("status", "paid"),
                                                                exists("paid_at"),
                                                                gte("paid_at", tilLastMonth)
                                                        )
                                                )
                                        )
                                        .lastMonthTeachReportsCount(
                                                teachScheduleRepository.countIndividualRegistrationsLastMonth()
                                        )
                                        .lastMonthTutorialCount(
                                                teachReportRepository.count(gte("created_at", tilLastMonth))
                                        )
                                        .pendingSettleRequests(
                                                settlementRequestRepository.count(eq("status", "pending"))
                                        )
                                        .pendingUpgradeLevelRequests(
                                                ticketRepository.count(
                                                        and(
                                                                eq("section", "upgradelevel"),
                                                                eq("status", "pending")
                                                        )
                                                )
                                        )
                                        .pendingTickets(
                                                ticketRepository.count(
                                                        and(
                                                                eq("is_for_teacher", false),
                                                                eq("status", "pending")
                                                        )
                                                )
                                        )
                                        .pendingRequestForStudentPay(
                                                advisorRequestsRepository.count(and(
                                                        eq("answer", "accept"),
                                                        exists("paid_at", false)
                                                ))
                                        )
                                        .build()
                        )
                        .build(),
                HttpStatus.OK
        );

        lastAdminDashboardFetch = response;
        lastAdminDashboardFetchTime = curr;

        return response;
    }

    public ResponseEntity<ResponseDto<AdvisorDashboardStatsDto>> advisorDashboardInfo(
            Document user
    ) {
        long tilLastMonth = System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC * 30;
        int studentsCountForAdvice = user.containsKey("students")
                ? user.getList("students", Document.class).size()
                : 0;

        // todo: fill studentsCountForTeach from db query
        int studentsCountForTeach = 0;

        AdvisorDashboardStatsDto dashboardStatsDto = AdvisorDashboardStatsDto
                .builder()
                .lastMonthCreatedExams(
                        schoolQuizRepository.count(
                                and(
                                        eq("created_by", user.getObjectId("_id")),
                                        or(
                                                eq("status", "finish"),
                                                eq("status", "semi_finish")
                                        ),
                                        gte("created_at", tilLastMonth)
                                )
                        )
                )
                .lastMonthKarbargs(
                        scheduleRepository.count(
                                and(
                                        eq("advisors", user.getObjectId("_id")),
                                        gte("week_start_at_int", Integer.parseInt(getPast("", 30)))
                                )
                        )
                )
                .studentsCountForAdvice(studentsCountForAdvice)
                .studentsCountForTeach(studentsCountForTeach)
                .pendingExamsForPay(
                        schoolQuizRepository.count(
                                and(
                                        eq("created_by", user.getObjectId("_id")),
                                        eq("status", "init")
                                )
                        )
                )
                .lastMonthMeetings(
                        advisorMeetingRepository.count(
                                and(
                                        eq("advisor_id", user.getObjectId("_id")),
                                        gte("created_at", tilLastMonth)
                                )
                        )
                )
                .build();

        if(studentsCountForAdvice > 0) {
            ArrayList<Bson> constraints = new ArrayList<>();
            constraints.add(eq("advisor_id", user.getObjectId("_id")));
            constraints.add(eq("section", "advisor"));
            AggregateIterable<Document> docs =
                    ticketRepository.findWithJoinUser("user_id", "student",
                            match(and(constraints)),
                            project(TICKET_PROJECTION),
                            Sorts.descending("send_date"), 0, 5,
                            project(USER_DIGEST.append("accesses", 1))
                    );
            dashboardStatsDto.setFutureMeetings(
                    advisorMeetingRepository.fetchAdvisorCurrentMeetings(user.getObjectId("_id"))
            );
        }

        return new ResponseEntity<>(
                ResponseDto
                        .builder(AdvisorDashboardStatsDto.class)
                        .status("ok")
                        .data(dashboardStatsDto)
                        .build()
                , HttpStatus.OK
        );
    }
}
