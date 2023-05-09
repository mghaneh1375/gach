package irysc.gachesefid.Security;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import irysc.gachesefid.Exception.CustomException;
import irysc.gachesefid.Utility.Cache;
import javafx.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.StaticValues.TOKEN_EXPIRATION;

// We should use OncePerRequestFilter since we are doing a database call, there is no point in doing this more than once
public class JwtTokenFilter extends OncePerRequestFilter {

    public class ValidateToken {

        private String token;
        private long issue;
        private Authentication auth;

        public Authentication getAuth() {
            return auth;
        }

        ValidateToken(String token, Authentication auth) {
            this.token = token;
            this.auth = auth;
            this.issue = System.currentTimeMillis();
        }

        public boolean isValidateYet() {
            return System.currentTimeMillis() - issue <= TOKEN_EXPIRATION; // 1 week
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof String)
                return o.equals(token);
            return false;
        }
    }

    private JwtTokenProvider jwtTokenProvider;
    public static final ArrayList<ValidateToken> validateTokens = new ArrayList<>();
    public static final ArrayList<Pair<String, Long>> blackListTokens = new ArrayList<>();

    public static void removeTokenFromCache(String token) {

        for(int i = 0; i < validateTokens.size(); i++) {
            if(validateTokens.get(i).token.equals(token)) {
                blackListTokens.add(new Pair<>(token, TOKEN_EXPIRATION + validateTokens.get(i).issue));
                validateTokens.remove(i);
                return;
            }
        }
    }

    public JwtTokenFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public JwtTokenFilter() {
        jwtTokenProvider = new JwtTokenProvider();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        String token = jwtTokenProvider.resolveToken(httpServletRequest);

        boolean validate = false;

        if(token != null) {

            for(Pair<String, Long> itr : blackListTokens) {
                if(itr.getKey().equals(token)) {
                    if(itr.getValue() < System.currentTimeMillis()) {
                        blackListTokens.remove(itr);
                        break;
                    }
                    SecurityContextHolder.clearContext();
                    httpServletResponse.sendError(HttpStatus.FORBIDDEN.value(), "Token invalid");
                    return;
                }
            }

            for (ValidateToken v : validateTokens) {
                if(v.equals(token)) {

                    if(v.isValidateYet()) {
                        System.out.println("Hitt token is JwtTokenFilter");
                        validate = true;
                        SecurityContextHolder.getContext().setAuthentication(v.getAuth());
                    }
                    else
                        validateTokens.remove(v);

                    break;
                }
            }
        }

        if(!validate) {

            try {
                if (token != null && jwtTokenProvider.validateToken(token)) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    validateTokens.add(new ValidateToken(token, auth));
                }
            } catch (CustomException ex) {
                //this is very important, since it guarantees the user is not authenticated at all
                SecurityContextHolder.clearContext();
                httpServletResponse.sendError(ex.getHttpStatus().value(), ex.getMessage());
                return;
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    public boolean isAuth(HttpServletRequest request) {

        String token = jwtTokenProvider.resolveToken(request);

        if(token != null) {

            for (ValidateToken v : validateTokens) {
                if(v.equals(token)) {

                    if(v.isValidateYet())
                        return true;
                    else
                        validateTokens.remove(v);

                    break;
                }
            }
        }

        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                validateTokens.add(new ValidateToken(token, auth));
                return true;
            }
        } catch (CustomException ex) {
            SecurityContextHolder.clearContext();
            return false;
        }

        return false;
    }
}
