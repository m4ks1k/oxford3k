package ru.golovkin.oxford3000.dictionary.model;

import io.swagger.annotations.ApiModelProperty;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "term_reference", schema = "dict")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TermReference {

    @Id
    @Column(name = "id", columnDefinition = "bigint")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(dataType = "Long")
    @EqualsAndHashCode.Exclude
    @Nullable
    Long id;

    @ManyToOne(optional = false, cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    @JoinColumn(name = "term_id", columnDefinition = "bigint", nullable = false)
    @ApiModelProperty(dataType = "Long")
    @NonNull
    Term term;


    @ManyToOne(optional = false, cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    @JoinColumn(name = "term_reference_id", columnDefinition = "bigint", nullable = false)
    @ApiModelProperty(dataType = "Long")
    @NonNull
    Term termReference;
}
