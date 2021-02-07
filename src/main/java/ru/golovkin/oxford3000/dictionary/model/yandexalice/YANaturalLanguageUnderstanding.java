package ru.golovkin.oxford3000.dictionary.model.yandexalice;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YANaturalLanguageUnderstanding {
    List<String> tokens = new ArrayList<>();
    List<YAEntity> entities = new ArrayList<>();
}
