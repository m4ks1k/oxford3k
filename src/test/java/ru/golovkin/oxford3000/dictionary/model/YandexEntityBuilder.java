package ru.golovkin.oxford3000.dictionary.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YAEntity;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YAToken;

public class YandexEntityBuilder {
    private final YAEntity entity;
    public YandexEntityBuilder(String type) {
        entity = new YAEntity(new YAToken(), type, null);
    }

    public YandexEntityBuilder from(int from) {
        entity.getToken().setStart(from);
        return this;
    }

    public YandexEntityBuilder to(int to) {
        entity.getToken().setEnd(to);
        return this;
    }

    @SneakyThrows
    public YandexEntityBuilder withValue(String json) {
        ObjectMapper mapper = new ObjectMapper();
        entity.setValue(mapper.readTree(json));
        return this;
    }

    public YAEntity please() {
        return entity;
    }
}
