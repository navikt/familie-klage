package no.nav.familie.klage.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull

inline fun <reified T, ID: Any> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T = findByIdOrNull(id) ?: throw IllegalStateException("Finner ikke ${T::class.simpleName} med id=$id")

inline fun <reified T, ID> CrudRepository<T, ID>.findAllByIdOrThrow(
    ids: Set<ID>,
    getId: (T) -> ID,
): List<T> {
    val result = findAllById(ids).toList()
    val resultPerId = result.map(getId)
    require(resultPerId.containsAll(ids)) {
        "Finner ikke ${T::class.simpleName} for ${ids.filterNot(resultPerId::contains)}"
    }
    return result
}
