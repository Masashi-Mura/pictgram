package com.example.pictgram.controller;

import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

@Controller
public class SessionsController {
	
	@Autowired
	private MessageSource messageSource;
	

	@GetMapping(path = "/login")
	public String index() {
		System.out.println("テストコメント セッションコンフィグのGet /login→sessions/new");
		return "sessions/new";
	}

	@GetMapping(path = "/login-failure")
	public String loginFailure(Model model, Locale locale) {
		model.addAttribute("hasMessage", true);
		model.addAttribute("class", "alert-danger");
		//メッセージ：Emailまたはパスワードに誤りがあります
		model.addAttribute("message", messageSource.getMessage("sessions.loginFailure.flash", new String[] {}, locale));
		System.out.println("テストコメント セッションコンフィグのGet /login-failure→sessions/new");
		return "sessions/new";
	}

	@GetMapping(path = "/logout-complete")
	public String logoutComplete(Model model, Locale locale) {
		model.addAttribute("hasMessage", true);
		model.addAttribute("class", "alert-info");
		//メッセージ：ログアウトしました
		model.addAttribute("message", messageSource.getMessage("sessions.logoutComplete.flash", new String[] {}, locale));
		System.out.println("テストコメント セッションコンフィグのGet /logout-complete→layouts/complete");
		return "layouts/complete";
	}
}