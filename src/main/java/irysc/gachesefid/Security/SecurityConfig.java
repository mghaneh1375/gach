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
                        "/advisor/manage/getOffers/**",
                        "/advisor/manage/getStudentSchedule/**",
                        "/admin/config/avatar/getAll",
                        "/quiz/manage/removeMember/**",
                        "/quiz/manage/onlineStandingAddMember/**"
                        ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_ADVISOR.getAuthority(),
                        Role.ROLE_TEACHER.getAuthority(), Role.ROLE_CLIENT.getAuthority(), Role.ROLE_SCHOOL.getAuthority(),
                        Role.ROLE_AGENT.getAuthority(), Role.ROLE_CONTENT.getAuthority(), Role.ROLE_EDITOR.getAuthority()
                )
                // EDITOR OR CONTENT ROLES SERVICES
                .antMatchers(
                        "/admin/config/author/getAuthorsKeyVals",
                        "/admin/quiz/**"
                ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(),
                        Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_CONTENT.getAuthority(),
                        Role.ROLE_EDITOR.getAuthority()
                )
                // ADMIN SERVICES
                .antMatchers(
                        "/teach/admin/**", "/badge/admin/**",
                        "/notifs/manage/**", "/admin/stats/**",
                        "/admin/settled/**", "/point/admin/**",
                        "/level/admin/**", "/exchange/admin/**",
                        "/questionReport/manage/**", "/admin/config/author/**",
                        "/daily_adv/admin/**", "/admin/transaction/**",
                        "/admin/config/tarazLevel/**", "/package_content/adv/**",
                        "/certificate/admin/**",
                        "/advisor/manage/getAdvisorTags/**",
                        "/advisor/manage/addAdvisorTag/**",
                        "/advisor/manage/removeAdvisorTag/**",
                        "/admin/off/**", "/admin/config/gift/**",
                        "/admin/config/avatar/**", "/admin/config/config/**",
                        "/admin/config/school/**", "/package_content/faq/**",
                        "/admin/content/**", "/admin/dashboard/**",
                        "/admin/report/**", "/ckeditor/quiz",
                        "/admin/advice_tag_report/**", "/admin/cv_question/**",
                        "/admin/command/**", "/admin/advisor/**"
                ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(),
                        Role.ROLE_SUPER_ADMIN.getAuthority()
                )
                // ADVISOR SERVICES
                .antMatchers(
                        "/teach/manage/**", "/advisor/manage/**",
                        "/advisor/dashboard/**"
                ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(),
                        Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_ADVISOR.getAuthority()
                )
                .antMatchers(
                        "/general/advice_tag_report/**"
                ).hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(),
                        Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_ADVISOR.getAuthority(),
                        Role.ROLE_CLIENT.getAuthority()
                )
                // PRIVILEGE ACCESS
                .antMatchers(
                        "/quiz/school/getMyMarkListForSpecificQuestion/**",
                        "/quiz/school/setMark/**",
                        "/quiz/manage/getAll/**",
                        "/quiz/manage/resetStudentQuizEntryTime/**",
                        "/quiz/manage/createTaraz/**",
                        "/quiz/manage/storeAnswers/**",
                        "/quiz/manage/setQuizAnswerSheet/**",
                        "/quiz/manage/getQuizAnswerSheet/**",
                        "/quiz/manage/getQuizAnswerSheets/**",
                        "/quiz/manage/fetchQuestions/**",
                        "/quiz/manage/removeCorrectors/**",
                        "/quiz/manage/addCorrector/**",
                        "/quiz/manage/getCorrector/**",
                        "/quiz/manage/getCorrectors/**",
                        "/quiz/manage/generateQuestionPDF/**",
                        "/quiz/manage/addBatchQuestionsToQuiz/**",
                        "/quiz/manage/removeQuestionFromQuiz/**",
                        "/quiz/manage/removeAttach/**",
                        "/quiz/manage/addAttach/**",
                        "/quiz/manage/getParticipants/**",
                        "/quiz/manage/setCorrectorByQuestionMode/**",
                        "/quiz/manage/setCorrectorByStudentMode/**",
                        "/admin/user/fetchUser/**",
                        "/admin/question/subjectQuestions"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_ADVISOR.getAuthority(),
                        Role.ROLE_TEACHER.getAuthority(), Role.ROLE_SCHOOL.getAuthority(),
                        Role.ROLE_AGENT.getAuthority(), Role.ROLE_CONTENT.getAuthority(), Role.ROLE_EDITOR.getAuthority()
                )
                // QUIZ ACCESS
                .antMatchers(
                        "/quiz/manage/setPDFQuizQuestions/**",
                        "/quiz/manage/setPDFQuizInfo/**",
                        "/quiz/manage/getGradesAndBranches/**",
                        "/quiz/manage/getPDFQuizInfo/**",
                        "/quiz/manage/setPDFQuizSubjectsAndChoicesCount/**",
                        "/quiz/manage/getPDFQuizAnswerSheet/**",
                        "/quiz/manage/setPDFQuizAnswerSheet/**",
                        "/quiz/manage/getPDFQuizQuestions/**",
                        "/quiz/manage/getPDFQuizSubjects/**",
                        "/quiz/manage/edit/**",
                        "/quiz/manage/store/**",
                        "/quiz/manage/toggleVisibility/**",
                        "/quiz/manage/remove/**",
                        "/quiz/manage/updateQuestionMark/**",
                        "/quiz/manage/addQuestionToQuizzes/**",
                        "/quiz/manage/arrangeQuestions/**",
                        "/quiz/school/createHW/**"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_ADVISOR.getAuthority(),
                        Role.ROLE_SCHOOL.getAuthority(), Role.ROLE_CONTENT.getAuthority()
                )
//                .antMatchers("/comment/public/**")
//                .hasAnyAuthority(Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_CONTENT.getAuthority(), Role.ROLE_EDITOR.getAuthority())
                // EDITOR ACCESS
                .antMatchers(
                        "/request/**",
                        "/quiz/manage/changeMainMember/**",
                        "/quiz/manage/onlineStandingForceRegistry/**"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_EDITOR.getAuthority()
                )
                .antMatchers(
                        "/admin/user/removeSchools",
                        "/admin/user/getMySchools"
                        )
                .hasAnyAuthority(
                        Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(),
                        Role.ROLE_AGENT.getAuthority()
                )
                .antMatchers(
                        "/admin/user/removeStudents"
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
