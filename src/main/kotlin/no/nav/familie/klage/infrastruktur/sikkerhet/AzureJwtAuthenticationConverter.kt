package no.nav.familie.klage.infrastruktur.sikkerhet

import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

fun BehandlerRolle.authority(): String = "ROLE_$name"

@Component
class AzureJwtAuthenticationConverter(
    private val rolleConfig: RolleConfig,
) : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val høyesteBehandlerRolle = utledHøyesteBehandlerRolle(jwt)

        val authorities =
            BehandlerRolle.entries
                .filter { it.nivå in 1..høyesteBehandlerRolle.nivå }
                .map { SimpleGrantedAuthority(it.authority()) }

        return JwtAuthenticationToken(jwt, authorities)
    }

    private fun utledHøyesteBehandlerRolle(jwt: Jwt): BehandlerRolle {
        val oid = jwt.getClaimAsString("oid")
        val erSystembruker = oid != null && oid == jwt.subject
        return if (erSystembruker) {
            BehandlerRolle.SYSTEM
        } else {
            val groups = jwt.getClaimAsStringList("groups") ?: emptyList()
            setOf(rolleConfig.ba, rolleConfig.ef, rolleConfig.ks)
                .map {
                    when {
                        it.beslutter in groups -> BehandlerRolle.BESLUTTER
                        it.saksbehandler in groups -> BehandlerRolle.SAKSBEHANDLER
                        it.veileder in groups -> BehandlerRolle.VEILEDER
                        else -> BehandlerRolle.UKJENT
                    }
                }.maxBy { it.nivå }
        }
    }
}
