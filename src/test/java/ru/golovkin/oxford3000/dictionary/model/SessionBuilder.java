package ru.golovkin.oxford3000.dictionary.model;

public class SessionBuilder {
    private final Session session;

    public SessionBuilder(String sessionId) {
        this.session = new Session(SessionState.INITIAL, sessionId);
    }

    public SessionBuilder withState(SessionState state) {
        session.setState(state);
        return this;
    }

    public SessionBuilder withUser(ServiceUser user) {
        session.setServiceUser(user);
        return this;
    }

    public Session please() {
        return session;
    }

    public SessionBuilder withWord(String word) {
        session.setWord(word);
        return this;
    }

    public SessionBuilder withLanguage(Language language) {
        session.setLanguage(language);
        return this;
    }

    public SessionBuilder withTestType(TestType testType) {
        session.setTestType(testType);
        return this;
    }

    public SessionBuilder withTestCount(int count) {
        session.setTestCount(count);
        return this;
    }

    public SessionBuilder withSuccessTestCount(int count) {
        session.setSuccessTestCount(count);
        return this;
    }

    public SessionBuilder withSuccessTestCountInRaw(int count) {
        session.setSuccessTestCountInRaw(count);
        return this;
    }

    public SessionBuilder withTestDictionary(TestDictionary testDictionary) {
        session.setTestDictionary(testDictionary);
        return this;
    }
}
