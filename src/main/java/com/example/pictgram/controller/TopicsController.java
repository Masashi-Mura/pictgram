package com.example.pictgram.controller;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.lang.Boolean;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Base64Utils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.pictgram.entity.Topic;
import com.example.pictgram.entity.UserInf;
import com.example.pictgram.form.TopicForm;
import com.example.pictgram.form.UserForm;
import com.example.pictgram.repository.TopicRepository;
import com.example.pictgram.entity.Favorite;
import com.example.pictgram.form.FavoriteForm;
import com.example.pictgram.entity.Comment;
import com.example.pictgram.form.CommentForm;

import java.util.Locale;
import org.springframework.context.MessageSource;


@Controller
public class TopicsController {

	@Autowired
	private MessageSource messageSource;
	
	protected static Logger log = LoggerFactory.getLogger(TopicsController.class);
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private TopicRepository repository;
	
	@Autowired
	private HttpServletRequest request;
	
	@Value("${image.local:false}")
	private String imageLocal;

//以下、トピック一覧表示関係
	//ログイン、投稿、お気に入り操作後の画面
	@GetMapping(path = "/topics")
	public String index(Principal principal, Model model) throws IOException {
		Authentication authentication = (Authentication) principal;
		UserInf user = (UserInf) authentication.getPrincipal();
		
		//Topicテーブルの全レコードをList<TopicForm> listに変換しmodelに格納
		//動作:Topicテーブルの全レコードをList<Topic> topicsに格納
		//     1行分ずつgetTopicメソッドでTopic→TopicFormに変換しList<TopicForm> listに格納
		//     modelにlistを格納し、view側に共有
		List<Topic> topics = repository.findAllByOrderByUpdatedAtDesc();
		List<TopicForm> list = new ArrayList<>();
		for (Topic entity : topics) {
			TopicForm form = getTopic(user, entity);
			list.add(form);
		}
		model.addAttribute("list", list);
		System.out.println("テストコメント トピックコントローラ トピック一覧をlistで返す");
		return "topics/index";
	}
	
	//TopicをTopicFormにマッピングするメソッド
	//①imageData②UserForm user,
	//③List<FavoriteForm> favorites, ④FavoriteForm favorite
	//⑤List<CommentForm> comments,
	//はそれぞれModelMapper以外でマッピングを行う。
	//引数のUserInfは④でログインuserIdとTopic entityのList<Favorite> のuserIdが等しいか確認時に使用
	public TopicForm getTopic(UserInf user, Topic entity) throws FileNotFoundException, IOException {
		modelMapper.getConfiguration().setAmbiguityIgnored(true);
		modelMapper.typeMap(Topic.class, TopicForm.class).addMappings(mapper -> mapper.skip(TopicForm::setUser));
		modelMapper.typeMap(Topic.class, TopicForm.class).addMappings(mapper -> mapper.skip(TopicForm::setFavorites));
		modelMapper.typeMap(Topic.class, TopicForm.class).addMappings(mapper -> mapper.skip(TopicForm::setComments));
		modelMapper.typeMap(Favorite.class, FavoriteForm.class).addMappings(mapper -> mapper.skip(FavoriteForm::setTopic));
		
		boolean isImageLocal = false;
		if (imageLocal != null) {
			isImageLocal = Boolean.parseBoolean(imageLocal);
		}
		TopicForm form = modelMapper.map(entity, TopicForm.class);  //なぜここでTopicFormのuserがコピーされる？
		
		//①Topic entityのpathを元にイメージデータを読み込み、TopicForm formのimageDataに格納
		//動作:pathからInputStreamで配列indataに画像データを書き込みByteArrayOutputStream osに格納
		//     StringBuilder dataに画像の追加情報とosを格納し、TopicForm formのimageDataに格納
		if (isImageLocal) {
			try (InputStream is = new FileInputStream(new File(entity.getPath()));
					ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				byte[] indata = new byte[10240 * 16];
				int size;
				    //isから配列indata.lengthバイト読み込み、配列indataの0から入力。
				while ((size = is.read(indata, 0, indata.length)) > 0) {
					//indataの値を0の位置からsizeバイト読み込んでos(ByteArrayOutputStrea)に格納
					os.write(indata, 0, size);  
				}
				StringBuilder data = new StringBuilder();
				data.append("data:");
				data.append(getMimeType(entity.getPath()));
				data.append(";base64,");
				
				data.append(new String(Base64Utils.encode(os.toByteArray()), "ASCII"));
				form.setImageData(data.toString());
				}
		}
		
		//②Topic entityのUser userを、TopicForm formのUserForm userにコピー
		UserForm userForm = modelMapper.map(entity.getUser(), UserForm.class);
		form.setUser(userForm);
		
		//③Topic entityのList＜Favorite＞favoritesを、TopicForm formのList<FavoriteForm> favoritesにコピー
		List<FavoriteForm> favorites = new ArrayList<FavoriteForm>();
		for (Favorite favoriteEntity : entity.getFavorites()) { 
			FavoriteForm favorite = modelMapper.map(favoriteEntity,  FavoriteForm.class);
			favorites.add(favorite);
			//④ログインuserIdとTopic entityのList<Favorite> のuserIdが等しいFavoriteのみ
			//FavoriteForm favoriteにマッピングしてTopicForm formのFavorite favoriteに格納
			if (user.getUserId().equals(favoriteEntity.getUserId())) {
				form.setFavorite(favorite);  //TopicForm formのfavoriteのデータ有で、お気に入り登録済のボタンを表示
			}
		}
		form.setFavorites(favorites);
		
		//⑤Topic entityのList<Comment> commentsを、TopicForm formのList<CommentForm> commentsにコピー
		List<CommentForm> comments = new ArrayList<CommentForm>();
		for (Comment commentEntity : entity.getComments()) {
			CommentForm comment = modelMapper.map(commentEntity, CommentForm.class);
			comments.add(comment);
		}
		form.setComments(comments);
		
		System.out.println("テストコメント トピックコントローラ TopicをTopicFormに変換");
		return form;
	}
	
	private String getMimeType(String path) {
		String extension = FilenameUtils.getExtension(path);
		String mimeType = "image/";
		switch (extension) {
		case "jpg":
		case "jpeg":
			mimeType += "jpeg";
			break;
		case "png":
			mimeType += "png";
			break;
		case "gif":
			mimeType += "gif";
			break;
		}
		System.out.println("テストコメント トピックコントローラ getMimeTypeメソッド");
		return mimeType;
	}

//以下、画像投稿関係
	//画像投稿画面表示(TopicFormクラスをViewに共有)
	@GetMapping(path = "/topics/new")
	public String newTopic(Model model) {
		model.addAttribute("form", new TopicForm());
		System.out.println("テストコメント トピックコントローラ TopicFormをViewに共有");
		return "topics/new";
	}
	
	//画像投稿後の処理(画像を保存し、テーブルTopicにレコードを保存）
	@RequestMapping(value = "/topic", method = RequestMethod.POST)
	public String create(Principal principal, @Validated @ModelAttribute("form") TopicForm form,
			BindingResult result, Model model, @RequestParam MultipartFile image, 
			RedirectAttributes redirAttrs, Locale locale) throws IOException {
		//Validateチェック
		if (result.hasErrors()) {
			model.addAttribute("hasMessage", true);
			model.addAttribute("class", "alert-danger");
			//メッセージ：投稿に失敗しました。
			model.addAttribute("message", messageSource.getMessage("topics.create.flash.1", new String[] {}, locale));
			System.out.println("テストコメント トピックコントローラ 画像保存失敗");
			return "topics/new";
		}
		
		//プロパティ情報より画像を保存するか否か判定
		boolean isImageLocal = false;
		if(imageLocal != null) {
			isImageLocal = Boolean.parseBoolean(imageLocal);
			System.out.println("テストコメント トピックコントローラ 画像保存設定の読み込み");
		}

		//画像を保存し、テーブルTopicにuserId,保存先のパス,Descriptionを保存
		Topic entity = new Topic();
		Authentication authentication = (Authentication)principal;
		UserInf user = (UserInf) authentication.getPrincipal();
		entity.setUserId(user.getUserId());
		File destFile = null;
		if (isImageLocal) {
			//
			destFile = saveImageLocal(image,entity);
			entity.setPath(destFile.getAbsolutePath());
		} else {
			entity.setPath("");
		}
		entity.setDescription(form.getDescription());
		repository.saveAndFlush(entity);
		
		//フラッシュコメントの設定
		redirAttrs.addFlashAttribute("hasMessage", true);
		redirAttrs.addFlashAttribute("class", "alert-info");
		//メッセージ：投稿に成功しました
		redirAttrs.addFlashAttribute("message", messageSource.getMessage("topics.create.flash.2", new String[] {}, locale));
		System.out.println("テストコメント トピックコントローラ 画像保存とTopicテーブルにレコード追加");
		return "redirect:/topics";
	}
	
	//画像保存と、保存先のパスを返す
	private File saveImageLocal(MultipartFile image, Topic entity) throws IOException {
		File uploadDir = new File("/uploads");
		uploadDir.mkdir();
		
		String uploadsDir = "/uploads/";
		String realPathToUploads = request.getServletContext().getRealPath(uploadsDir);
		if (!new File(realPathToUploads).exists()) {
			new File(realPathToUploads).mkdir();
		}
		String fileName = image.getOriginalFilename();
		File destFile = new File(realPathToUploads, fileName);
		image.transferTo(destFile);
		System.out.println("テストコメント トピックコントローラ 画像保存と保存先パスを返すメソッド");
		return destFile;
		
	}
}
