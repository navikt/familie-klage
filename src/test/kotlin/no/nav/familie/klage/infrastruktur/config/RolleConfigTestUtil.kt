package no.nav.familie.klage.infrastruktur.config

object RolleConfigTestUtil {

    val rolleConfig = RolleConfig(
        ba = FagsystemRolleConfig("baSaksbehandler", "baBeslutter", "baVeileder"),
        ef = FagsystemRolleConfig("efSaksbehandler", "efBeslutter", "efVeileder"),
        ks = FagsystemRolleConfig("ksSaksbehandler", "ksBeslutter", "ksVeileder"),
        egenAnsatt = "egenAnsatt",
    )
}
