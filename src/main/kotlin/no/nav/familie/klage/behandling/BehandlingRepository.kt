package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.Klagebehandlingsesultat
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

    @Modifying
    @Query(
        """UPDATE behandling SET steg = :steg WHERE id = :behandling_id"""
    )
    fun updateSteg(behandling_id: UUID, steg: StegType)

    @Modifying
    @Query(
        """UPDATE behandling SET status = :nyStatus WHERE id = :behandling_id"""
    )
    fun updateStatus(@Param("behandling_id") behandlingId: UUID, nyStatus: BehandlingStatus)

    fun findByEksternBehandlingId(eksternBehandlingId: UUID): Behandling

    @Query(
        """
            SELECT 
             b.id,
             b.fagsak_id,
             f.fagsak_person_id,
             b.status,
             b.opprettet_tid opprettet,
             b.klage_mottatt mottatt_dato,
             b.resultat,
             v.arsak,
             b.vedtak_dato vedtaksdato
            FROM behandling b
            JOIN fagsak f ON f.id = b.fagsak_id
            LEFT JOIN vurdering v ON v.behandling_id = b.id
            WHERE f.ekstern_id = :eksternFagsakId AND f.fagsystem = :fagsystem
        """
    )
    fun finnKlagebehandlingsresultat(
        @Param("eksternFagsakId") eksternFagsakId: String,
        @Param("fagsystem") fagsystem: Fagsystem
    ): List<Klagebehandlingsesultat>

    fun findByFagsakId(fagsakId: UUID): List<Behandling>
}
