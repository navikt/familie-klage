package no.nav.familie.klage.søk.ereg

import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon

fun mapOrganisasjonDto(organisasjonDto: OrganisasjonDto): Organisasjon = organisasjonDto.let {
    Organisasjon(
        organisasjonsnummer = it.organisasjonsnummer,
        navn = it.navn.redigertnavn ?: it.navn.navnelinje1 ?: it.navn.navnelinje2 ?: "Ukjent navn"
    )
}
