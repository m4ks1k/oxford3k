package ru.golovkin.oxford3000.dictionary.ut;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_ENG_TRANSLATION;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_NAME;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_NEW_TERM;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_NEW_USER_NAME;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_RUS_TRANSLATION;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_TEST_DICTIONARY;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_TEST_RESPONSE;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_TEST_TYPE_CHOICE;
import static ru.golovkin.oxford3000.dictionary.model.SessionState.PENDING_USER_NAME_TO_SWITCH;

import java.util.Arrays;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.golovkin.oxford3000.dictionary.dao.DictionaryDao;
import ru.golovkin.oxford3000.dictionary.model.Language;
import ru.golovkin.oxford3000.dictionary.model.ServiceUser;
import ru.golovkin.oxford3000.dictionary.model.Session;
import ru.golovkin.oxford3000.dictionary.model.SessionBuilder;
import ru.golovkin.oxford3000.dictionary.model.SessionState;
import ru.golovkin.oxford3000.dictionary.model.Term;
import ru.golovkin.oxford3000.dictionary.model.TestDictionary;
import ru.golovkin.oxford3000.dictionary.model.TestObjectBuilder;
import ru.golovkin.oxford3000.dictionary.model.TestType;
import ru.golovkin.oxford3000.dictionary.model.UserSource;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YARequestMarkup;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceRequest;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceResponse;
import ru.golovkin.oxford3000.dictionary.service.DictionaryService;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
@FieldDefaults( level = AccessLevel.PRIVATE)
public class DictionaryServiceUT {
    @MockBean
    DictionaryDao dictionaryDao;
    @Autowired
    DictionaryService sut;
    @Mock
    Term term;
    private YandexAliceRequest yaRequest;
    private final String sessionId = "sessionId";
    private final TestObjectBuilder New = new TestObjectBuilder();
    private final String userId = "userId";
    private final String appId = "app1";
    private ServiceUser serviceUser;
    private YandexAliceResponse yaResponse;
    private Session session;
    @Mock
    private Term newTerm;

    @Test
    void should_greet_and_ask_for_name_when_state_is_not_PENDING_NAME_and_user_name_is_empty() {
        yaRequest = New.yaRequest(appId).withNewSession(sessionId).please();
        serviceUser = createServiceUser(null, null);
        session = newSession().please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Привет! Я знаю три тысячи самых важных английских слов по версии Оксфордского университета и помогу вам их выучить. Как вас зовут?").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NAME).withUser(serviceUser).please());
    }

    private SessionBuilder newSession() {
        return New.session(sessionId).withUser(serviceUser);
    }

    @Test
    void should_save_name_in_settings_suggest_to_add_new_words_in_dictionary_and_set_state_PENDING_NEW_TERM_when_state_PENDING_NAME_and_no_words_in_dict_and_FIO_defined_in_NLU() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("меня зовут иван петрович")
            .with(New.entity("YANDEX.FIO").from(2).to(4).withValue("{\"first_name\": \"иван\", \"patronymic_name\": \"петрович\"}").please())
            .please();
        serviceUser = createServiceUser(null);
        session = newSession().withState(PENDING_NAME).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Рада познакомиться, Иван Петрович. "
                + "Ваш словарь пуст. Вы можете добавить в него слова, которые хотите выучить. "
                + "Скажите одну из фраз: добавь русское слово или добавь английское слово. Затем произнесите слово, и я добавлю его в ваш словарь. "
                + "Например: добавь русское слово собака или добавь английское слово dog. "
                + "Либо можем начать проверку по общему словарю. Для этого скажите: начинаем проверку. ").please(),
            yaResponse);
        assertEquals("Иван Петрович", serviceUser.getName());
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NEW_TERM).please());
    }

    @Test
    void should_save_name_in_settings_suggest_to_add_new_words_in_dictionary_and_set_state_PENDING_NEW_TERM_when_state_PENDING_NAME_and_no_words_in_dict_and_user_called_its_name() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Name").please();
        serviceUser = createServiceUser(null);
        session = newSession().withState(PENDING_NAME).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Рада познакомиться, Name. "
                + "Ваш словарь пуст. Вы можете добавить в него слова, которые хотите выучить. "
                + "Скажите одну из фраз: добавь русское слово или добавь английское слово. Затем произнесите слово, и я добавлю его в ваш словарь. "
                + "Например: добавь русское слово собака или добавь английское слово dog. "
                + "Либо можем начать проверку по общему словарю. Для этого скажите: начинаем проверку. ").please(),
            yaResponse);
        assertEquals("Name", serviceUser.getName());
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NEW_TERM).please());
    }

    @Test
    void should_greet_and_suggest_to_add_new_words_in_dictionary_and_set_state_PENDING_NEW_TERM_when_state_is_INITIAL_and_user_name_not_empty_and_no_words_in_dict() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Name").please();
        serviceUser = createServiceUser("Максим");
        session = newSession().please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDictionarySize(serviceUser)).thenReturn(0L);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Привет, Максим! "
                + "Я знаю три тысячи самых важных английских слов по версии Оксфордского университета и помогу вам их выучить. "
                + "Ваш словарь пуст. Вы можете добавить в него слова, которые хотите выучить. "
                + "Скажите одну из фраз: добавь русское слово или добавь английское слово. Затем произнесите слово, и я добавлю его в ваш словарь. "
                + "Например: добавь русское слово собака или добавь английское слово dog. "
                + "Либо можем начать проверку по общему словарю. Для этого скажите: начинаем проверку. ").please(),
            yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NEW_TERM).please());
    }

    @Test
    void should_tell_about_command_help_when_state_PENDING_NEW_TERM_and_command_unknown() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("не понятная фраза").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Я пока не понимаю, что мне делать. Скажите: \"расскажи, что ты понимаешь\", "
            + " чтобы я рассказала про команды, на которые я умею отвечать.").please(), yaResponse);
    }

    @Test
    void should_tell_help_when_command_is_tell_what_you_understand() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("расскажи что ты понимаешь").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Вот, какие команды я понимаю."
            + "Скажите, \"добавь русское слово\" или \"добавь английское слово\" и произнесите слово, чтобы добавить его в свой словарь."
            + "Скажите: \"начинаем проверку\", чтобы я начала вас спрашивать слова."
            + "Скажите: \"останови проверку\", чтобы я перестала проверять слова.").please(), yaResponse);
    }

    @Test
    void should_ask_for_translation_and_set_state_PENDING_ENG_TRANSLATION_when_state_PENDING_NEW_TERM_and_command_starts_with_add_russian_word_and_no_such_word_nor_in_user_dictionary_neither_in_global_dictionary() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Добавь русское слово кошка").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.wordExistsInGlobalDictionary("кошка", Language.RUSSIAN)).thenReturn(false);
        when(dictionaryDao.wordExistsInUserDictionary("кошка", Language.RUSSIAN, 1L)).thenReturn(false);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("В общем словаре нет такого слова, но я могу добавить, если вы знаете перевод. Как оно переводится на английский язык?").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_ENG_TRANSLATION)
            .withWord("кошка").withLanguage(Language.RUSSIAN).please());
    }

    @Test
    void should_tell_refine_request_and_set_state_PENDING_ENG_TRANSLATION_when_state_PENDING_NEW_TERM_and_command_starts_with_add_russian_word() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Добавь русское слово").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Вы не произнесли слово, которое нужно добавить. Нужно было сказать, к примеру, \"добавь русское слово помидор\".").please(), yaResponse);
    }

    @Test
    void should_tell_refine_request_and_set_state_PENDING_ENG_TRANSLATION_when_state_PENDING_NEW_TERM_and_command_starts_with_add_english_word() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Добавь английское слово").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Вы не произнесли слово, которое нужно добавить. Нужно было сказать, к примеру, \"добавь английское слово potato\".").please(), yaResponse);
    }

    @Test
    void should_respond_that_word_already_exists_in_user_dictionary_when_state_PENDING_NEW_TERM_and_command_starts_with_add_russian_word_and_word_exists_in_user_dictionary() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Добавь русское слово кошка").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.wordExistsInGlobalDictionary("кошка", Language.RUSSIAN)).thenReturn(false);
        when(dictionaryDao.wordExistsInUserDictionary("кошка", Language.RUSSIAN, 1L)).thenReturn(true);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Слово кошка уже есть в вашем словаре. Давайте добавим другое.").please(), yaResponse);
    }

    @Test
    void should_respond_that_word_already_exists_in_user_dictionary_when_state_PENDING_NEW_TERM_and_command_starts_with_add_english_word_and_word_exists_in_user_dictionary() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Добавь английское слово cat").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.wordExistsInGlobalDictionary("cat", Language.ENGLISH)).thenReturn(false);
        when(dictionaryDao.wordExistsInUserDictionary("cat", Language.ENGLISH, 1L)).thenReturn(true);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Слово cat уже есть в вашем словаре. Давайте добавим другое.").please(), yaResponse);
    }

    @Test
    void should_respond_that_word_is_known_and_call_addWordFromGlobalToUserDictionary_when_state_PENDING_NEW_TERM_and_command_starts_with_add_english_word_and_word_exists_in_global_dictionary() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Добавь английское слово cat").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.wordExistsInGlobalDictionary("cat", Language.ENGLISH)).thenReturn(true);
        when(dictionaryDao.wordExistsInUserDictionary("cat", Language.ENGLISH, 1L)).thenReturn(false);
        when(dictionaryDao.findTranslations("cat", Language.ENGLISH)).thenReturn(Arrays.asList(
           new Term(null, "кошка", Language.RUSSIAN), new Term(null, "кот", Language.RUSSIAN)
        ));

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Я знаю такое слово, оно переводится на русский как кошка или кот. Добавила в ваш словарь.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).addTermFromGlobalToUserDictionary("cat", Language.ENGLISH,
            serviceUser);
    }

    @Test
    void should_respond_that_word_is_known_and_call_addWordFromGlobalToUserDictionary_when_state_PENDING_NEW_TERM_and_command_starts_with_add_russian_word_and_word_exists_in_global_dictionary() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Добавь русское слово кошка").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.wordExistsInGlobalDictionary("кошка", Language.RUSSIAN)).thenReturn(true);
        when(dictionaryDao.wordExistsInUserDictionary("кошка", Language.RUSSIAN, 1L)).thenReturn(false);
        when(dictionaryDao.findTranslations("кошка", Language.RUSSIAN)).thenReturn(
            Collections.singletonList(
                new Term(null, "cat", Language.ENGLISH)
            ));

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Я знаю такое слово, оно переводится на английский как cat. Добавила в ваш словарь.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).addTermFromGlobalToUserDictionary("кошка", Language.RUSSIAN, serviceUser);
    }
    @Test
    void should_call_addTermWithTranslationInDictionary_with_source_term_language_RUSSIAN_and_set_state_PENDING_NEW_TERM_when_command_is_non_empty_and_session_state_PENDING_ENG_TRANSLATION_and_getPendingWordInsert_returns_russian_word() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("cat").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_ENG_TRANSLATION).withWord("кошка").withLanguage(Language.RUSSIAN).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Слово добавлено в ваш словарь.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NEW_TERM).please());
        verify(dictionaryDao, atLeastOnce()).addTermWithTranslationInUserDictionary("кошка", Language.RUSSIAN, "cat", Language.ENGLISH, serviceUser);
    }

    @Test
    void should_ask_for_translation_and_set_state_PENDING_RUS_TRANSLATION_when_state_PENDING_NEW_TERM_and_command_starts_with_add_englsh_word_and_no_such_word_nor_in_user_dictionary_neither_in_global_dictionary() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("добавь английское слово cat").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.wordExistsInGlobalDictionary("cat", Language.ENGLISH)).thenReturn(false);
        when(dictionaryDao.wordExistsInUserDictionary("cat", Language.ENGLISH, 1L)).thenReturn(false);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("В общем словаре нет такого слова, но я могу добавить, если вы знаете перевод. Как оно переводится на русский язык?").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_RUS_TRANSLATION)
            .withWord("cat").withLanguage(Language.ENGLISH).please());
    }

    @Test
    void should_call_addTermWithTranslationInDictionary_with_source_term_language_ENGLIGH_and_set_state_PENDING_NEW_TERM_when_command_is_non_empty_and_session_state_PENDING_RUS_TRANSLATION_and_getPendingWordInsert_returns_english_word() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("кошка").please();
        serviceUser = new ServiceUser(1L, "User", UserSource.YANDEX_ALICE, userId, appId, "Y");
        session = newSession().withState(PENDING_RUS_TRANSLATION).withWord("cat").withLanguage(Language.ENGLISH).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Слово добавлено в ваш словарь.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NEW_TERM).please());
        verify(dictionaryDao, atLeastOnce()).addTermWithTranslationInUserDictionary("cat", Language.ENGLISH, "кошка", Language.RUSSIAN, serviceUser);
    }

    @Test
    void should_warn_about_dangerous_content_when_dangerousContent_flag_is_set() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Name").please();
        serviceUser = createServiceUser();
        session = newSession().please();
        yaRequest.getRequest().setMarkup(new YARequestMarkup(true));
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Не поняла вас. Попробуйте сказать это другими словами.").please(), yaResponse);
    }

    @Test
    void should_ask_to_add_new_words_or_start_testing_and_set_state_PENDING_NEW_TERM_when_state_is_INITIAL_and_user_name_not_empty_and_getDictionarySize_returns_more_than_zero() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Name").please();
        serviceUser = createServiceUser();
        session = newSession().please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDictionarySize(serviceUser)).thenReturn(4L);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Привет, Максим! "
                + "Я знаю три тысячи самых важных английских слов по версии Оксфордского университета и помогу вам их выучить. "
                + "В вашем словаре сейчас 4 слова. Я могу добавить ещё или начать проверку. "
                + "Скажите одну из фраз: добавь русское слово или добавь английское слово. Затем произнесите слово, и я добавлю его в ваш словарь. "
                + "Либо скажите: начинаем проверку, и я начну спрашивать вас слова.").please(),
            yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NEW_TERM).please());
    }

    @Test
    void should_ask_to_add_new_words_or_start_testing_and_set_state_PENDING_NEW_TERM_when_state_is_INITIAL_and_getUserDictionarySizePercentile_returns_90() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("Name").please();
        serviceUser = createServiceUser();
        session = newSession().please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDictionarySize(serviceUser)).thenReturn(4L);
        when(dictionaryDao.getUserDictionarySizePercentile(serviceUser)).thenReturn(90);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Привет, Максим! "
                + "Я знаю три тысячи самых важных английских слов по версии Оксфордского университета и помогу вам их выучить. "
                + "В вашем словаре сейчас 4 слова, и это больше, чем у 90 процентов пользователей. Вы очень усердный ученик. Я могу добавить ещё или начать проверку. "
                + "Скажите одну из фраз: добавь русское слово или добавь английское слово. Затем произнесите слово, и я добавлю его в ваш словарь. "
                + "Либо скажите: начинаем проверку, и я начну спрашивать вас слова.").please(),
            yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NEW_TERM).please());
    }

    @Test
    void should_ask_for_test_type_and_set_state_PENDING_TEST_TYPE_CHOICE_when_state_is_PENDING_NEW_TERM_and_command_is_start_test_and_getDictionarySize_return_less_than_10() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("начинаем проверку").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDictionarySize(serviceUser)).thenReturn(9L);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
                "В вашем словаре сейчас всего 9 слов, поэтому проверять будем по общему словарю. "
                    + "Вы хотите проверять английские слова, русские или вперемешку? "
                    + "Если вы общаетесь со мной голосом, то рекомендую английские, чтобы не возникали сложности с произношением. "
                ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_TYPE_CHOICE).withTestDictionary(TestDictionary.COMMON).please());

    }

    @Test
    void should_ask_for_test_type_and_set_state_PENDING_TEST_TYPE_CHOICE_when_state_is_PENDING_NEW_TERM_and_command_is_start_test_and_getDictionarySize_return_0() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("начинаем проверку").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDictionarySize(serviceUser)).thenReturn(0L);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Ваш словарь пуст, поэтому проверять будем по общему словарю. "
                + "Вы хотите проверять английские слова, русские или вперемешку? "
                + "Если вы общаетесь со мной голосом, то рекомендую английские, чтобы не возникали сложности с произношением. "
        ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_TYPE_CHOICE).withTestDictionary(
            TestDictionary.COMMON).please());

    }

    @Test
    void should_ask_for_test_dictionary_and_set_state_PENDING_TEST_DICTIONARY_when_state_is_PENDING_NEW_TERM_and_command_is_start_test_and_getDictionarySize_returns_10() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("начинаем проверку").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDictionarySize(serviceUser)).thenReturn(10L);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Будем проверять слова из вашего словаря или из общего?"
        ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_DICTIONARY).please());
    }

    @Test
    void should_ask_to_test_by_letters_and_store_test_dictionary_COMMON_and_set_state_PENDING_TEST_TYPE_CHOICE_when_state_is_PENDING_TEST_DICTIONARY_and_command_is_common_dictionary() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("из общего словаря").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_DICTIONARY).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Вы хотите проверять английские слова, русские или вперемешку? "
                + "Если вы общаетесь со мной голосом, то рекомендую английские, чтобы не возникали сложности с произношением. "
        ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withTestDictionary(TestDictionary.COMMON).withState(PENDING_TEST_TYPE_CHOICE).please());
    }

    @Test
    void should_ask_to_test_by_letters_and_store_test_dictionary_USER_and_set_state_PENDING_TEST_TYPE_CHOICE_when_state_is_PENDING_TEST_DICTIONARY_and_command_is_user_dictionary() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("из моего словаря").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_DICTIONARY).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Вы хотите проверять английские слова, русские или вперемешку? "
                + "Если вы общаетесь со мной голосом, то рекомендую английские, чтобы не возникали сложности с произношением. "
        ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withTestDictionary(TestDictionary.USER).withState(PENDING_TEST_TYPE_CHOICE).please());
    }

    @Test
    void should_ask_again_for_test_dictionary_when_state_is_PENDING_TEST_DICTIONARY_and_command_is_unrecognized() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("из твоего словаря").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_DICTIONARY).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Не поняла. Скажите: \"из моего словаря\" или \"из общего словаря\"."
        ).please(), yaResponse);
    }

    @Test
    void should_store_test_type_english_word_and_language_and_ask_for_new_word_returned_by_getNextRandomTermToTest_with_parameter_ENGLISH_and_set_state_PENDING_TEST_RESPONSE_when_state_is_PENDING_TEST_TYPE_CHOICE_and_command_is_english() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("английские").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_TYPE_CHOICE).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getNextRandomTermToTest(serviceUser, TestType.ENGLISH,
            session.getTestDictionary())).thenReturn(term);
        when(term.getLanguage()).thenReturn(Language.ENGLISH);
        when(term.getTerm()).thenReturn("cat");

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Отлично! Начинаем. Как переводится на русский слово cat?"
        ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_RESPONSE)
            .withLanguage(Language.ENGLISH).withWord("cat").withTestType(TestType.ENGLISH).please());
    }

    @Test
    void should_store_test_type_russian_word_and_language_and_ask_for_new_word_returned_by_getNextRandomTermToTest_with_parameter_RUSSIAN_and_set_state_PENDING_TEST_RESPONSE_when_state_is_PENDING_TEST_TYPE_CHOICE_and_command_is_russian() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("русские слова").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_TYPE_CHOICE).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getNextRandomTermToTest(serviceUser, TestType.RUSSIAN,
            session.getTestDictionary())).thenReturn(term);
        when(term.getLanguage()).thenReturn(Language.RUSSIAN);
        when(term.getTerm()).thenReturn("кошка");

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Отлично! Начинаем. Как переводится на английский слово кошка?"
        ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_RESPONSE)
            .withLanguage(Language.RUSSIAN).withWord("кошка").withTestType(TestType.RUSSIAN).please());
    }

    @Test
    void should_store_test_type_mix_word_and_language_and_ask_for_new_word_returned_by_getNextRandomTermToTest_with_parameter_MIX_and_set_state_PENDING_TEST_RESPONSE_when_state_is_PENDING_TEST_TYPE_CHOICE_and_command_is_mix() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("вперемешку").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_TYPE_CHOICE).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getNextRandomTermToTest(serviceUser, TestType.MIX,
            session.getTestDictionary())).thenReturn(term);
        when(term.getLanguage()).thenReturn(Language.RUSSIAN);
        when(term.getTerm()).thenReturn("кошка");

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Отлично! Начинаем. Как переводится на английский слово кошка?"
        ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_RESPONSE)
            .withLanguage(Language.RUSSIAN).withWord("кошка").withTestType(TestType.MIX).please());
    }

    @Test
    void should_ask_for_test_type_when_state_is_PENDING_TEST_TYPE_CHOICE_and_command_neither_russian_nor_english_nor_mix() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("вразброс").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_TYPE_CHOICE).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse(
            "Не поняла вас. Произнесите один из вариантов: английские, русские или вперемешку."
        ).please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_TYPE_CHOICE)
            .please());
    }

    @Test
    void should_tell_about_stats_and_set_state_PENDING_NEW_TERM_when_state_PENDING_TEST_RESPONSE_and_command_is_stop_testing() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("останови проверку").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_RESPONSE).withWord("кошка").withLanguage(Language.RUSSIAN).withTestType(TestType.MIX)
            .withTestCount(20).withSuccessTestCount(5).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Останавливаю проверку. Максим, мы с вами проверили 20 слов, из них правильных ответов 5. "
            + "Вы хорошо справляетесь.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_NEW_TERM).withTestCount(20).withSuccessTestCount(5).please());
    }

    @Test
    void should_answer_with_success_and_offer_to_test_another_word_and_set_state_PENDING_TEST_RESPONSE_when_state_PENDING_TEST_RESPONSE_and_word_with_language_in_session_and_user_utterance_matches_of_translations_returned_by_findTranslations() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("cat").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_RESPONSE).withWord("кошка").withLanguage(Language.RUSSIAN).withTestType(TestType.MIX)
            .withTestDictionary(TestDictionary.COMMON)
            .withTestCount(10).withSuccessTestCount(5).withSuccessTestCountInRaw(2).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getNextRandomTermToTest(serviceUser, TestType.MIX, TestDictionary.COMMON)).thenReturn(newTerm);
        when(newTerm.getLanguage()).thenReturn(Language.ENGLISH);
        when(newTerm.getTerm()).thenReturn("dog");
        when(dictionaryDao.findTranslations("кошка", Language.RUSSIAN)).thenReturn(
            Arrays.asList(new Term(null, "cat", Language.ENGLISH), new Term(null, "kitty", Language.ENGLISH)));

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Верно! Как переводится на русский слово dog?").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_RESPONSE)
            .withLanguage(Language.ENGLISH).withWord("dog").withTestType(TestType.MIX).withTestDictionary(TestDictionary.COMMON)
            .withTestCount(11).withSuccessTestCount(6).withSuccessTestCountInRaw(3).please());
        verify(dictionaryDao, atLeastOnce()).updateTestResult(serviceUser, "кошка", Language.RUSSIAN, DictionaryService.SUCCESS);
    }

    @Test
    void should_answer_with_success_and_make_a_compliment_and_offer_to_test_another_word_and_set_state_PENDING_TEST_RESPONSE_when_state_PENDING_TEST_RESPONSE_and_answer_is_successful_and_more_that_5_success_test_in_raw() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("cat").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_RESPONSE).withWord("кошка").withLanguage(Language.RUSSIAN).withTestType(TestType.MIX)
            .withTestDictionary(TestDictionary.USER)
            .withTestCount(10).withSuccessTestCount(5).withSuccessTestCountInRaw(9).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getNextRandomTermToTest(serviceUser, TestType.MIX, TestDictionary.USER)).thenReturn(newTerm);
        when(newTerm.getLanguage()).thenReturn(Language.ENGLISH);
        when(newTerm.getTerm()).thenReturn("dog");
        when(dictionaryDao.findTranslations("кошка", Language.RUSSIAN)).thenReturn(
            Arrays.asList(new Term(null, "cat", Language.ENGLISH), new Term(null, "kitty", Language.ENGLISH)));

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertThat(yaResponse.getResponse().getText()).isIn(
            "Максим, вы умничка! 10 правильных ответов подряд. Как переводится на русский слово dog?",
            "Максим, так держать! 10 правильных ответов подряд. Как переводится на русский слово dog?",
            "Максим, прекрасные результаты! 10 правильных ответов подряд. Как переводится на русский слово dog?",
            "Максим, вы на волне успеха! 10 правильных ответов подряд. Как переводится на русский слово dog?"
        );
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_RESPONSE)
            .withLanguage(Language.ENGLISH).withWord("dog").withTestType(TestType.MIX).withTestDictionary(TestDictionary.USER)
            .withTestCount(11).withSuccessTestCount(6).withSuccessTestCountInRaw(10).please());
        verify(dictionaryDao, atLeastOnce()).updateTestResult(serviceUser, "кошка", Language.RUSSIAN, DictionaryService.SUCCESS);
    }

    @Test
    void should_answer_with_failure_tell_correct_response_and_offer_to_test_another_word_and_set_state_PENDING_TEST_RESPONSE_when_state_PENDING_TEST_RESPONSE_and_word_with_language_in_session_and_user_utterance_not_matches_and_of_translations_returned_by_findTranslations() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("dog").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_TEST_RESPONSE).withWord("кошка").withLanguage(Language.RUSSIAN).withTestType(TestType.RUSSIAN)
            .withTestDictionary(TestDictionary.USER)
            .withTestCount(10).withSuccessTestCount(5).withSuccessTestCountInRaw(2).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getNextRandomTermToTest(serviceUser, TestType.RUSSIAN, TestDictionary.USER)).thenReturn(newTerm);
        when(newTerm.getLanguage()).thenReturn(Language.RUSSIAN);
        when(newTerm.getTerm()).thenReturn("собака");
        when(dictionaryDao.findTranslations("кошка", Language.RUSSIAN)).thenReturn(
            Arrays.asList(new Term(null, "cat", Language.ENGLISH), new Term(null, "kitty", Language.ENGLISH)));

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Не верно! Слово кошка переводится на английский как cat или kitty."
            + " Как переводится на английский слово собака?").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(PENDING_TEST_RESPONSE)
            .withLanguage(Language.RUSSIAN).withWord("собака").withTestType(TestType.RUSSIAN).withTestDictionary(TestDictionary.USER)
            .withTestCount(11).withSuccessTestCount(5).withSuccessTestCountInRaw(0).please());
        verify(dictionaryDao, atLeastOnce()).updateTestResult(serviceUser, "кошка", Language.RUSSIAN, DictionaryService.FAIL);
    }

    @Test
    void should_ask_for_name_and_set_state_PENDING_NEW_USER_NAME_when_state_is_PENDING_TERM_and_user_ask_to_add_new_user() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("добавь пользователя").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Как зовут нового пользователя?").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(SessionState.PENDING_NEW_USER_NAME).please());
    }

    @Test
    void should_ask_again_for_name_when_state_is_PENDING_NEW_USER_NAME_and_dictionaryDao_return_true_when_calling_checkUserNameExistsOnDevice_with_new_user_name_supplied() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("василий петрович").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_USER_NAME).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.checkUserNameExistsOnDevice(yaRequest.getSession(), "василий петрович")).thenReturn(true);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Пользователь с именем Василий Петрович уже есть на вашем устройстве. Назовите другое имя.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(SessionState.PENDING_NEW_USER_NAME).please());
    }

    @Test
    void should_call_addNewUser_and_update_userId_in_session_and_set_state_PENDING_NEW_TERM_when_state_is_PENDING_NEW_USER_NAME_and_dictionaryDao_return_false_when_calling_checkUserNameExistsOnDevice_with_new_user_name_supplied() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("меня зовут василий петрович").with(
            New.entity("YANDEX.FIO").from(2).to(4).withValue("{\"first_name\": \"василий\", \"patronymic_name\": \"петрович\"}").please()
        ).please();
        serviceUser = createServiceUser();
        ServiceUser newServiceUser = createServiceUser("василий петрович", userId);
        session = newSession().withState(PENDING_NEW_USER_NAME).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.checkUserNameExistsOnDevice(yaRequest.getSession(), "василий петрович")).thenReturn(false);
        when(dictionaryDao.addNewUser(yaRequest.getSession(), "Василий Петрович")).thenReturn(newServiceUser);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Пользователь Василий Петрович добавлен на ваше устройство.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).addNewUser(yaRequest.getSession(), "Василий Петрович");
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withUser(newServiceUser).withState(
            PENDING_NEW_TERM).please());
    }

    @Test
    void should_ask_again_for_name_when_state_is_PENDING_NEW_USER_NAME_and_defined_entity_FIO_and_dictionaryDao_return_true_when_calling_checkUserNameExistsOnDevice_with_new_user_name_supplied() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("меня зовут василий петрович").with(
            New.entity("YANDEX.FIO").from(2).to(4).withValue("{\"first_name\": \"василий\", \"patronymic_name\": \"петрович\"}").please()
        ).please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_USER_NAME).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.checkUserNameExistsOnDevice(yaRequest.getSession(), "василий петрович")).thenReturn(true);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Пользователь с именем Василий Петрович уже есть на вашем устройстве. Назовите другое имя.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(SessionState.PENDING_NEW_USER_NAME).please());
    }

    @Test
    void should_ask_for_name_and_set_state_PENDING_USER_NAME_FOR_SWITCH_when_state_is_PENDING_TERM_and_user_ask_to_switch_user_and_getDeviceUserCount_returns_2() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("переключи пользователя").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDeviceUserCount(yaRequest.getSession())).thenReturn(2);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Назовите имя пользователя.").please(), yaResponse);
        verify(dictionaryDao, atLeastOnce()).updateSessionState(newSession().withState(SessionState.PENDING_USER_NAME_TO_SWITCH).please());
    }

    @Test
    void should_say_about_only_one_user_on_device_when_state_is_PENDING_TERM_and_user_ask_to_switch_user_and_getDeviceUserCount_returns_1() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("переключи пользователя").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_NEW_TERM).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDeviceUserCount(yaRequest.getSession())).thenReturn(1);

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("На этом устройстве всего один пользователь. Если вы хотите создать нового, скажите \"добавь пользователя\".").please(), yaResponse);
    }

    @Test
    void should_ask_for_another_name_and_enumerate_device_users_from_getDeviceUsers_when_state_is_PENDING_USER_NAME_FOR_SWITCH_and_user_said_name_but_getDeviceUserByName_returned_null() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("коля").please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_USER_NAME_TO_SWITCH).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDeviceUserByName(yaRequest.getSession(), "коля")).thenReturn(null);
        when(dictionaryDao.getDeviceUsers(yaRequest.getSession())).thenReturn(
            Arrays.asList(serviceUser, createServiceUser("Петя", userId))
        );

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Максим, я не знаю пользователя Коля на вашем устройстве. Кроме вас зарегистрирован только Петя.").please(), yaResponse);
    }

    @Test
    void should_ask_for_another_name_and_enumerate_device_users_from_getDeviceUsers_when_state_is_PENDING_USER_NAME_FOR_SWITCH_and_FIO_defined_but_getDeviceUserByName_returned_null() {
        yaRequest = New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).withUtterance("коля").with(
            New.entity("YANDEX.FIO").from(2).to(4).withValue("{\"first_name\": \"василий\", \"patronymic_name\": \"петрович\"}").please()
        ).please();
        serviceUser = createServiceUser();
        session = newSession().withState(PENDING_USER_NAME_TO_SWITCH).please();
        when(dictionaryDao.getSessionState(yaRequest.getSession())).thenReturn(session);
        when(dictionaryDao.getDeviceUserByName(yaRequest.getSession(), "коля")).thenReturn(null);
        when(dictionaryDao.getDeviceUsers(yaRequest.getSession())).thenReturn(
            Arrays.asList(serviceUser, createServiceUser("Петя", userId), createServiceUser("антонина воротынская", userId))
        );

        yaResponse = sut.talkYandexAlice(yaRequest);

        assertEquals(New.yaResponse("Максим, я не знаю пользователя Василий Петрович на вашем устройстве. Кроме вас зарегистрированы Петя, Антонина Воротынская.").please(), yaResponse);
    }

    private ServiceUser createServiceUser(String name, String userId) {
        return new ServiceUser(null, name, UserSource.YANDEX_ALICE, userId, appId, "Y");
    }

    private ServiceUser createServiceUser(String name) {
        return createServiceUser(name, userId);
    }

    private ServiceUser createServiceUser() {
        return createServiceUser("Максим", userId);
    }
}
