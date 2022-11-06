package com.arcore.AI_ResourceControl

import android.app.Activity
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.tensorflow.lite.examples.imagesegmentation.ImageSegmentationHelper

import java.io.File


/**
 * Collects Bitmaps from a coroutine source (BitmapSource for static JPG or DynamicBitmapSource for
 * a stream of changing files)
 */
class BitmapCollector(
    /**
     * If using static jpg, comment out DynamicBitmapSource, uncomment BitmapSource
     */

    private val bitmapSource: DynamicBitmapSource?,
    val classifier: ImageClassifier?,
    var index: Int, // to ensure unique filename.
    private val activity: Activity,
    var mInstance: MainActivity,
    var objectDetector: ObjectDetectorHelper?,
    var newClassifier: ImageClassifierHelper?,
    var imgSegmentation: ImageSegmentationHelper?


): ViewModel() {

    var start: Long = 0
    var end: Long = 0
    var overhead: Long = 0
    var InferenceTime: Long = 0
    var responseTime: Long = 0
    var totalResponseTime: Long = 0
    var numOfTimesExecuted = 0
  // first worked: var  objectDetectorHelper = ObjectDetectorHelper( context = mInstance, fileseries = mInstance.fileseries)


    //
    private val outputPath = activity.getExternalFilesDir(null)
    private val childDirectory = File(outputPath, "data")
    var run = false
    private var job : Job? = null
    var outputText = SpannableStringBuilder("null")

    /**
     * Resets response time collection data so changing model does not give erroneous first result
     */
    fun resetRtData() {
        totalResponseTime = 0
        numOfTimesExecuted = 0
        end = System.nanoTime()/1000000

    }

    /**
     * Stops running collector
     */
    fun pauseCollect() {
        run = false
        job?.cancel()
        Log.d("CANCEL", "Classifier $index cancelled")
    }

    /**
     * Starts collection
     * Precondition: bitmapSource must be emitting a stream to collect
     */
    fun startCollect() = runBlocking <Unit>{
        run = true
        resetRtData()
        Log.d("CANCEL", "Starting classifier $index")
        launch {
            collectStream()
        }
    }

    /**
     * launches coroutine to collect bitmap from bitmapSource, scales bitmap to
     * ImageClassifier requirements. Writes output to file.
     */
    private suspend fun collectStream() {
//        childDirectory.mkdirs()
//        val file = File(childDirectory,
//            index.toString() + '_' +
//                    classifier?.modelName + '_' +
//                    classifier?.device + '_'+
//                    classifier?.numThreads + "T_"+
//                    classifier?.time +
//                    ".csv")


/****** the problem was with the dispatcher--- it should be set to Default NOT IO */////////
        job = viewModelScope.launch(Dispatchers.Default) {
            bitmapSource?.bitmapStream?.collect {
                Log.d("CANCEL", "$index collected $it")

              //  nill added to get the latest bitmap for inference -> nut no needed now since we get
              //  bitmap=  bitmapSource?.bitmapUpdaterApi?.latestBitmap

                if( run) { // resize it for image classification


                    var  bitmap= it
                    if(classifier!= null )
                    {
                        val bitmap2 = Bitmap.createScaledBitmap(
                            // it,
                            bitmap!!,
                            classifier!!.imageSizeX,
                            classifier.imageSizeY,
                            true
                        )
                        bitmap=bitmap2// resized value for classification
                    }

                    if (newClassifier?.modelUsed()  =="inception_v1_224_quant.tflite" )
                    {
                        bitmap=Bitmap.createScaledBitmap(
                            // it,
                            bitmap!!,
                           229,
                           229,
                            true
                        ) //229
                    }



                    start = System.nanoTime()/1000000
                    if(end!=0L) {
                        overhead = start-end
                    }

//object detection version complex: this is the main
                    if(objectDetector!= null)
                        objectDetector!!.detect(bitmap!!,0)

                   else if(classifier!= null)
                        classifier.classifyFrame(bitmap)


                    else if (newClassifier!=null)
                         newClassifier?.classify(bitmap!!,0)


                    else if (imgSegmentation!=null)
                        imgSegmentation?.segment(bitmap!!,0)




                    end = System.nanoTime()/1000000
                    InferenceTime = end-start
                    responseTime=overhead+InferenceTime
                    numOfTimesExecuted++
                    totalResponseTime+=responseTime
                    Log.d("times", "${overhead},${InferenceTime},${responseTime}")
                    outputText.append("${overhead},${InferenceTime},${responseTime}\n")
//                    file.appendText(outputText.toString())
                }
            }
        }
    }


//    fun getThroughput(): Long {
//        return if(numOfTimesExecuted>0)  {
//            totalResponseTime/numOfTimesExecuted
//        } else {
//            0
//        }
//    }
}


