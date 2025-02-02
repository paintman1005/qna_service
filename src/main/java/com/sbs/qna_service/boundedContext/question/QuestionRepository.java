package com.sbs.qna_service.boundedContext.question;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.transaction.Transactional;

public interface QuestionRepository extends JpaRepository<Question, Integer>{//QuestionエンティティのPKEYがIntegerなので、指定する。

	Question findBySubject(String subject);

	Question findBySubjectAndContent(String subject, String content);

	List<Question> findBySubjectLike(String subject);
	
	Question findFirstByIdAndDepth(Integer Id, Integer depth);
	
	List<Question> findByParentIdAndDepth(Integer parentId, Integer depth);
	
	Page<Question> findAll(Specification<Question> spec, Pageable pageable);

	List<Question> findByParent(Question question);
	
	@Modifying //INSERT,UPDATE,DELETEのような、データ変更作業にて使用
	// nativeQuery = trueしか、MySQL Query使用可能
	@Transactional
	@Query(value = "ALTER TABLE question AUTO_INCREMENT = 1", nativeQuery = true)
	void clearAutoIncrement();

}
