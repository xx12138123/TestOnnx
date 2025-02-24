package com.test.test_onnx

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.test.test_onnx.databinding.ActivityCaptureBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CaptureActivity : ImageAnalysis.Analyzer, AppCompatActivity() {
    private val REQUEST_CAMERA = 100
    private val selector = ResolutionSelector.Builder()
        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
        .build()
    private val imageCapture = ImageCapture.Builder()
        .setResolutionSelector(selector)
        .setFlashMode(ImageCapture.FLASH_MODE_OFF)
        .build()
    private val preview = Preview.Builder()
        .setTargetRotation(Surface.ROTATION_270)
        .build()

    private val analysisExec: Executor = Executors.newSingleThreadExecutor()
    private val analysis = ImageAnalysis.Builder()
        .setResolutionSelector(selector)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build().apply {
            setAnalyzer(analysisExec, this@CaptureActivity)
        }

    private lateinit var onnxRunner: OnnxRunner

    //private val captureExec: Executor = Executors.newSingleThreadExecutor()

    private lateinit var binding: ActivityCaptureBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preview.surfaceProvider = binding.cameraPreview.surfaceProvider
        onnxRunner = OnnxRunner(this)
    }


    private fun checkPermission():Boolean{
        return (ContextCompat.checkSelfPermission(applicationContext,
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA
        )
    }

    override fun onResume() {
        super.onResume()
        if(checkPermission()){
            startCamera()
        }else{
            requestPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            if (checkPermission()) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    @SuppressLint("RepeatOnLifecycleWrongUsage", "RestrictedApi")
    private fun startCamera(){
        // 预览裁剪
        //preview.setViewPortCropRect(Rect(0, -80, 640, 720))

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val processCameraProvider = cameraProviderFuture.get()
                processCameraProvider.unbindAll()


                val camera = processCameraProvider.bindToLifecycle(
                    this@CaptureActivity,
                    //CameraSelector.DEFAULT_FRONT_CAMERA,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    analysis,
                )
                //camera.cameraControl.setZoomRatio(3.5f)

                camera.cameraControl.setExposureCompensationIndex(0)

                //每秒对焦一次   现在摄像头是定焦的
                lifecycleScope.launch{
                    repeatOnLifecycle(Lifecycle.State.RESUMED){
                        camera.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(
                                binding.cameraPreview.meteringPointFactory.createPoint(
                                    (binding.cameraPreview.width / 2).toFloat(),
                                    (binding.cameraPreview.height / 2).toFloat(),
                                )
                            ).build()
                        )
                        delay(2000)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private var drawResult = mutableListOf<OnnxResult>()
    override fun analyze(image: ImageProxy) {
        var bitmap = image.toBitmap()
        bitmap = rotateBitmap(bitmap, 90)
        val results = onnxRunner.predict(bitmap)

        val hScale = binding.cameraPreview.height / bitmap.height.toFloat()
        val wScale = binding.cameraPreview.width / bitmap.width.toFloat()
        drawResult.clear()
        drawResult.addAll(results.map { OnnxResult(RectF(it.rectf.left*wScale, it.rectf.top*hScale, it.rectf.right*wScale, it.rectf.bottom*hScale), it.score, it.type) })
        runOnUiThread {
            binding.previewPaperDraw.setResults(drawResult)
        }

        bitmap.recycle()
        image.close()
    }

    fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }
}