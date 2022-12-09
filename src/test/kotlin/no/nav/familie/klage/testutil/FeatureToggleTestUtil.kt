package no.nav.familie.klage.testutil

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService

fun mockFeatureToggleService(enabled: Boolean = true): FeatureToggleService {
    val mockk = mockk<FeatureToggleService>()
    every { mockk.isEnabled(any()) } returns enabled
    return mockk
}
