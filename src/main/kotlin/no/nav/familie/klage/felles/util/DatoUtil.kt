package no.nav.familie.klage.felles.util

import java.time.LocalDate
import java.time.LocalDateTime

fun dagensDatoMedNorskFormat() = LocalDateTime.now().format(TekstUtil.DatoFormat.GOSYS_DATE_TIME)

fun LocalDate.isEqualOrAfter(other: LocalDate) = this == other || this.isAfter(other)
