package ru.golovkin.oxford3000.dictionary.util;

public class RussianDeclensionUtil {

    public static String inclineWithNumeral(long numeral, Gender gender, String wordVar1, String wordVar2, String wordVar3) {

        return String.format("%d %s", numeral,
            numeral % 10 == 1 && numeral % 100 / 10 != 1?wordVar1:
            numeral % 10 >= 2 && numeral % 10 <= 4 && numeral % 100 / 10 != 1?wordVar2:
            wordVar3);
    }
}
