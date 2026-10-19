package ac.cr.utn.campusgo

import ac.cr.utn.campusgo.data.datasource.local.SharedPreferencesPlaceDataSource
import ac.cr.utn.campusgo.data.repositories.PlaceRepositoryImpl
import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.services.PlaceService
import ac.cr.utn.campusgo.presentation.PlacePresenter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.UUID
import ac.cr.utn.campusgo.data.datasource.local.InternalPhotoLocalDataSource
import ac.cr.utn.campusgo.data.repositories.PhotoRepositoryImpl
import ac.cr.utn.campusgo.domain.services.PhotoService
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    private lateinit var presenter: PlacePresenter
    private lateinit var listContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var categoryFilter: Spinner
    private lateinit var emptyMessage: TextView
    private lateinit var photoService: PhotoService

    private val categories = listOf(
        "Todas",
        "Edificio",
        "Biblioteca",
        "Comedor",
        "Laboratorio",
        "Oficina",
        "Deporte",
        "Otro"
    )

    private var pendingImageCallback: ((Uri) -> Unit)? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                pendingImageCallback?.invoke(uri)
            } catch (exception: SecurityException) {
                Toast.makeText(
                    this,
                    "No se pudo conservar el acceso a la imagen.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        pendingImageCallback = null
    }

    private val cameraCapture = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                pendingImageCallback?.invoke(photoService.save(bitmap))
            } catch (exception: Exception) {
                Toast.makeText(
                    this,
                    exception.message ?: "No se pudo guardar la fotografía.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        pendingImageCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataSource = SharedPreferencesPlaceDataSource(applicationContext)
        val repository = PlaceRepositoryImpl(dataSource)

        val photoDataSource = InternalPhotoLocalDataSource(applicationContext)
        val photoRepository = PhotoRepositoryImpl(photoDataSource)
        photoService = PhotoService(photoRepository)

        val service = PlaceService(repository, photoService)

        presenter = PlacePresenter(
            service = service,
            onPlacesChanged = { places -> renderPlaces(places) },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        )

        buildScreen()
        presenter.load()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(12))
            setBackgroundColor(0xFFF6F7FB.toInt())
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "CampusGo"
            subtitle = "Explorador de servicios universitarios"
            setTitleTextColor(0xFF172554.toInt())
            setSubtitleTextColor(0xFF475569.toInt())
            setBackgroundColor(0xFFF6F7FB.toInt())
            elevation = 0f
        }
        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(76)
            )
        )

        searchInput = EditText(this).apply {
            hint = "Buscar lugares..."
            setSingleLine(true)
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(
            searchInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply { bottomMargin = dp(8) }
        )

        categoryFilter = Spinner(this)
        categoryFilter.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        root.addView(
            categoryFilter,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val sectionTitle = TextView(this).apply {
            text = "Lugares del campus"
            textSize = 20f
            setTextColor(0xFF172554.toInt())
            setPadding(0, dp(16), 0, dp(8))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(sectionTitle)

        emptyMessage = TextView(this).apply {
            text = "No hay lugares para mostrar. Agregá el primero."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(0xFF64748B.toInt())
            setPadding(dp(12), dp(24), dp(12), dp(24))
        }
        root.addView(emptyMessage)

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val addButton = MaterialButton(this).apply {
            text = "Agregar lugar"
            setOnClickListener { showPlaceDialog(null) }
        }
        root.addView(
            addButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply { topMargin = dp(8) }
        )

        setContentView(root)

        searchInput.addTextChangedListener(SimpleTextWatcher {
            presenter.filter(
                searchInput.text.toString(),
                categoryFilter.selectedItem?.toString() ?: "Todas"
            )
        })

        categoryFilter.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    presenter.filter(
                        searchInput.text.toString(),
                        categories[position]
                    )
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) = Unit
            }
    }

    private fun renderPlaces(places: List<Place>) {
        listContainer.removeAllViews()
        emptyMessage.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE

        places.forEach { place ->
            val card = MaterialCardView(this).apply {
                radius = dp(14).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(0xFFFFFFFF.toInt())
                strokeWidth = dp(1)
                strokeColor = 0xFFE2E8F0.toInt()
            }

            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            val name = TextView(this).apply {
                text = place.name
                textSize = 18f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(0xFF172554.toInt())
            }
            content.addView(name)

            val category = TextView(this).apply {
                text = place.category
                textSize = 13f
                setTextColor(0xFF2563EB.toInt())
                setPadding(0, dp(3), 0, dp(5))
            }
            content.addView(category)

            if (place.description.isNotBlank()) {
                content.addView(TextView(this).apply {
                    text = place.description
                    textSize = 14f
                    setTextColor(0xFF475569.toInt())
                })
            }

            place.imageUri?.let { value ->
                val image = ImageView(this).apply {
                    contentDescription = "Fotografía de ${place.name}"
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = true
                    setImageURI(Uri.parse(value))
                }

                content.addView(
                    image,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(180)
                    ).apply {
                        topMargin = dp(8)
                        bottomMargin = dp(8)
                    }
                )
            }

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            if (place.latitude != null && place.longitude != null) {
                actions.addView(actionButton("Mapa") { openMap(place) })
            }
            actions.addView(actionButton("Editar") { showPlaceDialog(place) })
            actions.addView(actionButton("Eliminar") { confirmDelete(place) })
            content.addView(actions)

            card.addView(content)
            listContainer.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            )
        }
    }

    private fun actionButton(label: String, action: () -> Unit): Button {
        return MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            textSize = 12f
            minWidth = 0
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { action() }
        }
    }

    private fun showPlaceDialog(existing: Place?) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        fun field(hintText: String, value: String = ""): EditText {
            return EditText(this).apply {
                hint = hintText
                setText(value)
                setSingleLine(hintText != "Descripción")
                if (hintText == "Descripción") {
                    minLines = 2
                    maxLines = 4
                }
            }.also {
                form.addView(
                    it,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        }

        val nameInput = field("Nombre", existing?.name.orEmpty())

        val categorySpinner = Spinner(this)
        val availableCategories = categories.filterNot { it == "Todas" }
        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            availableCategories
        )
        val existingCategoryIndex = availableCategories.indexOf(existing?.category)
        if (existingCategoryIndex >= 0) {
            categorySpinner.setSelection(existingCategoryIndex)
        }
        form.addView(categorySpinner)

        val descriptionInput = field("Descripción", existing?.description.orEmpty())
        val latitudeInput = field("Latitud", existing?.latitude?.toString().orEmpty())
        val longitudeInput = field("Longitud", existing?.longitude?.toString().orEmpty())

        val imageStatus = TextView(this).apply {
            text = if (existing?.imageUri != null) {
                "Fotografía seleccionada"
            } else {
                "Sin fotografía"
            }
            setTextColor(0xFF475569.toInt())
            setPadding(0, dp(8), 0, dp(4))
        }
        form.addView(imageStatus)

        var imageUri = existing?.imageUri

        val galleryButton = MaterialButton(this).apply {
            text = "Seleccionar desde galería"
            setOnClickListener {
                pendingImageCallback = { uri ->
                    imageUri = uri.toString()
                    imageStatus.text = "Fotografía seleccionada"
                }
                imagePicker.launch(arrayOf("image/*"))
            }
        }

        form.addView(galleryButton)

        val cameraButton = MaterialButton(this).apply {
            text = "Tomar fotografía"
            setOnClickListener {
                pendingImageCallback = { uri ->
                    imageUri = uri.toString()
                    imageStatus.text = "Fotografía capturada"
                }
                cameraCapture.launch(null)
            }
        }

        form.addView(cameraButton)

        val scroll = ScrollView(this).apply {
            addView(form)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nuevo lugar" else "Editar lugar")
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val latitudeText = latitudeInput.text.toString().trim()
                val longitudeText = longitudeInput.text.toString().trim()

                val latitude = latitudeText.toDoubleOrNull()
                val longitude = longitudeText.toDoubleOrNull()

                if ((latitudeText.isNotEmpty() && latitude == null) ||
                    (longitudeText.isNotEmpty() && longitude == null)
                ) {
                    Toast.makeText(
                        this,
                        "La latitud y longitud deben ser números válidos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                val place = Place(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = nameInput.text.toString(),
                    category = categorySpinner.selectedItem.toString(),
                    description = descriptionInput.text.toString().trim(),
                    latitude = latitude,
                    longitude = longitude,
                    imageUri = imageUri
                )

                presenter.save(place)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmDelete(place: Place) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar lugar")
            .setMessage("¿Deseás eliminar \"${place.name}\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                presenter.delete(place.id)
            }
            .show()
    }

    private fun openMap(place: Place) {
        val latitude = place.latitude ?: return
        val longitude = place.longitude ?: return
        val label = Uri.encode(place.name)
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No hay una aplicación de mapas instalada.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
