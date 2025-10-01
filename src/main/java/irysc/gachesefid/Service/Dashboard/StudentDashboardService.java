package irysc.gachesefid.Service.Dashboard;

import com.mongodb.BasicDBObject;
import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Dto.Dashboard.Student.DashboardStatsDto;
import irysc.gachesefid.Dto.Dashboard.Student.MyAdvisorDigestDto;
import irysc.gachesefid.Dto.Dashboard.Student.StudentDashboardConfig;
import irysc.gachesefid.Dto.QuizDigestDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.UserDigest;
import irysc.gachesefid.Utility.Utility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.regex;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.getConfig;

@Service
public class StudentDashboardService {

    @Autowired
    private ConfigDashboardService configDashboardService;
    @Autowired
    private DashboardUtil dashboardUtil;

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


    public ResponseEntity<ResponseDto<DashboardStatsDto>> getStudentDashboard(Document user) {

        ResponseEntity<ResponseDto<StudentDashboardConfig>> response =
                configDashboardService.getConfig(user.getObjectId("_id"), StudentDashboardConfig.class);
        StudentDashboardConfig studentDashboardConfig = Objects.requireNonNull(response.getBody()).getData();
        DashboardStatsDto dashboardStatsDto;
        long curr = System.currentTimeMillis();

        ArrayList<Document> userIRYSCQuizzes = studentDashboardConfig.getShowDashboard() ||
                studentDashboardConfig.getShowSuggestionForQuiz()
                ? iryscQuizRepository.find(
                and(
                        exists("start"),
                        in("students._id", user.getObjectId("_id"))
                ),
                QUIZ_DIGEST
        ) : null;

        List<Document> userOpenQuizzes = studentDashboardConfig.getShowDashboard() ||
                studentDashboardConfig.getShowSuggestionForQuiz()
                ? openQuizRepository.find(
                in("students._id", user.getObjectId("_id")),
                new BasicDBObject("tags", 1)
        ) : null;

        if (studentDashboardConfig.getShowDashboard()) {
            Document rank = tarazRepository.findBySecKey(user.getObjectId("_id"));
            Document config = getConfig();
            double exchangeRate = ((Number) config.get("coin_rate_coef")).doubleValue();
            Document generalCache = Repository.isInCache("general", "first");

            dashboardStatsDto = DashboardStatsDto
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
                            userIRYSCQuizzes.stream().filter(document -> document.getLong("start") > curr).count()
                    )
                    .userOpenQuizzes(userOpenQuizzes.size())
                    .totalQuizzes(userIRYSCQuizzes.size())
                    .build();
        } else
            dashboardStatsDto = DashboardStatsDto.builder().build();

        if (studentDashboardConfig.getShowMeeting()) {
            dashboardStatsDto.setCurrMeetings(
                    advisorMeetingRepository.fetchStudentCurrentMeetings(user.getObjectId("_id"))
            );
        }

        if (studentDashboardConfig.getShowFutureQuiz()) {
            dashboardStatsDto.setFutureQuizzes(
                    userIRYSCQuizzes == null
                            ? null
                            : userIRYSCQuizzes
                            .stream()
                            .filter(document -> document.getLong("start") > curr && document.getLong("start") <= curr + ONE_DAY_MIL_SEC * 3)
                            .map(QuizDigestDto::buildFromDoc)
                            .collect(Collectors.toList())
            );
        }

        if (studentDashboardConfig.getShowRequestsStatusForTeach()) {

        }

        if (studentDashboardConfig.getShowLastTickets()) {
            dashboardStatsDto.setUnSeenTickets(
                    dashboardUtil.getMyLastTickets(user.getObjectId("user_id"))
            );
        }

        if (studentDashboardConfig.getShowRequestsStatusForAdvice()) {
            dashboardStatsDto.setAdviceRequests(
                    advisorRequestsRepository.myLastWeekRequests(user.getObjectId("_id"))
            );
        }

        if (studentDashboardConfig.getShowLastNotifs()) {
            dashboardStatsDto.setLastNotifs(
                    dashboardUtil.getMyLastNotifs(user)
            );
        }

        Set<String> branches = studentDashboardConfig.getShowSuggestionForContent() ||
                studentDashboardConfig.getShowSuggestionForQuiz()
                ? detectUserBranches(user, userIRYSCQuizzes, userOpenQuizzes)
                : null;

        if (studentDashboardConfig.getShowSuggestionForQuiz()) {

        }

        if (studentDashboardConfig.getShowSuggestionForContent() && !branches.isEmpty()) {
            List<Bson> tags = branches.stream().map(s -> regex("tags", Pattern.compile(Pattern.quote(s), Pattern.CASE_INSENSITIVE))).collect(Collectors.toList());
            dashboardStatsDto.setTutorialsSuggestion(
                    contentRepository.getSuggestion(user.getObjectId("_id"), tags)
            );
        }

        if (studentDashboardConfig.getShowCurrentKarbargs()) {
            dashboardStatsDto.setCurrentSchedules(
                    scheduleRepository.getInProgressSchedulesDigestForStudent(user.getObjectId("_id"))
            );
        }

        if (studentDashboardConfig.getShowMyAdvisor() && user.containsKey("my_advisors")) {
            dashboardStatsDto.setMyAdvisors(
                    userRepository.findByIds(
                            user.getList("my_advisors", Object.class), false,
                            ADVISOR_PUBLIC_INFO_FOR_STD
                    ).stream().map(advisor -> {
                        Document std = Utility.searchInDocumentsKeyVal(
                                advisor.getList("students", Document.class),
                                "_id", user.getObjectId("_id")
                        );
                        return MyAdvisorDigestDto
                                .builder()
                                .advisor(UserDigest.buildFromDoc(advisor))
                                .startAt(std.getLong("created_at"))
                                .endAt(std.getLong("created_at") + ONE_MONTH_MIL_SEC)
                                .rate(Integer.parseInt(std.getOrDefault("rate", 0).toString()))
                                .build();
                    }).collect(Collectors.toList())
            );
        }

        return new ResponseEntity<>(
                ResponseDto
                        .builder(DashboardStatsDto.class)
                        .data(dashboardStatsDto)
                        .status("ok")
                        .build(),
                HttpStatus.OK
        );
    }
}
