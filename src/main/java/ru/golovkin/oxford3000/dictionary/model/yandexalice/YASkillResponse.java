package ru.golovkin.oxford3000.dictionary.model.yandexalice;

import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@AllArgsConstructor
@NoArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YASkillResponse {
    @ApiModelProperty(required = true)
    String text;
    @ApiModelProperty
    String tts;
    @ApiModelProperty
    List<YAButton> buttons;
    @JsonProperty("end_session")
    @ApiModelProperty(name = "end_session", required = true)
    boolean endSession = false;
}
