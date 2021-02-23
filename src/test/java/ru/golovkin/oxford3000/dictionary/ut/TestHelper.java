package ru.golovkin.oxford3000.dictionary.ut;

import ru.golovkin.oxford3000.dictionary.model.ServiceUser;
import ru.golovkin.oxford3000.dictionary.model.SessionBuilder;
import ru.golovkin.oxford3000.dictionary.model.TestObjectBuilder;
import ru.golovkin.oxford3000.dictionary.model.UserSource;

public class TestHelper {
    protected final String sessionId = "sessionId";
    protected final TestObjectBuilder New = new TestObjectBuilder();
    protected final String appId = "appId1";
    protected final String userId = "userId1";
    protected ServiceUser serviceUser;

    protected ServiceUser createDefaultUser(String name, String userId, String appId) {
        return createDefaultUser(name, userId, appId, true);
    }

    protected ServiceUser createDefaultUser() {
        return createDefaultUser("Name", userId, appId);
    }

    protected SessionBuilder newSession() {
        return New.session(sessionId).withUser(serviceUser);
    }

    protected ServiceUser createDefaultUser(String name, String userId, String appId,
        boolean isLastUsed) {
        return new ServiceUser(null, name, UserSource.YANDEX_ALICE, userId, appId,
            isLastUsed ? "Y" : "N");
    }
}
