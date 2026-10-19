
package ac.cr.utn.campusgo.domain.services

import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.repositories.PhotoRepository
import android.graphics.Bitmap
import android.net.Uri

class PhotoService(
    private val repository: PhotoRepository
) {
    fun save(bitmap: Bitmap): Uri {
        require(bitmap.width > 0 && bitmap.height > 0) {
            "La fotografía no es válida."
        }
        return repository.save(bitmap)
    }

    fun delete(uri: Uri) {
        repository.delete(uri)
    }

    fun deleteIfUnused(uri: Uri?, places: List<Place>) {
        if (uri == null) return

        val isReferenced = places.any { place ->
            place.imageUri == uri.toString()
        }

        if (!isReferenced) {
            repository.delete(uri)
        }
    }
}
