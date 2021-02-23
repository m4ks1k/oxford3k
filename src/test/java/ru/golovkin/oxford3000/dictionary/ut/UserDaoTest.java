package ru.golovkin.oxford3000.dictionary.ut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
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
import ru.golovkin.oxford3000.dictionary.dao.UserDao;
import ru.golovkin.oxford3000.dictionary.model.ServiceUser;
import ru.golovkin.oxford3000.dictionary.model.Session;
import ru.golovkin.oxford3000.dictionary.model.SessionState;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class UserDaoTest extends TestHelper {
    private Session session;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserDao sut;


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

    @Test
    @Transactional
    void should_return_session_with_state_PENDING_NEW_TERM_and_linked_user_with_last_used_flag_set_when_calling_getSessionState_and_exists_two_users_with_appId_and_empty_user_id() {
        serviceUser = createDefaultUser("петя", null, appId);
        ServiceUser anotherServiceUser = createDefaultUser("коля", null, appId, false);
        entityManager.persist(serviceUser);
        entityManager.persist(anotherServiceUser);

        Session state = sut.getSessionState(New.yaRequest(appId).withSessionId(sessionId).please().getSession());

        assertEquals(serviceUser, state.getServiceUser());
        entityManager.refresh(anotherServiceUser);
        assertEquals("N", anotherServiceUser.getLastUsed());
    }

    @Test
    @Transactional
    void should_return_session_with_state_PENDING_NEW_TERM_and_linked_user_with_last_used_flag_set_when_calling_getSessionState_and_exists_two_users_with_appId_and_user_id() {
        serviceUser = createDefaultUser("петя", userId, appId);
        ServiceUser anotherServiceUser = createDefaultUser("коля", userId, appId, false);
        entityManager.persist(serviceUser);
        entityManager.persist(anotherServiceUser);

        Session state = sut.getSessionState(New.yaRequest(appId).withUserId(userId).withSessionId(sessionId).please().getSession());

        assertEquals(serviceUser, state.getServiceUser());
        entityManager.refresh(anotherServiceUser);
        assertEquals("N", anotherServiceUser.getLastUsed());
    }

    @Test
    @Transactional
    void should_return_1_when_calling_getDeviceUserCount_and_only_one_user_exists_with_same_appId_and_empty_userId() {
        serviceUser = createDefaultUser("коля", null, appId);
        entityManager.persist(serviceUser);
        ServiceUser anotherServiceUser = createDefaultUser("петя", null, appId + "1");
        entityManager.persist(anotherServiceUser);

        assertEquals(1, sut.getDeviceUserCount(New.yaRequest(appId).withSessionId(sessionId).please().getSession()));
    }

    @Test
    @Transactional
    void should_return_2_when_calling_getDeviceUserCount_and_two_users_exist_with_same_appId_and_empty_userId() {
        serviceUser = createDefaultUser("коля", null, appId);
        entityManager.persist(serviceUser);
        ServiceUser anotherServiceUser = createDefaultUser("петя", null, appId);
        entityManager.persist(anotherServiceUser);

        assertEquals(2, sut.getDeviceUserCount(New.yaRequest(appId).withSessionId(sessionId).please().getSession()));
    }

    @Test
    @Transactional
    void should_return_1_when_calling_getDeviceUserCount_and_only_one_user_exists_with_same_userId() {
        serviceUser = createDefaultUser("коля", userId, appId);
        entityManager.persist(serviceUser);
        ServiceUser anotherServiceUser = createDefaultUser("петя", userId + "1", appId);
        entityManager.persist(anotherServiceUser);

        assertEquals(1, sut.getDeviceUserCount(New.yaRequest(appId).withUserId(userId).withSessionId(sessionId).please().getSession()));
    }

    @Test
    @Transactional
    void should_return_2_when_calling_getDeviceUserCount_and_two_users_exist_with_same_userId() {
        serviceUser = createDefaultUser("коля", userId, appId);
        entityManager.persist(serviceUser);
        ServiceUser anotherServiceUser = createDefaultUser("петя", userId, appId + "1");
        entityManager.persist(anotherServiceUser);

        assertEquals(2, sut.getDeviceUserCount(New.yaRequest(appId).withUserId(userId).withSessionId(sessionId).please().getSession()));
    }

    @Test
    @Transactional
    void should_return_user_by_name_and_appId_when_calling_getDeviceUserByName_and_userId_from_session_is_empty() {
        ServiceUser userToReturn = createDefaultUser("петя", null, appId);
        entityManager.persist(userToReturn);
        entityManager.persist(createDefaultUser("петя", userId, appId));
        entityManager.persist(createDefaultUser("коля", null, appId));

        assertEquals(userToReturn, sut.getDeviceUserByName(New.yaRequest(appId).withSessionId(sessionId).please().getSession(), "петя"));
    }

    @Test
    @Transactional
    void should_return_null_when_calling_getDeviceUserByName_and_userId_from_session_is_empty_and_no_records_in_table_user_with_such_name_and_appId() {
        entityManager.persist(createDefaultUser("петя", userId, appId));
        entityManager.persist(createDefaultUser("петя", null, appId + "1"));
        entityManager.persist(createDefaultUser("коля", null, appId));

        assertNull(sut.getDeviceUserByName(New.yaRequest(appId).withSessionId(sessionId).please().getSession(), "петя"));
    }

    @Test
    @Transactional
    void should_return_user_by_name_and_userId_when_calling_getDeviceUserByName_and_userId_from_session_is_not_empty() {
        ServiceUser userToReturn = createDefaultUser("петя", userId, appId + "1");
        entityManager.persist(userToReturn);
        entityManager.persist(createDefaultUser("петя", userId + "1", appId));
        entityManager.persist(createDefaultUser("петя", null, appId));
        entityManager.persist(createDefaultUser("коля", userId, appId));

        assertEquals(userToReturn, sut.getDeviceUserByName(New.yaRequest(appId).withUserId(userId).withSessionId(sessionId).please().getSession(), "петя"));
    }

    @Test
    @Transactional
    void should_return_null_when_calling_getDeviceUserByName_and_userId_from_session_is_not_empty_and_no_records_in_table_user_with_such_name_and_appId() {
        entityManager.persist(createDefaultUser("петя", null, appId));
        entityManager.persist(createDefaultUser("петя", userId + "1", appId));
        entityManager.persist(createDefaultUser("коля", userId, appId));

        assertNull(sut.getDeviceUserByName(New.yaRequest(appId).withUserId(userId).withSessionId(sessionId).please().getSession(), "петя"));
    }

    @Test
    @Transactional
    void should_return_empty_list_when_calling_getDeviceUsers_for_session_with_empty_userId_and_no_record_for_such_appId_in_table_users() {
        entityManager.persist(createDefaultUser("вася", null, appId + "1"));
        entityManager.persist(createDefaultUser("петя", userId, appId));

        assertEquals(Collections.emptyList(), sut.getDeviceUsers(New.yaRequest(appId).withSessionId(sessionId).please().getSession()));
    }

    @Test
    @Transactional
    void should_return_empty_list_when_calling_getDeviceUsers_for_session_with_non_empty_userId_and_no_record_for_such_userId_in_table_users() {
        entityManager.persist(createDefaultUser("вася", userId + "1", appId));
        entityManager.persist(createDefaultUser("петя", null, appId));

        assertEquals(Collections.emptyList(), sut.getDeviceUsers(New.yaRequest(appId).withUserId(userId).withSessionId(sessionId).please().getSession()));
    }

    @Test
    @Transactional
    void should_return_list_of_two_users_when_calling_getDeviceUsers_for_session_with_non_empty_userId_and_2_records_exist_for_such_userId_in_table_users() {
        entityManager.persist(createDefaultUser("вася", userId + "1", appId));
        entityManager.persist(createDefaultUser("петя", null, appId));
        ServiceUser user1 = createDefaultUser("коля", userId, appId);
        entityManager.persist(user1);
        ServiceUser user2 = createDefaultUser("гена", userId, appId + "1");
        entityManager.persist(user2);

        assertEquals(
            Arrays.asList(user2, user1), sut.getDeviceUsers(New.yaRequest(appId).withUserId(userId).withSessionId(sessionId).please().getSession()));
    }

    @Test
    @Transactional
    void should_return_list_of_two_users_when_calling_getDeviceUsers_for_session_with_empty_userId_and_2_records_exist_for_such_appId_in_table_users() {
        entityManager.persist(createDefaultUser("вася", userId, appId));
        entityManager.persist(createDefaultUser("петя", null, appId + "1"));
        ServiceUser user1 = createDefaultUser("коля", null, appId);
        entityManager.persist(user1);
        ServiceUser user2 = createDefaultUser("гена", null, appId);
        entityManager.persist(user2);

        assertEquals(Arrays.asList(user2, user1), sut.getDeviceUsers(New.yaRequest(appId).withSessionId(sessionId).please().getSession()));
    }

    @Test
    @Transactional
    void should_update_last_used_flag_to_N_for_all_users_with_same_appId_and_empty_userId_except_provided_when_calling_clearDeviceUserLastUsedFlag_with_empty_userId_in_session() {
        ServiceUser userToClearFlag = createDefaultUser("вася", null, appId);
        ServiceUser userToNotTouchFlag1 = createDefaultUser("вася", null, appId + "1");
        ServiceUser userToNotTouchFlag2 = createDefaultUser("вася", userId, appId);
        ServiceUser userToExclude = createDefaultUser("коля", null, appId);
        entityManager.persist(userToClearFlag);
        entityManager.persist(userToNotTouchFlag1);
        entityManager.persist(userToNotTouchFlag2);
        entityManager.persist(userToExclude);

        sut.clearDeviceUserLastUsedFlag(New.yaRequest(appId).withSessionId(sessionId).please().getSession(), userToExclude);

        entityManager.refresh(userToClearFlag);
        entityManager.refresh(userToNotTouchFlag1);
        entityManager.refresh(userToNotTouchFlag2);
        entityManager.refresh(userToExclude);
        assertEquals("N", userToClearFlag.getLastUsed());
        assertEquals("Y", userToNotTouchFlag1.getLastUsed());
        assertEquals("Y", userToNotTouchFlag2.getLastUsed());
        assertEquals("Y", userToExclude.getLastUsed());
    }

    @Test
    @Transactional
    void should_update_last_used_flag_to_N_for_all_users_with_same_userId_except_provided_when_calling_clearDeviceUserLastUsedFlag_with_non_empty_userId_in_session() {
        ServiceUser userToClearFlag = createDefaultUser("вася", userId, appId);
        ServiceUser userToNotTouchFlag1 = createDefaultUser("вася", userId + "1", appId);
        ServiceUser userToNotTouchFlag2 = createDefaultUser("вася", null, appId);
        ServiceUser userToExclude = createDefaultUser("коля", userId, appId);
        entityManager.persist(userToClearFlag);
        entityManager.persist(userToNotTouchFlag1);
        entityManager.persist(userToNotTouchFlag2);
        entityManager.persist(userToExclude);

        sut.clearDeviceUserLastUsedFlag(New.yaRequest(appId).withSessionId(sessionId).withUserId(userId).please().getSession(), userToExclude);

        entityManager.refresh(userToClearFlag);
        entityManager.refresh(userToNotTouchFlag1);
        entityManager.refresh(userToNotTouchFlag2);
        entityManager.refresh(userToExclude);
        assertEquals("N", userToClearFlag.getLastUsed());
        assertEquals("Y", userToNotTouchFlag1.getLastUsed());
        assertEquals("Y", userToNotTouchFlag2.getLastUsed());
        assertEquals("Y", userToExclude.getLastUsed());
    }
}
