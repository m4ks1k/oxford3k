package ru.golovkin.oxford3000.dictionary.ut;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.golovkin.oxford3000.dictionary.dao.DictionaryDao;
import ru.golovkin.oxford3000.dictionary.model.Language;
import ru.golovkin.oxford3000.dictionary.model.ServiceUser;
import ru.golovkin.oxford3000.dictionary.model.Session;
import ru.golovkin.oxford3000.dictionary.model.SessionBuilder;
import ru.golovkin.oxford3000.dictionary.model.SessionState;
import ru.golovkin.oxford3000.dictionary.model.Term;
import ru.golovkin.oxford3000.dictionary.model.TermReference;
import ru.golovkin.oxford3000.dictionary.model.TestDictionary;
import ru.golovkin.oxford3000.dictionary.model.TestObjectBuilder;
import ru.golovkin.oxford3000.dictionary.model.TestType;
import ru.golovkin.oxford3000.dictionary.model.UserDictionaryEntry;
import ru.golovkin.oxford3000.dictionary.model.UserSource;
import ru.golovkin.oxford3000.dictionary.service.DictionaryService;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class DictionaryDaoTest {
    @Autowired
    DictionaryDao sut;

    @Autowired
    EntityManager entityManager;
    private final String sessionId = "sessionId";
    private final TestObjectBuilder New = new TestObjectBuilder();
    private final String appId = "appId1";
    private final String userId = "userId1";
    private final String russianWord = "кошка";
    private Long serviceUserId;
    private final String englishWord = "cat";
    private Session session;
    private ServiceUser serviceUser;

    @Test
    void should_return_state_INITIAL_when_calling_getSessionState_and_no_records_in_session_state_table_for_sessionId_specified() {
        session = sut.getSessionState(New.yaRequest(appId).withSessionId(sessionId).please().getSession());

        assertEquals(SessionState.INITIAL, session.getState());
    }

    @Test
    @Transactional
    void should_return_service_user_in_session_when_calling_getSessionState_and_user_id_is_not_empty_in_session_and_there_are_record_in_user_table_with_user_id_provided() {
        serviceUser = createDefaultUser(null, userId, "");
        entityManager.persist(serviceUser);

        session = sut.getSessionState(New.yaRequest(appId).withUserId(userId).withSessionId(sessionId).please().getSession());

        assertEquals(serviceUser, session.getServiceUser());
    }
    @Test
    @Transactional
    void should_return_service_user_in_session_when_calling_getSessionState_and_user_id_is_empty_in_session_and_there_are_record_in_user_table_with_application_id_provided() {
        serviceUser = createDefaultUser(null, null, appId);
        entityManager.persist(serviceUser);

        session = sut.getSessionState(New.yaRequest(appId).withSessionId(sessionId).please().getSession());

        assertEquals(serviceUser, session.getServiceUser());
    }

    @Test
    @Transactional
    void should_add_record_with_sessionId_and_state_specified_in_session_state_table_and_in_user_table_when_calling_updateSessionState() {
        serviceUser = createDefaultUser(null, null, appId);
        session = newSession().withState(SessionState.PENDING_NAME).please();

        sut.updateSessionState(session);

        TypedQuery<Session> query = entityManager.createQuery("select s "
            + " from Session s "
            + " where s.sessionId = :sessionId ", Session.class);
        query.setParameter("sessionId", sessionId);

        assertEquals(SessionState.PENDING_NAME, query.getSingleResult().getState());

        TypedQuery<ServiceUser> query1 = entityManager.createQuery("select u "
            + " from ServiceUser u "
            + " where u.extAppId = :extAppId ", ServiceUser.class);
        query1.setParameter("extAppId", appId);

        assertEquals(serviceUser, query1.getSingleResult());
    }


    @Test
    @Transactional
    void should_update_name_user_table_when_calling_updateSessionState_and_name_is_null_in_user_table_and_name_is_provided_in_input_param() {
        serviceUser = createDefaultUser(null, null, appId);
        entityManager.persist(serviceUser);
        session = newSession().withState(SessionState.PENDING_NAME).please();
        entityManager.persist(session);

        serviceUser.setName("Name");
        sut.updateSessionState(session);

        TypedQuery<ServiceUser> query = entityManager.createQuery("select u "
            + " from ServiceUser u "
            + " where u.extAppId = :extAppId ", ServiceUser.class);
        query.setParameter("extAppId", appId);

        assertEquals("Name", query.getSingleResult().getName());
    }

    @Test
    @Transactional
    void should_return_false_when_calling_wordExistsInUserDictionary_and_no_term_in_table_term() {
        serviceUser = createDefaultUser("Name", userId, appId);
        entityManager.persist(serviceUser);
        serviceUserId = serviceUser.getId();

        assertFalse(sut.wordExistsInUserDictionary(russianWord, Language.RUSSIAN, serviceUserId));
    }

    @Test
    @Transactional
    void should_return_false_when_calling_wordExistsInDictionary_and_term_in_table_term_but_no_references_in_term_reference() {
        serviceUser = createDefaultUser("Name", userId, appId);
        entityManager.persist(serviceUser);
        Term term = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(term);
        serviceUserId = serviceUser.getId();

        assertFalse(sut.wordExistsInUserDictionary(russianWord, Language.RUSSIAN, serviceUserId));

    }

    @Test
    @Transactional
    void should_return_false_when_calling_wordExistsInDictionary_and_term_in_table_term_exists_reference_in_term_reference_but_no_reference_in_user_dictionary_entry() {
        serviceUser = createDefaultUser("Name", userId, appId);
        entityManager.persist(serviceUser);
        Term term = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(term);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, term, englishTerm);
        entityManager.persist(termReference);
        serviceUserId = serviceUser.getId();

        assertFalse(sut.wordExistsInUserDictionary(russianWord, Language.RUSSIAN, serviceUserId));

    }

    @Test
    @Transactional
    void should_return_false_when_calling_wordExistsInDictionary_and_term_in_table_term_exists_reference_in_term_reference_exists_reference_in_user_dictionary_entry_but_language_ENGLISH() {
        serviceUser = createDefaultUser("Name", userId, appId);
        entityManager.persist(serviceUser);
        Term term = new Term(null, russianWord, Language.ENGLISH);
        entityManager.persist(term);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, term, englishTerm);
        entityManager.persist(termReference);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);
        entityManager.persist(userDictionaryEntry);
        serviceUserId = serviceUser.getId();

        assertFalse(sut.wordExistsInUserDictionary(russianWord, Language.RUSSIAN, serviceUserId));

    }

    @Test
    @Transactional
    void should_return_true_when_calling_wordExistsInDictionary_and_term_in_table_term_exists_reference_in_term_reference_exists_reference_in_user_dictionary_entry_and_language_RUSSIAN() {
        serviceUser = createDefaultUser("Name", userId, appId);
        entityManager.persist(serviceUser);
        Term term = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(term);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, term, englishTerm);
        entityManager.persist(termReference);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);
        entityManager.persist(userDictionaryEntry);
        serviceUserId = serviceUser.getId();

        assertTrue(sut.wordExistsInUserDictionary(russianWord, Language.RUSSIAN, serviceUserId));

    }

    @Test
    @Transactional
    void should_return_false_when_calling_wordExistsInGlobalDictionary_and_no_term_in_table_term() {
        assertFalse(sut.wordExistsInGlobalDictionary(russianWord, Language.RUSSIAN));
    }

    @Test
    @Transactional
    void should_return_false_when_calling_wordExistsInGlobalDictionary_and_term_in_table_term_but_no_references_in_term_reference() {
        Term term = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(term);

        assertFalse(sut.wordExistsInGlobalDictionary(russianWord, Language.RUSSIAN));

    }

    @Test
    @Transactional
    void should_return_false_when_calling_wordExistsInGlobalDictionary_and_term_in_table_term_exists_reference_in_term_reference_but_language_ENGLISH() {
        Term term = new Term(null, russianWord, Language.ENGLISH);
        entityManager.persist(term);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, term, englishTerm);
        entityManager.persist(termReference);

        assertFalse(sut.wordExistsInGlobalDictionary(russianWord, Language.RUSSIAN));

    }

    @Test
    @Transactional
    void should_return_true_when_calling_wordExistsInGlobalDictionary_and_term_in_table_term_exists_reference_in_term_reference_and_language_RUSSIAN() {
        Term term = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(term);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, term, englishTerm);
        entityManager.persist(termReference);

        assertTrue(sut.wordExistsInGlobalDictionary(russianWord, Language.RUSSIAN));
    }

    private ServiceUser createDefaultUser(String name, String userId, String appId) {
        return new ServiceUser(null, name, UserSource.YANDEX_ALICE, userId, appId);
    }

    private ServiceUser createDefaultUser() {
        return createDefaultUser("Name", userId, appId);
    }

    @Test
    @Transactional
    void should_add_both_terms_with_specified_languages_with_reference_between_them_and_reference_to_user_when_calling_addTermWithTranslationInUserDictionary_and_both_terms_not_exists() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm = new Term(null, russianWord, Language.RUSSIAN);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        TermReference termReference = new TermReference(null, russianTerm, englishTerm);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);

        sut.addTermWithTranslationInUserDictionary(russianWord, Language.RUSSIAN, englishWord, Language.ENGLISH, serviceUser);

        TypedQuery<UserDictionaryEntry> query = entityManager.createQuery(
            "select e from UserDictionaryEntry e "
                + "join fetch e.termReference r "
                + "join fetch e.user u "
                + "join fetch r.term t "
                + "join fetch r.termReference tr "
                + "where t.term = :word and u.id = :user ", UserDictionaryEntry.class);
        query.setParameter("word", russianWord.toLowerCase());
        query.setParameter("user", serviceUser.getId());
        assertEquals(userDictionaryEntry, query.getSingleResult());
    }

    @Test
    @Transactional
    void should_add_term_and_term_reference_and_user_dictionary_entry_when_calling_addTermWithTranslationInUserDictionary_and_no_term_and_no_term_reference_and_no_user_dictionary_entry_but_translation_exists() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm = new Term(null, russianWord, Language.RUSSIAN);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, russianTerm, englishTerm);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);

        sut.addTermWithTranslationInUserDictionary(russianWord, Language.RUSSIAN, englishWord, Language.ENGLISH, serviceUser);

        TypedQuery<UserDictionaryEntry> query = entityManager.createQuery(
            "select e from UserDictionaryEntry e "
                + "join fetch e.termReference r "
                + "join fetch e.user u "
                + "join fetch r.term t "
                + "join fetch r.termReference tr "
                + "where t.term = :word and u.id = :user ", UserDictionaryEntry.class);
        query.setParameter("word", russianWord.toLowerCase());
        query.setParameter("user", serviceUser.getId());
        assertEquals(userDictionaryEntry, query.getSingleResult());
    }

    @Test
    @Transactional
    void should_add_translation_and_term_reference_and_user_dictionary_entry_when_calling_addTermWithTranslationInUserDictionary_and_no_translation_and_no_term_reference_and_no_user_dictionary_entry_but_term_exists() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(russianTerm);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        TermReference termReference = new TermReference(null, russianTerm, englishTerm);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);

        sut.addTermWithTranslationInUserDictionary(russianWord, Language.RUSSIAN, englishWord, Language.ENGLISH, serviceUser);

        TypedQuery<UserDictionaryEntry> query = entityManager.createQuery(
            "select e from UserDictionaryEntry e "
                + "join fetch e.termReference r "
                + "join fetch e.user u "
                + "join fetch r.term t "
                + "join fetch r.termReference tr "
                + "where t.term = :word and u.id = :user ", UserDictionaryEntry.class);
        query.setParameter("word", russianWord.toLowerCase());
        query.setParameter("user", serviceUser.getId());
        assertEquals(userDictionaryEntry, query.getSingleResult());
    }

    @Test
    @Transactional
    void should_add_user_dictionary_entry_when_calling_addTermWithTranslationInUserDictionary_and_no_user_dictionary_entry_but_term_and_translation_and_term_reference_exist() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(russianTerm);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, russianTerm, englishTerm);
        entityManager.persist(termReference);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);

        sut.addTermWithTranslationInUserDictionary(russianWord, Language.RUSSIAN, englishWord, Language.ENGLISH, serviceUser);

        TypedQuery<UserDictionaryEntry> query = entityManager.createQuery(
            "select e from UserDictionaryEntry e "
                + "join fetch e.termReference r "
                + "join fetch e.user u "
                + "join fetch r.term t "
                + "join fetch r.termReference tr "
                + "where t.term = :word and u.id = :user ", UserDictionaryEntry.class);
        query.setParameter("word", russianWord.toLowerCase());
        query.setParameter("user", serviceUser.getId());
        assertEquals(userDictionaryEntry, query.getSingleResult());
    }

    @Test
    @Transactional
    void should_do_nothing_when_calling_addTermWithTranslationInUserDictionary_and_term_and_translation_and_term_reference_and_user_dictionary_entry_exist() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(russianTerm);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, russianTerm, englishTerm);
        entityManager.persist(termReference);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);
        entityManager.persist(userDictionaryEntry);

        sut.addTermWithTranslationInUserDictionary(russianWord, Language.RUSSIAN, englishWord, Language.ENGLISH, serviceUser);

        TypedQuery<UserDictionaryEntry> query = entityManager.createQuery(
            "select e from UserDictionaryEntry e "
                + "join fetch e.termReference r "
                + "join fetch e.user u "
                + "join fetch r.term t "
                + "join fetch r.termReference tr "
                + "where t.term = :word and u.id = :user ", UserDictionaryEntry.class);
        query.setParameter("word", russianWord.toLowerCase());
        query.setParameter("user", serviceUser.getId());
        assertEquals(userDictionaryEntry, query.getSingleResult());
    }

    @Test
    @Transactional
    void return_zero_when_no_record_in_user_dictionary_entry_for_user_specified_when_calling_dictionarySize() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);

        assertEquals(0, sut.getDictionarySize(serviceUser));
    }


    @Test
    @Transactional
    void return_2_when_2_records_exist_in_user_dictionary_entry_for_user_specified_when_calling_dictionarySize() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(russianTerm);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, russianTerm, englishTerm);
        entityManager.persist(termReference);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);
        entityManager.persist(userDictionaryEntry);
        russianTerm = new Term(null, russianWord + "ц", Language.RUSSIAN);
        entityManager.persist(russianTerm);
        englishTerm = new Term(null, englishWord + "w", Language.ENGLISH);
        entityManager.persist(englishTerm);
        termReference = new TermReference(null, russianTerm, englishTerm);
        entityManager.persist(termReference);
        userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);
        entityManager.persist(userDictionaryEntry);

        assertEquals(2, sut.getDictionarySize(serviceUser));
    }

    private SessionBuilder newSession() {
        return New.session(sessionId).withUser(serviceUser);
    }

    @Test
    @Transactional
    void should_return_english_term_when_calling_getNextRandomTermToTest_with_test_type_ENGLISH_and_exist_linked_records_in_user_dictionary_entry_term_and_term_reference() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(russianTerm);
        Term englishTerm = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm);
        TermReference termReference = new TermReference(null, russianTerm, englishTerm);
        entityManager.persist(termReference);
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);
        entityManager.persist(userDictionaryEntry);

        Term randomTerm = sut.getNextRandomTermToTest(serviceUser, TestType.ENGLISH, TestDictionary.USER);

        assertThat(randomTerm).isEqualTo(englishTerm);
    }

    @Test
    @Transactional
    void should_return_one_of_english_terms_when_calling_getNextRandomTermToTest_with_test_type_ENGLISH_and_test_dictionary_common_and_two_term_references_exist_with_linked_terms() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm1 = new Term(null, russianWord, Language.RUSSIAN);
        entityManager.persist(russianTerm1);
        Term englishTerm1 = new Term(null, englishWord, Language.ENGLISH);
        entityManager.persist(englishTerm1);
        TermReference termReference1 = new TermReference(null, englishTerm1, russianTerm1);
        entityManager.persist(termReference1);
        Term russianTerm2 = new Term(null, russianWord + "ж", Language.RUSSIAN);
        entityManager.persist(russianTerm2);
        Term englishTerm2 = new Term(null, englishWord + "z", Language.ENGLISH);
        entityManager.persist(englishTerm2);
        TermReference termReference2 = new TermReference(null, englishTerm2, russianTerm2);
        entityManager.persist(termReference2);

        Term randomTerm = sut.getNextRandomTermToTest(serviceUser, TestType.ENGLISH, TestDictionary.COMMON);

        assertThat(randomTerm).isIn(englishTerm1, englishTerm2);
    }

    @Test
    @Transactional
    void should_return_one_of_russian_terms_when_calling_getNextRandomTermToTest_with_test_type_RUSSIAN_and_test_dictionary_user() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term russianTerm1 = new Term(null, russianWord, Language.RUSSIAN);
        Term englishTerm1 = new Term(null, englishWord, Language.ENGLISH);
        Term russianTerm2 = new Term(null, russianWord + "ц", Language.RUSSIAN);
        Term englishTerm2 = new Term(null, englishWord + "w", Language.ENGLISH);
        TermReference termReference1 = new TermReference(null, russianTerm1, englishTerm1);
        TermReference termReference2 = new TermReference(null, russianTerm2, englishTerm2);
        UserDictionaryEntry userDictionaryEntry1 = new UserDictionaryEntry(null, serviceUser, termReference1, 0, 0);
        UserDictionaryEntry userDictionaryEntry2 = new UserDictionaryEntry(null, serviceUser, termReference2, 10, 10);
        entityManager.persist(russianTerm1);
        entityManager.persist(englishTerm1);
        entityManager.persist(termReference1);
        entityManager.persist(userDictionaryEntry1);
        entityManager.persist(russianTerm2);
        entityManager.persist(englishTerm2);
        entityManager.persist(termReference2);
        entityManager.persist(userDictionaryEntry2);

        Term randomTerm = sut.getNextRandomTermToTest(serviceUser, TestType.RUSSIAN, TestDictionary.USER);

        assertThat(randomTerm).isIn(russianTerm1, russianTerm2);
    }

    @Test
    @Transactional
    void should_return_empty_list_when_calling_findTranslation_and_no_term_exists_with_term_reference() {

        assertEquals(Collections.emptyList(), sut.findTranslations("some word", Language.ENGLISH));
    }


    @Test
    @Transactional
    void should_return_list_of_two_terms_when_calling_findTranslation_and_exist_term_referenced_by_2_other_terms() {
        Term term = new Term(null, "cat", Language.ENGLISH);
        Term validTerm1 = new Term(null, "кошка", Language.RUSSIAN);
        Term validTerm2 = new Term(null, "кот", Language.RUSSIAN);
        TermReference validTermReference1 = new TermReference(null, term, validTerm1);
        TermReference validTermReference2 = new TermReference(null, validTerm2, term);
        Term anotherTerm = new Term(null, "dog", Language.ENGLISH);
        Term invalidTerm = new Term(null, "собака", Language.ENGLISH);
        TermReference invalidTermReference = new TermReference(null, anotherTerm, invalidTerm);
        entityManager.persist(term);
        entityManager.persist(validTerm1);
        entityManager.persist(validTerm2);
        entityManager.persist(anotherTerm);
        entityManager.persist(invalidTerm);
        entityManager.persist(validTermReference1);
        entityManager.persist(validTermReference2);
        entityManager.persist(invalidTermReference);

        assertEquals(Set.of(
            validTerm1,
            validTerm2
        ), Set.copyOf(sut.findTranslations("cat", Language.ENGLISH)));
    }

    @Test
    @Transactional
    void should_add_records_to_user_dictionary_entry_when_calling_addTermFromGlobalToUserDictionary() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term term = new Term(null, "cat", Language.ENGLISH);
        Term validTerm1 = new Term(null, "кошка", Language.RUSSIAN);
        Term validTerm2 = new Term(null, "кот", Language.RUSSIAN);
        TermReference validTermReference1 = new TermReference(null, term, validTerm1);
        TermReference validTermReference2 = new TermReference(null, validTerm2, term);
        entityManager.persist(term);
        entityManager.persist(validTerm1);
        entityManager.persist(validTerm2);
        entityManager.persist(validTermReference1);
        entityManager.persist(validTermReference2);

        sut.addTermFromGlobalToUserDictionary("cat", Language.ENGLISH, serviceUser);

        TypedQuery<TermReference> query = entityManager
            .createQuery("select r from UserDictionaryEntry e join e.termReference r "
                + " where e.user.id = :user", TermReference.class);
        query.setParameter("user", serviceUser.getId());
        assertEquals(Set.of(validTermReference1, validTermReference2),
            Set.copyOf(query.getResultList())
        );
    }

    @Test
    @Transactional
    void should_increment_SUCCESS_COUNT_and_TOTAL_COUNT_in_user_dictionary_entry_when_calling_updateTestResult_with_SUCCESS() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term term = new Term(null, "cat", Language.ENGLISH);
        Term validTerm1 = new Term(null, "кошка", Language.RUSSIAN);
        Term validTerm2 = new Term(null, "кот", Language.RUSSIAN);
        TermReference validTermReference1 = new TermReference(null, term, validTerm1);
        TermReference validTermReference2 = new TermReference(null, validTerm2, term);
        UserDictionaryEntry entry1 = new UserDictionaryEntry(null, serviceUser, validTermReference1, 10, 11);
        UserDictionaryEntry entry2 = new UserDictionaryEntry(null, serviceUser, validTermReference2, 20, 20);
        entityManager.persist(term);
        entityManager.persist(validTerm1);
        entityManager.persist(validTerm2);
        entityManager.persist(validTermReference1);
        entityManager.persist(validTermReference2);
        entityManager.persist(entry1);
        entityManager.persist(entry2);

        sut.updateTestResult(serviceUser, "cat", Language.ENGLISH, DictionaryService.SUCCESS);

        entityManager.refresh(entry1);
        entityManager.refresh(entry2);
        assertEquals(11, entry1.getSuccessCount());
        assertEquals(12, entry1.getTotalCount());
        assertEquals(21, entry2.getSuccessCount());
        assertEquals(21, entry2.getTotalCount());
    }


    @Test
    @Transactional
    void should_add_record_in_user_dictionary_entry_when_calling_updateTestResult_with_FAIL_and_no_term_reference_user_dictionary() {
        serviceUser = createDefaultUser();
        entityManager.persist(serviceUser);
        Term term = new Term(null, "cat", Language.ENGLISH);
        Term validTerm1 = new Term(null, "кошка", Language.RUSSIAN);
        Term validTerm2 = new Term(null, "кот", Language.RUSSIAN);
        TermReference validTermReference1 = new TermReference(null, term, validTerm1);
        TermReference validTermReference2 = new TermReference(null, validTerm2, term);
        UserDictionaryEntry entry1 = new UserDictionaryEntry(null, serviceUser, validTermReference1, 10, 11);
        entityManager.persist(term);
        entityManager.persist(validTerm1);
        entityManager.persist(validTerm2);
        entityManager.persist(validTermReference1);
        entityManager.persist(validTermReference2);
        entityManager.persist(entry1);

        sut.updateTestResult(serviceUser, "cat", Language.ENGLISH, DictionaryService.FAIL);

        entityManager.refresh(entry1);
        assertEquals(10, entry1.getSuccessCount());
        assertEquals(12, entry1.getTotalCount());
        TypedQuery<UserDictionaryEntry> query = entityManager.createQuery(
            "select e from UserDictionaryEntry e where e.user = :user and e.termReference = :ref",
            UserDictionaryEntry.class);
        query.setParameter("user", serviceUser);
        query.setParameter("ref", validTermReference2);
        assertEquals(new UserDictionaryEntry(null, serviceUser, validTermReference2, 0, 1),
            query.getSingleResult()
            );
    }

    @Test
    @Transactional
    void should_return_75_when_calling_getUserDictionarySizePercentile_and_user_has_more_words_in_dictionary_than_other() {
        serviceUser = createDefaultUser();
        ServiceUser user2 = createDefaultUser("Another user", userId + "1", appId + "1");
        ServiceUser user3 = createDefaultUser("Another user", userId + "2", appId + "2");
        ServiceUser user4 = createDefaultUser("Another user", userId + "3", appId + "3");
        entityManager.persist(serviceUser);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.persist(user4);
        Term term = new Term(null, "cat", Language.ENGLISH);
        Term validTerm1 = new Term(null, "кошка", Language.RUSSIAN);
        Term term2 = new Term(null, "doc", Language.ENGLISH);
        Term validTerm2 = new Term(null, "собака", Language.RUSSIAN);
        TermReference validTermReference1 = new TermReference(null, term, validTerm1);
        TermReference validTermReference2 = new TermReference(null, term2, validTerm2);
        UserDictionaryEntry entry1 = new UserDictionaryEntry(null, serviceUser, validTermReference1, 10, 11);
        UserDictionaryEntry entry2 = new UserDictionaryEntry(null, serviceUser, validTermReference2, 10, 11);
        UserDictionaryEntry entry3 = new UserDictionaryEntry(null, user2, validTermReference1, 10, 11);
        UserDictionaryEntry entry4 = new UserDictionaryEntry(null, user3, validTermReference1, 10, 11);
        UserDictionaryEntry entry5 = new UserDictionaryEntry(null, user4, validTermReference1, 10, 11);
        entityManager.persist(term);
        entityManager.persist(validTerm1);
        entityManager.persist(term2);
        entityManager.persist(validTerm2);
        entityManager.persist(validTermReference1);
        entityManager.persist(validTermReference2);
        entityManager.persist(entry1);
        entityManager.persist(entry2);
        entityManager.persist(entry3);
        entityManager.persist(entry4);
        entityManager.persist(entry5);

        int percentile = sut.getUserDictionarySizePercentile(serviceUser);

        assertEquals(75, percentile);
    }

    @Test
    @Transactional
    void return_false_when_calling_checkUserNameExistsOnDevice_and_no_record_in_users_with_such_name_userId_appId() {
        ServiceUser user = createDefaultUser("коля", userId, appId + "1");
        entityManager.persist(user);
        session = newSession().please();

        assertFalse(sut.checkUserNameExistsOnDevice(New.yaRequest(appId).withUserId(userId).please().getSession(), "вася"));
    }

    @Test
    @Transactional
    void return_false_when_calling_checkUserNameExistsOnDevice_and_no_record_in_users_with_such_name_appId_and_empty_user_id() {
        ServiceUser user = createDefaultUser("коля", null, appId);
        entityManager.persist(user);
        session = newSession().please();

        assertFalse(sut.checkUserNameExistsOnDevice(New.yaRequest(appId).please().getSession(), "вася"));
    }

    @Test
    @Transactional
    void return_true_when_calling_checkUserNameExistsOnDevice_and_no_record_in_users_with_such_name_userId_appId() {
        ServiceUser user = createDefaultUser("ВАСЯ", userId, appId + "1");
        entityManager.persist(user);

        assertTrue(sut.checkUserNameExistsOnDevice(New.yaRequest(appId).withUserId(userId).please().getSession(), "вася"));
    }

    @Test
    @Transactional
    void return_true_when_calling_checkUserNameExistsOnDevice_and_no_record_in_users_with_such_name_appId_and_empty_user_id() {
        ServiceUser user = createDefaultUser("ВАСЯ", null, appId);
        entityManager.persist(user);

        assertTrue(sut.checkUserNameExistsOnDevice(New.yaRequest(appId).please().getSession(), "вася"));
    }

    @Test
    @Transactional
    void should_add_new_record_in_user_with_given_name_appId_and_null_userId_when_calling_addNewUser_and_userId_is_empty() {
        serviceUser = createDefaultUser("Василий Иванович", null, appId);
        entityManager.persist(serviceUser);

        ServiceUser newServiceUser = sut.addNewUser(New.yaRequest(appId).please().getSession(), "Василий Петрович");

        assertEquals(createDefaultUser("Василий Петрович", null, appId), newServiceUser);
    }

    @Test
    @Transactional
    void should_add_new_record_in_user_with_given_name_appId_and_userId_when_calling_addNewUser_and_userId_is_not_empty() {
        serviceUser = createDefaultUser("Василий Иванович", userId, appId);
        entityManager.persist(serviceUser);

        ServiceUser newServiceUser = sut.addNewUser(New.yaRequest(appId).withUserId(userId).please().getSession(), "Василий Петрович");

        assertEquals(createDefaultUser("Василий Петрович", userId, appId), newServiceUser);
    }
}
