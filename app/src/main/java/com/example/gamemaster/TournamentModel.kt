package com.example.gamemaster
import android.os.Parcel
import android.os.Parcelable

data class TournamentModel(
    var tournamentName: String,
    var matchType: String,
    var matchFormat: String,
    var teams: String,
    var referees: String,
    var matchTimes: String,
    var generatedMatches: MutableList<MatchModel>? = mutableListOf()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun equals(other: Any?): Boolean {
        return (other is TournamentModel) && (tournamentName == other.tournamentName)
    }

    override fun hashCode(): Int {
        return tournamentName.hashCode()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(tournamentName)
        parcel.writeString(matchType)
        parcel.writeString(matchFormat)
        parcel.writeString(teams)
        parcel.writeString(referees)
        parcel.writeString(matchTimes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TournamentModel> {
        override fun createFromParcel(parcel: Parcel): TournamentModel {
            return TournamentModel(parcel)
        }

        override fun newArray(size: Int): Array<TournamentModel?> {
            return arrayOfNulls(size)
        }
    }
}
