package ac.cr.utn.campusgo.data.repositories

import ac.cr.utn.campusgo.data.datasource.local.PlaceLocalDataSource
import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.repositories.PlaceRepository

class PlaceRepositoryImpl(
    private val localDataSource: PlaceLocalDataSource
) : PlaceRepository {

    override fun getAll(): List<Place> = localDataSource.readAll()

    override fun save(place: Place) {
        val places = localDataSource.readAll().toMutableList()
        val index = places.indexOfFirst { it.id == place.id }

        if (index >= 0) {
            places[index] = place
        } else {
            places.add(place)
        }

        localDataSource.writeAll(places)
    }

    override fun delete(id: String) {
        val places = localDataSource.readAll().filterNot { it.id == id }
        localDataSource.writeAll(places)
    }
}
