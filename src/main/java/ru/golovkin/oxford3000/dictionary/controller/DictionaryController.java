package ru.golovkin.oxford3000.dictionary.controller;

import static lombok.AccessLevel.PRIVATE;

import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceRequest;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceResponse;
import ru.golovkin.oxford3000.dictionary.service.DictionaryService;

@RestController
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Slf4j
@RequestMapping("/api/v1/dictionary")
public class DictionaryController {
    @Autowired
    private DictionaryService dictionaryService;

    @PostMapping
    @ApiOperation(value = "ResponseToYandexAliceWebhook", notes = "Webhook for Yandex Alice Skill")
    @RequestMapping("/yandex-alice-skill")
    public YandexAliceResponse talkYandexAlice(@RequestBody YandexAliceRequest request) {
        return dictionaryService.talkYandexAlice(request);
    }
}
