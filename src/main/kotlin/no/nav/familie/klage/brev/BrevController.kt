package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.domain.BrevMedAvsnitt
import no.nav.familie.klage.brev.dto.FritekstBrevDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevController (
    private val brevService: BrevService,
    private val behandlingService: BehandlingService
    ){

    @GetMapping("/{behandlingId}")
    fun hentBrev(@PathVariable behandlingId: UUID): Ressurs<BrevMedAvsnitt?> {
        return Ressurs.success(brevService.hentBrev(behandlingId))
    }
    @PostMapping
    fun lagBrev(@RequestBody brevInnhold: FritekstBrevDto): Ressurs<ByteArray> {
        return Ressurs.success(brevService.lagBrev(brevInnhold))
    }

    @PostMapping("/{behandlingId}")
    fun forhåndsviFritekstBrev(@RequestBody brevInnhold: FritekstBrevDto): Ressurs<ByteArray>{
        return Ressurs.success(brevService.forhåndsvisFritekstBrev(brevInnhold))
    }

    /*
    @PostMapping("/{behandlingId}")
    fun forhåndsvisBeslutterbrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray>{
        return forhåndsvis(behandlingId)
    }

    private fun forhåndsvis(behandlingId: UUID): Ressurs<ByteArray>{
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(brevService.forhåndsvisBrev(behandling))
    }*/
}