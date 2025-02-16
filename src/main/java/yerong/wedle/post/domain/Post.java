package yerong.wedle.post.domain;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yerong.wedle.board.domain.Board;
import yerong.wedle.comment.domain.Comment;
import yerong.wedle.common.domain.BaseTimeEntity;
import yerong.wedle.member.domain.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isAnonymous;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private boolean isHot = false;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @Builder
    public Post(String title, String content, boolean isAnonymous, Member member, Board board) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.isAnonymous = isAnonymous;
        this.board = board;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void increaseLike() {
        this.likeCount++;
        if (this.likeCount >= 10) {
            this.isHot = true;
        }
    }

    public void decreaseLike() {
        this.likeCount--;
        if (this.likeCount < 10) {
            this.isHot = false;
        }
    }
}
