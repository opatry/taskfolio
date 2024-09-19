/*
 * Copyright (c) 2024 Olivier Patry
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.opatry.google.people.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Person(
    @SerialName("resourceName")
    val resourceName: String,
    @SerialName("etag")
    val etag: String,
    @SerialName("metadata")
    val metadata: FieldMetadata? = null,
    @SerialName("addresses")
    val addresses: List<Address> = emptyList(),
    @SerialName("ageRange")
    val ageRange: AgeRange = AgeRange.Unspecified,
    @SerialName("ageRanges")
    val ageRanges: List<AgeRange> = emptyList(),
    @SerialName("biographies")
    val biographies: List<Biography> = emptyList(),
    @SerialName("birthdays")
    val birthdays: List<Birthday> = emptyList(),
    @SerialName("braggingRights")
    val braggingRights: List<BraggingRight> = emptyList(),
    @SerialName("calendarUrls")
    val calendarUrls: List<CalendarUrl> = emptyList(),
    @SerialName("clientData")
    val clientData: List<ClientData> = emptyList(),
    @SerialName("coverPhotos")
    val coverPhotos: List<CoverPhoto> = emptyList(),
    @SerialName("emailAddresses")
    val emailAddresses: List<EmailAddress> = emptyList(),
    @SerialName("events")
    val events: List<Event> = emptyList(),
    @SerialName("externalIds")
    val externalIds: List<ExternalId> = emptyList(),
    @SerialName("fileAses")
    val fileAses: List<FileAs> = emptyList(),
    @SerialName("genders")
    val genders: List<Gender> = emptyList(),
    @SerialName("imClients")
    val imClients: List<IMClient> = emptyList(),
    @SerialName("interests")
    val interests: List<Interest> = emptyList(),
    @SerialName("locales")
    val locales: List<Locale> = emptyList(),
    @SerialName("locations")
    val locations: List<Location> = emptyList(),
    @SerialName("memberships")
    val memberships: List<Membership> = emptyList(),
    @SerialName("miscKeywords")
    val miscKeywords: List<MiscKeyword> = emptyList(),
    @SerialName("names")
    val names: List<Name> = emptyList(),
    @SerialName("nicknames")
    val nicknames: List<Nickname> = emptyList(),
    @SerialName("occupations")
    val occupations: List<Occupation> = emptyList(),
    @SerialName("organizations")
    val organizations: List<Organization> = emptyList(),
    @SerialName("phoneNumbers")
    val phoneNumbers: List<PhoneNumber> = emptyList(),
    @SerialName("photos")
    val photos: List<Photo> = emptyList(),
    @SerialName("relations")
    val relations: List<Relation> = emptyList(),
    @SerialName("relationshipInterests")
    val relationshipInterests: List<RelationshipInterest> = emptyList(),
    @SerialName("relationshipStatuses")
    val relationshipStatuses: List<RelationshipStatus> = emptyList(),
    @SerialName("residences")
    val residences: List<Residence> = emptyList(),
    @SerialName("sipAddresses")
    val sipAddresses: List<SIPAddress> = emptyList(),
    @SerialName("skills")
    val skills: List<Skill> = emptyList(),
    @SerialName("taglines")
    val taglines: List<Tagline> = emptyList(),
    @SerialName("urls")
    val urls: List<Url> = emptyList(),
    @SerialName("userDefined")
    val userDefined: List<UserDefined> = emptyList()
) {
    @Serializable
    class Address

    @Serializable
    enum class AgeRange {
        @SerialName("AGE_RANGE_UNSPECIFIED")
        Unspecified,
        @SerialName("LESS_THAN_EIGHTEEN")
        LessThanEighteen,
        @SerialName("EIGHTEEN_TO_TWENTY")
        EighteenToTwenty,
        @SerialName("TWENTY_ONE_OR_OLDER")
        TwentyOneOrOlder,
    }

    @Serializable
    class Biography

    @Serializable
    class Birthday

    @Serializable
    class BraggingRight

    @Serializable
    class CalendarUrl

    @Serializable
    class ClientData

    @Serializable
    class CoverPhoto

    @Serializable
    class EmailAddress

    @Serializable
    class Event

    @Serializable
    class ExternalId

    @Serializable
    class FileAs

    @Serializable
    class Gender

    @Serializable
    class IMClient

    @Serializable
    class Interest

    @Serializable
    class Locale

    @Serializable
    class Location

    @Serializable
    class Membership

    @Serializable
    class MiscKeyword

    @Serializable
    class Name

    @Serializable
    class Nickname

    @Serializable
    class Occupation

    @Serializable
    class Organization

    @Serializable
    class PhoneNumber

    @Serializable
    class Relation

    @Serializable
    class RelationshipInterest

    @Serializable
    class RelationshipStatus

    @Serializable
    class Residence

    @Serializable
    class SIPAddress

    @Serializable
    class Skill

    @Serializable
    class Tagline

    @Serializable
    class Url

    @Serializable
    class UserDefined
}