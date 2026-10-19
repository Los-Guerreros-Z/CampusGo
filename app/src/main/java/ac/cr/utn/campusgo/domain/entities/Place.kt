package ac.cr.utn.campusgo.domain.entities

data class Place(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val latitude: Double?,
    val longitude: Double?,
    val imageUri: String?
)
