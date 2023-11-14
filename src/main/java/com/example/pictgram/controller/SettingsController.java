package com.example.pictgram.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingsController {
	
	@GetMapping(path = "/settings")
	public String index(Model model) {
		System.out.println("settings呼び出し");
		return "settings/index";
	}

}