package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.domain.Brev
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
    ){
    @GetMapping("/{behandlingId}")
    fun hentBrev(@PathVariable behandlingId: UUID): Ressurs<Brev?> {
        return Ressurs.success(brevService.hentMellomlagretBrev(behandlingId))
    }
    @PostMapping("")
    fun lagBrev(@RequestBody brevInnhold: FritekstBrevDto): Ressurs<ByteArray> {
        return Ressurs.success(brevService.lagBrev(brevInnhold))
    }
}