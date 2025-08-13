// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.model.user

import com.google.gson.annotations.SerializedName

data class EmailRequest(
    val email: String
)

data class VerifyEmailRequest(
    val verificationCode: String
)

data class SmsRequest(
    val phoneNumber: String
)

data class VerifySmsRequest(
    val verificationCode: String
)

data class DocumentOfferResponse(
    @SerializedName("urlData") val offerUrl: String
)

data class PersonResponse(
    val id: String,
    val code: String,
    val givenName: String,
    val familyName: String,
    val contacts: List<Contact>
)

data class Contact(
    val type: String,
    val value: String
)