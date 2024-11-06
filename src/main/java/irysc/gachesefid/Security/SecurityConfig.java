package irysc.gachesefid.Security;

import irysc.gachesefid.Models.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import javax.servlet.http.HttpServletResponse;

import static irysc.gachesefid.Security.WhiteList.WHITE_LIST;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http = http.csrf().disable();
        http = http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();

        http = http
                .exceptionHandling()
                .authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                ex.getMessage()
                        )
                )
                .and();

        http.authorizeRequests()
                .antMatchers(WHITE_LIST)
                .permitAll()
                // ANY ROLE SERVICES
                .antMatchers(
                        "/api/advisor/manage/getOffers/**",
                        "/api/advisor/manage/getStudentSchedule/**",
                        "/api/admin/config/avatar/getAll",
                        "/api/quiz/manage/removeMember/**",
                        "/api/quiz/manage/onlineStandingAddMember/**"
                        ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_ADVISOR.getAuthority(),
                        Role.ROLE_TEACHER.getAuthority(), Role.ROLE_CLIENT.getAuthority(), Role.ROLE_SCHOOL.getAuthority(),
                        Role.ROLE_AGENT.getAuthority(), Role.ROLE_CONTENT.getAuthority(), Role.ROLE_EDITOR.getAuthority()
                )
                // EDITOR OR CONTENT ROLES SERVICES
                .antMatchers("/api/admin/config/author/getAuthorsKeyVals"
                ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(),
                        Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_CONTENT.getAuthority(),
                        Role.ROLE_EDITOR.getAuthority()
                )
                // ADMIN SERVICES
                .antMatchers(
                        "/api/teach/admin/**", "/api/badge/admin/**",
                        "/api/notifs/manage/**", "/api/admin/stats/**",
                        "/api/admin/settled/**", "/api/point/admin/**",
                        "/api/level/admin/**", "/api/exchange/admin/**",
                        "/api/questionReport/manage/**", "/api/admin/config/author/**",
                        "/api/daily_adv/admin/**", "/api/admin/transaction/**",
                        "/api/admin/config/tarazLevel/**", "/api/package_content/adv/**",
                        "/api/certificate/admin/**",
                        "/api/advisor/manage/getAdvisorTags/**",
                        "/api/advisor/manage/addAdvisorTag/**",
                        "/api/advisor/manage/removeAdvisorTag/**",
                        "/api/admin/off/**", "/api/admin/config/gift/**",
                        "/api/admin/config/avatar/**", "/api/admin/config/config/**",
                        "/api/admin/config/school/**", "/api/package_content/faq/**",
                        "//api/admin/content/**"
                ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(),
                        Role.ROLE_SUPER_ADMIN.getAuthority()
                )
                // ADVISOR SERVICES
                .antMatchers(
                        "/api/teach/manage/**", "/api/advisor/manage/**"
                ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(),
                        Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_ADVISOR.getAuthority()
                )
                // PRIVILEGE ACCESS
                .antMatchers(
                        "/api/quiz/school/getMyMarkListForSpecificQuestion/**",
                        "/api/quiz/school/setMark/**",
                        "/api/quiz/manage/getAll/**",
                        "/api/quiz/manage/resetStudentQuizEntryTime/**",
                        "/api/quiz/manage/createTaraz/**",
                        "/api/quiz/manage/storeAnswers/**",
                        "/api/quiz/manage/setQuizAnswerSheet/**",
                        "/api/quiz/manage/getQuizAnswerSheet/**",
                        "/api/quiz/manage/getQuizAnswerSheets/**",
                        "/api/quiz/manage/fetchQuestions/**",
                        "/api/quiz/manage/removeCorrectors/**",
                        "/api/quiz/manage/addCorrector/**",
                        "/api/quiz/manage/getCorrector/**",
                        "/api/quiz/manage/getCorrectors/**",
                        "/api/quiz/manage/generateQuestionPDF/**",
                        "/api/quiz/manage/addBatchQuestionsToQuiz/**",
                        "/api/quiz/manage/removeQuestionFromQuiz/**",
                        "/api/quiz/manage/removeAttach/**",
                        "/api/quiz/manage/addAttach/**",
                        "/api/quiz/manage/getParticipants/**",
                        "/api/quiz/manage/setCorrectorByQuestionMode/**",
                        "/api/quiz/manage/setCorrectorByStudentMode/**",
                        "/api/admin/user/fetchUser/**",
                        "/api/admin/question/subjectQuestions"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_ADVISOR.getAuthority(),
                        Role.ROLE_TEACHER.getAuthority(), Role.ROLE_SCHOOL.getAuthority(),
                        Role.ROLE_AGENT.getAuthority(), Role.ROLE_CONTENT.getAuthority(), Role.ROLE_EDITOR.getAuthority()
                )
                // QUIZ ACCESS
                .antMatchers(
                        "/api/quiz/manage/setPDFQuizQuestions/**",
                        "/api/quiz/manage/setPDFQuizInfo/**",
                        "/api/quiz/manage/getGradesAndBranches/**",
                        "/api/quiz/manage/getPDFQuizInfo/**",
                        "/api/quiz/manage/setPDFQuizSubjectsAndChoicesCount/**",
                        "/api/quiz/manage/getPDFQuizAnswerSheet/**",
                        "/api/quiz/manage/setPDFQuizAnswerSheet/**",
                        "/api/quiz/manage/getPDFQuizQuestions/**",
                        "/api/quiz/manage/getPDFQuizSubjects/**",
                        "/api/quiz/manage/edit/**",
                        "/api/quiz/manage/store/**",
                        "/api/quiz/manage/toggleVisibility/**",
                        "/api/quiz/manage/remove/**",
                        "/api/quiz/manage/updateQuestionMark/**",
                        "/api/quiz/manage/addQuestionToQuizzes/**",
                        "/api/quiz/manage/arrangeQuestions/**",
                        "/api/quiz/school/createHW/**"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_ADVISOR.getAuthority(),
                        Role.ROLE_SCHOOL.getAuthority(), Role.ROLE_CONTENT.getAuthority()
                )
                .antMatchers("/api/comment/public/**").hasAnyAuthority(Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_CONTENT.getAuthority(), Role.ROLE_EDITOR.getAuthority())
                // EDITOR ACCESS
                .antMatchers(
                        "/api/request/**",
                        "/api/quiz/manage/changeMainMember/**",
                        "/api/quiz/manage/onlineStandingForceRegistry/**"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_EDITOR.getAuthority()
                )
                .antMatchers(
                        "/api/admin/user/removeSchools",
                        "/api/admin/user/getMySchools"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_AGENT.getAuthority()
                )
                .antMatchers(
                        "/api/admin/user/removeStudents"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_SCHOOL.getAuthority()
                )
                .anyRequest()
                .authenticated()
        ;

        http.addFilterBefore(
                jwtTokenFilter,
                UsernamePasswordAuthenticationFilter.class
        );
        http.headers()
                .httpStrictTransportSecurity()
                .includeSubDomains(true)
                .preload(false)
                .maxAgeInSeconds(31536000)
                .requestMatcher(AnyRequestMatcher.INSTANCE);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
