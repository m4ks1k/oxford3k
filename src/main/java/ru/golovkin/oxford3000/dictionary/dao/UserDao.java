package ru.golovkin.oxford3000.dictionary.dao;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.golovkin.oxford3000.dictionary.model.ServiceUser;
import ru.golovkin.oxford3000.dictionary.model.Session;
import ru.golovkin.oxford3000.dictionary.model.SessionState;
import ru.golovkin.oxford3000.dictionary.model.UserSource;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YASession;

@Slf4j
@Repository
public class UserDao {
    @Autowired
    EntityManager entityManager;

    @Transactional
    public void updateSessionState(Session session) {
        log.info("Saving {}", session);
        entityManager.merge(session);
    }

    public boolean checkUserNameExistsOnDevice(YASession session, String userName) {
        String userId =
            session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId()) ?
                session.getUser().getUserId() :
                null;
        TypedQuery<Integer> query = entityManager.createQuery(
            "select cast(count(1) as integer) as cnt "
                + " from ServiceUser u "
                + " where u.userSource = :userSource and lower(u.name) = :user"
                + (userId == null ? " and u.extAppId = :extAppId " : "")
                + " and u.extUserId " + (userId == null ? "is null " : "= :extUserId"),
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

    @Transactional
    public ServiceUser addNewUser(YASession session, String userName) {
        String extUserId =
            session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId()) ?
                session.getUser().getUserId().trim() : null;
        ServiceUser user = new ServiceUser(null, userName, UserSource.YANDEX_ALICE,
            extUserId, session.getApplication().getApplicationId(), "Y");
        entityManager.persist(user);

        Query userUpdateQuery = entityManager
            .createQuery("update ServiceUser u set u.lastUsed = 'N' "
                + " where u.extAppId = :extAppId "
                + " and u.extUserId " + (extUserId == null ? "is null" : " = :extUserId ")
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
        TypedQuery<Integer> query = entityManager
            .createQuery("select cast(count(1) as integer) "
                + " from ServiceUser u "
                + " where "
                + (session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId()) ?
                " extUserId = :userId " :
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
            + (session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId()) ?
            " u.extUserId = :userId " :
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
        TypedQuery<ServiceUser> query = entityManager.createQuery("select u "
            + " from ServiceUser u "
            + " where "
            + (session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId()) ?
            " u.extUserId = :userId " :
            " u.extAppId = :appId and u.extUserId is null "
        )
            + " order by u.name", ServiceUser.class);
        if (session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId())) {
            query.setParameter("userId", session.getUser().getUserId());
        } else {
            query.setParameter("appId", session.getApplication().getApplicationId());
        }
        return query.getResultList();
    }

    @Transactional
    public void clearDeviceUserLastUsedFlag(YASession session, ServiceUser user) {
        String extUserId =
            session.getUser() != null && Strings.isNotBlank(session.getUser().getUserId()) ? session
                .getUser().getUserId().trim() : null;
        Query userUpdateQuery = entityManager
            .createQuery("update ServiceUser u set u.lastUsed = 'N' "
                + " where u.extAppId = :extAppId "
                + " and u.extUserId " + (extUserId == null ? "is null" : " = :extUserId ")
                + " and u.id <> :id ");
        userUpdateQuery.setParameter("extAppId", session.getApplication().getApplicationId());
        if (extUserId != null) {
            userUpdateQuery.setParameter("extUserId", extUserId);
        }
        userUpdateQuery.setParameter("id", user.getId());
        userUpdateQuery.executeUpdate();

    }

    @Transactional
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
}