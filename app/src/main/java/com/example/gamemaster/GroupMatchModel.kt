package com.example.gamemaster
import android.os.Parcel
import android.os.Parcelable

data class GroupMatchModel(
    var group: String,
    var matchTime: String,
    var playingField: String,
    var referee: String,
    var teamA: String,
    var teamB: String,
    var matchId : String,
    var scoreA: Int? = null,
    var scoreB: Int? = null,
    var isFinished: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(matchTime)
        parcel.writeString(playingField)
        parcel.writeString(referee)
        parcel.writeString(teamA)
        parcel.writeString(teamB)
        parcel.writeString(matchId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<KnockoutMatchModel> {
        override fun createFromParcel(parcel: Parcel): KnockoutMatchModel {
            return KnockoutMatchModel(parcel)
        }

        override fun newArray(size: Int): Array<KnockoutMatchModel?> {
            return arrayOfNulls(size)
        }
    }
}
