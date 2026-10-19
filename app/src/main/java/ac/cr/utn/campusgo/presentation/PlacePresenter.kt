package ac.cr.utn.campusgo.presentation

import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.services.PlaceService

class PlacePresenter(
    private val service: PlaceService,
    private val onPlacesChanged: (List<Place>) -> Unit,
    private val onError: (String) -> Unit
) {
    private var places: List<Place> = emptyList()
    private var searchText = ""
    private var selectedCategory = "Todas"

    fun load() {
        execute {
            places = service.getPlaces()
            publish()
        }
    }

    fun filter(search: String, category: String) {
        searchText = search.trim()
        selectedCategory = category
        publish()
    }

    fun save(place: Place) {
        execute {
            service.savePlace(place)
            places = service.getPlaces()
            publish()
        }
    }

    fun delete(id: String) {
        execute {
            service.deletePlace(id)
            places = service.getPlaces()
            publish()
        }
    }

    private fun publish() {
        val filtered = places.filter { place ->
            val matchesCategory =
                selectedCategory == "Todas" || place.category == selectedCategory
            val searchable = "${place.name} ${place.description} ${place.category}"
            matchesCategory && searchable.contains(searchText, ignoreCase = true)
        }.sortedBy { it.name.lowercase() }

        onPlacesChanged(filtered)
    }

    private inline fun execute(action: () -> Unit) {
        try {
            action()
        } catch (exception: IllegalArgumentException) {
            onError(exception.message ?: "Los datos ingresados no son válidos.")
        } catch (exception: Exception) {
            onError(exception.message ?: "Ocurrió un error inesperado.")
        }
    }
}
