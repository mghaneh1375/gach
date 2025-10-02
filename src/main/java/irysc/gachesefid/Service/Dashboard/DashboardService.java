package irysc.gachesefid.Service.Dashboard;

import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Dto.Dashboard.Admin.AdminDashboardConfig;
import irysc.gachesefid.Dto.Dashboard.AdminDashboardStatsDto;
import irysc.gachesefid.Dto.Dashboard.Advisor.AdvisorDashboardConfig;
import irysc.gachesefid.Dto.Dashboard.Advisor.AdvisorDashboardStatsDto;
import irysc.gachesefid.Dto.Dashboard.Advisor.MyCurrStudent;
import irysc.gachesefid.Dto.Dashboard.Student.DashboardStatsDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.UserDigest;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.ONE_MONTH_MIL_SEC;
import static irysc.gachesefid.Utility.StaticValues.STUDENT_PUBLIC_INFO;
import static irysc.gachesefid.Utility.Utility.getPast;

@Service
public class DashboardService {
    private static Long lastAdminDashboardFetchTime = null;
    private static ResponseEntity<ResponseDto<AdminDashboardStatsDto>> lastAdminDashboardFetch = null;
    private final static long FIVE_MIN_MSEC = StaticValues.ONE_MIN_MSEC * 5;

    @Autowired
    private ConfigDashboardService configDashboardService;

    @Autowired
    private AdvisorDashboardUtil advisorDashboardUtil;

    @Autowired
    private DashboardUtil dashboardUtil;

    public ResponseEntity<ResponseDto<DashboardStatsDto>> siteStats(Document user) {
        Document generalCache = Repository.isInCache("general", "first");

        ResponseEntity<ResponseDto<AdminDashboardConfig>> response =
                configDashboardService.getConfig(user.getObjectId("_id"), AdminDashboardConfig.class);
        AdminDashboardConfig adminDashboardConfig = Objects.requireNonNull(response.getBody()).getData();
        DashboardStatsDto dashboardStatsDto;

        if (adminDashboardConfig.getShowDashboard()) {
            dashboardStatsDto = DashboardStatsDto
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
                    .build();
        } else
            dashboardStatsDto = DashboardStatsDto.builder().build();

        return new ResponseEntity<>(
                ResponseDto
                        .builder(DashboardStatsDto.class)
                        .status("ok")
                        .data(dashboardStatsDto)
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
                                        .activeTeachers(
                                                userRepository.count(and(
                                                        eq("accesses", "advisor"),
                                                        exists("teach"),
                                                        eq("teach", true)
                                                ))
                                        )
                                        .activeAdvisors(
                                                userRepository.count(and(
                                                        eq("accesses", "advisor"),
                                                        exists("advice"),
                                                        eq("advice", true)
                                                ))
                                        )
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
        ResponseEntity<ResponseDto<AdvisorDashboardConfig>> response =
                configDashboardService.getConfig(user.getObjectId("_id"), AdvisorDashboardConfig.class);
        AdvisorDashboardConfig advisorDashboardConfig = Objects.requireNonNull(response.getBody()).getData();

        long tilLastMonth = System.currentTimeMillis() - StaticValues.ONE_DAY_MIL_SEC * 30;
        int studentsCountForAdvice = user.containsKey("students")
                ? user.getList("students", Document.class).size()
                : 0;

        // todo: fill studentsCountForTeach from db query
        int studentsCountForTeach = 0;

        AdvisorDashboardStatsDto dashboardStatsDto = advisorDashboardConfig.getShowDashboard()
                ? AdvisorDashboardStatsDto
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
                .lastMonthSettled(advisorDashboardUtil.getLastMonthSettled(user.getObjectId("_id"), tilLastMonth))
                .pendingSettled(
                        settlementRequestRepository.count(
                                and(
                                        eq("user_id", user.getObjectId("_id")),
                                        eq("status", "pending")
                                )
                        )
                )
                .build()
                : AdvisorDashboardStatsDto.builder().build();

        if (advisorDashboardConfig.getShowLastTickets()) {
            dashboardStatsDto.setUnSeenTickets(
                    advisorDashboardUtil.getMyLastTickets(user.getObjectId("_id"))
            );
        }

        if (studentsCountForAdvice + studentsCountForTeach > 0 &&
                advisorDashboardConfig.getShowMeeting()
        ) {
            dashboardStatsDto.setCurrMeetings(
                    advisorMeetingRepository.fetchAdvisorCurrentMeetings(user.getObjectId("_id"))
            );
        }

        if (advisorDashboardConfig.getShowIncomingRequestsForAdvice()) {
            dashboardStatsDto.setAdviceRequests(
                    advisorRequestsRepository.myPendingRequest(user.getObjectId("_id"))
            );
        }

        if (advisorDashboardConfig.getShowLastNotifs()) {
            dashboardStatsDto.setLastNotifs(
                    dashboardUtil.getMyLastNotifs(user)
            );
        }

        if (advisorDashboardConfig.getShowMyLastComments()) {
            dashboardStatsDto.setLastComments(
                    advisorDashboardUtil.getMyLastComments(user.getObjectId("_id"))
            );
        }

        if (advisorDashboardConfig.getShowInProgressKarbargs()) {
            dashboardStatsDto.setInProgressSchedules(
                    scheduleRepository.getInProgressSchedulesDigest(user.getObjectId("_id"))
            );
        }

        if (advisorDashboardConfig.getShowFilledKarbargs()) {
            dashboardStatsDto.setFilledSchedules(
                    scheduleRepository.getDoneSchedulesDigest(user.getObjectId("_id"))
            );
        }

        if (advisorDashboardConfig.getShowIncomingRequestsForTeach()) {
            dashboardStatsDto.setTeachRequests(
                    teachScheduleRepository.getTeachPendingRequests(user.getObjectId("_id"))
            );
        }

        if (advisorDashboardConfig.getShowMyCurrStudents() && user.containsKey("students")) {
            List<Document> students = user.getList("students", Document.class);
            ArrayList<Document> studentsDoc = userRepository.findByIds(
                    students.stream().map(student -> student.getObjectId("_id")).collect(Collectors.toList()),
                    false, STUDENT_PUBLIC_INFO
            );

            List<MyCurrStudent> myCurrStudents = new ArrayList<>();
            for (Document student : studentsDoc) {
                Document doc = students
                        .stream()
                        .filter(document -> document.getObjectId("_id").equals(student.getObjectId("_id")))
                        .findFirst()
                        .get();
                myCurrStudents.add(
                        MyCurrStudent
                                .builder()
                                .student(
                                        UserDigest.buildFromDoc(student)
                                )
                                .startAt(doc.getLong("created_at"))
                                .endAt(doc.getLong("created_at") + ONE_MONTH_MIL_SEC)
                                .build()
                );
            }
            dashboardStatsDto.setMyCurrStudents(myCurrStudents);
        }

        return new ResponseEntity<>(
                ResponseDto
                        .builder(AdvisorDashboardStatsDto.class)
                        .status("ok")
                        .data(dashboardStatsDto)
                        .build(),
                HttpStatus.OK
        );
    }
}
