package irysc.gachesefid.Routes;

import irysc.gachesefid.Exception.NotAccessException;
import irysc.gachesefid.Exception.NotActivateAccountException;
import irysc.gachesefid.Exception.NotCompleteAccountException;
import irysc.gachesefid.Exception.UnAuthException;
import irysc.gachesefid.Security.JwtTokenFilter;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Models.Access;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

public class Router {

    @Autowired
    private UserService userService;

    private final static JwtTokenFilter JWT_TOKEN_FILTER = new JwtTokenFilter();

    protected Document getUser(HttpServletRequest request)
            throws NotActivateAccountException, NotCompleteAccountException, UnAuthException {

        boolean auth = new JwtTokenFilter().isAuth(request);

        if(auth) {
            Document u = userService.whoAmI(request);
            if (u != null) {
                if(!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if(u.getString("access").equals(Access.STUDENT.getName())) {
                    if (
                            (!u.containsKey("NID") && !u.containsKey("passport_no")) ||
                                    !u.containsKey("pic")
                    )
                        throw new NotCompleteAccountException("Account not complete");
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

        if(auth) {
            Document u = userService.whoAmI(request);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if(!Authorization.isStudent(u.getList("accesses", String.class)))
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

    protected void getPrivilegeUserVoid(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        isPrivilegeUser(request);
    }

    protected Document getPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {
        return isPrivilegeUser(request);
    }

    protected Document getUserWithOutCheckCompleteness(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        boolean auth = new JwtTokenFilter().isAuth(request);
        System.out.println(auth);
        Document u;
        if(auth) {
            u = userService.whoAmI(request);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
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
        if(auth) {
            u = userService.whoAmI(request);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
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
        if(auth) {
            u = userService.whoAmI(request);
            if (u != null) {

                if(!u.getString("status").equals("active"))
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

                if(wantedAccess.equals(Access.TEACHER.getName()) &&
                        !Authorization.isTeacher(u.getList("accesses", String.class)))
                    throw new NotAccessException("Access denied");

                if(wantedAccess.equals(Access.ADMIN.getName()) &&
                        !Authorization.isAdmin(u.getList("accesses", String.class)))
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
}
