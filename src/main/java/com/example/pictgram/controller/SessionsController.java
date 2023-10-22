package com.example.pictgram.controller;

import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SessionsController {

	@GetMapping(path = "/login")
	public String index() {
		System.out.println("テストコメント セッションコンフィグのGet /login→sessions/new");
		return "sessions/new";
	}

	@GetMapping(path = "/login-failure")
	public String loginFailure(Model model) {
		model.addAttribute("hasMessage", true);
		model.addAttribute("class", "alert-danger");
		model.addAttribute("message", "Emailまたはパスワードに誤りがあります。");
		System.out.println("テストコメント セッションコンフィグのGet /login-failure→sessions/new");
		return "sessions/new";
	}

	@GetMapping(path = "/logout-complete")
	public String logoutComplete(Model model) {
		model.addAttribute("hasMessage", true);
		model.addAttribute("class", "alert-info");
		model.addAttribute("message", "ログアウトしました。");
		System.out.println("テストコメント セッションコンフィグのGet /logout-complete→layouts/complete");
		return "layouts/complete";
	}
}