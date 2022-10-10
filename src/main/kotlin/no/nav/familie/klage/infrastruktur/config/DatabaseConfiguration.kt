package no.nav.familie.klage.infrastruktur.config

import no.nav.familie.klage.felles.domain.Endret
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import org.apache.commons.lang3.StringUtils
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
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean
    fun auditSporbarEndret(): AuditorAware<Endret> {
        return AuditorAware {
            Optional.of(Endret())
        }
    }

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(
            listOf(
                PropertiesWrapperTilStringConverter(),
                StringTilPropertiesWrapperConverter(),
                StringListTilStringConverter(),
                StringTilStringList(),
                FilTilBytearrayConverter(),
                BytearrayTilFilConverter()
            )
        )
    }

    @Bean
    fun verifyIgnoreIfProd(
        @Value("\${spring.flyway.placeholders.ignoreIfProd}") ignoreIfProd: String,
        environment: Environment
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

    data class StringListWrapper(val verdier: List<String>)

    @WritingConverter
    class StringListTilStringConverter : Converter<StringListWrapper, String> {

        override fun convert(wrapper: StringListWrapper): String {
            return StringUtils.join(wrapper.verdier, ";")
        }
    }

    @ReadingConverter
    class StringTilStringList : Converter<String, StringListWrapper> {

        override fun convert(verdi: String): StringListWrapper {
            return StringListWrapper(verdi.split(";"))
        }
    }

    @WritingConverter
    class FilTilBytearrayConverter : Converter<Fil, ByteArray> {

        override fun convert(fil: Fil): ByteArray {
            return fil.bytes
        }
    }

    @ReadingConverter
    class BytearrayTilFilConverter : Converter<ByteArray, Fil> {

        override fun convert(bytes: ByteArray): Fil {
            return Fil(bytes)
        }
    }
}
