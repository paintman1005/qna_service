package com.sbs.qna_service.boundedContext.answer;

import java.util.List;

import com.sbs.qna_service.boundedContext.question.Question;

import lombok.Data;

@Data
public class AnswerDto {
	private Question comment; // 댓글 (depth=1)
	private List<Question> replies; // 대댓글 (depth=2)
}