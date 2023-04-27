package irysc.gachesefid.Routes;

import irysc.gachesefid.Exception.*;
import irysc.gachesefid.Models.Access;
import irysc.gachesefid.Security.JwtTokenFilter;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

import static irysc.gachesefid.Main.GachesefidApplication.userRepository;

public class Router {

    private final static JwtTokenFilter JWT_TOKEN_FILTER = new JwtTokenFilter();
    @Autowired
    private UserService userService;

    protected Document getUser(HttpServletRequest request)
            throws NotActivateAccountException, NotCompleteAccountException, UnAuthException {

        boolean auth = new JwtTokenFilter().isAuth(request);

        if (auth) {
            Document u = userService.whoAmI(request);
            if (u != null) {
                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if (Authorization.isPureStudent(u.getList("accesses", String.class))) {
//                    if (!u.containsKey("pic"))
//                        throw new NotCompleteAccountException("Account not complete");
                }

                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected Document getStudentUser(HttpServletRequest request)
            throws NotActivateAccountException, NotCompleteAccountException,
            UnAuthException, NotAccessException {

        boolean auth = new JwtTokenFilter().isAuth(request);

        if (auth) {
            Document u = userService.whoAmI(request);
            if (u != null) {

                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if (!Authorization.isStudent(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (
                        (!u.containsKey("NID") && !u.containsKey("passport_no")) ||
                                !u.containsKey("pic")
                )
                    throw new NotCompleteAccountException("Account not complete");

                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected void getAdminPrivilegeUserVoid(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        isWantedAccess(request, Access.ADMIN.getName());
    }

    protected Document getAdminPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isWantedAccess(request, Access.ADMIN.getName());
    }

    protected Document getSuperAdminPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return null;
//        return isWantedAccess(request, Access.SUPERADMIN.getName());
    }

    protected Document getTeacherPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isWantedAccess(request, Access.TEACHER.getName());
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

        boolean auth = new JwtTokenFilter().isAuth(request);
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

        boolean auth = new JwtTokenFilter().isAuth(request);

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

        boolean auth = new JwtTokenFilter().isAuth(request);

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

    private Document isWantedAccess(HttpServletRequest request, String wantedAccess
    ) throws NotActivateAccountException, NotAccessException, UnAuthException {

        if (JWT_TOKEN_FILTER.isAuth(request)) {

            Document u = userService.whoAmI(request);

            if (u != null) {

                if (!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if (wantedAccess.equals(Access.ADMIN.getName()) &&
                        !Authorization.isAdmin(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals(Access.SCHOOL.getName()) &&
                        !Authorization.isSchool(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals(Access.ADVISOR.getName()) &&
                        !Authorization.isAdvisor(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if (wantedAccess.equals("quiz") &&
                        !Authorization.isSchool(u.getList("accesses", String.class)) &&
                        !Authorization.isAdvisor(u.getList("accesses", String.class))
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

        if (new JwtTokenFilter().isAuth(request)) {
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
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException, InvalidFieldsException {

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
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException, InvalidFieldsException {

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
}
