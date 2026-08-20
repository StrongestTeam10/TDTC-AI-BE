package com.markettwin.backend.service;

import com.markettwin.backend.domain.entity.Post;
import com.markettwin.backend.domain.entity.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * 게시판 기능
 * Post.writerId는 다른 엔티티들과 동일하게 순수 Long 컬럼이라(JPA @ManyToOne 관계
 * 없음) "작성자 검색"은 User를 서브쿼리로 걸어 writerId IN (...) 형태로 구현함.
 */
public final class PostSpecs {

    private PostSpecs() {
    }

    public static Specification<Post> isNotNotice() {
        return (root, query, cb) -> cb.isFalse(root.get("notice"));
    }

    public static Specification<Post> marketCodeEquals(String marketCode) {
        return (root, query, cb) -> cb.equal(root.get("marketCode"), marketCode);
    }

    public static Specification<Post> categoryCodeEquals(String categoryCode) {
        return (root, query, cb) -> cb.equal(root.get("categoryCode"), categoryCode);
    }

    public static Specification<Post> keywordMatches(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
            Predicate contentMatch = cb.like(cb.lower(root.get("content")), pattern);

            Subquery<Long> writerSubquery = query.subquery(Long.class);
            Root<User> userRoot = writerSubquery.from(User.class);
            writerSubquery.select(userRoot.get("userId"))
                    .where(cb.like(cb.lower(userRoot.get("name")), pattern));
            Predicate writerMatch = root.get("writerId").in(writerSubquery);

            return cb.or(titleMatch, contentMatch, writerMatch);
        };
    }
}
