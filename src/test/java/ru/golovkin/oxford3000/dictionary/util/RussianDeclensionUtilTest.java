package ru.golovkin.oxford3000.dictionary.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class RussianDeclensionUtilTest {


    public static final String WORD_VAR_1 = "слово";
    public static final String WORD_VAR_2 = "слова";
    public static final String WORD_VAR_3 = "слов";

    @Test
    void should_incline_neuter_gender_word_for_2_3_4_22_23_24() {
        assertEquals("2 слова", RussianDeclensionUtil.inclineWithNumeral(2, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("3 слова", RussianDeclensionUtil.inclineWithNumeral(3, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("4 слова", RussianDeclensionUtil.inclineWithNumeral(4, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("22 слова", RussianDeclensionUtil.inclineWithNumeral(22, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("23 слова", RussianDeclensionUtil.inclineWithNumeral(23, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("24 слова", RussianDeclensionUtil.inclineWithNumeral(24, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
    }

    @Test
    void should_incline_neuter_gender_word_for_5_6_11_12_13_14_25_26_100_0() {
        assertEquals("5 слов", RussianDeclensionUtil.inclineWithNumeral(5, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("6 слов", RussianDeclensionUtil.inclineWithNumeral(6, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("11 слов", RussianDeclensionUtil.inclineWithNumeral(11, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("12 слов", RussianDeclensionUtil.inclineWithNumeral(12, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("13 слов", RussianDeclensionUtil.inclineWithNumeral(13, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("14 слов", RussianDeclensionUtil.inclineWithNumeral(14, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("25 слов", RussianDeclensionUtil.inclineWithNumeral(25, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("26 слов", RussianDeclensionUtil.inclineWithNumeral(26, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("100 слов", RussianDeclensionUtil.inclineWithNumeral(100, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("0 слов", RussianDeclensionUtil.inclineWithNumeral(0, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
    }

    @Test
    void should_incline_neuter_gender_word_for_1_21_31_101() {
        assertEquals("1 слово", RussianDeclensionUtil.inclineWithNumeral(1, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("21 слово", RussianDeclensionUtil.inclineWithNumeral(21, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("31 слово", RussianDeclensionUtil.inclineWithNumeral(31, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
        assertEquals("101 слово", RussianDeclensionUtil.inclineWithNumeral(101, Gender.NEUTER,
            WORD_VAR_1, WORD_VAR_2, WORD_VAR_3));
    }
}
