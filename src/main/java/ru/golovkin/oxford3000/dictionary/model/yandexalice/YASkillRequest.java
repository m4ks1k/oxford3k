package ru.golovkin.oxford3000.dictionary.model.yandexalice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YASkillRequest {
    @ApiModelProperty(required = true)
    String command;
    @JsonProperty("original_utterance")
    @ApiModelProperty(required = true, name = "original_utterance")
    String originalUtterance;
    @ApiModelProperty(required = true)
    YARequestType type;
    @ApiModelProperty
    YARequestMarkup markup;
    YANaturalLanguageUnderstanding nlu;
}
