package com.tpjy.config;

import com.tpjy.common.Constants;
import com.tpjy.common.LoginUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** 登录与角色鉴权拦截器（轻量，代替 Spring Security）。 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        HttpSession session = req.getSession(false);
        LoginUser user = session == null ? null : (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        String uri = req.getRequestURI();
        if (uri.startsWith("/checker/") && !Constants.Role.CHECKER.equals(user.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/");
            return false;
        }
        if (uri.startsWith("/reviewer/") && !Constants.Role.REVIEWER.equals(user.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/");
            return false;
        }
        if (uri.startsWith("/admin/") && !Constants.Role.ADMIN.equals(user.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/");
            return false;
        }
        return true;
    }
}
