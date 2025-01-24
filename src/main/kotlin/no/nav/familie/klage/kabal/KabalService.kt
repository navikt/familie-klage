package no.nav.familie.klage.kabal

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.brev.domain.Brevmottaker
import no.nav.familie.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.domain.BrevmottakerPerson
import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.tilYtelse
import no.nav.familie.klage.infrastruktur.config.LenkeConfig
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.kabal.domain.OversendtPartId
import no.nav.familie.klage.kabal.domain.OversendtPartIdType
import no.nav.familie.klage.kabal.domain.OversendtSak
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import org.springframework.stereotype.Service

@Service
class KabalService(
    private val kabalClient: KabalClient,
    private val integrasjonerClient: FamilieIntegrasjonerClient,
    private val lenkeConfig: LenkeConfig,
) {
    fun sendTilKabal(
        fagsak: Fagsak,
        behandling: Behandling,
        vurdering: Vurdering,
        saksbehandlerIdent: String,
        brevMottakere: Brevmottakere,
    ) {
        val saksbehandler = integrasjonerClient.hentSaksbehandlerInfo(saksbehandlerIdent)
        val oversendtKlageAnkeV3 =
            lagKlageOversendelseV3(fagsak, behandling, vurdering, saksbehandler.enhet, brevMottakere)
        kabalClient.sendTilKabal(oversendtKlageAnkeV3)
    }

    private fun lagKlageOversendelseV3(
        fagsak: Fagsak,
        behandling: Behandling,
        vurdering: Vurdering,
        saksbehandlersEnhet: String,
        brevMottakere: Brevmottakere,
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
            innsynUrl = lagInnsynUrl(fagsak, behandling.påklagetVedtak),
            hjemler = vurdering.hjemmel?.let { listOf(it.kabalHjemmel) } ?: emptyList(),
            forrigeBehandlendeEnhet = saksbehandlersEnhet,
            tilknyttedeJournalposter = listOf(),
            brukersHenvendelseMottattNavDato = behandling.klageMottatt,
            innsendtTilNav = behandling.klageMottatt,
            kilde = fagsak.fagsystem.tilFellesFagsystem(),
            ytelse = fagsak.stønadstype.tilYtelse(),
            hindreAutomatiskSvarbrev = behandling.årsak == Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL,
        )

    private fun utledFullmektigFraBrevmottakere(brevMottakere: Brevmottakere): OversendtProsessfullmektig? {
        val fullmektigEllerVerge =
            brevMottakere.personer.firstOrNull { it.mottakerRolle == MottakerRolle.FULLMAKT }
                ?: brevMottakere.personer.firstOrNull { it.mottakerRolle == MottakerRolle.VERGE }
                ?: brevMottakere.organisasjoner.firstOrNull()

        return fullmektigEllerVerge?.let {
            val oversendtPartId: OversendtPartId = utledPartIdFraFullmektigEllerVerge(it)
            return OversendtProsessfullmektig(id = oversendtPartId, skalKlagerMottaKopi = false)
        }
    }

    private fun utledPartIdFraFullmektigEllerVerge(it: Brevmottaker) =
        when (it) {
            is BrevmottakerPerson -> {
                when (it) {
                    is BrevmottakerPersonMedIdent -> OversendtPartId(
                        type = OversendtPartIdType.PERSON,
                        verdi = it.personIdent,
                    )

                    is BrevmottakerPersonUtenIdent -> throw IllegalStateException("BrevmottakerPersonUtenIdent er foreløpig ikke støttet.")
                }
            }

            is BrevmottakerOrganisasjon -> {
                OversendtPartId(
                    type = OversendtPartIdType.VIRKSOMHET,
                    verdi = it.organisasjonsnummer,
                )
            }
        }

    private fun lagInnsynUrl(
        fagsak: Fagsak,
        påklagetVedtak: PåklagetVedtak,
    ): String {
        val fagsystemUrl =
            when (fagsak.fagsystem) {
                Fagsystem.EF -> lenkeConfig.efSakLenke
                Fagsystem.BA -> lenkeConfig.baSakLenke
                Fagsystem.KS -> lenkeConfig.ksSakLenke
            }
        val påklagetVedtakDetaljer = påklagetVedtak.påklagetVedtakDetaljer
        return if (påklagetVedtakDetaljer != null &&
            påklagetVedtakDetaljer.fagsystemType == FagsystemType.ORDNIÆR &&
            påklagetVedtakDetaljer.eksternFagsystemBehandlingId != null
        ) {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}"
        } else {
            "$fagsystemUrl/fagsak/${fagsak.eksternId}/saksoversikt"
        }
    }
}
