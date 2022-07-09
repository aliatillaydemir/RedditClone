package com.ayd.redditclone

import android.Manifest
import android.annotation.TargetApi
import android.app.DownloadManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import java.io.File

class MainActivity : AppCompatActivity(),PhotoShare, PhotoDownload {

    private val viewModel: SplashViewModel by viewModels()
    private var photoArray = ArrayList<Photos>()

    private lateinit var pAdapter: Adapter
    var isLoad = false

    var message: String ? =""
    var lastMessage: String= ""

    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //installSplashScreen setContentView'dan önce gelmeli. Ana ekran gelmeden bu gelecek çünkü. Aksi halde hata veriyor.
        installSplashScreen().apply {
            setKeepOnScreenCondition{
                viewModel.isloading.value
            }
        }
        setContentView(R.layout.activity_main)
        //en son bağlama yapıldı.

        progressBar = findViewById(R.id.progressBar)

        val layoutManager = LinearLayoutManager(this)
        val photoAdp : RecyclerView = findViewById(R.id.recyclerView)
        photoAdp.layoutManager = layoutManager

        loadPhotos()

        pAdapter = Adapter(this,this)  //main constructorda kalıtım aldığımız interface için contexti belirtiyoruz(gönderiyoruz)
        photoAdp.adapter = pAdapter

        photoAdp.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val visibleItemcount : Int = layoutManager.childCount
                val pastVisibleItem : Int = layoutManager.findFirstCompletelyVisibleItemPosition()
                val totalItem = pAdapter.itemCount

                if(!isLoad){
                    if(visibleItemcount + pastVisibleItem >= totalItem){
                        loadPhotos()
                    }
                }

                super.onScrolled(recyclerView, dx, dy)
            }
        })

    }

    private fun loadPhotos() {

        isLoad = true

        progressBar.visibility = View.VISIBLE

        val url = "https://meme-api.herokuapp.com/gimme/30"
        //https://api.pexels.com/v1/search/?query=wallpaper
        //https://meme-api.herokuapp.com/gimme/30
        //https://gist.github.com/nielsutrecht/26102b7b624d81b12538

        val JsonObjectRequest = JsonObjectRequest(Request.Method.GET,url,null,{

        val photoJsonArray = it.getJSONArray("memes") //"memes"
            for (i in 0 until photoJsonArray.length()){
                val photoJsonObject = photoJsonArray.getJSONObject(i)
                val photos = Photos(photoJsonObject.getString("url"))
                photoArray.add(photos)
                Log.d(TAG,photoJsonArray.toString())
                pAdapter.updatePhotos(photoArray)
                isLoad = false
                progressBar.visibility = View.GONE
            }
        },{

        })
        SingletonObject.getInstance(this).addToRequestQueue(JsonObjectRequest)
    }

    override fun PhotoShareClick(photoUrl: String) {

        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT,"Reddit post:...\n $photoUrl")
        intent.type = "text/plain"
        startActivity(intent)
    }

    override fun PhotoDownloadClick(photoUrl: String) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            askPermission(photoUrl)
        }else{
            downloadImage(photoUrl)
        }


    }

    private fun downloadImage(photoUrl: String) {

        val directory = File(Environment.DIRECTORY_PICTURES)

        if(!directory.exists()){
            directory.mkdirs()
        }

        val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(photoUrl)

        val request = DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(photoUrl.substring(photoUrl.lastIndexOf("/")+1))
                .setDescription("")
                .setDestinationInExternalPublicDir(
                    directory.toString(),
                    photoUrl.substring(photoUrl.lastIndexOf("/")+1)
                )
        }

        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)

        Thread(Runnable {
            var downloading = true
            while(downloading){
                val cursor: Cursor = downloadManager.query(query)
                cursor.moveToFirst()
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS).toInt())==DownloadManager.STATUS_SUCCESSFUL){
                    downloading = false
                }

                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS).toInt())
                message = statMessage(photoUrl,directory,status)

                if(message != lastMessage){
                    this.runOnUiThread{
                        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
                    }
                    lastMessage = message ?: ""
                }
                cursor.close()
            }
        }).start()

    }

    private fun statMessage(photoUrl: String, directory: File, status: Int): String? {

        var message = ""
        message = when(status){
            DownloadManager.STATUS_FAILED -> "Download failed. try again..."
            DownloadManager.STATUS_PENDING -> "Pending"
            DownloadManager.STATUS_RUNNING -> "Downloading..."
            DownloadManager.STATUS_PAUSED -> "Paused"
            DownloadManager.STATUS_SUCCESSFUL ->"Image download successfully in $directory" + File.separator + photoUrl.substring(
                photoUrl.lastIndexOf("/") + 1
            )
            else -> "There is nothing download..."
        }

        return message
    }



    @TargetApi(Build.VERSION_CODES.M) //M -> marshmallow
    private fun askPermission(photoUrl: String) {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("Give the permission")
                    .setPositiveButton("Accept"){
                        dialog, id -> ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_CODE)

                        finish()
                        downloadImage(photoUrl)
                    }
                    .setNegativeButton("Deny"){
                        dialog, id ->dialog.cancel()
                    }.show()
            }else{
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_CODE)
            }
        }else{
            downloadImage(photoUrl)
        }

    }

    companion object {
        private const val PERMISSION_CODE = 1
    }



}