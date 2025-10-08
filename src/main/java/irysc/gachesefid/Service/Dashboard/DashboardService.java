package irysc.gachesefid.Service.Dashboard;

import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Dto.Dashboard.Admin.AdminDashboardConfig;
import irysc.gachesefid.Dto.Dashboard.Admin.AdminDashboardStatsDto;
import irysc.gachesefid.Dto.Dashboard.Advisor.ReportProblemDigestDto;
import irysc.gachesefid.Dto.Dashboard.Student.DashboardStatsDto;
import irysc.gachesefid.Dto.ResponseDto;
import irysc.gachesefid.Utility.StaticValues;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Filters.*;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.ONE_MONTH_MIL_SEC;
import static irysc.gachesefid.Utility.Utility.getPast;

@Service
public class DashboardService {
    private static Long lastAdminDashboardFetchTime = null;
    private static ResponseEntity<ResponseDto<AdminDashboardStatsDto>> lastAdminDashboardFetch = null;
    private final static long FIVE_MIN_MSEC = StaticValues.ONE_MIN_MSEC * 5;

    @Autowired
    private ConfigDashboardService configDashboardService;

    public ResponseEntity<ResponseDto<DashboardStatsDto>> siteStats() {
        Document generalCache = Repository.isInCache("general", "first");

        return new ResponseEntity<>(
                ResponseDto
                        .builder(DashboardStatsDto.class)
                        .status("ok")
                        .data(DashboardStatsDto
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

    public ResponseEntity<ResponseDto<AdminDashboardStatsDto>> adminDashboardInfo(
            ObjectId userId
    ) {
        long curr = System.currentTimeMillis();
        if (lastAdminDashboardFetchTime != null &&
                lastAdminDashboardFetch != null &&
                lastAdminDashboardFetchTime >= curr - FIVE_MIN_MSEC
        )
            return lastAdminDashboardFetch;

        ResponseEntity<ResponseDto<AdminDashboardConfig>> configResponse =
                configDashboardService.getConfig(userId, AdminDashboardConfig.class);
        AdminDashboardConfig adminDashboardStatsDto = Objects.requireNonNull(configResponse.getBody()).getData();
        AdminDashboardStatsDto statsDto;

        long tilLastMonth = curr - ONE_MONTH_MIL_SEC;
        if(adminDashboardStatsDto.getShowDashboard()) {
            statsDto = AdminDashboardStatsDto
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
                    .build();
        }
        else
            statsDto = AdminDashboardStatsDto.builder().build();

        if(adminDashboardStatsDto.getShowIncomingRequestsForAdvice()) {
            statsDto.setAdviceRequests(
                    advisorRequestsRepository.pendingRequest()
            );
        }

        if(adminDashboardStatsDto.getShowMeetings()) {
            statsDto.setCurrMeetings(
                    advisorMeetingRepository.fetchCurrentMeetings()
            );
        }

        if(adminDashboardStatsDto.getShowLastUserReports()) {
            List<ReportProblemDigestDto> reports = new ArrayList<>();
            reports.addAll(teachReportRepository.getLastReports());
            statsDto.setProblemReports(reports);
        }

        if(adminDashboardStatsDto.getShowTopTeachers()) {

        }

        if(adminDashboardStatsDto.getShowTopAdvisors()) {

        }

        if(adminDashboardStatsDto.getShowLastSettleRequest()) {

        }

        if(adminDashboardStatsDto.getShowIncomingRequestsForTeach()) {

        }

        ResponseEntity<ResponseDto<AdminDashboardStatsDto>> response = new ResponseEntity<>(
                ResponseDto
                        .builder(AdminDashboardStatsDto.class)
                        .data(statsDto)
                        .build(),
                HttpStatus.OK
        );

        lastAdminDashboardFetch = response;
        lastAdminDashboardFetchTime = curr;

        return response;
    }

}
