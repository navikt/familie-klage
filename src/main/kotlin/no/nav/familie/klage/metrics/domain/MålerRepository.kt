package no.nav.familie.klage.metrics.domain

import no.nav.familie.klage.behandling.domain.Behandling
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MålerRepository : CrudRepository<Behandling, UUID> {

    // language=PostgreSQL
    @Query(
        """SELECT 
            f.stonadstype,
            b.status,
            COUNT(*) AS antall
           FROM fagsak f
            JOIN behandling b ON f.id = b.fagsak_id
           GROUP BY f.stonadstype, b.status"""
    )
    fun finnBehandlingerPerStatus(): List<BehandlingerPerStatus>

    // language=PostgreSQL
    @Query(
        """SELECT 
            f.stonadstype,
            b.status,
            EXTRACT(ISOYEAR FROM b.opprettet_tid) AS år,
            EXTRACT(WEEK FROM b.opprettet_tid) AS uke,
            COUNT(*) AS antall
           FROM fagsak f
            JOIN behandling b ON f.id = b.fagsak_id
           WHERE status IN ('OPPRETTET', 'UTREDES')
           GROUP BY f.stonadstype, b.status, år, uke"""
    )
    fun finnÅpneBehandlingerPerUke(): List<ÅpneBehandlingerFraUke>

    // language=PostgreSQL
    @Query(
        """SELECT 
            f.stonadstype,
            b.resultat,
            COUNT(*) AS antall
           FROM fagsak f
           JOIN behandling b ON f.id = b.fagsak_id
           WHERE b.status IN ('VENTER', 'FERDIGSTILT')
           GROUP BY f.stonadstype, b.resultat"""
    )
    fun antallVedtak(): List<AntallVedtak>

}
