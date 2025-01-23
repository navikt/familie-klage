package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.brev.domain.Brevmottaker
import no.nav.familie.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.domain.BrevmottakerPerson
import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.tilYtelse
import no.nav.familie.klage.kabal.KabalHjemmel
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import java.time.LocalDate

data class OversendtKlageAnkeV4(
    val type: OversendtType,
    val sakenGjelder: OversendtPartV4,
    val prosessfullmektig: OversendtProsessfullmektigV4?,
    val fagsak: OversendtSak,
    val kildeReferanse: String,
    val dvhReferanse: String? = null,
    val hjemler: List<KabalHjemmel>,
    val forrigeBehandlendeEnhet: String,
    val tilknyttedeJournalposter: List<OversendtDokumentReferanse>,
    val brukersKlageMottattVedtaksinstans: LocalDate?,
    val ytelse: Ytelse,
    val kommentar: String? = null,
    val hindreAutomatiskSvarbrev: Boolean?,
) : OversendtKlageAnke {
    companion object {
        fun lagKlageOversendelse(
            fagsak: Fagsak,
            behandling: Behandling,
            vurdering: Vurdering,
            saksbehandlersEnhet: String,
            brevMottakere: Brevmottakere,
        ): OversendtKlageAnkeV4 =
            OversendtKlageAnkeV4(
                type = OversendtType.KLAGE,
                sakenGjelder = OversendtPartV4(
                    id = OversendtPartId(
                        type = OversendtPartIdType.PERSON,
                        verdi = fagsak.hentAktivIdent(),
                    ),
                ),
                prosessfullmektig = utledFullmektigFraBrevmottakere(brevMottakere),
                fagsak = OversendtSak(fagsakId = fagsak.eksternId, fagsystem = fagsak.fagsystem.tilFellesFagsystem()),
                kildeReferanse = behandling.eksternBehandlingId.toString(),
                hjemler = vurdering.hjemmel?.let { listOf(it.kabalHjemmel) } ?: emptyList(),
                forrigeBehandlendeEnhet = saksbehandlersEnhet,
                tilknyttedeJournalposter = emptyList(),
                brukersKlageMottattVedtaksinstans = behandling.klageMottatt,
                ytelse = fagsak.stønadstype.tilYtelse(),
                hindreAutomatiskSvarbrev = behandling.årsak == Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL,
            )

        private fun utledFullmektigFraBrevmottakere(brevMottakere: Brevmottakere): OversendtProsessfullmektigV4? =
            utledFullmektigEllerVerge(brevMottakere)?.let {
                OversendtProsessfullmektigV4(
                    id = utledPartIdFraFullmektigEllerVerge(it),
                    navn = utledNavnFraFullmektigEllerVerge(it),
                    adresse = utledAdresseFraFullmektigEllerVerge(it),
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

                is BrevmottakerPersonUtenIdent -> null
            }

        private fun utledNavnFraFullmektigEllerVerge(brevmottaker: Brevmottaker): String =
            when (brevmottaker) {
                is BrevmottakerOrganisasjon -> brevmottaker.organisasjonsnavn
                is BrevmottakerPerson -> brevmottaker.navn
            }

        private fun utledAdresseFraFullmektigEllerVerge(brevmottaker: Brevmottaker): OversendtAdresseV4? =
            when (brevmottaker) {
                is BrevmottakerPersonUtenIdent ->
                    OversendtAdresseV4(
                        adresselinje1 = brevmottaker.adresselinje1,
                        adresselinje2 = brevmottaker.adresselinje2,
                        postnummer = brevmottaker.postnummer,
                        poststed = brevmottaker.poststed,
                        land = brevmottaker.landkode,
                    )

                else -> null
            }
    }
}

enum class OversendtType {
    KLAGE,
    ANKE,
}

data class OversendtProsessfullmektigV4(
    val id: OversendtPartId?,
    val navn: String?,
    val adresse: OversendtAdresseV4?,
)

data class OversendtAdresseV4(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val land: String,
)

data class OversendtPartV4(
    val id: OversendtPartId,
)
