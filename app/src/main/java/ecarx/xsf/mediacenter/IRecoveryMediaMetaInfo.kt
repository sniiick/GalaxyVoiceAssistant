package ecarx.xsf.mediacenter

import android.os.Parcel
import android.os.Parcelable

class IRecoveryMediaMetaInfo : Parcelable {
    var mediaList: List<IMedia>? = null
    var mediaListId: String? = null
    var mediaListType: Int = 0
    var packageName: String? = null
    var sourceType: Int = 0
    var title: String? = null

    constructor(parcel: Parcel) {
        title = parcel.readString()
        packageName = parcel.readString()
        sourceType = parcel.readInt()
        mediaListType = parcel.readInt()
        mediaListId = parcel.readString()
        mediaList = parcel.readArrayList(IMedia::class.java.classLoader) as? List<IMedia>
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(packageName)
        parcel.writeInt(sourceType)
        parcel.writeInt(mediaListType)
        parcel.writeString(mediaListId)
        parcel.writeList(mediaList)
    }

    companion object CREATOR : Parcelable.Creator<IRecoveryMediaMetaInfo> {
        override fun createFromParcel(parcel: Parcel): IRecoveryMediaMetaInfo {
            return IRecoveryMediaMetaInfo(parcel)
        }

        override fun newArray(size: Int): Array<IRecoveryMediaMetaInfo?> {
            return arrayOfNulls(size)
        }
    }
}
