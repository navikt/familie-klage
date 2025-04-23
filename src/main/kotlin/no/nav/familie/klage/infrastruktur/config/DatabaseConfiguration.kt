package no.nav.familie.klage.infrastruktur.config

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.behandling.domain.FagsystemRevurdering
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.felles.domain.Endret
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import org.apache.commons.lang3.StringUtils
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import java.util.Optional
import javax.sql.DataSource

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories("no.nav.familie")
class DatabaseConfiguration : AbstractJdbcConfiguration() {
    @Bean
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations = NamedParameterJdbcTemplate(dataSource)

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)

    @Bean
    fun auditSporbarEndret(): AuditorAware<Endret> =
        AuditorAware {
            Optional.of(Endret())
        }

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions =
        JdbcCustomConversions(
            listOf(
                PropertiesWrapperTilStringConverter(),
                StringTilPropertiesWrapperConverter(),
                StringListTilStringConverter(),
                StringTilStringList(),
                FilTilBytearrayConverter(),
                BytearrayTilFilConverter(),
                BrevmottakereTilBytearrayConverter(),
                BytearrayTilBrevmottakereConverter(),
                BrevmottakereJournalposterTilBytearrayConverter(),
                BytearrayTilBrevmottakereJournalposterConverter(),
                PåklagetVedtakDetaljerTilBytearrayConverter(),
                BytearrayTilPåklagetVedtakDetaljerConverter(),
                OpprettetRevurderingTilBytearrayConverter(),
                BytearrayTilOpprettetRevurderingConverter(),
            ),
        )

    @Bean
    fun verifyIgnoreIfProd(
        @Value("\${spring.flyway.placeholders.ignoreIfProd}") ignoreIfProd: String,
        environment: Environment,
    ): FlywayConfigurationCustomizer {
        val isProd = environment.activeProfiles.contains("prod")
        val ignore = ignoreIfProd == "--"
        return FlywayConfigurationCustomizer {
            if (isProd && !ignore) {
                throw RuntimeException("Prod profile men har ikke riktig verdi for placeholder ignoreIfProd=$ignoreIfProd")
            }
            if (!isProd && ignore) {
                throw RuntimeException("Profile=${environment.activeProfiles} men har ignoreIfProd=--")
            }
        }
    }

    data class StringListWrapper(
        val verdier: List<String>,
    )

    @WritingConverter
    class StringListTilStringConverter : Converter<StringListWrapper, String> {
        override fun convert(wrapper: StringListWrapper): String = StringUtils.join(wrapper.verdier, ";")
    }

    @ReadingConverter
    class StringTilStringList : Converter<String, StringListWrapper> {
        override fun convert(verdi: String): StringListWrapper = StringListWrapper(verdi.split(";"))
    }

    @WritingConverter
    class FilTilBytearrayConverter : Converter<Fil, ByteArray> {
        override fun convert(fil: Fil): ByteArray = fil.bytes
    }

    @ReadingConverter
    class BytearrayTilFilConverter : Converter<ByteArray, Fil> {
        override fun convert(bytes: ByteArray): Fil = Fil(bytes)
    }

    @WritingConverter
    class BrevmottakereTilBytearrayConverter : Converter<Brevmottakere, PGobject> {
        override fun convert(o: Brevmottakere): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(o)
            }
    }

    @ReadingConverter
    class BytearrayTilBrevmottakereConverter : Converter<PGobject, Brevmottakere> {
        override fun convert(pGobject: PGobject): Brevmottakere = objectMapper.readValue(pGobject.value!!)
    }

    @WritingConverter
    class BrevmottakereJournalposterTilBytearrayConverter : Converter<BrevmottakereJournalposter, PGobject> {
        override fun convert(o: BrevmottakereJournalposter): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(o)
            }
    }

    @ReadingConverter
    class BytearrayTilBrevmottakereJournalposterConverter : Converter<PGobject, BrevmottakereJournalposter> {
        override fun convert(pGobject: PGobject): BrevmottakereJournalposter = objectMapper.readValue(pGobject.value!!)
    }

    @WritingConverter
    class PåklagetVedtakDetaljerTilBytearrayConverter : Converter<PåklagetVedtakDetaljer, PGobject> {
        override fun convert(o: PåklagetVedtakDetaljer): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(o)
            }
    }

    @ReadingConverter
    class BytearrayTilPåklagetVedtakDetaljerConverter : Converter<PGobject, PåklagetVedtakDetaljer> {
        override fun convert(pGobject: PGobject): PåklagetVedtakDetaljer = objectMapper.readValue(pGobject.value!!)
    }

    @WritingConverter
    class OpprettetRevurderingTilBytearrayConverter : Converter<FagsystemRevurdering, PGobject> {
        override fun convert(o: FagsystemRevurdering): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(o)
            }
    }

    @ReadingConverter
    class BytearrayTilOpprettetRevurderingConverter : Converter<PGobject, FagsystemRevurdering> {
        override fun convert(pGobject: PGobject): FagsystemRevurdering = objectMapper.readValue(pGobject.value!!)
    }
}
