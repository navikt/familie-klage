package no.nav.familie.klage.formkrav


import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.FormDto
import java.time.LocalDateTime
import java.util.UUID

fun formDto(
    id: UUID = UUID.randomUUID(),
    fagsakId: UUID = UUID.randomUUID(),
    vedtaksdato: LocalDateTime = LocalDateTime.now(), // TODO: endre til mulig nullverdi
    klageMottat: LocalDateTime = LocalDateTime.now().minusDays(10),
    klageaarsak: String = "Fikk ikke nok penger",
    klageBeskrivelse: String = "jeg er sinna",
    klagePart: FormVilkår = FormVilkår.IKKE_OPPFYLT,
    klageKonkret: FormVilkår = FormVilkår.IKKE_OPPFYLT,
    klagefristOverholdt: FormVilkår = FormVilkår.IKKE_OPPFYLT,
    klageSignert: FormVilkår = FormVilkår.IKKE_OPPFYLT,
    saksbehandlerBegrunnelse: String = "OK HAN SKAL FÅ MER PEGNER",
    sakSistEndret: LocalDateTime = LocalDateTime.now().minusDays(1)
): FormDto =
    FormDto(
        id,
        fagsakId,
        vedtaksdato,
        klageMottat,
        klageaarsak,
        klageBeskrivelse,
        klagePart,
        klageKonkret,
        klagefristOverholdt,
        klageSignert,
        saksbehandlerBegrunnelse,
        sakSistEndret
    )