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

                if(!Authorization.isStudent(u.getString("access")))
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
        boolean auth = new JwtTokenFilter().isAuth(request);
        if(auth) {
            Document u = userService.whoAmI(request);
            if (isAdmin(request, u)) return;
        }
        throw new UnAuthException("Token is not valid");
    }

    private boolean isAdmin(HttpServletRequest request, Document u) throws NotActivateAccountException, NotAccessException {

        if (u != null) {

            if(!u.getString("status").equals("active")) {
                JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                throw new NotActivateAccountException("Account not activated");
            }

            if(!Authorization.isAdmin(u.getString("access")))
                throw new NotAccessException("Access denied");

            return true;
        }

        return false;
    }

    protected Document getAdminPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        boolean auth = new JwtTokenFilter().isAuth(request);
        if(auth) {
            Document u = userService.whoAmI(request);
            if (isAdmin(request, u)) return u;
        }

        throw new UnAuthException("Token is not valid");
    }

    protected Document getTeacherPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if(new JwtTokenFilter().isAuth(request)) {
            Document u = userService.whoAmI(request);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if(!Authorization.isTeacher(u.getString("access")))
                    throw new NotAccessException("Access denied");

                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected void getPrivilegeUserVoid(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if(new JwtTokenFilter().isAuth(request)) {
            Document u = userService.whoAmI(request);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if(Authorization.isPureStudent(u.getString("access")))
                    throw new NotAccessException("Access denied");

                return;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected Document getPrivilegeUser(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException, NotAccessException {

        if(new JwtTokenFilter().isAuth(request)) {
            Document u = userService.whoAmI(request);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                if(!u.getBoolean("level"))
                    throw new NotAccessException("Access denied");

                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    protected Document getUserWithOutCheckCompleteness(HttpServletRequest request)
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
}
