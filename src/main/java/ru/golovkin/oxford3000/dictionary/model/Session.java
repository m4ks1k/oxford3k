package ru.golovkin.oxford3000.dictionary.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.lang.Nullable;

@Table(name = "SESSION_STATE", schema = "DICT")
@Entity
@RequiredArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    @Nullable
    @Column(name = "ID", columnDefinition = "bigint", nullable = false)
    Long id;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    @NonNull
    @Enumerated(EnumType.STRING)
    SessionState state;

    @Column(columnDefinition = "varchar(255)", nullable = false, name = "SESSION_ID")
    @NonNull
    String sessionId;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(columnDefinition = "bigint", name = "SERVICE_USER_ID", referencedColumnName = "ID", nullable = false)
    ServiceUser serviceUser;

    @Column(columnDefinition = "varchar(255)", name = "WORD")
    @Nullable
    String word;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(32)", name = "LANGUAGE")
    @Nullable
    Language language;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(32)", name = "TEST_TYPE")
    @Nullable
    TestType testType;

    @Column(columnDefinition = "integer", name = "TEST_TOTAL_COUNT")
    int testCount;

    @Column(columnDefinition = "integer", name = "TEST_SUCCESS_COUNT")
    int successTestCount;

    @Column(columnDefinition = "integer", name = "TEST_SUCCESS_COUNT_IN_RAW")
    int successTestCountInRaw;

    @Column(columnDefinition = "varchar(32)", name = "TEST_DICTIONARY")
    @Enumerated(EnumType.STRING)
    TestDictionary testDictionary = TestDictionary.COMMON;

    public void clearWordInfo() {
        word = null;
        language = null;
    }
}
