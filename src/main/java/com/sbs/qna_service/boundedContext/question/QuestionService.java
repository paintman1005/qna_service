package com.sbs.qna_service.boundedContext.question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.sbs.qna_service.boundedContext.DataNotFoundException;
import com.sbs.qna_service.boundedContext.answer.AnswerDto;
import com.sbs.qna_service.boundedContext.user.SiteUser;

import lombok.RequiredArgsConstructor;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionService {

	private final QuestionRepository questionRepository;

	public List<Question> findAll() {
		return questionRepository.findAll();
	}

	public Question getQuestion(Integer id) {
		Optional<Question> question = questionRepository.findById(id);
		if (question.isPresent()) {
			return question.get();
		} else {
			throw new DataNotFoundException("question not found");
		}
	}

	public List<AnswerDto> getAnswersWithReplies(Integer parentId) {
	    // depth=1 (댓글) 가져오기
	    List<Question> comments = questionRepository.findByParentIdAndDepth(parentId, 1);
	    List<AnswerDto> answerDtos = new ArrayList<>();

	    for (Question comment : comments) {
	    	AnswerDto dto = new AnswerDto();
	        dto.setComment(comment);
	        
	        // 해당 댓글의 depth=2 (대댓글) 가져오기
	        List<Question> replies = questionRepository.findByParentIdAndDepth(comment.getId(), 2);
	        dto.setReplies(replies);
	        
	        answerDtos.add(dto);
	    }
	    return answerDtos;
	}
	
	public Question create(String subject, String content, SiteUser author, Integer parentId) {
		Question question = new Question();
		question.setDepth(0);
		question.setSubject(subject);
		question.setContent(content);
		question.setAuthor(author);
		question.setCreateDate(LocalDateTime.now());
		
	    if (parentId != null) {
	        // 부모 질문(댓글)을 찾아서 설정
	        Question parentQuestion = questionRepository.findById(parentId)
	                .orElseThrow(() -> new IllegalArgumentException("Parent question not found"));
	        question.setParent(parentQuestion);
	        question.setDepth(parentQuestion.getDepth() + 1); // 부모보다 한 단계 깊은 depth 설정
	    } else {
	        question.setDepth(0); // 질문은 depth 0
	    }
		
		questionRepository.save(question);
		
		return question;
	}
	
    public void modify(Question question, String subject, String content) {
        question.setSubject(subject);
        question.setContent(content);
        question.setUpdateDate(LocalDateTime.now());
        this.questionRepository.save(question);
    }
	
    @Transactional
    public void delete(Question question) {
        // 먼저 자식(댓글, 대댓글) 데이터를 삭제
        List<Question> childQuestions = questionRepository.findByParent(question);
        for (Question child : childQuestions) {        
            questionRepository.delete(child);
        }
        
        // 모든 자식 삭제 후 부모 삭제
        questionRepository.delete(question);
    }
    
	public Page<Question> getList(int page, String kw) {
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createDate"));// 작성일자 순으로 정렬
		// page : 요청된 페이지 번호(0부터 시작)
		// 10 : 한 페이지에 표시할 데이터 갯수
		// Sort.by(sorts) : 전렬 기준, 'createDate'를 기준을 적용
		Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts)); // 한페이지에 10개씩
        Specification<Question> spec = search(kw);
        
		return questionRepository.findAll(spec, pageable);
	}
	
	private Specification<Question> search(String kw) {
	    return (Root<Question> q, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
	        query.distinct(true);
	        Predicate depthPredicate = cb.equal(q.get("depth"), 0); // 질문만 가져오기

	        if (kw == null || kw.trim().isEmpty()) {
	            return depthPredicate; // 검색어 없으면 depth=0인 질문만 필터링
	        }
	        Predicate subjectPredicate = cb.like(q.get("subject"), "%" + kw + "%");
	        Predicate contentPredicate = cb.like(q.get("content"), "%" + kw + "%");
	        Join<Question, SiteUser> userJoin = q.join("author", JoinType.LEFT);
	        Predicate authorPredicate = cb.like(userJoin.get("username"), "%" + kw + "%");

	        return cb.and(depthPredicate, cb.or(subjectPredicate, contentPredicate, authorPredicate));
	    };
    }

	public Question getOneAnswer(Integer id, Integer depth) {
		return questionRepository.findFirstByIdAndDepth(id, depth);
	}
    
}
