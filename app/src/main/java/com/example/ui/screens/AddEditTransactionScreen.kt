package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.currency.CurrencyManager
import com.example.data.model.Category
import com.example.data.model.PaymentMethod
import com.example.data.model.ReceiptScanResult
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.TicketPreviewDialog
import com.example.ui.components.saveBitmapToPersistentFile
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    initialTransaction: Transaction?,
    initialScanResult: ReceiptScanResult?,
    categories: List<Category>,
    activeCurrencyCode: String,
    onSave: (Transaction) -> Unit,
    onDelete: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val isEdit = initialTransaction != null

    // Determine initial values
    var type by remember {
        mutableStateOf(
            initialTransaction?.type ?: TransactionType.EXPENSE
        )
    }

    var amountText by remember {
        mutableStateOf(
            if (initialTransaction != null) initialTransaction.amount.toString()
            else if (initialScanResult != null && initialScanResult.totalAmount > 0.0) initialScanResult.totalAmount.toString()
            else ""
        )
    }

    var title by remember {
        mutableStateOf(
            initialTransaction?.title ?: initialScanResult?.merchant ?: ""
        )
    }

    var selectedCurrency by remember {
        mutableStateOf(initialTransaction?.currencyCode ?: activeCurrencyCode)
    }

    var selectedCategoryId by remember {
        val matchingCategory = if (initialScanResult != null) {
            categories.find { it.name.contains(initialScanResult.suggestedCategory, ignoreCase = true) }
        } else null
        mutableStateOf(initialTransaction?.categoryId ?: matchingCategory?.id ?: categories.firstOrNull { it.isIncome == (type == TransactionType.INCOME) }?.id ?: 1L)
    }

    var dateMillis by remember {
        mutableLongStateOf(initialTransaction?.date ?: System.currentTimeMillis())
    }

    var paymentMethod by remember {
        mutableStateOf(initialTransaction?.paymentMethod ?: PaymentMethod.CARD)
    }

    var notes by remember {
        mutableStateOf(initialTransaction?.notes ?: initialScanResult?.notes ?: "")
    }

    var tagsText by remember {
        mutableStateOf(initialTransaction?.tags?.joinToString(", ") ?: "")
    }

    var isRecurring by remember {
        mutableStateOf(initialTransaction?.isRecurring ?: false)
    }

    // Receipt image state
    var receiptImagePath by remember {
        mutableStateOf(initialTransaction?.receiptImagePath ?: initialScanResult?.imagePath)
    }
    var showTicketPreviewDialog by remember { mutableStateOf(false) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null && tempCameraFile!!.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(tempCameraFile!!.absolutePath)
                if (bitmap != null) {
                    val saved = saveBitmapToPersistentFile(context, bitmap)
                    receiptImagePath = saved ?: tempCameraFile!!.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al procesar la foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val takePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val saved = saveBitmapToPersistentFile(context, bitmap)
            receiptImagePath = saved
        }
    }

    val triggerCamera = {
        try {
            val cacheReceiptsDir = File(context.cacheDir, "receipts").apply {
                if (!exists()) mkdirs()
            }
            val file = File(cacheReceiptsDir, "ticket_${System.currentTimeMillis()}.jpg")
            tempCameraFile = file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                takePreviewLauncher.launch(null)
            } catch (e2: Exception) {
                Toast.makeText(context, "No se pudo iniciar la cámara: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerCamera()
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para tomar foto del ticket", Toast.LENGTH_LONG).show()
        }
    }

    val onTakePhotoClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            triggerCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
                val saved = saveBitmapToPersistentFile(context, bitmap)
                receiptImagePath = saved
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al cargar imagen de la galería", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    val availableCategories = categories.filter { it.isIncome == (type == TransactionType.INCOME) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("add_edit_transaction_screen")
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (isEdit) "Editar Movimiento" else "Nuevo Movimiento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isEdit) {
                IconButton(onClick = { initialTransaction?.let { onDelete(it.id) } }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- TYPE SEGMENTED BUTTON (GASTO / INGRESO) ---
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = type == TransactionType.EXPENSE,
                onClick = {
                    type = TransactionType.EXPENSE
                    val defaultCat = categories.firstOrNull { !it.isIncome }
                    if (defaultCat != null) selectedCategoryId = defaultCat.id
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ExpenseRed.copy(alpha = 0.15f),
                    activeContentColor = ExpenseRed
                )
            ) {
                Text("Gasto", fontWeight = FontWeight.Bold)
            }

            SegmentedButton(
                selected = type == TransactionType.INCOME,
                onClick = {
                    type = TransactionType.INCOME
                    val defaultCat = categories.firstOrNull { it.isIncome }
                    if (defaultCat != null) selectedCategoryId = defaultCat.id
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = IncomeGreen.copy(alpha = 0.15f),
                    activeContentColor = IncomeGreen
                )
            ) {
                Text("Ingreso", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- AMOUNT INPUT CARD ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Monto de la transacción",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = CurrencyManager.getCurrency(selectedCurrency).symbol,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountText = input
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        placeholder = { Text("0.00", fontSize = 32.sp, color = MaterialTheme.colorScheme.outline) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .width(220.dp)
                            .testTag("amount_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                }

                // Moneda Fija: Pesos Mexicanos
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🇲🇽", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Peso Mexicano ($ MXN)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TITLE / MERCHANT ---
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Concepto o Comercio") },
            placeholder = { Text("Ej. Walmart, Nómina, Gasolina") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_title_input"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- CATEGORY SELECTOR CHIPS ---
        Text(
            text = "Categoría",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableCategories.forEach { category ->
                val isSelected = selectedCategoryId == category.id
                val catColor = CategoryIconHelper.parseColor(category.colorHex)

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryId = category.id },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(category.iconName),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else catColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- DATE PICKER BUTTON ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fecha: ${dateFormatter.format(Date(dateMillis))}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            TextButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val updatedCal = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth)
                            }
                            dateMillis = updatedCal.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cambiar Fecha")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- PAYMENT METHOD ---
        Text(
            text = "Método de Pago",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PaymentMethod.values().forEach { method ->
                FilterChip(
                    selected = paymentMethod == method,
                    onClick = { paymentMethod = method },
                    label = { Text(method.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TICKET / COMPROBANTE IMAGE SECTION ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ticket / Comprobante de Compra",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!receiptImagePath.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Adjuntado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!receiptImagePath.isNullOrBlank()) {
                    val file = File(receiptImagePath!!)
                    if (file.exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { showTicketPreviewDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Ticket",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "Toca para ver completo",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onTakePhotoClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cámara")
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        galleryLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No se pudo abrir la galería", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galería")
                            }

                            IconButton(
                                onClick = { receiptImagePath = null },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Quitar ticket", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Text(
                            text = "Archivo de imagen no encontrado. Puedes tomar una nueva foto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        text = "Guarda la foto de tu recibo para tener el comprobante visible en la lista de movimientos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTakePhotoClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tomar Foto")
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    galleryLauncher.launch("image/*")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir la galería", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Galería")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- NOTES & TAGS ---
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas adicionales") },
            placeholder = { Text("Detalles sobre la compra...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            label = { Text("Etiquetas (separadas por coma)") },
            placeholder = { Text("despensa, fin de semana, fijo") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- RECURRING TOGGLE ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transacción Recurrente",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Se repite automáticamente cada mes (ej. sueldo, alquiler)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SAVE BUTTON ---
        Button(
            onClick = {
                val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && parsedAmount > 0.0) {
                    val convertedAmount = CurrencyManager.convert(
                        amount = parsedAmount,
                        fromCode = selectedCurrency,
                        toCode = activeCurrencyCode
                    )

                    val newTx = Transaction(
                        id = initialTransaction?.id ?: 0,
                        title = title.trim(),
                        amount = convertedAmount,
                        type = type,
                        categoryId = selectedCategoryId,
                        date = dateMillis,
                        currencyCode = selectedCurrency,
                        originalAmount = parsedAmount,
                        exchangeRateToMain = convertedAmount / parsedAmount,
                        notes = notes.trim(),
                        receiptImagePath = receiptImagePath,
                        paymentMethod = paymentMethod,
                        tags = if (tagsText.isBlank()) emptyList() else tagsText.split(",").map { it.trim() },
                        isRecurring = isRecurring
                    )
                    onSave(newTx)
                }
            },
            enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_transaction_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEdit) "Guardar Cambios" else "Registrar Movimiento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showTicketPreviewDialog && !receiptImagePath.isNullOrBlank()) {
        TicketPreviewDialog(
            imagePath = receiptImagePath!!,
            title = title.ifBlank { "Ticket de Compra" },
            subtitle = if (amountText.isNotBlank()) "Monto: $amountText $selectedCurrency" else "",
            onDismiss = { showTicketPreviewDialog = false }
        )
    }
}
