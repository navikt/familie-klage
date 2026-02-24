package no.nav.familie.klage.infrastruktur.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    @Primary
    fun cacheManager(): CacheManager =
        object : ConcurrentMapCacheManager() {
            override fun createConcurrentMapCache(name: String): Cache {
                val concurrentMap =
                    Caffeine
                        .newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(60, TimeUnit.MINUTES)
                        .recordStats()
                        .build<Any, Any>()
                        .asMap()
                return ConcurrentMapCache(name, concurrentMap, true)
            }
        }

    @Bean("shortCache")
    fun shortCache(): CacheManager =
        object : ConcurrentMapCacheManager() {
            override fun createConcurrentMapCache(name: String): Cache {
                val concurrentMap =
                    Caffeine
                        .newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build<Any, Any>()
                        .asMap()
                return ConcurrentMapCache(name, concurrentMap, true)
            }
        }
}

/**
 * Forventer treff, skal ikke brukes hvis en cache inneholder nullverdi
 * this.getCache(cache) burde aldri kunne returnere null, då den lager en cache hvis den ikke finnes fra før
 */
fun <K : Any, T> CacheManager.getValue(
    cache: String,
    key: K,
    valueLoader: () -> T,
): T = this.getNullable(cache, key, valueLoader) ?: error("Finner ikke cache for cache=$cache key=$key")

fun <K : Any, T> CacheManager.getNullable(
    cacheName: String,
    key: K,
    valueLoader: () -> T?,
): T? {
    val cache = getCacheOrThrow(cacheName)

    @Suppress("UNCHECKED_CAST")
    return (cache.get(key)?.get() as T?)
        ?: valueLoader().also { cache.put(key, it) }
}

fun CacheManager.getCacheOrThrow(cache: String) = this.getCache(cache) ?: error("Finner ikke cache=$cache")
