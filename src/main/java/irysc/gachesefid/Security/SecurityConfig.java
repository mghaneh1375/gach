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

import javax.servlet.http.HttpServletResponse;

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
                .antMatchers(
                        "/api/comment/public/getTopComments/**",
                        "/api/comment/public/getComments/**",
                        "/api/comment/public/getCommentsCount/**",
                        "/api/comment/public/getTeacherMarkedComments/**",
                        "/api/exchange/public/getAll/**",
                        "/api/general/getRankingList/**",
                        "/api/general/checkCert/**",
                        "/api/general/getSiteStats",
                        "/api/general/getTagsKeyVals",
                        "/api/general/fetchStates",
                        "/api/general/fetchSchoolsDigest",
                        "/api/general/fetchContentDigests",
                        "/api/general/rss",
                        "/api/profile/public/getUserProfile/**",
                        "/api/profile/public/getUserComments/**",
                        "/api/profile/public/getUserAdvisors/**",
                        "/api/profile/public/getUserTeachers/**",
                        "/api/profile/public/getUserContents/**",
                        "/api/profile/public/getUserQuizzes/**",
                )
                .permitAll()
                .antMatchers("/api/teach/admin/**").hasAnyAuthority(Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority())
                .antMatchers("/api/badge/admin/**").hasAnyAuthority(Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority())
                .antMatchers("/api/questionReport/manage/**").hasAnyAuthority(Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority())
                .antMatchers("/api/admin/config/author/**").hasAnyAuthority(Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority())
                .antMatchers("/api/admin/config/author/getAuthorsKeyVals").hasAnyAuthority(Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_CONTENT.getAuthority(), Role.ROLE_EDITOR.getAuthority())
                .antMatchers("/api/comment/public/**").hasAnyRole(Role.ROLE_ADMIN.getAuthority(), Role.ROLE_SUPER_ADMIN.getAuthority(), Role.ROLE_CONTENT.getAuthority(), Role.ROLE_EDITOR.getAuthority())
                .anyRequest()
                .authenticated()
        ;

        http.addFilterBefore(
                jwtTokenFilter,
                UsernamePasswordAuthenticationFilter.class
        );
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
