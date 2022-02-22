package irysc.gachesefid.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        JwtTokenFilter customFilter = new JwtTokenFilter(jwtTokenProvider);

        http.authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/assets/**").permitAll()
                .antMatchers(HttpMethod.GET, "/mongo/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/report/admin/**").hasAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.POST, "/api/regularQuiz/admin/**").hasAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.GET, "/api/regularQuiz/admin/**").hasAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/regularQuiz/**").hasAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.POST, "/api/openQuiz/admin/**").hasAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.GET, "/api/openQuiz/admin/**").hasAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/openQuiz/**").hasAuthority("ROLE_ADMIN")

                .antMatchers(HttpMethod.GET, "/test/**").permitAll()
                .antMatchers(HttpMethod.GET, "/admin/**").hasAuthority("ROLE_ADMIN")
                .antMatchers(HttpMethod.POST, "/api/user/signIn").permitAll()
                .antMatchers(HttpMethod.POST, "/api/user/signUp").permitAll()
                .anyRequest()
                .authenticated()
                .and().addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class)
        ;

        http.exceptionHandling().accessDeniedPage("/login");
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

    @Override
    public void configure(WebSecurity web) {
        web.ignoring()
                .antMatchers("/assets/**")
                .antMatchers(HttpMethod.GET, "/hotels/**")
                .antMatchers(HttpMethod.GET, "/ticket/**")
                .antMatchers(HttpMethod.POST, "/place/**")
                .antMatchers(HttpMethod.POST, "/api/upload/doUpload")
                .antMatchers(HttpMethod.POST, "/api/user/signIn/**")
                .antMatchers(HttpMethod.POST, "/api/user/signUp/**")
        ;
    }
}
