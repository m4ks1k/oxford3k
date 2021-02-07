package ru.golovkin.oxford3000.dictionary.model;

import java.util.Arrays;
import java.util.stream.Collectors;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YAApplication;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YAEntity;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YAMetadata;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YANaturalLanguageUnderstanding;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YASession;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YASkillRequest;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YAUser;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceRequest;

public class YandexAliceRequestBuilder {
    private final YandexAliceRequest request = new YandexAliceRequest(new YAMetadata(), new YASkillRequest(), new YASession(), "1.0");

    public YandexAliceRequestBuilder(String appId) {
        request.getSession().setApplication(new YAApplication(appId));
    }

    public YandexAliceRequestBuilder withSessionId(String sessionId) {
        request.getSession().setSessionId(sessionId);
        return this;
    }

    public YandexAliceRequest please() {
        return request;
    }

    public YandexAliceRequestBuilder withNewSession(String sessionId) {
        request.getSession().setSessionId(sessionId);
        request.getSession().setNew(true);
        return this;
    }

    public YandexAliceRequestBuilder withUserId(String userId) {
        YAUser user = new YAUser();
        user.setUserId(userId);
        request.getSession().setUser(user);
        return this;
    }

    public YandexAliceRequestBuilder withUtterance(String name) {
        YASkillRequest skillRequest = request.getRequest();
        skillRequest.setCommand(name);
        if (skillRequest.getNlu() == null) {
            skillRequest.setNlu(new YANaturalLanguageUnderstanding());
        }
        getYaNaturalLanguageUnderstanding(skillRequest).getTokens().addAll(
            Arrays.stream(name.split(" ")).map(String::toLowerCase).collect(Collectors.toList()));
        return this;
    }

    private YANaturalLanguageUnderstanding getYaNaturalLanguageUnderstanding(
        YASkillRequest skillRequest) {
        YANaturalLanguageUnderstanding nlu = skillRequest.getNlu();
        if (nlu == null) {
            nlu = new YANaturalLanguageUnderstanding();
            skillRequest.setNlu(nlu);
        }
        return nlu;
    }

    public YandexAliceRequestBuilder with(YAEntity entity) {
        YASkillRequest skillRequest = request.getRequest();
        YANaturalLanguageUnderstanding nlu = getYaNaturalLanguageUnderstanding(skillRequest);
        nlu.getEntities().add(entity);
        return this;
    }
}
