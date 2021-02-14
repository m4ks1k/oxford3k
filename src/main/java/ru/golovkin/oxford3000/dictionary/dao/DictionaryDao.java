package ru.golovkin.oxford3000.dictionary.dao;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.golovkin.oxford3000.dictionary.model.Language;
import ru.golovkin.oxford3000.dictionary.model.ServiceUser;
import ru.golovkin.oxford3000.dictionary.model.Session;
import ru.golovkin.oxford3000.dictionary.model.SessionState;
import ru.golovkin.oxford3000.dictionary.model.Term;
import ru.golovkin.oxford3000.dictionary.model.TermReference;
import ru.golovkin.oxford3000.dictionary.model.TestDictionary;
import ru.golovkin.oxford3000.dictionary.model.TestType;
import ru.golovkin.oxford3000.dictionary.model.UserDictionaryEntry;
import ru.golovkin.oxford3000.dictionary.model.UserSource;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YASession;

@Repository
@Slf4j
public class DictionaryDao {
    @Autowired
    EntityManager entityManager;

    public Session getSessionState(YASession session) {
        TypedQuery<Session> query = entityManager
            .createQuery("select s from Session s join fetch s.serviceUser where s.sessionId = :sessionId",
                Session.class);
        query.setParameter("sessionId", session.getSessionId());
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            log.info("No persisted session for {}, returning default object", session.getSessionId());
            Session defaultSession = new Session(SessionState.INITIAL, session.getSessionId());
            ServiceUser serviceUser = getServiceUser(UserSource.YANDEX_ALICE, session.getUser() == null?null:session.getUser().getUserId(),
                session.getApplication() == null?null:session.getApplication().getApplicationId());
            defaultSession.setServiceUser(serviceUser);
            return defaultSession;
        }
    }

    private ServiceUser getServiceUser(UserSource userSource, String userId, String applicationId) {
        TypedQuery<ServiceUser> serviceUserTypedQuery = entityManager.createQuery(
            "select u from ServiceUser u where u.userSource = :userSource and u.extUserId = :extUserId and u.lastUsed = 'Y'", ServiceUser.class);
        serviceUserTypedQuery.setParameter("userSource", userSource);
        serviceUserTypedQuery.setParameter("extUserId", userId);
        try {
            return serviceUserTypedQuery.getSingleResult();
        } catch (NoResultException e) {
            log.info("No user for userId {}, trying to find user by appId", userId);
        }

        serviceUserTypedQuery = entityManager.createQuery(
            "select u from ServiceUser u where u.userSource = :userSource and u.extUserId is null and u.extAppId = :extAppId and u.lastUsed = 'Y'", ServiceUser.class);
        serviceUserTypedQuery.setParameter("userSource", userSource);
        serviceUserTypedQuery.setParameter("extAppId", applicationId);
        try {
            return serviceUserTypedQuery.getSingleResult();
        } catch (NoResultException e) {
            log.info("No user for appId {}, returning default object", applicationId);
        }
        return new ServiceUser(null, null, userSource, userId, applicationId, "Y");
    }

    @Transactional
    public void updateSessionState(Session session) {
        log.info("Saving {}", session);
        entityManager.merge(session);
    }

    public long getDictionarySize(ServiceUser serviceUser) {
        TypedQuery<Long> query = entityManager.createQuery(
            "select count(ude) "
                + " from UserDictionaryEntry ude "
                + " join ude.termReference r "
                + " join r.term t "
                + " join r.termReference tr "
                + " where ude.user = :user "
            , Long.class);
        query.setParameter("user", serviceUser);
        return query.getSingleResult();
    }

    public boolean wordExistsInUserDictionary(String word, Language language, Long userId) {
        TypedQuery<Long> query = entityManager.createQuery(
            "select count(ude) "
                + " from UserDictionaryEntry ude "
                + " join ude.termReference r "
                + " join r.term t "
                + " join r.termReference tr "
                + " where t.term = :term and t.language = :language and ude.user.id = :userId "
            , Long.class);
        query.setParameter("term", word.toLowerCase());
        query.setParameter("language", language);
        query.setParameter("userId", userId);

        return query.getSingleResult().intValue() > 0;
    }

    public boolean wordExistsInGlobalDictionary(String word, Language language) {
        TypedQuery<Long> query = entityManager.createQuery(
            "select count(r) "
                + " from TermReference r "
                + " join r.term t "
                + " join r.termReference tr "
                + " where t.term = :term and t.language = :language  "
            , Long.class);
        query.setParameter("term", word.toLowerCase());
        query.setParameter("language", language);

        return query.getSingleResult().intValue() > 0;
    }

    @Transactional
    public void addTermWithTranslationInUserDictionary(String word, Language sourceLanguage,
        String translation, Language targetLanguage, ServiceUser serviceUser) {
        Term term = findOrCreateTerm(word, sourceLanguage);
        Term termRef = findOrCreateTerm(translation, targetLanguage);
        TermReference termReference = findOrCreateTermReference(term, termRef);
        if (termReference.getId() != null) {
            TypedQuery<Long> query = entityManager.createQuery(
                "select count(e) from UserDictionaryEntry e where e.user = :user and e.termReference.id = :termReference",
                Long.class);
            query.setParameter("termReference", termReference.getId());
            query.setParameter("user", serviceUser);
            if (query.getSingleResult() > 0) {
                return;
            }
        }
        UserDictionaryEntry userDictionaryEntry = new UserDictionaryEntry(null, serviceUser, termReference, 0, 0);
        entityManager.merge(userDictionaryEntry);
    }

    private TermReference findOrCreateTermReference(Term term, Term termReference) {
        TypedQuery<TermReference> query = entityManager.createQuery("select t from TermReference t "
            + " where t.term.term = :term and t.term.language = :termLanguage and t.termReference.term = :termReference and t.termReference.language = :termReferenceLanguage "
            + " or t.term.term = :termReference and t.term.language = :termReferenceLanguage and t.termReference.term = :term and t.termReference.language = :termLanguage", TermReference.class);
        query.setParameter("term", term.getTerm());
        query.setParameter("termLanguage", term.getLanguage());
        query.setParameter("termReference", termReference.getTerm());
        query.setParameter("termReferenceLanguage", termReference.getLanguage());
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return new TermReference(null, term, termReference);
        }
    }

    private Term findOrCreateTerm(String word, Language language) {
        TypedQuery<Term> query = entityManager.createQuery("select t from Term t where t.term = :term and t.language = :language", Term.class);
        query.setParameter("term", word);
        query.setParameter("language", language);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return new Term(null, word.toLowerCase(), language);
        }
    }

    public Term getNextRandomTermToTest(ServiceUser serviceUser, TestType testType, TestDictionary testDictionary) {
        Random random = new Random();
        TermReference termReference;
        if (TestDictionary.USER.equals(testDictionary)) {

            TypedQuery<UserDictionaryEntry> query =
                entityManager.createQuery(
                    "select e "
                        + "from UserDictionaryEntry e "
                        + "join fetch e.termReference r "
                        + "join fetch r.term t "
                        + "join fetch r.termReference tr "
                        + "where e.user = :user ",
                    UserDictionaryEntry.class);
            query.setParameter("user", serviceUser);
            List<UserDictionaryEntry> entries = query.getResultList();
            if (entries.isEmpty()) {
                return null;
            }
            termReference = entries.get(random.nextInt(entries.size())).getTermReference();
        } else {
            TypedQuery<TermReference> query =
                entityManager.createQuery(
                    "select r "
                        + "from TermReference r "
                        + "join fetch r.term t "
                        + "join fetch r.termReference tr ",
                    TermReference.class);
            List<TermReference> entries = query.getResultList();
            if (entries.isEmpty()) {
                return null;
            }
            termReference = entries.get(random.nextInt(entries.size()));
        }
        if (TestType.ENGLISH.equals(testType)) {
            return Language.ENGLISH
                .equals(termReference.getTerm().getLanguage()) ?
                termReference.getTerm()
                : termReference.getTermReference();
        } else if (TestType.RUSSIAN.equals(testType)) {
            return Language.RUSSIAN
                .equals(termReference.getTerm().getLanguage()) ?
                termReference.getTerm()
                : termReference.getTermReference();
        } else {
            return random.nextBoolean() ? termReference.getTerm()
                : termReference.getTermReference();
        }
    }

    @Transactional
    public List<Term> findTranslations(String word, Language language) {
        Term term = new Term(null, word, language);
        List<TermReference> references = getTranslationReferences(word, language, null);
        return references.stream().filter(r -> r != null && r.getTerm() != null && r.getTermReference() != null ).map(
            r -> term.equals(r.getTerm())?r.getTermReference():r.getTerm()
        ).collect(Collectors.toList());
    }

    private List<TermReference> getTranslationReferences(String word, Language language, ServiceUser serviceUser) {
        TypedQuery<TermReference> query = entityManager.createQuery(
            "select r "
                + " from TermReference r "
                + " join fetch r.term t "
                + " join fetch r.termReference tr "
                + " where (t.term = :term and t.language = :language "
                + "   or tr.term = :term and tr.language = :language) "
                + (serviceUser != null?
                " and r not in (select e.termReference from UserDictionaryEntry e where e.user = :user) "
                :"")
            , TermReference.class);
        query.setParameter("term", word);
        query.setParameter("language", language);
        if (serviceUser != null) {
            query.setParameter("user", serviceUser);
        }
        return query.getResultList();
    }

    @Transactional
    public void addTermFromGlobalToUserDictionary(String word, Language language, ServiceUser serviceUser) {
        getTranslationReferences(word, language, serviceUser).forEach(
            ref -> entityManager.merge(new UserDictionaryEntry(null, serviceUser, ref, 0, 0))
        );
    }

    @Transactional
    public void updateTestResult(ServiceUser user, String word, Language language, int result) {
        final Query query = entityManager.createQuery("update UserDictionaryEntry e "
            + " set e.successCount = e.successCount + :result,"
                + " e.totalCount = e.totalCount + 1 "
            + " where e.user = :user and e.termReference = :reference");
        query.setParameter("user", user);
        query.setParameter("result", result);
        getTranslationReferences(word, language, null).forEach(
            r -> {
                query.setParameter("reference", r);
                query.executeUpdate();
            }
        );
        getTranslationReferences(word, language, user).forEach(
            ref -> entityManager.merge(new UserDictionaryEntry(null, user, ref, result, 1))
        );
    }

    public int getUserDictionarySizePercentile(ServiceUser serviceUser) {
        long userDictionarySize = getDictionarySize(serviceUser);
        int userCount = entityManager.createQuery("select cast(count(1) as integer) as cnt from ServiceUser u ", Integer.class).getSingleResult();
        Query query = entityManager.createNativeQuery(
            "select cast(count(1) as integer) as cnt "
            + " from ( "
                + " select cast(count(1) as integer), e.user_id "
                + " from dict.user_dictionary_entry e "
                + " group by e.user_id "
                + " having count(1) < :userDictSize) as a ");
        query.setParameter("userDictSize", userDictionarySize);
        int userCountWithLessDictionarySize = ((Number)query.getSingleResult()).intValue();

        return userCount > 0? userCountWithLessDictionarySize * 100 / userCount: 0;
    }

    public boolean checkUserNameExistsOnDevice(YASession session, String userName) {
        String userId =
            session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId())?
            session.getUser().getUserId() :
            null;
        TypedQuery<Integer> query = entityManager.createQuery(
            "select cast(count(1) as integer) as cnt "
            + " from ServiceUser u "
            + " where u.userSource = :userSource and lower(u.name) = :user"
            + (userId == null?" and u.extAppId = :extAppId ":"")
            + " and u.extUserId " + (userId == null? "is null ":  "= :extUserId"),
            Integer.class);
        query.setParameter("userSource", UserSource.YANDEX_ALICE);
        if (userId == null) {
            query.setParameter("extAppId", session.getApplication().getApplicationId());
        }
        if (userId != null) {
            query.setParameter("extUserId", userId);
        }
        query.setParameter("user", userName.toLowerCase());

        return query.getSingleResult() > 0;
    }

    public ServiceUser addNewUser(YASession session, String userName) {
        String extUserId =
            session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId()) ?
                session.getUser().getUserId().trim() : null;
        ServiceUser user = new ServiceUser(null, userName, UserSource.YANDEX_ALICE,
            extUserId, session.getApplication().getApplicationId(), "Y");
        entityManager.persist(user);

        Query userUpdateQuery = entityManager.createQuery("update ServiceUser u set u.lastUsed = 'N' "
            + " where u.extAppId = :extAppId "
            + " and u.extUserId " + (extUserId == null? "is null":" = :extUserId ")
            + " and u.id <> :id ");
        userUpdateQuery.setParameter("extAppId", session.getApplication().getApplicationId());
        if (extUserId != null) {
            userUpdateQuery.setParameter("extUserId", extUserId);
        }
        userUpdateQuery.setParameter("id", user.getId());
        userUpdateQuery.executeUpdate();

        return user;
    }

    public int getDeviceUserCount(YASession session) {
        TypedQuery<Integer> query = entityManager.createQuery("select cast(count(1) as integer) "
            + " from ServiceUser u "
            + " where "
            + (session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId())?
                " extUserId = :userId ":
                " extAppId = :appId and extUserId is null "), Integer.class);
        if (session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId())) {
            query.setParameter("userId", session.getUser().getUserId());
        } else {
            query.setParameter("appId", session.getApplication().getApplicationId());
        }
        return query.getSingleResult();
    }

    public ServiceUser getDeviceUserByName(YASession session, String name) {
        TypedQuery<ServiceUser> query = entityManager.createQuery("select u "
            + " from ServiceUser u "
            + " where lower(u.name) = :name and "
            + (session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId())?
            " u.extUserId = :userId ":
            " u.extAppId = :appId and u.extUserId is null "), ServiceUser.class);
        query.setParameter("name", name.toLowerCase());
        if (session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId())) {
            query.setParameter("userId", session.getUser().getUserId());
        } else {
            query.setParameter("appId", session.getApplication().getApplicationId());
        }
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<ServiceUser> getDeviceUsers(YASession session) {
        return null;
    }
}
