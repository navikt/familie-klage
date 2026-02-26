package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.tilYtelse
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import java.time.LocalDate

data class OversendtKlageAnkeV4(
    val type: OversendtType,
    val sakenGjelder: OversendtPartV4,
    val klager: OversendtPartV4? = null,
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
            brevmottakere: Brevmottakere?,
        ): OversendtKlageAnkeV4 =
            OversendtKlageAnkeV4(
                type = OversendtType.KLAGE,
                sakenGjelder =
                    OversendtPartV4(
                        id =
                            OversendtPartId(
                                type = OversendtPartIdType.PERSON,
                                verdi = fagsak.hentAktivIdent(),
                            ),
                    ),
                klager =
                    fagsak.institusjon?.let {
                        OversendtPartV4(
                            id =
                                OversendtPartId(
                                    type = OversendtPartIdType.VIRKSOMHET,
                                    verdi = it.orgNummer,
                                ),
                        )
                    },
                prosessfullmektig = brevmottakere?.let { utledFullmektigFraBrevmottakere(brevmottakere) },
                fagsak = OversendtSak(fagsakId = fagsak.eksternId, fagsystem = fagsak.fagsystem.tilFellesFagsystem()),
                kildeReferanse = behandling.eksternBehandlingId.toString(),
                hjemler = vurdering.hjemmel?.let { listOf(it.kabalHjemmel) } ?: emptyList(),
                forrigeBehandlendeEnhet = saksbehandlersEnhet,
                tilknyttedeJournalposter = emptyList(),
                brukersKlageMottattVedtaksinstans = behandling.klageMottatt,
                ytelse = fagsak.stønadstype.tilYtelse(),
                hindreAutomatiskSvarbrev = behandling.årsak == Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL,
            )

        private fun utledFullmektigFraBrevmottakere(brevmottakere: Brevmottakere): OversendtProsessfullmektigV4? =
            utledFullmektigEllerVerge(brevmottakere)?.let {
                OversendtProsessfullmektigV4(
                    id = utledPartIdFraBrevmottaker(it),
                    navn = utledNavnFraBrevmottaker(it),
                    adresse = utledAdresseFraBrevmottaker(it),
                )
            }

        private fun utledPartIdFraBrevmottaker(brevmottaker: Brevmottaker) =
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

                is BrevmottakerPersonUtenIdent -> {
                    null
                }
            }

        private fun utledNavnFraBrevmottaker(brevmottaker: Brevmottaker): String? =
            if (brevmottaker is BrevmottakerPersonUtenIdent) {
                brevmottaker.navn
            } else {
                null
            }

        private fun utledAdresseFraBrevmottaker(brevmottaker: Brevmottaker): OversendtAdresseV4? =
            if (brevmottaker is BrevmottakerPersonUtenIdent) {
                OversendtAdresseV4(
                    adresselinje1 = brevmottaker.adresselinje1,
                    adresselinje2 = brevmottaker.adresselinje2,
                    postnummer = brevmottaker.postnummer,
                    poststed = brevmottaker.poststed,
                    land = brevmottaker.landkode,
                )
            } else {
                null
            }
    }
}

@Suppress("unused")
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
