package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.brev.domain.Brevmottaker
import no.nav.familie.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.tilYtelse
import no.nav.familie.klage.kabal.KabalHjemmel
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import java.time.LocalDate

// objektet som skal sendes til kabal
data class OversendtKlageAnkeV3(
    val type: Type,
    val klager: OversendtKlager,
    val sakenGjelder: OversendtSakenGjelder? = null,
    val fagsak: OversendtSak? = null,
    val kildeReferanse: String,
    val dvhReferanse: String? = null,
    val innsynUrl: String? = null, // url til vår løsning
    val hjemler: List<KabalHjemmel>,
    val forrigeBehandlendeEnhet: String,
    val tilknyttedeJournalposter: List<OversendtDokumentReferanse> = emptyList(),
    val brukersHenvendelseMottattNavDato: LocalDate,
    val innsendtTilNav: LocalDate,
    val kilde: Fagsystem,
    val ytelse: Ytelse,
    val kommentar: String? = null,
    val hindreAutomatiskSvarbrev: Boolean,
) : OversendtKlageAnke {
    companion object {
        fun lagKlageOversendelse(
            fagsak: Fagsak,
            behandling: Behandling,
            vurdering: Vurdering,
            saksbehandlersEnhet: String,
            brevMottakere: Brevmottakere,
            innsynUrl: String?,
        ): OversendtKlageAnkeV3 =
            OversendtKlageAnkeV3(
                type = Type.KLAGE,
                klager =
                    OversendtKlager(
                        id =
                            OversendtPartId(
                                type = OversendtPartIdType.PERSON,
                                verdi = fagsak.hentAktivIdent(),
                            ),
                        klagersProsessfullmektig = utledFullmektigFraBrevmottakere(brevMottakere),
                    ),
                fagsak = OversendtSak(fagsakId = fagsak.eksternId, fagsystem = fagsak.fagsystem.tilFellesFagsystem()),
                kildeReferanse = behandling.eksternBehandlingId.toString(),
                innsynUrl = innsynUrl,
                hjemler = vurdering.hjemmel?.let { listOf(it.kabalHjemmel) } ?: emptyList(),
                forrigeBehandlendeEnhet = saksbehandlersEnhet,
                tilknyttedeJournalposter = listOf(),
                brukersHenvendelseMottattNavDato = behandling.klageMottatt,
                innsendtTilNav = behandling.klageMottatt,
                kilde = fagsak.fagsystem.tilFellesFagsystem(),
                ytelse = fagsak.stønadstype.tilYtelse(),
                hindreAutomatiskSvarbrev = behandling.årsak == Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL,
            )

        private fun utledFullmektigFraBrevmottakere(brevMottakere: Brevmottakere): OversendtProsessfullmektigV3? =
            utledFullmektigEllerVerge(brevMottakere)?.let {
                OversendtProsessfullmektigV3(
                    id = utledPartIdFraFullmektigEllerVerge(it),
                    skalKlagerMottaKopi = false,
                )
            }

        private fun utledPartIdFraFullmektigEllerVerge(brevmottaker: Brevmottaker) =
            when (brevmottaker) {
                is BrevmottakerPersonMedIdent -> {
                    OversendtPartId(
                        type = OversendtPartIdType.PERSON,
                        verdi = brevmottaker.personIdent,
                    )
                }

                is BrevmottakerOrganisasjon -> {
                    OversendtPartId(
                        type = OversendtPartIdType.VIRKSOMHET,
                        verdi = brevmottaker.organisasjonsnummer,
                    )
                }

                is BrevmottakerPersonUtenIdent -> throw IllegalStateException("Person uten ident er ikke støttet i V3")
            }
    }
}

enum class Type(
    val id: String,
    val navn: String,
    val beskrivelse: String,
) {
    KLAGE("1", "Klage", "Klage"),
    ANKE("2", "Anke", "Anke"),
    ANKE_I_TRYGDERETTEN("3", "Anke i trygderetten", "Anke i trygderetten"),
    OMGJOERINGSKRAV("4", "Omgjøringskrav", "Omgjøringskrav"),
}

data class OversendtKlager(
    val id: OversendtPartId,
    val klagersProsessfullmektig: OversendtProsessfullmektigV3? = null,
)

data class OversendtProsessfullmektigV3(
    val id: OversendtPartId,
    @Deprecated("Denne er deprecated i Kabal og brukes ikke")
    val skalKlagerMottaKopi: Boolean,
)

data class OversendtSakenGjelder(
    val id: OversendtPartId,
    val skalMottaKopi: Boolean,
)
