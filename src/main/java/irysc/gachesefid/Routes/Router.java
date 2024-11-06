package irysc.gachesefid.Routes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import irysc.gachesefid.Exception.InvalidFieldsException;
import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Models.Role;
import irysc.gachesefid.Security.JwtTokenFilter;
import irysc.gachesefid.Security.MyUserDetailsService;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;

@Service
public class Router {

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Autowired
    private UserService userService;

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    protected Document getUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        boolean auth = jwtTokenFilter.isAuth(request);
        if (auth) {
            Document u = userService.whoAmI(request);
            if (u != null) {
                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }
                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected ObjectId getUserId(HttpServletRequest request
    ) throws UnAuthException {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            Claims claims;

            try {
                claims = Jwts.parser()
                        .setSigningKey(myUserDetailsService.getSharedKeyBytes())
                        .parseClaimsJws(token)
                        .getBody();

                if(claims == null || !claims.containsKey("id"))
                    throw new UnAuthException("Token is not valid");
                return new ObjectId(claims.get("id").toString());
            } catch (Exception x) {
                x.printStackTrace();
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    public class UserTokenInfo {
        private ObjectId id;
        private List<String> accesses;

        public ObjectId getId() {
            return id;
        }

        public List<String> getAccesses() {
            return accesses;
        }

        public UserTokenInfo(ObjectId id, List<String> accesses) {
            this.id = id;
            this.accesses = accesses;
        }
    }
    protected UserTokenInfo getUserTokenInfo(HttpServletRequest request
    ) throws UnAuthException {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            Claims claims;

            try {
                claims = Jwts.parser()
                        .setSigningKey(myUserDetailsService.getSharedKeyBytes())
                        .parseClaimsJws(token)
                        .getBody();

                if(claims == null || !claims.containsKey("id"))
                    throw new UnAuthException("Token is not valid");

                return new UserTokenInfo(
                        new ObjectId(claims.get("id").toString()),
                        ((List<HashMap<?, ?>>)claims.get("roles")).stream().map(hashMap -> {
                            switch (Role.valueOf(hashMap.get("authority").toString().toUpperCase())) {
                                case ROLE_SUPER_ADMIN:
                                    return Access.SUPERADMIN.getName();
                                case ROLE_ADMIN:
                                    return Access.ADMIN.getName();
                                case ROLE_ADVISOR:
                                    return Access.ADVISOR.getName();
                                case ROLE_EDITOR:
                                    return Access.EDITOR.getName();
                                case ROLE_CONTENT:
                                    return Access.CONTENT.getName();
                                case ROLE_AGENT:
                                    return Access.AGENT.getName();
                                case ROLE_SCHOOL:
                                    return Access.SCHOOL.getName();
                                case ROLE_TEACHER:
                                    return Access.TEACHER.getName();
                                case ROLE_CLIENT:
                                default:
                                    return Access.STUDENT.getName();
                            }
                        }).collect(Collectors.toList())
                );
            } catch (Exception x) {
                x.printStackTrace();
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected Document getStudentUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        boolean auth = jwtTokenFilter.isAuth(request);

        if (auth) {
            Document u = userService.whoAmI(request);
            if (u != null) {

                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if (!Authorization.isStudent(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected void getAdminPrivilegeUserVoid(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        isWantedAccess(request.getHeader("Authorization"), Role.ROLE_ADMIN);
    }

    protected void getWeakAdminPrivilegeUserVoid(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        isWantedAccess(request, Access.WEAK_ADMIN.getName());
    }
    protected void getEditorPrivilegeUserVoid(HttpServletRequest request)
            throws UnAuthException, NotAccessException {
        isWantedAccess(request.getHeader("Authorization"), Role.ROLE_EDITOR);
    }

    protected Document getEditorPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isWantedAccess(request, Access.EDITOR.getName());
    }

    protected void getContentPrivilegeUserVoid(HttpServletRequest request)
            throws UnAuthException, NotAccessException {
        isWantedAccess(request.getHeader("Authorization"), Role.ROLE_CONTENT);
    }

    protected Document getAdminPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isWantedAccess(request, Access.ADMIN.getName());
    }

    protected void getSuperAdminPrivilegeUserVoid(HttpServletRequest request)
            throws UnAuthException, NotAccessException {
        isWantedAccess(request.getHeader("Authorization"), Role.ROLE_SUPER_ADMIN);
    }

    protected Document getAgentUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isWantedAccess(request, Access.AGENT.getName());
    }

    protected Document getQuizUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isWantedAccess(request, "quiz");
    }

    protected Document getSchoolUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isWantedAccess(request, Access.SCHOOL.getName());
    }

    protected Document getAdvisorUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isWantedAccess(request, Access.ADVISOR.getName());
    }

    protected Document getPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isPrivilegeUser(request);
    }

    protected Document getUserWithOutCheckCompleteness(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        boolean auth = jwtTokenFilter.isAuth(request);
        Document u;
        if (auth) {
            u = userService.whoAmI(request);
            if (u != null) {

                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected void getUserWithOutCheckCompletenessVoid(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        boolean auth = jwtTokenFilter.isAuth(request);

        Document u;
        if (auth) {
            u = userService.whoAmI(request);
            if (u != null) {

                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                return;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected Document getUserIfLogin(HttpServletRequest request) {
        boolean auth = jwtTokenFilter.isAuth(request);
        Document u;
        if (auth) {
            u = userService.whoAmI(request);
            if (u != null) {
                if (!u.getString("status").equals("active"))
                    return null;

                return u;
            }
        }

        return null;
    }

    protected void isWantedAccess(String bearerToken, Role wantedRole
    ) throws UnAuthException, NotAccessException {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(myUserDetailsService.getSharedKeyBytes())
                        .parseClaimsJws(token)
                        .getBody();
                if(claims == null || !claims.containsKey("roles"))
                    throw new UnAuthException("Token is not valid");

                if(((List<HashMap<?, ?>>)claims.get("roles"))
                        .stream().map(hashMap -> Role.valueOf(hashMap.get("authority").toString()))
                        .noneMatch(role -> Objects.equals(role, wantedRole)))
                    throw new NotAccessException("not access");

                return;
            } catch (Exception x) {
                if(x instanceof NotAccessException)
                    throw x;
                x.printStackTrace();
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    private Document isWantedAccess(HttpServletRequest request, String wantedAccess
    ) throws NotActivateAccountException, NotAccessException, UnAuthException {
        if (jwtTokenFilter.isAuth(request)) {
            Document u = userService.whoAmI(request);
            if (u != null) {

                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if (wantedAccess.equals(Access.ADMIN.getName()) &&
                        !Authorization.isAdmin(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals(Access.EDITOR.getName()) &&
                        !Authorization.isEditor(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals(Access.CONTENT.getName()) &&
                        !Authorization.isContent(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals(Access.WEAK_ADMIN.getName()) &&
                        !Authorization.isWeakAdmin(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals(Access.SCHOOL.getName()) &&
                        !Authorization.isSchool(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals(Access.ADVISOR.getName()) &&
                        !Authorization.isAdvisor(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals("quiz") &&
                        !Authorization.isSchool(u.getList("accesses", String.class)) &&
                        !Authorization.isAdvisor(u.getList("accesses", String.class)) &&
                        !Authorization.isContent(u.getList("accesses", String.class))
                )
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals(Access.TEACHER.getName()) &&
                        !Authorization.isTeacher(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");


                if (wantedAccess.equals(Access.AGENT.getName()) &&
                        !Authorization.isAgent(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");



                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    private Document isPrivilegeUser(HttpServletRequest request
    ) throws NotActivateAccountException, NotAccessException, UnAuthException {

        if (jwtTokenFilter.isAuth(request)) {
            Document u = userService.whoAmI(request);
            if (u != null) {

                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if (Authorization.isPureStudent(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected Document getUserWithAdminAccess(HttpServletRequest request,
                                              boolean checkCompleteness,
                                              boolean isPrivilege,
                                              String userId
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {

        Document user = checkCompleteness ? getUser(request) : getUserWithOutCheckCompleteness(request);

        if (isPrivilege && Authorization.isPureStudent(user.getList("accesses", String.class)))
            throw new InvalidFieldsException("Access denied");

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));

        if (userId != null && !isAdmin)
            throw new InvalidFieldsException("no access");

        if (userId != null && !ObjectId.isValid(userId))
            throw new InvalidFieldsException("invalid objectId");

        if (userId != null)
            user = userRepository.findById(new ObjectId(userId));

        if (user == null)
            throw new InvalidFieldsException("invalid userId");

        return new Document("user", user).append("isAdmin", isAdmin);
    }

    protected Document getUserWithSchoolAccess(HttpServletRequest request,
                                               boolean checkCompleteness,
                                               boolean isPrivilege,
                                               String userId
    ) throws UnAuthException, NotActivateAccountException, InvalidFieldsException {

        Document user = checkCompleteness ? getUser(request) : getUserWithOutCheckCompleteness(request);

        if (isPrivilege && Authorization.isPureStudent(user.getList("accesses", String.class)))
            throw new InvalidFieldsException("Access denied");

        boolean isAdmin = Authorization.isAdmin(user.getList("accesses", String.class));
        boolean isSchool = Authorization.isSchool(user.getList("accesses", String.class));

        if (userId != null && !isAdmin && !isSchool)
            throw new InvalidFieldsException("no access");

        if (userId != null && !ObjectId.isValid(userId))
            throw new InvalidFieldsException("invalid objectId");


        if (userId != null) {

            ObjectId oId = new ObjectId(userId);
            if(isSchool && !isAdmin &&
                    !Authorization.hasAccessToThisStudent(oId, user.getObjectId("_id"))
            )
                throw new InvalidFieldsException("Access denied");

            user = userRepository.findById(oId);
        }

        if (user == null)
            throw new InvalidFieldsException("invalid userId");

        return new Document("user", user).append("isAdmin", isSchool);
    }


    protected Document getUserWithAdvisorAccess(HttpServletRequest request,
                                               boolean weakAccess,
                                               String userId
    ) throws UnAuthException, InvalidFieldsException {

        UserTokenInfo userTokenInfo = getUserTokenInfo(request);

        boolean isAdmin = Authorization.isAdmin(userTokenInfo.getAccesses());
        boolean isAdvisor = Authorization.isAdvisor(userTokenInfo.getAccesses());
        Document user;

        if (userId != null && !isAdmin && !isAdvisor)
            throw new InvalidFieldsException("no access");

        if (userId != null && !ObjectId.isValid(userId))
            throw new InvalidFieldsException("invalid objectId");

        if (userId != null) {

            ObjectId oId = new ObjectId(userId);

            if(isAdvisor && !isAdmin &&
                    (weakAccess && !Authorization.hasWeakAccessToThisStudent(oId, userTokenInfo.getId())) ||
                    (!weakAccess && !Authorization.hasAccessToThisStudent(oId, userTokenInfo.getId()))
            )
                throw new InvalidFieldsException("Access denied");

            user = userRepository.findById(oId);
        }
        else
            user = userService.whoAmI(request);

        if (user == null)
            throw new InvalidFieldsException("invalid userId");

        return new Document("user", user).append("isAdmin", isAdvisor);
    }
}
