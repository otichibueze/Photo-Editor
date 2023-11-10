package org.hyperskill.photoeditor


import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Color class




class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView

    val brightness: Slider by lazy { findViewById<Slider>(R.id.slBrightness) }
    var brightnessFilter = 0 //the value from brightness slider
    @Volatile
    var avgBrightness : Long = 0L //Average brightness value calculation

    val contrast: Slider by lazy { findViewById<Slider>(R.id.slContrast) }
    @Volatile
    var contractFilter = 0

    @Volatile
    var contractAlpha : Double = 0.0

    val saturation : Slider by lazy {findViewById(R.id.slSaturation)}
    @Volatile
    var saturationAlpha = 0.0
    @Volatile
    var saturationFilter = 0



    val gamma : Slider by lazy {findViewById(R.id.slGamma)}
    @Volatile
    var gammaFilter = 1.0


    private val btnGallery: Button by lazy { findViewById(R.id.btnGallery) }
    private val saveButton : Button by lazy { findViewById(R.id.btnSave) }
    val writePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE

    //This is temp storage
    lateinit var defaultBitmap : Bitmap

    private var pixelJob : Job? = null

    //This activity for result used to open picture
    private val activityResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val photoUri = result.data?.data ?: return@registerForActivityResult
                // code to update ivPhoto with loaded image
                if (photoUri != null) {
                    currentImage.setImageURI(photoUri)
                    defaultBitmap = currentImage.drawable.toBitmap()
                    avgBrightness = 0L
                    contractAlpha = 0.0
                    pixelJob?.cancel()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        //do not change this line
        currentImage.setImageBitmap(createBitmap())
        defaultBitmap = currentImage.drawable.toBitmap()


        btnGallery.setOnClickListener {
            openGallery()
        }

        brightness.addOnChangeListener { slider, value, fromUser ->
            brightnessFilter = value.toInt()


            applyFilters()


        }

        contrast.addOnChangeListener { slider, value, fromUser ->
            contractFilter = value.toInt()


            applyFilters()

        }

        saturation.addOnChangeListener { slider, value, fromUser ->
            saturationFilter = value.toInt()


            applyFilters()

        }

        gamma.addOnChangeListener { slider, value, fromUser ->
            gammaFilter = value.toDouble()


            applyFilters()

        }


        saveButton.setOnClickListener {
            if ( hasPermission(writePermission) ) {

                val bitmap: Bitmap = currentImage.drawable.toBitmap()
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.Images.ImageColumns.WIDTH, bitmap.width)
                values.put(MediaStore.Images.ImageColumns.HEIGHT, bitmap.height)

                val uri = this@MainActivity.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return@setOnClickListener

                contentResolver.openOutputStream(uri).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)

                    Toast.makeText(this, "Image saved in pictures", Toast.LENGTH_LONG).show()
                }
            } else {
                /* request permission */
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MEDIA_REQUEST_CODE)
            }
        }

    }

    fun applyFilters() {
        avgBrightness = 0L

        pixelJob?.cancel()

        pixelJob = CoroutineScope(Dispatchers.Default).launch {

            println(" running on thread ${Thread.currentThread().name}")
            val brightBitmap = this.async { bitmapBrightness(defaultBitmap, brightnessFilter) }
            val brightBitmapCopy = brightBitmap.await()
            val contractBitmap = this.async { bitmapContrast(brightBitmapCopy, contractFilter) }
            val contractBitmapCopy = contractBitmap.await()
            val saturationBitmap =
                this.async { bitmapSaturation(contractBitmapCopy, saturationFilter) }
            val saturationBitmapCopy = saturationBitmap.await()
            val gammaBitmap = this.async { bitmapGamma(saturationBitmapCopy, gammaFilter) }
            val gammaBitmapCopy = gammaBitmap.await()

            ensureActive()

            withContext(Dispatchers.Main) {
                println("Updating on thread ${Thread.currentThread().name}")
                currentImage.setImageBitmap(gammaBitmapCopy)
            }
        }


    }


    //
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(intent)
    }


    private fun bindViews() {
        currentImage = findViewById(R.id.ivPhoto)
    }

    // do not change this function
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)
        // get pixel array from source

        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x + y) % 100 + 120

                pixels[index] = Color.rgb(R, G, B)
            }
        }

        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }

    suspend fun bitmapBrightness(bitmap: Bitmap, value: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        //create array for all pixel
        val pixels = IntArray(width * height)

        //original bitmap is immutable so we need copy
        val mutableBitmap: Bitmap = bitmap.copy(bitmap.config, true)

        //here we get pixel
        mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var R: Int
        var G: Int
        var B: Int
        var A: Int
        var index: Int



        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x

                //pixel index
                val pixel = pixels[index]
                // get color of each pixel the color are saved in one line of 24 bits
                //0xFF represents 255 in decimal 0xFF in hexadecimal is 11111111 in binary,
                R = (pixel shr 16) and 0xFF //16-24 red
                G = (pixel shr 8) and  0xFF // shr means shift 8bits 8-16 green
                B = pixel and 0xFF //0-7 blue


                //we now add our value to change brightness
                R += value
                G += value
                B += value

                avgBrightness += R + G + B


                // Ensure the value is within the valid range
                pixels[index] = Color.rgb(
                    R.coerceIn(0, 255),
                    G.coerceIn(0, 255),
                    B.coerceIn(0, 255)
                )
            }
        }

        //here we set pixel
        mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        avgBrightness /= (width * height * 3) // Calculate average brightness
        return mutableBitmap
    }

    fun bitmapContrast(bitmap: Bitmap, value: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        //create array for all pixel
        val pixels = IntArray(width * height)

        //original bitmap is immutable so we need copy
        val mutableBitmap: Bitmap = bitmap.copy(bitmap.config, true)

        //here we get pixel
        mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var R: Int
        var G: Int
        var B: Int

        var index: Int

        //var avgBright = avgBrightness.toInt()
        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x

                //pixel index
                val pixel = pixels[index]
                // get color of each pixel the color are saved in one line of 24 bits
                //0xFF represents 255 in decimal 0xFF in hexadecimal is 11111111 in binary,
                R = (pixel shr 16) and 0xFF //16-24 red
                G = (pixel shr 8) and  0xFF // shr means shift 8bits 8-16 green
                B = pixel and 0xFF //0-7 blue

                //we now add our value to change brightness

                contractAlpha = (255 + value).toDouble() / (255 - value)

                R = (contractAlpha * (R - avgBrightness) + avgBrightness).toInt()
                G = (contractAlpha * (G - avgBrightness) + avgBrightness).toInt()
                B = (contractAlpha * (B - avgBrightness) + avgBrightness).toInt()


                // Ensure the value is within the valid range
                pixels[index] = Color.rgb(
                    R.coerceIn(0, 255),
                    G.coerceIn(0, 255),
                    B.coerceIn(0, 255)
                )

            }
        }


        //here we set pixel
        mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return mutableBitmap
    }

    fun bitmapSaturation(bitmap: Bitmap, value: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        //create array for all pixel
        val pixels = IntArray(width * height)

        //original bitmap is immutable so we need copy
        val mutableBitmap: Bitmap = bitmap.copy(bitmap.config, true)

        //here we get pixel
        mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var R: Int
        var G: Int
        var B: Int
        var rgb: Int
        var index: Int

        //var avgBright = avgBrightness.toInt()
        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x

                //pixel index
                val pixel = pixels[index]
                // get color of each pixel the color are saved in one line of 24 bits
                //0xFF represents 255 in decimal 0xFF in hexadecimal is 11111111 in binary,
                R = (pixel shr 16) and 0xFF //16-24 red
                G = (pixel shr 8) and  0xFF // shr means shift 8bits 8-16 green
                B = pixel and 0xFF //0-7 blue

                //we now add our value to change brightness

                rgb = (R + G + B) / 3

                saturationAlpha = (255 + value).toDouble() / (255 - value)

                R = (saturationAlpha * (R - rgb) + rgb).toInt()
                G = (saturationAlpha * (G - rgb) + rgb).toInt()
                B = (saturationAlpha * (B - rgb) + rgb).toInt()


                // Ensure the value is within the valid range
                pixels[index] = Color.rgb(
                    R.coerceIn(0, 255),
                    G.coerceIn(0, 255),
                    B.coerceIn(0, 255)
                )

            }
        }


        //here we set pixel
        mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return mutableBitmap
    }

    fun bitmapGamma(bitmap: Bitmap, value: Double): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        //create array for all pixel
        val pixels = IntArray(width * height)

        //original bitmap is immutable so we need copy
        val mutableBitmap: Bitmap = bitmap.copy(bitmap.config, true)

        //here we get pixel
        mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var R: Int
        var G: Int
        var B: Int
        var index: Int

        //var avgBright = avgBrightness.toInt()
        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x

                //pixel index
                val pixel = pixels[index]
                // get color of each pixel the color are saved in one line of 24 bits
                //0xFF represents 255 in decimal 0xFF in hexadecimal is 11111111 in binary,
                R = (pixel shr 16) and 0xFF //16-24 red
                G = (pixel shr 8) and  0xFF // shr means shift 8bits 8-16 green
                B = pixel and 0xFF //0-7 blue

                R = (255.0 * Math.pow(R.toDouble() / 255.0, value)).toInt()
                G = (255.0 * Math.pow(G.toDouble() / 255.0, value)).toInt()
                B = (255.0 * Math.pow(B.toDouble() / 255.0, value)).toInt()


                // Ensure the value is within the valid range
                pixels[index] = Color.rgb(
                    R.coerceIn(0, 255),
                    G.coerceIn(0, 255),
                    B.coerceIn(0, 255)
                )

            }
        }


        //here we set pixel
        mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return mutableBitmap
    }


    private fun hasPermission(manifestPermission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.checkSelfPermission(manifestPermission) == PackageManager.PERMISSION_GRANTED
        } else {
            PermissionChecker.checkSelfPermission(this, manifestPermission) == PermissionChecker.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MEDIA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted, use the restricted features
                    saveButton.callOnClick()

                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()

                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }



    companion object {
        private const val MEDIA_REQUEST_CODE = 0
    }

}


