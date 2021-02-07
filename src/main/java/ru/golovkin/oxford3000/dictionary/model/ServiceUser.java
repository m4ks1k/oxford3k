package ru.golovkin.oxford3000.dictionary.model;

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

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user", schema = "dict")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    @Nullable
    @Column(name = "ID", columnDefinition = "bigint", nullable = false)
    Long id;

    @Column(columnDefinition = "varchar(255)", name = "USER_NAME")
    @Nullable
    String name;

    @Column(columnDefinition = "varchar(255)", name = "EXT_USER_SOURCE")
    @NonNull
    @Enumerated(EnumType.STRING)
    UserSource userSource;

    @Column(columnDefinition = "varchar(255)", name = "EXT_USER_ID")
    @Nullable
    String extUserId;

    @Column(columnDefinition = "varchar(255)", name = "EXT_APPLICATION_ID")
    String extAppId;

    @NonNull
    @Column(columnDefinition = "char(1)", name = "LAST_USED", nullable = false)
    String lastUsed;
}
