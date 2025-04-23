package no.nav.familie.klage.infrastruktur.health

import no.nav.familie.http.health.AbstractHealthIndicator
import no.nav.familie.klage.brev.BrevClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class BrevHealth(
    client: BrevClient,
) : AbstractHealthIndicator(client, "familie.brev")
