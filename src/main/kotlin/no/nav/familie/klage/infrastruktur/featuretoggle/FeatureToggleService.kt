package no.nav.familie.klage.infrastruktur.featuretoggle

import org.springframework.beans.factory.DisposableBean

interface FeatureToggleService : DisposableBean {

    fun isEnabled(toggle: Toggle): Boolean {
        return isEnabled(toggle, false)
    }

    fun isEnabled(toggle: Toggle, defaultValue: Boolean): Boolean
}

enum class Toggle(val toggleId: String, val beskrivelse: String? = null) {
    START_BEHANDLING("familie.klage.start-behandling"),
    OPPRETT_REVURDERING("familie.klage.opprett-revurdering"),
    BEHANDLINGSSTATISTIKK("familie.klage.behandlingsstatistikk"),
    TILBAKEKREVING_INFOTRYGD_PÅKLAGET_VEDTAK("familie.klage.infotrygd-vedtak");

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
