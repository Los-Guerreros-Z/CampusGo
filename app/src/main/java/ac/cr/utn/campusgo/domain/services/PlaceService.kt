
package ac.cr.utn.campusgo.domain.services

import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.repositories.PlaceRepository
import android.net.Uri

class PlaceService(
    private val repository: PlaceRepository,
    private val photoService: PhotoService
) {
    fun getPlaces(): List<Place> = repository.getAll()

    fun savePlace(place: Place) {
        val normalizedName = place.name.trim()

        require(normalizedName.isNotEmpty()) {
            "El nombre es obligatorio."
        }
        require(normalizedName.length <= 80) {
            "El nombre no puede superar 80 caracteres."
        }
        require(place.description.length <= 500) {
            "La descripción no puede superar 500 caracteres."
        }

        val latitude = place.latitude
        val longitude = place.longitude

        require((latitude == null) == (longitude == null)) {
            "Ingresá tanto la latitud como la longitud."
        }

        if (latitude != null && longitude != null) {
            require(latitude in -90.0..90.0) {
                "La latitud debe estar entre -90 y 90."
            }
            require(longitude in -180.0..180.0) {
                "La longitud debe estar entre -180 y 180."
            }
        }

        val previousPlace = repository.getAll()
            .firstOrNull { it.id == place.id }

        val normalizedPlace = place.copy(name = normalizedName)

        repository.save(normalizedPlace)

        val previousImage = previousPlace?.imageUri
        val currentImage = normalizedPlace.imageUri

        if (previousImage != null && previousImage != currentImage) {
            photoService.deleteIfUnused(
                Uri.parse(previousImage),
                repository.getAll()
            )
        }
    }

    fun deletePlace(id: String) {
        require(id.isNotBlank()) {
            "El identificador del lugar no es válido."
        }

        val placeToDelete = repository.getAll()
            .firstOrNull { it.id == id }
            ?: return

        repository.delete(id)

        photoService.deleteIfUnused(
            placeToDelete.imageUri?.let(Uri::parse),
            repository.getAll()
        )
    }
}
