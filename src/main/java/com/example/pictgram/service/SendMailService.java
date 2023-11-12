package com.example.pictgram.service;

import java.nio.charset.StandardCharsets;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;


@Service
public class SendMailService {

	@Autowired
	private JavaMailSender javaMailSender;
	
	@Value("${SPRING_MAIL_USERNAME}")
	private String springMailUsername;
	
	public void sendMail(Context context) {
		
		javaMailSender.send(new MimeMessagePreparator() {
			
			@Override
			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,
						StandardCharsets.UTF_8.name());
				helper.setFrom(springMailUsername);
				helper.setTo(springMailUsername);
				helper.setSubject((String) context.getVariable("title"));
				helper.setText(getMailBody("email", context), true);
			}
		});
	}
	
	private String getMailBody(String templateName, Context context) {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(mailTemplateResolver());
		// テンプレート名(templateName)とテンプレート情報(mailTemplateResolverで設定した情報)を元に
		// テンプレートファイルを探し、与えられたパラメータ(context)でテンプレート処理した結果を返す。
		return templateEngine.process(templateName, context);
	}
	
	private ClassLoaderTemplateResolver mailTemplateResolver() {
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		// テンプレートファイルのモード
		templateResolver.setTemplateMode(TemplateMode.HTML);
		// ファイルの場所(/src/main/resourcesからの相対パス)
		templateResolver.setPrefix("mailtemplates/");
		// 拡張子
		templateResolver.setSuffix(".html");
		// 文字コード
		templateResolver.setCharacterEncoding("UTF-8");
		// キャッシュの有無
		templateResolver.setCacheable(true);
		return templateResolver;
	}
}
