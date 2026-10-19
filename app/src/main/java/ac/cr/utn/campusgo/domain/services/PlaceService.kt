package ac.cr.utn.campusgo.domain.services

import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.repositories.PlaceRepository

class PlaceService(private val repository: PlaceRepository) {

    fun getPlaces(): List<Place> = repository.getAll()

    fun savePlace(place: Place) {
        val normalizedName = place.name.trim()
        require(normalizedName.isNotEmpty()) { "El nombre es obligatorio." }
        require(normalizedName.length <= 80) { "El nombre no puede superar 80 caracteres." }
        require(place.description.length <= 500) {
            "La descripción no puede superar 500 caracteres."
        }

        val latitude = place.latitude
        val longitude = place.longitude

        require((latitude == null) == (longitude == null)) {
            "Ingresá tanto la latitud como la longitud."
        }
        if (latitude != null && longitude != null) {
            require(latitude in -90.0..90.0) { "La latitud debe estar entre -90 y 90." }
            require(longitude in -180.0..180.0) {
                "La longitud debe estar entre -180 y 180."
            }
        }

        repository.save(place.copy(name = normalizedName))
    }

    fun deletePlace(id: String) {
        require(id.isNotBlank()) { "El identificador del lugar no es válido." }
        repository.delete(id)
    }
}
