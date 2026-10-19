
package ac.cr.utn.campusgo.domain.repositories

import android.graphics.Bitmap
import android.net.Uri

interface PhotoRepository {
    fun save(bitmap: Bitmap): Uri
    fun delete(uri: Uri)
}
