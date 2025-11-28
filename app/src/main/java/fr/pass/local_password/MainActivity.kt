package fr.pass.local_password


import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import fr.pass.local_password.MainActivity.LogConstant.MANUAL_TESTING_LOG
import fr.pass.local_password.ui.theme.LocalPasswordTheme

class MainActivity : ComponentActivity() {
    object LogConstant {
        const val MANUAL_TESTING_LOG = "LogTagForTest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LocalPasswordTheme {
                SelectImageFromPicker()
            }
        }
    }
}

data class PasswordEntry(val title: String, val user: String, val password: String) {
    companion object {
        fun fromEntry(entry: String): PasswordEntry {
            val tokens = entry.split(",")
            return PasswordEntry(tokens[0], tokens[1], tokens[2])
        }
    }
}

fun decodeQRCode(context: Context, uri: Uri?, onSuccess: (Barcode?) -> Unit) {
    // Callback is invoked after the user selects a media item or closes the
    // photo picker.
    if (uri == null) {
        Log.d("PhotoPicker", "No media selected")
        return
    }

    Log.d("PhotoPicker", "Selected URI: $uri")
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val barcodeScanner = BarcodeScanning.getClient(options)
    val image = InputImage.fromFilePath(context, uri)
    val task = barcodeScanner.process(image)
    task.addOnSuccessListener { barcodes ->
        if (barcodes.isEmpty()) {
            Log.v(MANUAL_TESTING_LOG, "No barcode has been detected")
        } else {
            // Assume there is only one QR code in the image
            onSuccess(barcodes[0])
        }
    }
}

@Composable
fun SelectImageFromPicker() {
    val context = LocalContext.current
    var barcode by remember { mutableStateOf<Barcode?>(null) }

    // Registers a photo picker activity launcher in single-select mode.
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> decodeQRCode(context, uri) { result -> barcode = result } }
    )
    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            // Launch the photo picker and let the user choose only images.
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                        Icon(Icons.Filled.Photo, contentDescription = "Select photo")
                    }
                },
            )
        },
    ) { innerPadding ->
        val code = barcode
        if (code == null || code.rawValue == null) {
            Greeting(
                name = "Android",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        val raw = code.rawValue.toString().trimEnd(';').replace("\n", "")
        Log.v(MANUAL_TESTING_LOG, raw)
        val entries = raw.split(";")

        LazyColumn(
            modifier = Modifier.padding(innerPadding)
        ) {
            items(entries) { entry ->
                val item = PasswordEntry.fromEntry(entry)

                Column(
                    modifier = Modifier
                        .border(1.dp, Color.Red)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                    ) {
                        Text(modifier = Modifier.align(Alignment.Center), text = item.title)
                    }

                    EntryCopyPaste("username", item.user, false, paste = true)
                    EntryCopyPaste("password", item.password, true, paste = true)
                }
            }
        }
    }
}

@Composable
fun EntryCopyPaste(name: String, value: String, obfuscation: Boolean, paste: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    var showValue by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
            .padding(6.dp)
            .height(30.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 48.dp)
                .height(50.dp)
                .fillMaxWidth()
                .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                .weight(3f / 4f)

        ) {
            val output = if (obfuscation && !showValue) {
                "$name : ****"
            } else {
                "$name : $value"
            }
            Text(modifier = Modifier.align(Alignment.CenterStart), text = output)
        }

        if (obfuscation) {
            IconButton(onClick = { showValue = !showValue }) {
                Icon(
                    if (showValue) {
                        Icons.Filled.Visibility
                    } else {
                        Icons.Filled.VisibilityOff
                    },
                    contentDescription = "Toggle $name visibility",
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .padding(16.dp)
                        .weight(1f / 7f)
                        .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                )
            }
        }

        if (paste) {
            IconButton(onClick = {
                val clipData = ClipData.newPlainText(name, value)
                val clipEntry = ClipEntry(clipData)
                clipboardManager.setClip(clipEntry)
            }) {
                Icon(
                    Icons.Filled.ContentPaste,
                    contentDescription = "Copy $name",
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .padding(16.dp)
                        .weight(1f / 7f)
                        .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LocalPasswordTheme {
        Greeting("Android")
    }
}