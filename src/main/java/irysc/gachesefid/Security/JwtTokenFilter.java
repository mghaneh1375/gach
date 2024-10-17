package irysc.gachesefid.Security;

import irysc.gachesefid.Kavenegar.utils.PairValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.StaticValues.TOKEN_EXPIRATION;

// We should use OncePerRequestFilter since we are doing a database call, there is no point in doing this more than once
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    public class ValidateToken {

        private String token;
        private long issue;

        ValidateToken(String token) {
            this.token = token;
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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    public static final ArrayList<PairValue> blackListTokens = new ArrayList<>();

    public static void removeTokenFromCache(String token) {
        blackListTokens.add(new PairValue(token, TOKEN_EXPIRATION + System.currentTimeMillis()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        isAuth(httpServletRequest);
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    public boolean isAuth(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);

        if(token != null) {
            for (PairValue blackListToken : blackListTokens) {
                if (blackListToken.getKey().equals(token))
                    return false;
            }

            try {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                return true;
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                return false;
            }
        }

        return false;
    }
}
