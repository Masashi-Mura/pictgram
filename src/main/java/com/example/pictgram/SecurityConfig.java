package com.example.pictgram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.example.pictgram.filter.FormAuthenticationProvider;
import com.example.pictgram.repository.UserRepository;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import com.example.pictgram.entity.SocialUser;
import com.example.pictgram.entity.User;
import com.example.pictgram.entity.User.Authority;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	protected static Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	@Autowired
	private UserRepository repository;

	@Autowired
	UserDetailsService service;

	@Autowired
	private FormAuthenticationProvider authenticationProvider;

	private static final String[] URLS = { "/css/**", "/images/**", "/scripts/**", "/h2-console/**", "/favicon.ico",
			"/OneSignalSDKWorker.js" };

	/**
	* 認証から除外する
	*/
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers(URLS);
	}

	/**
	* 認証を設定する
	*/
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		// antMatchersのURLは認証不要で、それ以外は認証が必要。
		http.authorizeRequests().antMatchers("/login", "/logout-complete", "/users/new", "/user").permitAll()
				.anyRequest().authenticated()
				// ログアウト処理
				.and().logout().logoutUrl("/logout").logoutSuccessUrl("/logout-complete").clearAuthentication(true)
				.deleteCookies("JSESSIONID")
				.invalidateHttpSession(true).permitAll().and().csrf()
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				// form
				// userがログインしてない場合loginPageにリダイレクトされる
				.and().formLogin().loginPage("/login").defaultSuccessUrl("/topics").failureUrl("/login-failure")
				// oauth2
				.and().oauth2Login().loginPage("/login").defaultSuccessUrl("/topics").failureUrl("/login-failure")
				.permitAll()
				.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
						//Googleのソーシャルログイン設定
						.oidcUserService(this.oidcUserService())
						//GitHubのソーシャルログイン設定
						.userService(this.oauth2UserService())
				);
		// @formatter:on
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(authenticationProvider);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	//oidcUserServiceメソッド
	//googleから渡されたユーザー情報のemailがテーブルに無ければ新規ユーザとして登録する。
	public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
		final OidcUserService delegate = new OidcUserService();
		return (userRequest) -> {
			OidcUser oidcUser = delegate.loadUser(userRequest);
			OAuth2AccessToken accessToken = userRequest.getAccessToken();

			log.debug("accessToken={}", accessToken);

			//googleから渡されたemailがテーブルにあるか確認し、無ければテーブルに登録。
			oidcUser = new DefaultOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo());
			String email = oidcUser.getEmail();
			User user = repository.findByUsername(email);
			if (user == null) {
				user = new User(email, oidcUser.getFullName(), "", Authority.ROLE_USER);
				repository.saveAndFlush(user);
			}
			//戻り値の作成
			oidcUser = new SocialUser(oidcUser.getAuthorities(), oidcUser.getIdToken(),
					oidcUser.getUserInfo(), user.getUserId());
			return oidcUser;
		};
	}
	
	//oauth2UserServiceメソッド
	//GitHubから渡されたユーザー情報がテーブルに無ければ新規ユーザとして登録する。
	public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
		DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
		return request -> {
			OAuth2User oauth2User = delegate.loadUser(request);
			
			log.debug(oauth2User.toString());
			//githubから渡されたアカウント名がテーブルになければ新規ユーザとして登録。
			String name = oauth2User.getAttribute("login");
			User user = repository.findByUsername(name);
			System.out.println(name +"これはgithubのoauth2Userのlogin属性");
			if (user == null) {
				user = new User(name, name , "", Authority.ROLE_USER);
				repository.saveAndFlush(user);
			}
			//戻り値の作成
			SocialUser socialUser = new SocialUser(oauth2User.getAuthorities(),
					oauth2User.getAttributes(), "id", user.getUserId());
			
			return socialUser;
		};
	}

}