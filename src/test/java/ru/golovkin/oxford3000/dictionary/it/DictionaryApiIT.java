package ru.golovkin.oxford3000.dictionary.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DictionaryApiIT {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper jsonMapper;
    @Autowired
    EntityManager entityManager;

    @Test
    @SneakyThrows
    @Transactional
    void should_accept_request_from_yandex_alice_and_return_valid_response() {
        mockMvc.perform(
            post("/api/v1/dictionary/yandex-alice-skill/")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content("{\n"
                + "  \"meta\": {\n"
                + "    \"locale\": \"ru-RU\",\n"
                + "    \"timezone\": \"Europe/Moscow\",\n"
                + "    \"client_id\": \"ru.yandex.searchplugin/5.80 (Samsung Galaxy; Android 4.4)\",\n"
                + "    \"interfaces\": {\n"
                + "      \"screen\": { },\n"
                + "      \"account_linking\": { }\n"
                + "    }\n"
                + "  },\n"
                + "  \"request\": {\n"
                + "    \"command\": \"закажи пиццу на улицу льва толстого 16 на завтра\",\n"
                + "    \"original_utterance\": \"закажи пиццу на улицу льва толстого, 16 на завтра\",\n"
                + "    \"type\": \"SimpleUtterance\",\n"
                + "    \"markup\": {\n"
                + "      \"dangerous_context\": true\n"
                + "    },\n"
                + "    \"payload\": {},\n"
                + "    \"nlu\": {\n"
                + "      \"tokens\": [\n"
                + "        \"закажи\",\n"
                + "        \"пиццу\",\n"
                + "        \"на\",\n"
                + "        \"льва\",\n"
                + "        \"толстого\",\n"
                + "        \"16\",\n"
                + "        \"на\",\n"
                + "        \"завтра\"\n"
                + "      ],\n"
                + "      \"entities\": [\n"
                + "        {\n"
                + "          \"tokens\": {\n"
                + "            \"start\": 2,\n"
                + "            \"end\": 6\n"
                + "          },\n"
                + "          \"type\": \"YANDEX.GEO\",\n"
                + "          \"value\": {\n"
                + "            \"house_number\": \"16\",\n"
                + "            \"street\": \"льва толстого\"\n"
                + "          }\n"
                + "        },\n"
                + "        {\n"
                + "          \"tokens\": {\n"
                + "            \"start\": 3,\n"
                + "            \"end\": 5\n"
                + "          },\n"
                + "          \"type\": \"YANDEX.FIO\",\n"
                + "          \"value\": {\n"
                + "            \"first_name\": \"лев\",\n"
                + "            \"last_name\": \"толстой\"\n"
                + "          }\n"
                + "        },\n"
                + "        {\n"
                + "          \"tokens\": {\n"
                + "            \"start\": 5,\n"
                + "            \"end\": 6\n"
                + "          },\n"
                + "          \"type\": \"YANDEX.NUMBER\",\n"
                + "          \"value\": 16\n"
                + "        },\n"
                + "        {\n"
                + "          \"tokens\": {\n"
                + "            \"start\": 6,\n"
                + "            \"end\": 8\n"
                + "          },\n"
                + "          \"type\": \"YANDEX.DATETIME\",\n"
                + "          \"value\": {\n"
                + "            \"day\": 1,\n"
                + "            \"day_is_relative\": true\n"
                + "          }\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"session\": {\n"
                + "    \"message_id\": 0,\n"
                + "    \"session_id\": \"2eac4854-fce721f3-b845abba-20d60\",\n"
                + "    \"skill_id\": \"3ad36498-f5rd-4079-a14b-788652932056\",\n"
                + "    \"user\": {\n"
                + "      \"user_id\": \"6C91DA5198D1758C6A9F63A7C5CDDF09359F683B13A18A151FBF4C8B092BB0C2\",\n"
                + "      \"access_token\": \"AgAAAAAB4vpbAAApoR1oaCd5yR6eiXSHqOGT8dT\"\n"
                + "    },\n"
                + "    \"application\": {\n"
                + "      \"application_id\": \"47C73714B580ED2469056E71081159529FFC676A4E5B059D629A819E857DC2F8\"\n"
                + "    },\n"
                + "    \"new\": true\n"
                + "  },\n"
                + "  \"version\": \"1.0\"\n"
                + "}")
        ).andExpect(status().isOk())
            .andExpect(content().json("{\n"
                + "  \"response\": {\n"
                + "    \"text\": \"Привет! Я знаю три тысячи самых важных английских слов по версии Оксфордского университета и помогу вам их выучить. Как вас зовут?\",\n"
                + "    \"end_session\": false\n"
                + "  },\n"
                + "  \"version\": \"1.0\"\n"
                + "}"));
    }
}
