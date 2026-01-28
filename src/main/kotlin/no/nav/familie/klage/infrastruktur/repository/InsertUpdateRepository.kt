package no.nav.familie.klage.infrastruktur.repository

interface InsertUpdateRepository<T> {
    fun insert(t: T): T

    fun insertAll(list: List<T>): List<T>

    fun update(t: T): T

    fun updateAll(list: List<T>): List<T>
}
