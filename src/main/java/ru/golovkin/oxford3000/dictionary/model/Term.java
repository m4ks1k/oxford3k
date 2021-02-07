package ru.golovkin.oxford3000.dictionary.model;

import io.swagger.annotations.ApiModelProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.lang.Nullable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "term", schema = "dict")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Term {

    @Id
    @Column(name = "ID", columnDefinition = "bigint")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(dataType = "Long")
    @EqualsAndHashCode.Exclude
    @Nullable
    Long id;

    @Column(name = "term", columnDefinition = "varchar(255)", nullable = false)
    @ApiModelProperty(dataType = "String", required = true)
    @NonNull
    String term;

    @Enumerated(EnumType.STRING)
    @Column(name = "LANGUAGE", columnDefinition = "varchar(32)", nullable = false)
    @NonNull
    Language language;
}
