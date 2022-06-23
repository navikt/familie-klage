package no.nav.familie.klage.infrastruktur.config

import no.nav.familie.klage.felles.domain.Endret
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
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
                StringTilPropertiesWrapperConverter()
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
}
