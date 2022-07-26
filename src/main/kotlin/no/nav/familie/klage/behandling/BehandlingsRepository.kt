package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

import java.util.UUID

@Repository
interface BehandlingsRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

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

}