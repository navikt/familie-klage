package no.nav.familie.klage.kabal

// Se Kabals kodeverk som dette er hentet fra: https://github.com/navikt/klage-kodeverk/blob/main/src/main/kotlin/no/nav/klage/kodeverk/hjemmel/Hjemmel.kt

enum class KabalHjemmel(
    val id: String,
    val lovKilde: LovKilde,
    val spesifikasjon: String,
) {

    BTRL_2("619", LovKilde.BARNETRYGDLOVEN, "§ 2"),
    BTRL_4("587", LovKilde.BARNETRYGDLOVEN, "§ 4"),
    BTRL_5("588", LovKilde.BARNETRYGDLOVEN, "§ 5"),
    BTRL_9("592", LovKilde.BARNETRYGDLOVEN, "§ 9"),
    BTRL_10("BTRL_10", LovKilde.BARNETRYGDLOVEN, "§ 10"),
    BTRL_11("BTRL_11", LovKilde.BARNETRYGDLOVEN, "§ 11"),
    BTRL_12("BTRL_12", LovKilde.BARNETRYGDLOVEN, "§ 12"),
    BTRL_13("596", LovKilde.BARNETRYGDLOVEN, "§ 13"),
    BTRL_17("BTRL_17", LovKilde.BARNETRYGDLOVEN, "§ 17"),
    BTRL_18("BTRL_18", LovKilde.BARNETRYGDLOVEN, "§ 18"),

    KONTSL_2("606", LovKilde.KONTANTSTØTTELOVEN, "§ 2"),
    KONTSL_3("621", LovKilde.KONTANTSTØTTELOVEN, "§ 3"),
    KONTSL_6("609", LovKilde.KONTANTSTØTTELOVEN, "§ 6"),
    KONTSL_7("610", LovKilde.KONTANTSTØTTELOVEN, "§ 7"),
    KONTSL_8("611", LovKilde.KONTANTSTØTTELOVEN, "§ 8"),
    KONTSL_9("612", LovKilde.KONTANTSTØTTELOVEN, "§ 9"),
    KONTSL_10("613", LovKilde.KONTANTSTØTTELOVEN, "§ 10"),
    KONTSL_11("614", LovKilde.KONTANTSTØTTELOVEN, "§ 11"),
    KONTSL_12("615", LovKilde.KONTANTSTØTTELOVEN, "§ 12"),
    KONTSL_13("616", LovKilde.KONTANTSTØTTELOVEN, "§ 13"),
    KONTSL_16("618", LovKilde.KONTANTSTØTTELOVEN, "§ 16"),

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

    FTRL_22_12("1000.022.012", LovKilde.FOLKETRYGDLOVEN, "§ 22-12"),
    FTRL_22_13("1000.022.013", LovKilde.FOLKETRYGDLOVEN, "§ 22-13"),
    FTRL_22_15("1000.022.015", LovKilde.FOLKETRYGDLOVEN, "§ 22-15"),

    EOES_AVTALEN("601", LovKilde.EØS_AVTALEN, "EØS-avtalen"),
    NORDISK_KONVENSJON("NORDISK_KONVENSJON", LovKilde.NORDISK_KONVENSJON, "Nordisk konvensjon"),
    ANDRE_TRYGDEAVTALER("ANDRE_TRYGDEAVTALER", LovKilde.ANDRE_TRYGDEAVTALER, "Andre trygdeavtaler"),
    EOES_883_2004_5("EOES_883_2004_5", LovKilde.EØS_FORORDNING_883_2004, "art. 5"),
    EOES_883_2004_6("228", LovKilde.EØS_FORORDNING_883_2004, "art. 6"),
}
