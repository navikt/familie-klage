package no.nav.familie.klage.testutil

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevDto
import no.nav.familie.klage.brev.dto.FritekstBrevtype
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.klage.felles.domain.SporbarUtils
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration
import no.nav.familie.klage.kabal.BehandlingEventType
import no.nav.familie.klage.kabal.ExternalUtfall
import no.nav.familie.klage.kabal.domain.Klageresultat
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

object DomainUtil {

    fun fagsakDomain(
        id: UUID = UUID.randomUUID(),
        stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD,
        personId: UUID = UUID.randomUUID(),
        fagsystem: Fagsystem = Fagsystem.EF,
        eksternId: String = Random.nextInt().toString()
    ): FagsakDomain =
        FagsakDomain(
            id = id,
            fagsakPersonId = personId,
            stønadstype = stønadstype,
            eksternId = eksternId,
            fagsystem = fagsystem
        )

    fun FagsakDomain.tilFagsak(personIdent: String = "11223344551") =
        this.tilFagsakMedPerson(setOf(PersonIdent(ident = personIdent)))

    fun behandling(
        fagsak: Fagsak = fagsak(),
        id: UUID = UUID.randomUUID(),
        eksternBehandlingId: UUID = UUID.randomUUID(),
        eksternFagsystemBehandlingId: String = Random.nextInt().toString(),
        klageMottatt: LocalDate = LocalDate.now(),
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        steg: StegType = StegType.FORMKRAV,
        behandlendeEnhet: String = "4489"
    ): Behandling =
        Behandling(
            id = id,
            eksternBehandlingId = eksternBehandlingId,
            fagsakId = fagsak.id,
            eksternFagsystemBehandlingId = eksternFagsystemBehandlingId,
            klageMottatt = klageMottatt,
            status = status,
            steg = steg,
            behandlendeEnhet = behandlendeEnhet
        )

    fun vurdering(behandlingId: UUID, vedtak: Vedtak = Vedtak.OPPRETTHOLD_VEDTAK, hjemmel: Hjemmel = Hjemmel.FT_FEMTEN_FEM) =
        Vurdering(
            behandlingId = behandlingId,
            vedtak = vedtak,
            hjemmel = hjemmel,
            beskrivelse = "En begrunnelse"
        )

    fun oppfyltForm(behandlingId: UUID) =
        Form(
            behandlingId = behandlingId,
            klagePart = FormVilkår.OPPFYLT,
            klagefristOverholdt = FormVilkår.OPPFYLT,
            klageKonkret = FormVilkår.OPPFYLT,
            klageSignert = FormVilkår.OPPFYLT,
            saksbehandlerBegrunnelse = "Ok"
        )

    fun fritekstbrev(behandlingId: UUID) = FritekstBrevDto(
        overskrift = "Topp",
        avsnitt = listOf(
            AvsnittDto(
                avsnittId = UUID.randomUUID(),
                deloverskrift = "Deloverskrift",
                innhold = "Litt innhold",
                skalSkjulesIBrevbygger = false
            )
        ),
        behandlingId = behandlingId,
        brevType = FritekstBrevtype.VEDTAK_INVILGELSE
    )

    val defaultIdenter = setOf(PersonIdent("01010199999"))
    fun fagsak(
        identer: Set<PersonIdent> = defaultIdenter,
        stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD,
        id: UUID = UUID.randomUUID(),
        sporbar: Sporbar = Sporbar(),
        fagsakPersonId: UUID = UUID.randomUUID()
    ): Fagsak {
        return fagsak(stønadstype, id, FagsakPerson(id = fagsakPersonId, identer = identer), sporbar)
    }

    fun fagsak(
        stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD,
        id: UUID = UUID.randomUUID(),
        person: FagsakPerson,
        sporbar: Sporbar = Sporbar()
    ): Fagsak {
        return Fagsak(
            id = id,
            fagsakPersonId = person.id,
            personIdenter = person.identer,
            stønadstype = stønadstype,
            sporbar = sporbar,
            eksternId = "1",
            fagsystem = Fagsystem.EF
        )
    }

    fun klageresultat(
        eventId: UUID = UUID.randomUUID(),
        type: BehandlingEventType = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
        utfall: ExternalUtfall = ExternalUtfall.MEDHOLD,
        mottattEllerAvsluttetTidspunkt: LocalDateTime = SporbarUtils.now(),
        kildereferanse: UUID = UUID.randomUUID(),
        journalpostReferanser: List<String> = listOf("1", "2"),
        behandlingId: UUID = UUID.randomUUID()
    ): Klageresultat {
        return Klageresultat(
            eventId = eventId,
            type = type,
            utfall = utfall,
            mottattEllerAvsluttetTidspunkt = mottattEllerAvsluttetTidspunkt,
            kildereferanse = kildereferanse,
            journalpostReferanser = DatabaseConfiguration.StringListWrapper(verdier = journalpostReferanser),
            behandlingId = behandlingId
        )
    }
}
