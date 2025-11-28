package fr.pass.local_password

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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
        if (code != null && code.rawValue != null) {
            Text(text = code.rawValue.toString(), modifier = Modifier.padding(innerPadding))
        } else {
            Greeting(
                name = "Android",
                modifier = Modifier.padding(innerPadding)
            )
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