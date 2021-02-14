package ru.golovkin.oxford3000.dictionary.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.golovkin.oxford3000.dictionary.dao.DictionaryDao;
import ru.golovkin.oxford3000.dictionary.model.Language;
import ru.golovkin.oxford3000.dictionary.model.ServiceUser;
import ru.golovkin.oxford3000.dictionary.model.Session;
import ru.golovkin.oxford3000.dictionary.model.SessionState;
import ru.golovkin.oxford3000.dictionary.model.Term;
import ru.golovkin.oxford3000.dictionary.model.TestDictionary;
import ru.golovkin.oxford3000.dictionary.model.TestType;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YAEntity;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YASession;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YASkillRequest;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YASkillResponse;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceRequest;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceResponse;
import ru.golovkin.oxford3000.dictionary.util.Gender;
import ru.golovkin.oxford3000.dictionary.util.RussianDeclensionUtil;

@Service
@Slf4j
public class DictionaryService {

    public static final int SUCCESS = 1;
    public static final int FAIL = 0;
    private static final List<String> CONGRATULATIONS = Arrays.asList(
        "вы умничка", "так держать", "прекрасные результаты", "вы на волне успеха"
    );
    @Autowired
    private DictionaryDao dictiondaryDao;

    public YandexAliceResponse talkYandexAlice(YandexAliceRequest yandexAliceRequest) {
        YandexAliceResponse yandexAliceResponse = yandexResponse();
        yandexAliceResponse.getResponse().setText("Я пока не понимаю, что мне делать. Скажите: \"расскажи, что ты понимаешь\", "
            + " чтобы я рассказала про команды, на которые я умею отвечать.");

        YASession yandexSession = yandexAliceRequest.getSession();
        log.info("session {} userId {} applicationId {} utterance {}", yandexSession.getSessionId(),
            yandexSession.getUser() != null?yandexSession.getUser().getUserId():null,
            yandexSession.getApplication() != null?yandexSession.getApplication().getApplicationId():null,
            yandexAliceRequest.getRequest() != null?yandexAliceRequest.getRequest().getCommand():null);

        Session session = dictiondaryDao.getSessionState(yandexSession);
        YASkillRequest skillRequest = yandexAliceRequest.getRequest();
        String wordToAdd;
        if (yandexAliceRequest.getRequest() != null && yandexAliceRequest.getRequest().getMarkup() != null
            && yandexAliceRequest.getRequest().getMarkup().isDangerousContent()) {
            yandexAliceResponse.getResponse().setText("Не поняла вас. Попробуйте сказать это другими словами.");
        } else if (session.getState() != SessionState.PENDING_NAME && Strings.isBlank(session.getServiceUser().getName())) {
            yandexAliceResponse.getResponse().setText("Привет! Я знаю три тысячи самых важных английских слов по версии Оксфордского университета и помогу вам их выучить. Как вас зовут?");
            session.setState(SessionState.PENDING_NAME);
            dictiondaryDao.updateSessionState(session);
        } else if (session.getState() == SessionState.PENDING_NAME && commandDefined(skillRequest)) {
            String name = skillRequest.getCommand();
            String nameFromNLU;
            if ((nameFromNLU = definedEntityFIO(skillRequest)) != null) {
                name = nameFromNLU;
            }
            yandexAliceResponse.getResponse().setText(
                String.format("Рада познакомиться, %s. "
                + "Ваш словарь пуст. Вы можете добавить в него слова, которые хотите выучить. "
                    + "Скажите одну из фраз: добавь русское слово или добавь английское слово. Затем произнесите слово, и я добавлю его в ваш словарь. "
                    + "Например: добавь русское слово собака или добавь английское слово dog. "
                    + "Либо можем начать проверку по общему словарю. Для этого скажите: начинаем проверку. ", name));
            session.getServiceUser().setName(name);
            session.setState(SessionState.PENDING_NEW_TERM);
            dictiondaryDao.updateSessionState(session);
        } else if (session.getState() == SessionState.INITIAL && Strings.isNotBlank(session.getServiceUser().getName())) {
            long dictionarySize = dictiondaryDao.getDictionarySize(session.getServiceUser());
            if (dictionarySize == 0) {
                yandexAliceResponse.getResponse().setText(
                    String.format("Привет, %s! "
                        + "Я знаю три тысячи самых важных английских слов по версии Оксфордского университета и помогу вам их выучить. "
                        + "Ваш словарь пуст. Вы можете добавить в него слова, которые хотите выучить. "
                        + "Скажите одну из фраз: добавь русское слово или добавь английское слово. Затем произнесите слово, и я добавлю его в ваш словарь. "
                        + "Например: добавь русское слово собака или добавь английское слово dog. "
                        + "Либо можем начать проверку по общему словарю. Для этого скажите: начинаем проверку. ", session.getServiceUser().getName()));
                session.setState(SessionState.PENDING_NEW_TERM);
                dictiondaryDao.updateSessionState(session);
            } else {
                String userDictionarySizeRating = "";
                int percentile = dictiondaryDao.getUserDictionarySizePercentile(session.getServiceUser());
                if (percentile > 50) {
                    userDictionarySizeRating = String.format(", и это больше, чем у %s пользователей",
                        RussianDeclensionUtil.inclineWithNumeral(percentile, Gender.MASCULINE, "процент", "процента", "процентов")
                    );
                    if (percentile > 75) {
                        userDictionarySizeRating += ". Вы очень усердный ученик";
                    }
                }
                yandexAliceResponse.getResponse().setText(
                    String.format("Привет, %1$s! "
                        + "Я знаю три тысячи самых важных английских слов по версии Оксфордского университета и помогу вам их выучить. "
                        + "В вашем словаре сейчас %2$s%3$s. Я могу добавить ещё или начать проверку. "
                        + "Скажите одну из фраз: добавь русское слово или добавь английское слово. Затем произнесите слово, и я добавлю его в ваш словарь. "
                        + "Либо скажите: начинаем проверку, и я начну спрашивать вас слова.", session.getServiceUser().getName(),
                        RussianDeclensionUtil.inclineWithNumeral(dictionarySize, Gender.NEUTER, "слово", "слова", "слов"),
                        userDictionarySizeRating));
                session.setState(SessionState.PENDING_NEW_TERM);
                dictiondaryDao.updateSessionState(session);
            }
        } else if (session.getState() != SessionState.INITIAL && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && (tokensContain(skillRequest.getNlu().getTokens(), "расскажи", "что", "ты", "понимаешь") >= 0
            || tokensContain(skillRequest.getNlu().getTokens(), "что", "ты", "умеешь") >= 0
            || tokensContain(skillRequest.getNlu().getTokens(), "помощь") >= 0)) {
            yandexAliceResponse.getResponse().setText(
                "Вот, какие команды я понимаю."
                    + "Скажите, \"добавь русское слово\" или \"добавь английское слово\" и произнесите слово, чтобы добавить его в свой словарь."
                    + "Скажите: \"начинаем проверку\", чтобы я начала вас спрашивать слова."
                    + "Скажите: \"останови проверку\", чтобы я перестала проверять слова.");
        } else if (session.getState() == SessionState.PENDING_NEW_TERM && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && tokensEndWith(skillRequest.getNlu().getTokens(), "добавь", "английское", "слово")) {
            yandexAliceResponse.getResponse().setText("Вы не произнесли слово, которое нужно добавить. Нужно было сказать, к примеру, \"добавь английское слово potato\".");
        } else if (session.getState() == SessionState.PENDING_NEW_TERM && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && tokensEndWith(skillRequest.getNlu().getTokens(), "добавь", "русское", "слово")) {
            yandexAliceResponse.getResponse().setText("Вы не произнесли слово, которое нужно добавить. Нужно было сказать, к примеру, \"добавь русское слово помидор\".");
        } else if (session.getState() == SessionState.PENDING_NEW_TERM && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && (wordToAdd = getWordAfter(skillRequest.getNlu().getTokens(), "добавь", "русское", "слово")) != null) {
            if (dictiondaryDao.wordExistsInUserDictionary(wordToAdd, Language.RUSSIAN, session.getServiceUser().getId())) {
                yandexAliceResponse.getResponse().setText(String.format("Слово %s уже есть в вашем словаре. Давайте добавим другое.", wordToAdd));
            } else if (dictiondaryDao.wordExistsInGlobalDictionary(wordToAdd, Language.RUSSIAN)) {
                List<Term> translations = dictiondaryDao.findTranslations(wordToAdd, Language.RUSSIAN);
                String translation = translations.stream().filter(Objects::nonNull).map(Term::getTerm).collect(Collectors.joining(" или "));
                yandexAliceResponse.getResponse().setText(String.format("Я знаю такое слово, оно переводится на английский как %s. Добавила в ваш словарь.", translation));
                dictiondaryDao.addTermFromGlobalToUserDictionary(wordToAdd, Language.RUSSIAN, session.getServiceUser());
            } else {
                yandexAliceResponse.getResponse().setText("В общем словаре нет такого слова, но я могу добавить, если вы знаете перевод. Как оно переводится на английский язык?");
                session.setState(SessionState.PENDING_ENG_TRANSLATION);
                session.setLanguage(Language.RUSSIAN);
                session.setWord(wordToAdd);
                dictiondaryDao.updateSessionState(session);
            }
        } else if (session.getState() == SessionState.PENDING_NEW_TERM && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && (wordToAdd = getWordAfter(skillRequest.getNlu().getTokens(), "добавь", "английское", "слово")) != null) {
            if (dictiondaryDao.wordExistsInUserDictionary(wordToAdd, Language.ENGLISH, session.getServiceUser().getId())) {
                yandexAliceResponse.getResponse().setText(String.format("Слово %s уже есть в вашем словаре. Давайте добавим другое.", wordToAdd));
            } else if (dictiondaryDao.wordExistsInGlobalDictionary(wordToAdd, Language.ENGLISH)) {
                List<Term> translations = dictiondaryDao.findTranslations(wordToAdd, Language.ENGLISH);
                String translation = translations.stream().filter(Objects::nonNull).map(Term::getTerm).collect(Collectors.joining(" или "));
                yandexAliceResponse.getResponse().setText(String.format("Я знаю такое слово, оно переводится на русский как %s. Добавила в ваш словарь.", translation));
                dictiondaryDao.addTermFromGlobalToUserDictionary(wordToAdd, Language.ENGLISH, session.getServiceUser());
            } else {
                yandexAliceResponse.getResponse().setText("В общем словаре нет такого слова, но я могу добавить, если вы знаете перевод. Как оно переводится на русский язык?");
                session.setState(SessionState.PENDING_RUS_TRANSLATION);
                session.setLanguage(Language.ENGLISH);
                session.setWord(wordToAdd);
                dictiondaryDao.updateSessionState(session);
            }
        } else if (session.getState() == SessionState.PENDING_ENG_TRANSLATION && commandDefined(skillRequest)
            && Strings.isNotBlank(session.getWord()) && Language.RUSSIAN.equals(session.getLanguage())) {
            yandexAliceResponse.getResponse().setText("Слово добавлено в ваш словарь.");
            session.setState(SessionState.PENDING_NEW_TERM);
            dictiondaryDao.addTermWithTranslationInUserDictionary(session.getWord(), session.getLanguage(),
                skillRequest.getCommand(), Language.ENGLISH, session.getServiceUser());
            session.clearWordInfo();
            dictiondaryDao.updateSessionState(session);
        } else if (session.getState() == SessionState.PENDING_RUS_TRANSLATION && commandDefined(skillRequest)
            && Strings.isNotBlank(session.getWord()) && Language.ENGLISH.equals(session.getLanguage()) ) {
            yandexAliceResponse.getResponse().setText("Слово добавлено в ваш словарь.");
            session.setState(SessionState.PENDING_NEW_TERM);
            dictiondaryDao.addTermWithTranslationInUserDictionary(session.getWord(), session.getLanguage(),
                skillRequest.getCommand(), Language.RUSSIAN, session.getServiceUser());
            session.clearWordInfo();
            dictiondaryDao.updateSessionState(session);
        } else if (session.getState() == SessionState.PENDING_NEW_TERM && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && tokensContain(skillRequest.getNlu().getTokens(), "начинаем", "проверку") >= 0) {
            long dictionarySize = dictiondaryDao.getDictionarySize(session.getServiceUser());
            if (dictionarySize == 0) {
                yandexAliceResponse.getResponse().setText(
                    "Ваш словарь пуст, поэтому проверять будем по общему словарю. "
                    + "Вы хотите проверять английские слова, русские или вперемешку? "
                    + "Если вы общаетесь со мной голосом, то рекомендую английские, чтобы не возникали сложности с произношением. ");
                session.setState(SessionState.PENDING_TEST_TYPE_CHOICE);
                session.setTestDictionary(TestDictionary.COMMON);
                dictiondaryDao.updateSessionState(session);
            } else if (dictionarySize < 10) {
                yandexAliceResponse.getResponse().setText(
                    String.format("В вашем словаре сейчас всего %1$s, поэтому проверять будем по общему словарю. "
                        + "Вы хотите проверять английские слова, русские или вперемешку? "
                        + "Если вы общаетесь со мной голосом, то рекомендую английские, чтобы не возникали сложности с произношением. ",
                        RussianDeclensionUtil.inclineWithNumeral(dictionarySize, Gender.NEUTER, "слово", "слова", "слов")));
                session.setState(SessionState.PENDING_TEST_TYPE_CHOICE);
                session.setTestDictionary(TestDictionary.COMMON);
                dictiondaryDao.updateSessionState(session);
            } else {
                yandexAliceResponse.getResponse().setText("Будем проверять слова из вашего словаря или из общего?");
                session.setState(SessionState.PENDING_TEST_DICTIONARY);
                dictiondaryDao.updateSessionState(session);
            }
        } else if (session.getState() == SessionState.PENDING_NEW_TERM && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && tokensContain(skillRequest.getNlu().getTokens(), "добавь", "пользователя") >= 0) {

            yandexAliceResponse.getResponse().setText(
                "Как зовут нового пользователя?");
            session.setState(SessionState.PENDING_NEW_USER_NAME);
            dictiondaryDao.updateSessionState(session);
        } else if (session.getState() == SessionState.PENDING_NEW_USER_NAME && commandDefined(skillRequest)) {
            String newUserName = skillRequest.getCommand();
            String newNameFromNLU;
            if ((newNameFromNLU = definedEntityFIO(skillRequest)) != null) {
                newUserName = newNameFromNLU;
            }

            if (dictiondaryDao.checkUserNameExistsOnDevice(yandexSession, newUserName.toLowerCase())) {
                String capitalizedName = capitalize(newUserName);
                yandexAliceResponse.getResponse().setText(String.format(
                    "Пользователь с именем %1$s уже есть на вашем устройстве. Назовите другое имя.",
                    capitalizedName
                ));
                session.setState(SessionState.PENDING_NEW_USER_NAME);
                dictiondaryDao.updateSessionState(session);
            } else {
                ServiceUser newUser = dictiondaryDao.addNewUser(yandexSession, capitalize(newUserName));
                session.setServiceUser(newUser);
                session.setState(SessionState.PENDING_NEW_TERM);
                dictiondaryDao.updateSessionState(session);
                yandexAliceResponse.getResponse().setText(String.format(
                    "Пользователь %1$s добавлен на ваше устройство.", capitalize(newUserName)
                ));
            }
        } else if (session.getState() == SessionState.PENDING_NEW_TERM && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && (
                tokensContain(skillRequest.getNlu().getTokens(), "переключи", "пользователя") >= 0 ||
                tokensContain(skillRequest.getNlu().getTokens(), "переключите", "пользователя") >= 0 ||
                tokensContain(skillRequest.getNlu().getTokens(), "смени", "пользователя") >= 0 ||
                tokensContain(skillRequest.getNlu().getTokens(), "смените", "пользователя") >= 0 ||
                tokensContain(skillRequest.getNlu().getTokens(), "поменяй", "пользователя") >= 0 ||
                tokensContain(skillRequest.getNlu().getTokens(), "поменяйте", "пользователя") >= 0)) {
            if (dictiondaryDao.getDeviceUserCount(yandexSession) < 2) {
                yandexAliceResponse.getResponse().setText("На этом устройстве всего один пользователь. Если вы хотите создать нового, скажите \"добавь пользователя\".");
            } else {
                yandexAliceResponse.getResponse().setText("Назовите имя пользователя.");
                session.setState(SessionState.PENDING_USER_NAME_TO_SWITCH);
                dictiondaryDao.updateSessionState(session);
            }
        } else if (session.getState() == SessionState.PENDING_USER_NAME_TO_SWITCH && commandDefined(skillRequest)) {
            String userName = skillRequest.getCommand();
            String userNameFromNLU;
            if ((userNameFromNLU = definedEntityFIO(skillRequest)) != null) {
                userName = userNameFromNLU;
            }

            if (dictiondaryDao.getDeviceUserByName(yandexSession, userName) == null) {
                String capitalizedName = capitalize(userName);
                String capitalizedCurrentUser = capitalize(session.getServiceUser().getName());
                List<ServiceUser> deviceUsers = dictiondaryDao.getDeviceUsers(yandexSession);
                String capitalizedOtherDeviceUsers = deviceUsers.stream()
                    .filter( u -> !session.getServiceUser().equals(u)).map(u -> capitalize(u.getName())).collect(
                        Collectors.joining(", "));

                yandexAliceResponse.getResponse().setText(String.format(
                    "%1$s, я не знаю пользователя %2$s на вашем устройстве. Кроме вас зарегистрирован%4$s %3$s.",
                    capitalizedCurrentUser, capitalizedName, capitalizedOtherDeviceUsers, deviceUsers.size() > 2?"ы":" только"
                ));
            }
        } else if (session.getState() == SessionState.PENDING_TEST_DICTIONARY && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null) {
            if (tokensContain(skillRequest.getNlu().getTokens(), "общего") >= 0) {
                yandexAliceResponse.getResponse().setText("Вы хотите проверять английские слова, русские или вперемешку? "
                    + "Если вы общаетесь со мной голосом, то рекомендую английские, чтобы не возникали сложности с произношением. ");
                session.setTestDictionary(TestDictionary.COMMON);
                session.setState(SessionState.PENDING_TEST_TYPE_CHOICE);
                dictiondaryDao.updateSessionState(session);
            } else if (tokensContain(skillRequest.getNlu().getTokens(), "моего") >= 0) {
                yandexAliceResponse.getResponse().setText("Вы хотите проверять английские слова, русские или вперемешку? "
                    + "Если вы общаетесь со мной голосом, то рекомендую английские, чтобы не возникали сложности с произношением. ");
                session.setTestDictionary(TestDictionary.USER);
                session.setState(SessionState.PENDING_TEST_TYPE_CHOICE);
                dictiondaryDao.updateSessionState(session);
            } else {
                yandexAliceResponse.getResponse().setText("Не поняла. Скажите: \"из моего словаря\" или \"из общего словаря\".");
            }
        } else if (session.getState() == SessionState.PENDING_TEST_TYPE_CHOICE && commandDefined(skillRequest)) {
            TestType testType = null;
            if (skillRequest.getCommand().toLowerCase().contains("английски")) {
                testType = TestType.ENGLISH;
            } else if (skillRequest.getCommand().toLowerCase().contains("русски")) {
                testType = TestType.RUSSIAN;
            } else if (skillRequest.getCommand().toLowerCase().contains("перемеш")) {
                testType = TestType.MIX;
            }
            if (testType != null) {
                Term term = dictiondaryDao.getNextRandomTermToTest(session.getServiceUser(), testType, session.getTestDictionary());
                session.setState(SessionState.PENDING_TEST_RESPONSE);
                session.setTestType(testType);
                session.setWord(term.getTerm());
                session.setLanguage(term.getLanguage());
                dictiondaryDao.updateSessionState(session);

                yandexAliceResponse.getResponse().setText(
                    String.format("Отлично! Начинаем. Как переводится на %1$s слово %2$s?",
                        Language.RUSSIAN.equals(term.getLanguage()) ? "английский" : "русский",
                        term.getTerm()));
            } else {
                yandexAliceResponse.getResponse().setText("Не поняла вас. Произнесите один из вариантов: английские, русские или вперемешку.");
                dictiondaryDao.updateSessionState(session);
            }
        } else if (session.getState() == SessionState.PENDING_TEST_RESPONSE && commandDefined(skillRequest) &&
            skillRequest.getNlu() != null && tokensContain(skillRequest.getNlu().getTokens(), "останови", "проверку") >= 0) {
            session.setState(SessionState.PENDING_NEW_TERM);
            session.clearWordInfo();
            session.setTestType(null);
            yandexAliceResponse.getResponse().setText(String.format(
                "Останавливаю проверку. %1$s, мы с вами проверили %2$s, из них правильных ответов %3$d. "
                    + "Вы хорошо справляетесь.", session.getServiceUser().getName(),
                RussianDeclensionUtil.inclineWithNumeral(session.getTestCount(), Gender.NEUTER,
                    "слово", "слова", "слов"), session.getSuccessTestCount()));
            dictiondaryDao.updateSessionState(session);
        } else if (session.getState() == SessionState.PENDING_TEST_RESPONSE && commandDefined(skillRequest)) {
            List<Term> translations = dictiondaryDao.findTranslations(session.getWord(), session.getLanguage());
            Optional<Term> validTranslation = translations.stream().filter(t -> t != null && t.getTerm().equals(skillRequest.getCommand().toLowerCase())).findFirst();
            Term newTerm = dictiondaryDao.getNextRandomTermToTest(session.getServiceUser(), session.getTestType(), session.getTestDictionary());
            if (validTranslation.isPresent()) {
                String congratulation = "Верно!";
                if (session.getSuccessTestCountInRaw() > 0 && (session.getSuccessTestCountInRaw() + 1) % 5 == 0) {
                    congratulation = String.format("%s, %s! %s подряд.",
                        session.getServiceUser().getName(),
                        CONGRATULATIONS.get(new Random().nextInt(CONGRATULATIONS.size())),
                        RussianDeclensionUtil.inclineWithNumeral(session.getSuccessTestCountInRaw() + 1, Gender.MASCULINE,
                            "правильный ответ", "правильных ответа", "правильных ответов"));
                }
                yandexAliceResponse.getResponse().setText(
                    String.format("%s Как переводится на %s слово %s?",
                        congratulation, Language.RUSSIAN.equals(newTerm.getLanguage()) ? "английский" : "русский",
                        newTerm.getTerm()));
                session.setSuccessTestCount(session.getSuccessTestCount() + 1);
                session.setSuccessTestCountInRaw(session.getSuccessTestCountInRaw() + 1);
                session.setTestCount(session.getTestCount() + 1);
                dictiondaryDao.updateTestResult(session.getServiceUser(), session.getWord(), session.getLanguage(), SUCCESS);
            } else {
                String validResponse = translations.stream().filter(Objects::nonNull).map(Term::getTerm).collect(
                    Collectors.joining(" или "));
                yandexAliceResponse.getResponse().setText(
                    String.format("Не верно! Слово %1$s переводится на %2$s как %3$s. "
                            + "Как переводится на %4$s слово %5$s?",
                        session.getWord(), Language.RUSSIAN.equals(session.getLanguage()) ? "английский" : "русский", validResponse,
                        Language.RUSSIAN.equals(newTerm.getLanguage()) ? "английский" : "русский",
                        newTerm.getTerm()));
                session.setSuccessTestCountInRaw(0);
                session.setTestCount(session.getTestCount() + 1);
                dictiondaryDao.updateTestResult(session.getServiceUser(), session.getWord(), session.getLanguage(), FAIL);
            }
            session.setWord(newTerm.getTerm());
            session.setLanguage(newTerm.getLanguage());
            dictiondaryDao.updateSessionState(session);
        }

        return yandexAliceResponse;
    }

    private String capitalize(String newUserName) {
        return newUserName == null? null: Arrays.stream(newUserName.toLowerCase().split(" ")).map(
            t -> t.substring(0, 1).toUpperCase() + t.substring(1)
        ).collect(Collectors.joining(" "));
    }

    private String definedEntityFIO(YASkillRequest skillRequest) {
        if (skillRequest == null || skillRequest.getNlu() == null || skillRequest.getNlu().getEntities() == null
            || skillRequest.getNlu().getEntities().isEmpty()) {
            return null;
        }
        Optional<YAEntity> entity = skillRequest.getNlu().getEntities().stream().filter( t -> t != null && "YANDEX.FIO".equals(t.getType())
            && t.getValue().isObject())
            .findFirst();
        if (entity.isPresent()) {
            JsonNode nodeLastName = entity.get().getValue().get("last_name");
            JsonNode nodeFirstName = entity.get().getValue().get("first_name");
            JsonNode nodePatronymicName = entity.get().getValue().get("patronymic_name");
            List<JsonNode> nameParts = Arrays.asList(nodeFirstName, nodePatronymicName, nodeLastName);
            return nameParts.stream().filter(
                t -> t != null && !t.isNull() && t.isValueNode() && t.isTextual() && !t.asText().trim().isEmpty()
            ).map(JsonNode::asText).map(t -> t.substring(0, 1).toUpperCase() + t.substring(1)).collect(Collectors.joining(" "));
        }

        return null;
    }

    private String getWordAfter(List<String> tokens, String ...words) {
        int index = tokensContain(tokens, words);
        if (index >= 0 && index + words.length + 1 <= tokens.size()) {
            return tokens.get(index + words.length);
        }
        return null;
    }

    private int tokensContain(List<String> tokens, String ...words) {
        return Collections.indexOfSubList(tokens, Arrays.asList(words));
    }

    private boolean tokensEndWith(List<String> tokens, String ...words) {
        int index = tokensContain(tokens, words);
        return index >= 0 && tokens.size() == index + words.length;
    }
    private boolean commandDefined(YASkillRequest skillRequest) {
        return skillRequest != null && Strings.isNotBlank(skillRequest.getCommand());
    }

    private YandexAliceResponse yandexResponse() {
        YandexAliceResponse yandexAliceResponse = new YandexAliceResponse();
        yandexAliceResponse.setResponse(new YASkillResponse());
        return yandexAliceResponse;
    }
}
