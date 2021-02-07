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
public class YASession {
    @ApiModelProperty(required = true)
    @JsonProperty("message_id")
    int messageId;
    @JsonProperty("session_id")
    @ApiModelProperty(required = true, name = "session_id")
    String sessionId;
    @JsonProperty("skill_id")
    @ApiModelProperty(name = "skill_id")
    String skillId;
    @ApiModelProperty
    YAUser user;
    @ApiModelProperty
    YAApplication application;
    @ApiModelProperty(required = true, name = "new")
    boolean isNew;
}
