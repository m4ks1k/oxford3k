package ru.golovkin.oxford3000.dictionary.model;

public class TestObjectBuilder {

    public YandexAliceRequestBuilder yaRequest(String appId) {
        return new YandexAliceRequestBuilder(appId);
    }
    public YandexAliceResponseBuilder yaResponse(String text) {
        return new YandexAliceResponseBuilder(text);
    }

    public SessionBuilder session(String sessionId) {
        return new SessionBuilder(sessionId);
    }

    public YandexEntityBuilder entity(String type) {
        return new YandexEntityBuilder(type);
    }
}
