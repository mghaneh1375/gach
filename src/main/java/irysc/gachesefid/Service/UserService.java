package irysc.gachesefid.Service;

import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Exception.CustomException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Security.JwtTokenProvider;
import irysc.gachesefid.Utility.Cache;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Models.Role;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.userRepository;
import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;
import static irysc.gachesefid.Utility.StaticValues.TOKEN_EXPIRATION;

@Service
public class UserService {

    private static ArrayList<Cache> cachedToken = new ArrayList<>();
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public boolean isOldPassCorrect(String password, String dbPassword) {
        return passwordEncoder.matches(password, dbPassword);
    }

    public String getEncPass(String username, String pass) {
        deleteFromCache(username);
        return passwordEncoder.encode(pass);
    }

    public String getEncPass(String pass) {
        return passwordEncoder.encode(Utility.convertPersianDigits(pass));
    }

    public void deleteFromCache(String username) {

        for (int i = 0; i < cachedToken.size(); i++) {
            if (((PairValue) (cachedToken.get(i)).getKey()).getKey().equals(username)) {
                cachedToken.remove(i);
                return;
            }
        }
    }

    public String toggleStatus(ObjectId userId) {

        Document user = userRepository.findById(userId);
        if(user == null)
            return null;

        switch (user.getString("status")) {
            case "active":
                user.put("status", "deActive");
                break;
            case "deActive":
                user.put("status", "active");
                break;
            default:
                return null;
        }

        userRepository.updateOne(
                eq("_id", userId),
                set("status", user.getString("status"))
        );

        return Utility.generateSuccessMsg("newStatus", user.getString("status"));
    }

    public String signIn(String username, String password, boolean checkPass
    ) throws NotActivateAccountException {

        try {

            if(checkPass) {
                PairValue p = new PairValue(username, password);

//                for (int i = 0; i < cachedToken.size(); i++) {
//                    if (cachedToken.get(i).equals(p)) {
//                        if (cachedToken.get(i).checkExpiration())
//                            return (String) cachedToken.get(i).getValue();
//
//                        cachedToken.remove(i);
//                        break;
//                    }
//                }
            }

            Document user = userRepository.findByUnique(username, false);

            if (DEV_MODE || !checkPass) {
                if (user == null)
                    throw new CustomException("?????? ???????????? ?? ???? ?????????????? ???????????? ??????.", HttpStatus.UNPROCESSABLE_ENTITY);
            } else {
                if (user == null || !passwordEncoder.matches(password, user.getString("password")))
                    throw new CustomException("?????? ???????????? ?? ???? ?????????????? ???????????? ??????.", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (!user.getString("status").equals("active"))
                throw new NotActivateAccountException("?????????? ?????? ?????????????? ?????? ??????.");

            username = user.containsKey("phone") ?
                    user.getString("phone") :
                    user.getString("mail");

            String token = jwtTokenProvider.createToken(username, (user.getBoolean("level")) ? Role.ROLE_ADMIN : Role.ROLE_CLIENT);

            if(checkPass)
                cachedToken.add(new Cache(TOKEN_EXPIRATION, token, new PairValue(user.getString("username"), password)));

            return Utility.generateSuccessMsg("token", token, new PairValue("user", UserController.isAuth(user)));

        } catch (AuthenticationException x) {
            throw new CustomException("?????? ???????????? ?? ???? ?????????????? ???????????? ??????.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public void logout(String token) {
        for (int i = 0; i < cachedToken.size(); i++) {
            if (cachedToken.get(i).getValue().equals(token)) {
                cachedToken.remove(i);
                return;
            }
        }
    }

    public Document whoAmI(HttpServletRequest req) {
        return userRepository.findByUsername(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken(req)));
    }

    public Document whoAmI(String token) {
        return userRepository.findByUsername(jwtTokenProvider.getUsername(token));
    }

}
