package org.hyperskill.photoeditor

import android.graphics.Bitmap
import android.graphics.Color

class Filters {

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


}