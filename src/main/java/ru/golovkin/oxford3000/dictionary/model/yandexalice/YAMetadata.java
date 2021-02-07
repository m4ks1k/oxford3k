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
public class YAMetadata {
    @ApiModelProperty(required = true)
    String locale;
    @ApiModelProperty(required = true)
    String timezone;
    @JsonProperty("client_id")
    @ApiModelProperty(name = "client_id")
    String clientId;
}
