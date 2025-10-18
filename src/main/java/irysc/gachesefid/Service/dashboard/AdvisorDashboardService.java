package irysc.gachesefid.Service.dashboard;

import irysc.gachesefid.Dto.dashboard.Advisor.AdvisorDashboardConfig;
import irysc.gachesefid.Dto.dashboard.Advisor.AdvisorDashboardStatsDto;
import irysc.gachesefid.Dto.dashboard.Advisor.MyCurrStudent;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Dto.UserDigest;
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
import static com.mongodb.client.model.Filters.eq;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.ONE_MONTH_MIL_SEC;
import static irysc.gachesefid.Utility.StaticValues.STUDENT_PUBLIC_INFO;
import static irysc.gachesefid.Utility.Utility.getPast;

@Service
public class AdvisorDashboardService {

    @Autowired
    private AdvisorDashboardUtil advisorDashboardUtil;

    @Autowired
    private DashboardUtil dashboardUtil;

    @Autowired
    private ConfigDashboardService configDashboardService;

    public ResponseEntity<ResponseDto<AdvisorDashboardStatsDto>> advisorDashboardInfo(
            Document user
    ) {
        ResponseEntity<ResponseDto<AdvisorDashboardConfig>> response =
                configDashboardService.getConfig(user.getObjectId("_id"), AdvisorDashboardConfig.class);
        AdvisorDashboardConfig advisorDashboardConfig = Objects.requireNonNull(response.getBody()).getData();

        long tilLastMonth = System.currentTimeMillis() - ONE_MONTH_MIL_SEC;
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
