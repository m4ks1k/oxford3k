package ru.golovkin.oxford3000.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.golovkin.oxford3000.dictionary.Oxford3kApplication;

@ContextConfiguration(classes = Oxford3kApplication.class)
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class Oxford3kAppTest {

    @Test
    void should_start_context() {

    }
}
