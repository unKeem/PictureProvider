package com.example.pictureprovider

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.example.pictureprovider.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //인텐트를 통해서 갤러리앱에서 사진을 리턴받는 콜백처리를 진행하는
        val requestGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            //it.data(intent.data) 이미지의 경로를 받음
            //it.data.data : it.data ==intent ->intent.data(data: 이미지Uri)

            //비율계산
           val calRatio = calculateInSampleSize(it.data!!.data!!,
           resources.getDimensionPixelSize(R.dimen.imgSize), resources.getDimensionPixelSize(R.dimen.imgSize))
            //실제 이미지를 비율대로 가져온다
            val options = BitmapFactory.Options()
            options.inSampleSize = calRatio

            try{
                var inputStream = contentResolver.openInputStream(it.data!!.data!!)
                var bitmap = BitmapFactory.decodeStream(inputStream, null, options)

                //사진의 회전정보를 가져오기
                val orientation = getOrientationOfImage(it.data!!.data!!).toFloat()
                //회전된 사진을 원위치로 돌리 비트맵 가져오기
                val newBitmap = getRotatedBitmap(bitmap, orientation)

                newBitmap?.let{
                    binding.ivPicture.setImageBitmap(newBitmap)
                }?.let{
                    binding.ivPicture.setImageBitmap(bitmap)
                    Log.d("pictureprovide", "갤러리로부터 가져온 이미지는 null")
                }

            }catch(e:Exception){
                Log.d("pictureprovide", "${e.printStackTrace()}")
            }
        }

        binding.btnGallery.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            requestGalleryLauncher.launch(intent)
        }
    }
    //이미지 비율계산
    fun calculateInSampleSize(fileUri: Uri, reqWidth: Int, reqHeight: Int) : Int{
        //이미지 옵션(외부 이미지 사이즈 축소)
        val options = BitmapFactory.Options()
        //이미지 정보만을 가져와서 실제사이즈와 요청사이즈를 계산해서 비율조정하기
        options.inJustDecodeBounds = true

        try{
            var inputStream = contentResolver.openInputStream(fileUri)
            //실제이미지 정보를 options에 저장함
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            inputStream = null

        }catch(e: Exception){
            Log.d("pictureprovide", "${e.printStackTrace()}")
        }
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if(height > reqHeight || width>reqWidth){
            val halfHeight = height/2
            val halfWidth = width/2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    // 이미지 회전 정보 가져오기
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getOrientationOfImage(uri: Uri): Int {
        // uri -> inputStream
        val inputStream = contentResolver.openInputStream(uri)
        val exif: ExifInterface? = try {
            ExifInterface(inputStream!!)
        } catch (e: IOException) {
            e.printStackTrace()
            return -1
        }
        inputStream.close()

        // 회전된 각도 알아내기
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation != -1) {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> return 90
                ExifInterface.ORIENTATION_ROTATE_180 -> return 180
                ExifInterface.ORIENTATION_ROTATE_270 -> return 270
            }
        }
        return 0
    }
    // 이미지 회전하기
    @Throws(Exception::class)
    private fun getRotatedBitmap(bitmap: Bitmap?, degrees: Float): Bitmap? {
        if (bitmap == null) return null
        if (degrees == 0F) return bitmap
        val m = Matrix()
        m.setRotate(degrees, bitmap.width.toFloat() / 2, bitmap.height.toFloat() / 2)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }
}