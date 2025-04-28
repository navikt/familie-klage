package no.nav.familie.klage.distribusjon

import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.klage.Stønadstype

object DokumenttypeUtil {
    fun dokumenttypeBrev(stønadstype: Stønadstype): Dokumenttype =
        when (stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> Dokumenttype.KLAGE_VEDTAKSBREV_OVERGANGSSTØNAD
            Stønadstype.BARNETILSYN -> Dokumenttype.KLAGE_VEDTAKSBREV_BARNETILSYN
            Stønadstype.SKOLEPENGER -> Dokumenttype.KLAGE_VEDTAKSBREV_SKOLEPENGER
            Stønadstype.BARNETRYGD -> Dokumenttype.KLAGE_VEDTAKSBREV_BARNETRYGD
            Stønadstype.KONTANTSTØTTE -> Dokumenttype.KLAGE_VEDTAKSBREV_KONTANTSTØTTE
        }

    fun dokumenttypeSaksbehandlingsblankett(stønadstype: Stønadstype): Dokumenttype =
        when (stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_OVERGANGSSTØNAD
            Stønadstype.BARNETILSYN -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_BARNETILSYN
            Stønadstype.SKOLEPENGER -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_SKOLEPENGER
            Stønadstype.BARNETRYGD -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_BARNETRYGD
            Stønadstype.KONTANTSTØTTE -> Dokumenttype.KLAGE_BLANKETT_SAKSBEHANDLING_KONTANTSTØTTE
        }
}
