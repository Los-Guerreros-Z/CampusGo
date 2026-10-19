package ac.cr.utn.campusgo.data.datasource.local

import ac.cr.utn.campusgo.domain.entities.Place
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PlaceDataException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

interface PlaceLocalDataSource {
    fun readAll(): List<Place>
    fun writeAll(places: List<Place>)
}

class SharedPreferencesPlaceDataSource(context: Context) : PlaceLocalDataSource {
    private val preferences = context.getSharedPreferences(
        "campusgo_places",
        Context.MODE_PRIVATE
    )

    override fun readAll(): List<Place> {
        return try {
            val json = preferences.getString("places", "[]") ?: "[]"
            val array = JSONArray(json)
            val places = mutableListOf<Place>()

            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                places.add(
                    Place(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        category = item.getString("category"),
                        description = item.optString("description", ""),
                        latitude = if (item.isNull("latitude")) null
                            else item.optDouble("latitude"),
                        longitude = if (item.isNull("longitude")) null
                            else item.optDouble("longitude"),
                        imageUri = item.optString("imageUri", "").ifBlank { null }
                    )
                )
            }
            places
        } catch (exception: Exception) {
            throw PlaceDataException("No se pudieron leer los lugares guardados.", exception)
        }
    }

    override fun writeAll(places: List<Place>) {
        try {
            val array = JSONArray()
            places.forEach { place ->
                array.put(
                    JSONObject().apply {
                        put("id", place.id)
                        put("name", place.name)
                        put("category", place.category)
                        put("description", place.description)
                        put("latitude", place.latitude ?: JSONObject.NULL)
                        put("longitude", place.longitude ?: JSONObject.NULL)
                        put("imageUri", place.imageUri ?: JSONObject.NULL)
                    }
                )
            }

            val saved = preferences.edit()
                .putString("places", array.toString())
                .commit()

            if (!saved) {
                throw PlaceDataException("No se pudieron guardar los cambios.")
            }
        } catch (exception: PlaceDataException) {
            throw exception
        } catch (exception: Exception) {
            throw PlaceDataException("Error al guardar los lugares.", exception)
        }
    }
}
