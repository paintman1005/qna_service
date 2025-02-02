package com.sbs.qna_service.boundedContext.question;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.sbs.qna_service.boundedContext.answer.AnswerDto;
import com.sbs.qna_service.boundedContext.answer.AnswerForm;
import com.sbs.qna_service.boundedContext.user.SiteUser;
import com.sbs.qna_service.boundedContext.user.UserService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import lombok.RequiredArgsConstructor;

@RequestMapping("/question")
@RequiredArgsConstructor // 요청이 들어올 때만 객체를 생성한다.
@Controller
public class QuestionController {

	private final QuestionService questionService;

	private final UserService userService;

	@GetMapping("/list")
	// @ResponseBody 使わない理由、リターンされる”文字列”が表示される。
	public String list(Model model, @RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "kw", defaultValue = "") String kw) {
		// 페이지용 코드 작성
		Page<Question> paging = questionService.getList(page, kw);
		model.addAttribute("paging", paging);
		model.addAttribute("kw", kw);
		return "question_list";
	}

	@GetMapping(value = "/detail/{id}")
	public String detail(Model model, @PathVariable("id") Integer id, AnswerForm answerForm) {
		System.out.println("Received ID: " + id); // 로그 확인
		Question question = questionService.getQuestion(id);
		if (question == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 질문을 찾을 수 없습니다.");
		}
		List<AnswerDto> answers = questionService.getAnswersWithReplies(question.getId());

		model.addAttribute("question", question);
		model.addAttribute("answers", answers);

		return "question_detail";
	}

	/*
	 * 또 다른 방식
	 * 
	 * @GetMapping(value = "/question/detail") public String detail(Model
	 * model, @PathVariable("id") Integer id, @RequestParam int id) { return
	 * "question_detail"; }
	 */
	@PreAuthorize("isAuthenticated()")
	@GetMapping(value = "/create")
	public String questionCreate(@ModelAttribute QuestionForm questionForm) {// @ModelAttribute 명시안해도 됨.
		return "question_form";
	}

	/*
	 * 아래는 폼을 수동관리하기 용이함.
	 * 
	 * @GetMapping(value = "/create") public String questionCreate(Model model) {
	 * model.addAttribute("question", new QuestionForm()); return "question_form"; }
	 */
	@PreAuthorize("isAuthenticated()") // 로그인되지 않은 사용자는 접근 금지 principal이 null포인트에러가 나게하는 것을 미연에 방지도 함.
	@PostMapping(value = "/create")
	// Valid QuestionForm
	// questionForm값을 바인딩 할 때 유효성 체크를 해라
	public String create(@Valid QuestionForm questionForm, BindingResult bindingResult, Principal principal) {
		if (bindingResult.hasErrors()) {
			// question_form.html 실행
			// 다시 작성하라는 의미로 응답에 폼을 실어서 보냄
			return "question_form";
		}
		// principal.getName() : 글 작성자를 가져온다.
		SiteUser siteUser = userService.getUser(principal.getName());
		questionService.create(questionForm.getSubject(), questionForm.getContent(), siteUser, null);
		return "redirect:/question/list";
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping(value = "/modify/{id}")
	public String questionModify(QuestionForm questionForm, @PathVariable("id") Integer id, Principal principal) {// @ModelAttribute
		Question question = this.questionService.getQuestion(id);
		if (!question.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
		}
		questionForm.setSubject(question.getSubject());
		questionForm.setContent(question.getContent());
		
		return "question_form";
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/modify/{id}")
	public String questionModify(@Valid QuestionForm questionForm, BindingResult bindingResult, Principal principal,
			@PathVariable("id") Integer id) {
		if (bindingResult.hasErrors()) {
			return "question_form";
		}
		Question question = this.questionService.getQuestion(id);
		if (!question.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
		}
		this.questionService.modify(question, questionForm.getSubject(), questionForm.getContent());
		return String.format("redirect:/question/detail/%s", id);
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/delete/{id}")
	public String questionDelete(Principal principal, @PathVariable("id") Integer id) {
		Question question = this.questionService.getQuestion(id);
		if (!question.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
		}
		this.questionService.delete(question);
		return "redirect:/";
	}

}
