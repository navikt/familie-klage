package no.nav.familie.klage.infrastruktur.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class LenkeConfig(
    @Value("lenker.EF_SAK_FRONTEND_URL")
    val efSakLenke: String,
    @Value("lenker.BA_SAK_FRONTEND_URL")
    val baSakLenke: String
)
