package com.example.gamemaster
import android.os.Parcel
import android.os.Parcelable

data class MatchModel(
    var matchId: String,
    var matchTime: String,
    var referee: String,
    var teamA: String,
    var teamB: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(matchTime)
        parcel.writeString(referee)
        parcel.writeString(teamA)
        parcel.writeString(teamB)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MatchModel> {
        override fun createFromParcel(parcel: Parcel): MatchModel {
            return MatchModel(parcel)
        }

        override fun newArray(size: Int): Array<MatchModel?> {
            return arrayOfNulls(size)
        }
    }
}
