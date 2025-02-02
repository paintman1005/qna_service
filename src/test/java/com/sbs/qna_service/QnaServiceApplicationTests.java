package com.sbs.qna_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import com.sbs.qna_service.boundedContext.question.Question;
import com.sbs.qna_service.boundedContext.question.QuestionRepository;
import com.sbs.qna_service.boundedContext.question.QuestionService;
import com.sbs.qna_service.boundedContext.user.SiteUser;
import com.sbs.qna_service.boundedContext.user.UserRepository;
import com.sbs.qna_service.boundedContext.user.UserService;

import jakarta.transaction.Transactional;

@SpringBootTest
class QnaServiceApplicationTests {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private QuestionService questionService;

	@Autowired // フィールド注入
	private QuestionRepository questionRepository;

	@BeforeEach // テストケースが実行する前に一回実行する
	void beforeEach() {
		// すべてのデータ削除
		questionRepository.deleteAll();
		// 痕跡削除 （次のINSERT際にIDが1に設定させるために）
		questionRepository.clearAutoIncrement();

		userRepository.deleteAll();

		userRepository.clearAutoIncrement();

		// 회원 2명 생성
		SiteUser user1 = userService.create("user1", "user1@test.com", "1234");
		SiteUser user2 = userService.create("user2", "user2@test.com", "1234");

		// 질문 생성
		Question q1 = questionService.create("질문이 머에요", "질문에 대해서 알고 싶습니다.", user1, null);
		questionRepository.save(q1); // 질문과 질문내용 보관

		Question q2 = questionService.create("스프링부트 모델 질문입니다.", "id는 자동 생성되나요", user2, null);
		questionRepository.save(q1); // 질문과 질문내용 보관
		questionRepository.save(q2);

		// コメント生成
		Question qa = questionService.create(null, "질문에 답변 장소입니다.", user1, q2.getId());

		questionRepository.save(qa);
	}

	@Test
	// @DisplayName : テストの意図を人が読みやすい形で説明
	@DisplayName("データを保存する")
	void t001() {
		SiteUser user1 = userService.getUser("user1");

		Question q = questionService.create("겨울은 춥습니다.", "맞아요. 춥습니다.", user1, null);
		assertEquals("겨울은 춥습니다.", questionRepository.findById(4).get().getSubject());
		assertEquals("맞아요. 춥습니다.", questionRepository.findById(4).get().getContent());
	}

	/*
	 * SQL SELECT * FROM question;
	 */
	@Test
	@DisplayName("findAll")
	void t002() {
		List<Question> all = questionRepository.findAll();
		// select * from table
		assertEquals(3, all.size());

		Question q = all.get(0);
		assertEquals("질문이 머에요", q.getSubject());
	}

	/*
	 * SQL SELECT * FROM question WHERE id=1;
	 */
	@Test
	@DisplayName("findById")
	void t003() {
		Optional<Question> oq = questionRepository.findById(2);
		if (oq.isPresent()) { // 値の存在を確認 有り：TRUE 無し：FALSE
			Question q = oq.get();
			assertEquals("스프링부트 모델 질문입니다.", q.getSubject());
		}
	}

	/*
	 * SQL SELECT * FROM question WHERE subject='質問１';
	 */
	@Test
	@DisplayName("findBySubject")
	void t004() {
		Question q = questionRepository.findBySubject("질문이 머에요");
		assertEquals(1, q.getId());
	}

	/*
	 * SQL SELECT * FROM question WHERE subject='質問１' and
	 * content='質問１の内容内容内容内容内容内容内容内容内容';
	 */
	@Test
	@DisplayName("findBySubjectAndContent")
	void t005() {
		Question q = questionRepository.findBySubjectAndContent("질문이 머에요", "질문에 대해서 알고 싶습니다.");
		assertEquals(1, q.getId());
	}

	/*
	 * SQL SELECT * FROM question WHERE subject LIKE '質問１';
	 */
	@Test
	@DisplayName("findBySubjectLike")
	void t006() {
		List<Question> qList = questionRepository.findBySubjectLike("질문%");
		Question q = qList.get(0);
		assertEquals("질문이 머에요", q.getSubject());
	}

	/*
	 * UPDATE question SET content = ?, create_date = ?, subject = ?, where id = ?,
	 */
	@Test
	@DisplayName("データ修正する")
	void t007() {
		// SQL SELECT * FROM question WHERE id=1;
		Optional<Question> oq = questionRepository.findById(1);
		assertTrue(oq.isPresent());
		Question q = oq.get();
		q.setSubject("修正されたタイトル");
		questionRepository.save(q);
	}

	/*
	 * DELETE FROM question where id = ?
	 */
	@Test
	@DisplayName("データ削除する")
	void t008() {
		// SQL SELECT COUNT(*) FROM question;
		assertEquals(3, questionRepository.count());
		// SQL SELECT * FROM question WHERE id=1;
		Optional<Question> oq = questionRepository.findById(1);
		assertTrue(oq.isPresent());
		Question q = oq.get();
		questionRepository.delete(q);
		assertEquals(2, questionRepository.count());
	}

	/*
	 * 特定の質問を取得する SELECTE FROM question WHERE id = ?
	 * 
	 * 質問に対するコメント保管 INSERT INTO answer SET create_date = NOW(), content = ?,
	 * question_id= ? ;
	 * 
	 */
	@Transactional
	@Test
	@DisplayName("コメント生成後、保管")
	@Rollback(false) // テストメソッドが終わった後にも、TransactionがRollbackしなくて、Commitされる。
	void t009() {
		Optional<Question> oq = questionRepository.findById(2);
		assertTrue(oq.isPresent());
		Question q = oq.get();

		SiteUser user2 = userService.getUser("user2");
		Question a = questionService.create(null, "생성되어 보관됩니다.", user2, q.getId());
		assertEquals("생성되어 보관됩니다.", a.getContent());
	}

	/*
	 * SELECT A.*, Q.* FROM answer AS A LEFT JOIN question AS Q on Q.id =
	 * A.question_id WHERE A.id=?;
	 * 
	 **/
	/*
	 * @Test
	 * 
	 * @DisplayName("特定のコメントを取得する") void t010() { Optional<Answer> qa =
	 * answerRepository.findById(1); assertTrue(qa.isPresent()); Answer a =
	 * qa.get();
	 * 
	 * assertEquals(1, a.getId()); }
	 */

	/*
	 * EAGERの場合。以下のJOINを実行される SELECT Q.*, A.* FROM answer AS Q LEFT JOIN answer AS A
	 * on Q.id = A.question_id WHERE Q.id=?;
	 * 
	 **/
	// テストコードにてはTransactionalを使用しなければならない。
	// findByIdメソッドの実行後では、DBが切れてしまうため。
	// Transactionalアノテーションを使うとメソッドが終わりまで、DBが接続されている。
	@Transactional
	@Test
	@DisplayName("質問でコメント取得")
	@Rollback(false) // テストメソッドが終わった後にも、TransactionがRollbackしなくて、Commitされる。
	void t011() {
		// SQL : SELECT * FROM question WHERE id = 2;
		Optional<Question> oq = questionRepository.findById(2);
		assertTrue(oq.isPresent());
		Question q = oq.get();
		// テスト環境では、Getして取得後にDB接続が切れてしまう。

		// Transactionalを使う理由は下のSQL実行まで、DBの接続がつないでないため。
		// または、fetchタイプを変更。ただ、お勧めしない。@OneToMany(mappedBy = "question", cascade =
		// CascadeType.REMOVE, fetch = FetchType.EAGER)
		// 下SQL : SELECT * FROM answer WHERE question_id = 2;を実行する。

		// List<Answer> answerList = q.getAnswerList();// DB接続が切れたので、answer取得失敗

		// assertEquals(2, answerList.size());
		// assertEquals("생성되어 보관합니다.", answerList.get(0).getContent());
	}

//	@Test
//	@DisplayName("대량의 테스트 데이터 만들기")
//	void t012() {
//		SiteUser user2 = userService.getUser("user2");
//		IntStream.rangeClosed(3, 4)
//				.forEach(no -> questionService.create("테스트 제목입니다. %d".formatted(no), "테스트 내용입니다. %d".formatted(no), user2));
//	}

}
