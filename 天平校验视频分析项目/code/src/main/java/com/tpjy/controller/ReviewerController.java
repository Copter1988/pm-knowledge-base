package com.tpjy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** 复核确认人员（REVIEWER）页面。TODO 迭代2 接入业务。 */
@Controller
@RequestMapping("/reviewer")
public class ReviewerController {

    @GetMapping("/pending")
    public String pending() {
        return "reviewer/pending";
    }

    @GetMapping("/records")
    public String records() {
        return "reviewer/records";
    }

    @GetMapping("/balance")
    public String balance() {
        return "reviewer/balance";
    }
}
