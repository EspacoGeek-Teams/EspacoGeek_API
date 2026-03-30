package com.espacogeek.geek.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.models.UserModel;

@DataJpaTest
@ActiveProfiles("test")
class UserMediaListRepositoryTest {

    @Autowired
    private UserMediaListRepository userMediaListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaCategoryRepository mediaCategoryRepository;

    private UserModel user;
    private MediaModel media;

    @BeforeEach
    void setUp() {
        MediaCategoryModel category = mediaCategoryRepository.save(new MediaCategoryModel(null, CategoryType.ANIME, null));

        UserModel newUser = new UserModel();
        newUser.setUsername("testuser");
        newUser.setEmail("test@example.com");
        newUser.setPassword("password1".getBytes());
        user = userRepository.save(newUser);

        MediaModel newMedia = new MediaModel();
        newMedia.setName("Test Media");
        newMedia.setMediaCategory(category);
        media = mediaRepository.save(newMedia);
    }

    @Test
    void persist_ShouldSaveAndRetrieveUserMediaListWithGeneratedUuid() {
        UserMediaListModel entry = new UserMediaListModel();
        entry.setUser(user);
        entry.setMedia(media);
        entry.setStatus("watching");
        entry.setProgress(5);

        UserMediaListModel saved = userMediaListRepository.save(entry);

        assertThat(saved.getId()).isNotNull();
        assertThat(userMediaListRepository.findById(saved.getId())).isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getUser().getId()).isEqualTo(user.getId());
                    assertThat(found.getMedia().getId()).isEqualTo(media.getId());
                    assertThat(found.getStatus()).isEqualTo("watching");
                    assertThat(found.getProgress()).isEqualTo(5);
                });
    }

    @Test
    void persist_WhenDuplicateUserAndMedia_ShouldViolateUniqueConstraint() {
        UserMediaListModel first = new UserMediaListModel();
        first.setUser(user);
        first.setMedia(media);
        first.setStatus("watching");
        userMediaListRepository.saveAndFlush(first);

        UserMediaListModel duplicate = new UserMediaListModel();
        duplicate.setUser(user);
        duplicate.setMedia(media);
        duplicate.setStatus("completed");

        assertThatThrownBy(() -> userMediaListRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
