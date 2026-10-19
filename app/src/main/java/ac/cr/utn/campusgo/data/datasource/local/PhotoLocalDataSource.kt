
package ac.cr.utn.campusgo.data.datasource.local

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class PhotoStorageException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

interface PhotoLocalDataSource {
    fun save(bitmap: Bitmap): Uri
    fun delete(uri: Uri)
}

class InternalPhotoLocalDataSource(
    context: Context
) : PhotoLocalDataSource {

    private val directory = File(context.filesDir, "campusgo_photos").apply {
        if (!exists() && !mkdirs()) {
            throw PhotoStorageException("No se pudo crear el directorio de fotografías.")
        }
    }

    override fun save(bitmap: Bitmap): Uri {
        val file = File(directory, "${UUID.randomUUID()}.jpg")

        try {
            FileOutputStream(file).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                    throw PhotoStorageException("No se pudo codificar la fotografía.")
                }
                output.fd.sync()
            }
            return Uri.fromFile(file)
        } catch (exception: Exception) {
            file.delete()
            if (exception is PhotoStorageException) throw exception
            throw PhotoStorageException("No se pudo guardar la fotografía.", exception)
        }
    }

    override fun delete(uri: Uri) {
        if (uri.scheme != "file") return

        val file = File(requireNotNull(uri.path)).canonicalFile
        if (file.parentFile != directory.canonicalFile) {
            throw PhotoStorageException("La fotografía no pertenece al almacenamiento de CampusGo.")
        }

        if (file.exists() && !file.delete()) {
            throw PhotoStorageException("No se pudo eliminar la fotografía.")
        }
    }
}
