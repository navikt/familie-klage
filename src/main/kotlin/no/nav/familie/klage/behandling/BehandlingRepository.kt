package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.Klagebehandling
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

    @Query(
        """SELECT DISTINCT personopplysninger.navn FROM personopplysninger 
                JOIN behandling ON personopplysninger.person_id = behandling.person_id
              WHERE behandling.id = :behandling_id"""
    )
    fun findNavnByBehandlingId(behandling_id: UUID): String

    @Modifying
    @Query(
        """UPDATE behandling SET steg = :steg WHERE id = :behandling_id"""
    )
    fun updateSteg(behandling_id: UUID, steg: StegType)

    @Query(
        """SELECT steg FROM behandling WHERE id = :behandling_id"""
    )
    fun findStegById(behandling_id: UUID): StegType

    @Modifying
    @Query(
        """UPDATE behandling SET status = :nyStatus WHERE id = :behandling_id"""
    )
    fun updateStatus(@Param("behandling_id") behandlingId: UUID, nyStatus: BehandlingStatus)

    @Query(
        """SELECT b.* FROM behandling b 
            JOIN fagsak f ON b.fagsak_id=f.id
            WHERE b.ekstern_behandling_id = :eksternBehandlingId AND f.fagsystem = :fagsystem"""
    )
    fun findByEksternBehandlingIdAndFagsystem(eksternBehandlingId: String, fagsystem: String): Behandling

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
            WHERE f.ekstern_id = :eksternId AND f.fagsystem = :fagsystem
        """
    )
    fun finnBehandlinger(
        @Param("eksternId") eksternId: String,
        @Param("fagsystem") fagsystem: Fagsystem
    ): List<Klagebehandling>
}
