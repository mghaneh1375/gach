package irysc.gachesefid.Security;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import io.jsonwebtoken.*;
import irysc.gachesefid.Exception.CustomException;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.Role;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import static irysc.gachesefid.Utility.StaticValues.SERVER_TOKEN_EXPIRATION_MSEC;
import static irysc.gachesefid.Utility.StaticValues.TOKEN_EXPIRATION_MSEC;

@Component
public class JwtTokenProvider {

    /**
     * THIS IS NOT A SECURE PRACTICE! For simplicity, we are storing a static key here. Ideally, in a
     * microservices environment, this key would be kept on a config-server.
     */
    final static private String secretKey = "{MIP0kK^PGU;l/{";
    final static private String secretSocketKey = "eFe;ek+;6{B95cU=";
    final static private String secretServerKey = "zv#x![vph,YLf8/&";

    @Value("${security.jwt.token.expire-length:3600000}")

    private static MyUserDetails myUserDetails = new MyUserDetails();

    private String getSharedKeyBytes(boolean isForSocket) {
        return Base64.getEncoder().encodeToString(isForSocket ? secretSocketKey.getBytes() : secretKey.getBytes());
    }

    public String createToken(String username, Role role) {

        Claims claims = Jwts.claims().setSubject(username);
        claims.put("auth", new SimpleGrantedAuthority(role.getAuthority()));

        Date now = new Date();
        Date validity = new Date(now.getTime() + TOKEN_EXPIRATION_MSEC);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SignatureAlgorithm.HS256, getSharedKeyBytes(false))
                .compact();
    }

    public static String createToken(PairValue... pairValues) {

        Claims claims = Jwts.claims().setSubject("main_server");

        for(PairValue pairValue : pairValues)
            claims.put(pairValue.getKey().toString(), pairValue.getValue());

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + SERVER_TOKEN_EXPIRATION_MSEC))
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encodeToString(secretServerKey.getBytes()))
                .compact();
    }

    Authentication getAuthentication(String token, boolean isForSocket) {
        UserDetails userDetails = myUserDetails.loadUserByUsername(getUsername(token, isForSocket));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public HashMap<String, Object> getClaims(String token) {

        HashMap<String, Object> output = new HashMap<>();

        Claims claims = Jwts.parser().setSigningKey(getSharedKeyBytes(true)).parseClaimsJws(token).getBody();

        output.put("_id", new ObjectId(claims.get("user_id").toString()));
        output.put("username", claims.getSubject());
        output.put("name", claims.get("name").toString());
        output.put("targets", claims.get("targets"));
        output.put("access", claims.get("access"));

        return output;
    }

    public String getUsername(String token, boolean isForSocket) {
        return Jwts.parser().setSigningKey(getSharedKeyBytes(isForSocket)).parseClaimsJws(token).getBody().getSubject();
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    boolean validateAuthToken(String token) {
        return validateToken(token, false);
    }

    //token, expiration_time
    HashMap<String, Long> validatedTokens = new HashMap<>();

    boolean validateSocketToken(String token) {
        return validateToken(token, true);
    }

    private boolean validateToken(String token, boolean isForSocket) {

        try {

            Jws<Claims> cliams = Jwts.parser().setSigningKey(getSharedKeyBytes(isForSocket)).parseClaimsJws(token);

            if (isForSocket && !cliams.getBody().get("digest").equals(
                    cliams.getBody().get("user_id") + "_" + cliams.getBody().getSubject() + "_" +
                            cliams.getBody().get("access")
            ))
                return false;

            if(isForSocket)
                validatedTokens.put(token, cliams.getBody().getExpiration().getTime());

            return true;


        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("expire " + e.getMessage());
            throw new CustomException("Expired or invalid JWT token", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
