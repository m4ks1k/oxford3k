package ru.golovkin.oxford3000.dictionary.model;

import ru.golovkin.oxford3000.dictionary.model.yandexalice.YASkillResponse;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceResponse;

public class YandexAliceResponseBuilder {
    private final YandexAliceResponse response = new YandexAliceResponse(
            new YASkillResponse(null,null, null, false), "1.0");

    public YandexAliceResponseBuilder(String text) {
        response.getResponse().setText(text);
    }

    public YandexAliceResponse please() {
        return response;
    }
}
