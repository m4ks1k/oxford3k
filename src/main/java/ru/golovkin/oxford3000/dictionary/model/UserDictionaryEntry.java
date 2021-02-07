package ru.golovkin.oxford3000.dictionary.model;

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
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "USER_DICTIONARY_ENTRY", schema = "DICT")
public class UserDictionaryEntry {
    @Id
    @Column(columnDefinition = "bigint", name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    @Nullable
    Long id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "USER_ID", columnDefinition = "bigint", nullable = false, referencedColumnName = "ID")
    @NonNull
    ServiceUser user;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "TERM_REFERENCE_ID", columnDefinition = "bigint", nullable = false, referencedColumnName = "ID")
    @NonNull
    TermReference termReference;

    @Column(name = "SUCCESS_COUNT", columnDefinition = "integer", nullable = false)
    int successCount;

    @Column(name = "TOTAL_COUNT", columnDefinition = "integer", nullable = false)
    int totalCount;
}
