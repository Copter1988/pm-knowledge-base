package com.tpjy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** 检验人员（CHECKER）页面。TODO 迭代1/2 接入业务。 */
@Controller
@RequestMapping("/checker")
public class CheckerController {

    @GetMapping("/workbench")
    public String workbench() {
        return "checker/index";
    }

    @GetMapping("/new")
    public String newCheck() {
        return "checker/new";
    }

    @GetMapping("/verify")
    public String verify() {
        return "checker/verify";
    }

    @GetMapping("/records")
    public String records() {
        return "checker/records";
    }

    @GetMapping("/curve")
    public String curve() {
        return "checker/curve";
    }
}
