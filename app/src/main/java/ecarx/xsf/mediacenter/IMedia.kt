package ecarx.xsf.mediacenter

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

class IMedia : Parcelable {
    var album: String? = null
    var albumIndex: Int = 0
    var artWork: Uri? = null
    var artist: String? = null
    var author: String? = null
    var categoryStr: String? = null
    var collected: Int = 0
    var composer: String? = null
    var description: String? = null
    var duration: Long = 0
    var lyric: Uri? = null
    var lyricContent: String? = null
    var mediaCp: String? = null
    var mediaId: String? = null
    var mediaPath: Uri? = null
    var playingMediaListId: String? = null
    var playingMediaListType: Int = 0
    var positionInQueue: Int = 0
    var radioFrequency: String? = null
    var radioStationName: String? = null
    var rating: String? = null
    var sourceType: Int = 0
    var subCategoryStr: String? = null
    var subtitle: String? = null
    var supportCollect: Int = 0
    var targetType: String? = null
    var title: String? = null
    var uuid: String? = null
    var vip: Int = 0
    var year: String? = null

    constructor(parcel: Parcel) {
        uuid = parcel.readString()
        title = parcel.readString()
        artist = parcel.readString()
        album = parcel.readString()
        author = parcel.readString()
        composer = parcel.readString()
        description = parcel.readString()
        subtitle = parcel.readString()
        rating = parcel.readString()
        year = parcel.readString()
        duration = parcel.readLong()
        positionInQueue = parcel.readInt()
        albumIndex = parcel.readInt()
        categoryStr = parcel.readString()
        subCategoryStr = parcel.readString()
        mediaId = parcel.readString()
        mediaCp = parcel.readString()
        targetType = parcel.readString()
        sourceType = parcel.readInt()
        mediaPath = parcel.readParcelable(Uri::class.java.classLoader)
        lyricContent = parcel.readString()
        lyric = parcel.readParcelable(Uri::class.java.classLoader)
        artWork = parcel.readParcelable(Uri::class.java.classLoader)
        radioFrequency = parcel.readString()
        radioStationName = parcel.readString()
        vip = parcel.readInt()
        playingMediaListId = parcel.readString()
        playingMediaListType = parcel.readInt()
        supportCollect = parcel.readInt()
        collected = parcel.readInt()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uuid)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(album)
        parcel.writeString(author)
        parcel.writeString(composer)
        parcel.writeString(description)
        parcel.writeString(subtitle)
        parcel.writeString(rating)
        parcel.writeString(year)
        parcel.writeLong(duration)
        parcel.writeInt(positionInQueue)
        parcel.writeInt(albumIndex)
        parcel.writeString(categoryStr)
        parcel.writeString(subCategoryStr)
        parcel.writeString(mediaId)
        parcel.writeString(mediaCp)
        parcel.writeString(targetType)
        parcel.writeInt(sourceType)
        parcel.writeParcelable(mediaPath, flags)
        parcel.writeString(lyricContent)
        parcel.writeParcelable(lyric, flags)
        parcel.writeParcelable(artWork, flags)
        parcel.writeString(radioFrequency)
        parcel.writeString(radioStationName)
        parcel.writeInt(vip)
        parcel.writeString(playingMediaListId)
        parcel.writeInt(playingMediaListType)
        parcel.writeInt(supportCollect)
        parcel.writeInt(collected)
    }

    companion object CREATOR : Parcelable.Creator<IMedia> {
        override fun createFromParcel(parcel: Parcel): IMedia {
            return IMedia(parcel)
        }

        override fun newArray(size: Int): Array<IMedia?> {
            return arrayOfNulls(size)
        }
    }

    fun getArtwork(): Uri? = artWork
    fun getPlayingItemPositionInQueue(): Int = positionInQueue
}