package no.nav.familie.klage.kabal

enum class KabalHjemmel(
    val id: String,
    val lovKilde: LovKilde,
    val spesifikasjon: String
) {

    BTRL_2("619", LovKilde.BARNETRYGDLOVEN, "§ 2"),
    BTRL_4("587", LovKilde.BARNETRYGDLOVEN, "§ 4"),
    BTRL_5("588", LovKilde.BARNETRYGDLOVEN, "§ 5"),
    BTRL_9("592", LovKilde.BARNETRYGDLOVEN, "§ 9"),
    BTRL_13("596", LovKilde.BARNETRYGDLOVEN, "§ 13"),

    FTRL_15_2("431", LovKilde.FOLKETRYGDLOVEN, "§ 15-2"),
    FTRL_15_3("432", LovKilde.FOLKETRYGDLOVEN, "§ 15-3"),
    FTRL_15_4("433", LovKilde.FOLKETRYGDLOVEN, "§ 15-4"),
    FTRL_15_5("434", LovKilde.FOLKETRYGDLOVEN, "§ 15-5"),
    FTRL_15_6("435", LovKilde.FOLKETRYGDLOVEN, "§ 15-6"),
    FTRL_15_8("437", LovKilde.FOLKETRYGDLOVEN, "§ 15-8"),
    FTRL_15_9("439", LovKilde.FOLKETRYGDLOVEN, "§ 15-9"),
    FTRL_15_10("440", LovKilde.FOLKETRYGDLOVEN, "§ 15-10"),
    FTRL_15_11("441", LovKilde.FOLKETRYGDLOVEN, "§ 15-11"),
    FTRL_15_12("442", LovKilde.FOLKETRYGDLOVEN, "§ 15-12"),
    FTRL_15_13("443", LovKilde.FOLKETRYGDLOVEN, "§ 15-13"),

    FTRL_22_15("1000.022.015", LovKilde.FOLKETRYGDLOVEN, "§ 22-15"),

    EOES_AVTALEN("601", LovKilde.EØS_AVTALEN, "EØS-avtalen"),
    EOES_883_2004_6("228", LovKilde.EØS_FORORDNING_883_2004, "art. 6")
}
