package ac.cr.utn.campusgo.domain.repositories

import ac.cr.utn.campusgo.domain.entities.Place

interface PlaceRepository {
    fun getAll(): List<Place>
    fun save(place: Place)
    fun delete(id: String)
}
