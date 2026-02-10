package no.nav.familie.klage.infrastruktur.health

import no.nav.familie.klage.brev.BrevClient
import no.nav.familie.restklient.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class BrevHealth(
    client: BrevClient,
) : AbstractHealthIndicator(client, "familie.brev")
