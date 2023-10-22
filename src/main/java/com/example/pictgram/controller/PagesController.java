package com.example.pictgram.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PagesController {

    @RequestMapping("/")
    public String index() {
        System.out.println("テストコメント ページコントローラ /→pages/index");
        return "pages/index";
    }
}