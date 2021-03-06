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
public class YAUser {
    @ApiModelProperty(required = true, name = "user_id")
    @JsonProperty("user_id")
    String userId;
    @ApiModelProperty(required = true, name = "access_token")
    @JsonProperty("access_token")
    String accessToken;
}
