
package ac.cr.utn.campusgo.data.repositories

import ac.cr.utn.campusgo.data.datasource.local.PhotoLocalDataSource
import ac.cr.utn.campusgo.domain.repositories.PhotoRepository
import android.graphics.Bitmap
import android.net.Uri

class PhotoRepositoryImpl(
    private val dataSource: PhotoLocalDataSource
) : PhotoRepository {

    override fun save(bitmap: Bitmap): Uri = dataSource.save(bitmap)

    override fun delete(uri: Uri) = dataSource.delete(uri)
}
