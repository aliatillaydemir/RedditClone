package com.ayd.redditclone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class Adapter(private val listener : PhotoShare, private val downloadListener : PhotoDownload): RecyclerView.Adapter<Adapter.AdapterViewHolder>() {

    private val items: ArrayList<Photos> = ArrayList()

    companion object {
        private var likeState: Boolean = false
        private var dislikeState: Boolean = false
    }

    class AdapterViewHolder(item: View): RecyclerView.ViewHolder(item) {

        val photo : ImageView = item.findViewById(R.id.imageMain)
        val like : ImageView = item.findViewById(R.id.like)
        val dislike : ImageView = item.findViewById(R.id.dislike)
        val share : ImageView = item.findViewById(R.id.share)
        val download : ImageView = item.findViewById(R.id.download)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterViewHolder {
        return AdapterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list,parent,false))
    }

    override fun onBindViewHolder(holder: AdapterViewHolder, position: Int) {

        val currentItems = items[position]
        Glide.with(holder.itemView.context).load(currentItems.url).into(holder.photo)

        fun likeFun(){

            if(!likeState){
                holder.like.setImageResource(R.drawable.ilike)
                likeState = true

                holder.dislike.setImageResource(R.drawable.dislike)
                dislikeState = false

            }else if(likeState){
                holder.like.setImageResource(R.drawable.like)
                likeState = false

            }

        }

        fun dislikeFun(){

            if(!dislikeState){
                holder.dislike.setImageResource(R.drawable.idislike)
                dislikeState = true

                holder.like.setImageResource(R.drawable.like)
                likeState = false

            }else{
                holder.dislike.setImageResource(R.drawable.dislike)
                dislikeState = false
            }

        }

        //like and dislike tıklanmadan önce normal olursa altlarda da değişiklliklerin uygulanması önlenmiş olur.
        holder.like.setImageResource(R.drawable.like)
        holder.dislike.setImageResource(R.drawable.dislike)

        holder.photo.setOnClickListener(object : DoubleClickListener(){
            override fun onDoubleClickListener(p0: View?) {
                likeFun()
            }
        })

        holder.like.setOnClickListener{
            likeFun()
        }

        holder.dislike.setOnClickListener {
        dislikeFun()
        }

        holder.share.setOnClickListener {
            listener.PhotoShareClick(currentItems.url)
        }

        holder.download.setOnClickListener {
            downloadListener.PhotoDownloadClick(currentItems.url)
        }


    }


    override fun getItemCount(): Int {
        return items.size
    }

    fun updatePhotos(updatePhotos: ArrayList<Photos>){
        items.clear()
        items.addAll(updatePhotos)
        notifyDataSetChanged()

    }

}

interface PhotoShare {

    fun PhotoShareClick(photoUrl : String)

}

interface PhotoDownload{

     fun PhotoDownloadClick(photoUrl: String)

}


abstract class DoubleClickListener : View.OnClickListener {

    private var click: Long = 0

    override fun onClick(p0: View?) {
        val clicktime = System.currentTimeMillis()
        if(clicktime-click<DOUBLE_CLICK){

            onDoubleClickListener(p0)
        }
        click = clicktime


    }

    abstract fun onDoubleClickListener(p0: View?)

    companion object {
        private const val DOUBLE_CLICK: Long = 250
    }

}
