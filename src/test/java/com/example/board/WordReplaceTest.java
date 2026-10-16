package com.example.board;

import com.example.board.entity.Post;
import com.example.board.repository.PostRepository;
import com.example.board.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wordtestdb;DB_CLOSE_DELAY=-1")
class WordReplaceTest {

    @Autowired
    PostService service;

    @Autowired
    PostRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private Long save(String title, String content) {
        Post p = new Post();
        p.setTitle(title);
        p.setName("kim");
        p.setContent(content);
        return repository.save(p).getId();
    }

    @Test
    void 검색은_대소문자를_무시한다() {
        Long lower = save("kfc 치킨", "내용");
        Long upper = save("제목", "KFC 후기");
        save("무관", "무관");

        assertThat(service.searchIdsByWord("kfc")).containsExactly(lower, upper);
    }

    @Test
    void 치환은_제목과_내용을_모두_바꾸고_건수를_반환한다() {
        Long id = save("kfc 제목", "kfc 내용 kfc");

        int changed = service.replaceWord(List.of(id), "kfc", "bhc");

        Post p = repository.findById(id).orElseThrow();
        assertThat(changed).isEqualTo(1);
        assertThat(p.getTitle()).isEqualTo("bhc 제목");
        assertThat(p.getContent()).isEqualTo("bhc 내용 bhc");
    }

    @Test
    void 내용이_실제로_바뀌지_않은_글은_건수에_포함하지_않는다() {
        Long changedId = save("제목", "kfc 내용");
        Long sameId = save("제목", "KFC 내용만 대문자"); // 검색엔 걸리지만 치환 대상 아님

        int changed = service.replaceWord(List.of(changedId, sameId), "kfc", "bhc");

        assertThat(changed).isEqualTo(1);
        assertThat(repository.findById(sameId).orElseThrow().getContent()).isEqualTo("KFC 내용만 대문자");
    }

    @Test
    void 존재하지_않는_id는_건너뛴다() {
        Long id = save("kfc", "내용");

        int changed = service.replaceWord(List.of(id, 99999L), "kfc", "bhc");

        assertThat(changed).isEqualTo(1);
    }

    @Test
    void 퍼센트와_언더바는_일반_문자로_검색한다() {
        Long pct = save("할인 100%", "내용");
        save("a_b 아님 axb", "내용");

        assertThat(service.searchIdsByWord("%")).containsExactly(pct);
        assertThat(service.searchIdsByWord("a_b")).hasSize(1);
    }
}
