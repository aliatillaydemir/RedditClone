package com.ayd.redditclone

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest

class MainActivity : AppCompatActivity(),PhotoShare {

    private val viewModel: SplashViewModel by viewModels()
    private var photoArray = ArrayList<Photos>()

    private lateinit var pAdapter: Adapter
    var isLoad = false

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

        val layoutManager = LinearLayoutManager(this)
        val photoAdp : RecyclerView = findViewById(R.id.recyclerView)
        photoAdp.layoutManager = layoutManager

        loadPhotos()

        pAdapter = Adapter(this)
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


}