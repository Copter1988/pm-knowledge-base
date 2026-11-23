package com.tpjy.controller;

import com.tpjy.common.Constants;
import com.tpjy.common.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

/** 首页：按角色跳转到各自工作台。 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (Constants.Role.ADMIN.equals(user.getRole())) {
            return "redirect:/admin/users";
        }
        if (Constants.Role.REVIEWER.equals(user.getRole())) {
            return "redirect:/reviewer/review/pending";
        }
        return "redirect:/checker/record/list";
    }
}
