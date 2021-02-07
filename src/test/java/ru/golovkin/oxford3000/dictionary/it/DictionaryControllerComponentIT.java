package ru.golovkin.oxford3000.dictionary.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.golovkin.oxford3000.dictionary.controller.DictionaryController;
import ru.golovkin.oxford3000.dictionary.model.Term;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceRequest;
import ru.golovkin.oxford3000.dictionary.model.yandexalice.YandexAliceResponse;
import ru.golovkin.oxford3000.dictionary.service.DictionaryService;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(DictionaryController.class)
@ActiveProfiles("it")
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DictionaryControllerComponentIT {
    @Autowired
    DictionaryController dictionaryController;
    @MockBean
    DictionaryService dictionaryServiceMock;
    @Mock
    Term term;

    @Mock
    private YandexAliceResponse yaResponseMock;
    @Mock
    private YandexAliceRequest yaRequestMock;

    @Test
    void should_return_response_from_dictionary_service_when_calling_talk_yandex_alice() {
        when(dictionaryServiceMock.talkYandexAlice(any(YandexAliceRequest.class))).thenReturn(yaResponseMock);

        assertThat(dictionaryController.talkYandexAlice(yaRequestMock)).isEqualTo(yaResponseMock);
        verify(dictionaryServiceMock, only()).talkYandexAlice(yaRequestMock);
    }
}
