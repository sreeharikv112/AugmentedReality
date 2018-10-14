package com.augreality

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.ar.sceneform.ux.ArFragment

import kotlinx.android.synthetic.main.activity_main.*
import android.R.attr.fragment
import android.graphics.Point
import android.view.View
import android.R.attr.y
import android.R.attr.x
import android.net.Uri
import com.google.ar.core.*
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.widget.ImageView
import android.widget.LinearLayout
import android.support.v4.view.MenuItemCompat.setContentDescription
import android.support.v4.view.MenuItemCompat.setContentDescription
import android.support.v4.view.MenuItemCompat.setContentDescription
import android.support.v4.view.MenuItemCompat.setContentDescription
import android.R.attr.fragment
import com.google.ar.core.Trackable
import com.google.ar.core.HitResult
import android.R.attr.y
import android.R.attr.x

import android.R.attr.fragment
import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import android.support.v4.content.FileProvider
import com.google.ar.sceneform.rendering.ModelRenderable
import android.support.v4.view.accessibility.AccessibilityRecordCompat.setSource
import android.support.v7.app.AlertDialog
import android.view.PixelCopy
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var fragment: ArFragment
    private val MIN_OPENGL_VERSION: Double = 3.0
    private val TAG : String = MainActivity::class.java.canonicalName
    private var pointer: PointerDrawable = PointerDrawable()
    private var isTracking : Boolean =false
    private var isHitting : Boolean =false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        title=getString(R.string.aug_reality)
        fragment= supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        fragment.arSceneView.scene.addOnUpdateListener { frameTime ->
            fragment.onUpdate(frameTime)
            onUpdate()
        }
        fab.setOnClickListener { view ->
            takePhoto()
        }
        initializeGallery();
    }

    private fun onUpdate() {
        var trackingChanged: Boolean = updateTracking()
        var contentView : View = findViewById(android.R.id.content)
        if(trackingChanged){
            contentView.overlay.add(pointer)
        }else{
            contentView.overlay.remove(pointer)
        }
        contentView.invalidate()
        if(isTracking){
            var hitTestChanged: Boolean = updateHitTest()
            if(hitTestChanged){
                pointer.enabled = isHitting
                contentView.invalidate()
            }
        }
    }

    private fun updateTracking(): Boolean {
        var frame : Frame = fragment.arSceneView.arFrame
        var wasTracking : Boolean = isTracking
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    private fun updateHitTest(): Boolean {
        var frame : Frame = fragment.arSceneView.arFrame
        var pt : Point = getScreenCenter()
        var hits : List<HitResult>
        var wasHitting : Boolean = isHitting
        isHitting = false;
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && (trackable as Plane).isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting !== isHitting
    }

    private fun getScreenCenter(): Point {
        var vw: View = findViewById(android.R.id.content)
        return android.graphics.Point(vw.width / 2, vw.height / 2)
    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }*/

    private fun initializeGallery(){
        var gallery: LinearLayout = findViewById(R.id.gallery_layout)
        var house = ImageView(this)
        var igloo = ImageView(this)
        var earth = ImageView(this)
        var dino = ImageView(this)
        var lighthouse = ImageView(this)

        earth.setImageResource(R.drawable.earth_thumb)
        earth.contentDescription = "earth"
        earth.setOnClickListener { view -> addObject(Uri.parse("earth_ball.sfb")) }
        gallery.addView(earth)

        dino.setImageResource(R.drawable.dino_orange)
        dino.contentDescription = "dino"
        dino.setOnClickListener { view -> addObject(Uri.parse("mokele.sfb")) }
        gallery.addView(dino)

        lighthouse.setImageResource(R.drawable.lighthouse)
        lighthouse.contentDescription = "lighthouse"
        lighthouse.setOnClickListener { view -> addObject(Uri.parse("Lighthouse_Surrounds.sfb")) }
        gallery.addView(lighthouse)

        house.setImageResource(R.drawable.house_thumb)
        house.contentDescription = "house"
        house.setOnClickListener { view -> addObject(Uri.parse("House.sfb")) }
        gallery.addView(house)

        igloo.setImageResource(R.drawable.igloo_thumb)
        igloo.contentDescription = "igloo"
        igloo.setOnClickListener { view -> addObject(Uri.parse("igloo.sfb")) }
        gallery.addView(igloo)

    }

    private fun addObject(model:Uri){
        var frame : Frame = fragment.arSceneView.arFrame
        var pt : Point = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    placeObject(fragment, hit.createAnchor(), model)
                    break

                }
            }
        }
    }

    private fun placeObject(fragment: ArFragment , anchor :Anchor, model: Uri){
        val renderableFuture = ModelRenderable.builder()
                .setSource(fragment.context, model)
                .build()
                .thenAccept { renderable -> addNodeToScene(fragment, anchor, renderable) }
                .exceptionally { throwable ->
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(throwable.message)
                            .setTitle("Codelab error!")
                    val dialog = builder.create()
                    dialog.show()
                    null
                }
    }

    private fun addNodeToScene(fragment: ArFragment , anchor: Anchor , renderable: Renderable){
        var anchorNode = AnchorNode(anchor)
        var nodeObj = TransformableNode(fragment.getTransformationSystem())
        nodeObj.renderable = renderable
        nodeObj.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(anchorNode)
        nodeObj.select()
    }

    private fun takePhoto(){
        var filename =  generateFilename()
        var view : ArSceneView =  fragment.arSceneView
        // Create a bitmap the size of the scene view.
        var bitmap= Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        var handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (e: Exception) {
                    val toast = Toast.makeText(this@MainActivity, e.toString(),
                            Toast.LENGTH_LONG)
                    toast.show()
                    //return@PixelCopy.request
                    return@request
                }
                val snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Photo saved", Snackbar.LENGTH_LONG)
                snackbar.setAction("Open in Photos") { v ->
                    val photoFile = File(filename)
                    val photoURI = FileProvider.getUriForFile(this@MainActivity,
                            this@MainActivity.getPackageName() + ".ar.codelab.name.provider",
                            photoFile)
                    val intent = Intent(Intent.ACTION_VIEW, photoURI)
                    intent.setDataAndType(photoURI, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                snackbar.show()
            } else {
                val toast = Toast.makeText(this@MainActivity,
                        "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    private fun generateFilename():String{
        var date =SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date());
        var path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}" +
                File.separator + "Sceneform/" + date + "_screenshot.jpg";
        return path
    }

    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String ){
        var out = File(filename)
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try{
            var outputStream = FileOutputStream(filename)
            var outputData = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();

        }catch (ex: IOException){
            throw IOException("Failed to save bitmap to disk", ex)
        }
    }

    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later")
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show()
            activity.finish()
            return false
        }
        return true
    }
}
