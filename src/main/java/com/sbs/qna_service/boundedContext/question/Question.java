package com.sbs.qna_service.boundedContext.question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Check;

import com.sbs.qna_service.boundedContext.user.SiteUser;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

@Data
@Entity
public class Question {
	@Id // PKEY
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	private Integer id; // 投稿ID

	@ManyToOne
	@JoinColumn(name = "parent_id") // 親ID (Self-Join)
	private Question parent;

	@Check(constraints = "depth IN (0, 1, 2)")
	@Column(nullable = false)
	private Integer depth; // 0=質問, 1=コメント, 2=コメントのコメント

	@Column(length = 200)
	private String subject;

	@Column(columnDefinition = "TEXT", nullable = false)
    private String content;

	@ManyToOne
	// 아래 모델에서 외래키만 저장이됨.
	// FetchType.EAGER로 설정하면 author 필드에 저장된 userId뿐만 아니라 전체 사용자 정보도 함께 가져옴.
	// 그러나 모든 관계를 EAGER로 설정하면 성능 저하 가능성이 있음, 따라서 필요할 때만 사용해야 함.
    @JoinColumn(name = "author_id", nullable = false) // 작성자 ID (SITE_USER 테이블 참조)
    private SiteUser author; // 作成者ID

	@Column(nullable = false, updatable = false)
	private LocalDateTime createDate;
	
	@Column(nullable = false)
	private LocalDateTime updateDate;

    // 데이터 생성 시 자동으로 현재 시간 저장
    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();
        this.updateDate = createDate;
    }
    
    // 데이터 수정 시 수정 시간 자동 갱신
    @PreUpdate
    public void preUpdate() {
        this.updateDate = LocalDateTime.now();
    }
    
}