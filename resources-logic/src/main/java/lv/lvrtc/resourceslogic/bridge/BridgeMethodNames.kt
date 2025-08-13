// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.resourceslogic.bridge


object LX {
    const val LX_EMBED_RESPONSE = "lx-embed-response"
}

object SETTINGS {
    const val BRIDGE_NAME = "settings"

    const val ENABLE_BIOMETRICS = "enableBiometrics"
    const val GET_BIOMETRIC_AVAILABILITY = "getBiometricAvailability"
    const val SET_THEME = "setTheme"
    const val SET_LANGUAGE = "setLanguage"
    const val DELETE_WALLET = "deleteWallet"
}

object ISSUANCE {
    const val BRIDGE_NAME = "issuance"

    const val GET_SAMPLE_DOCUMENTS = "getSampleDocuments"
    const val DELETE_SAMPLE_DOCUMENTS = "deleteSampleDocuments"
    const val GET_DOCUMENT_OPTIONS = "getDocumentOptions"
    const val GET_PID_OPTIONS = "getPidOptions"
    const val ISSUE_DOCUMENT = "issueDocument"
    const val RESUME_ISSUANCE = "resumeIssuance"
    const val GET_PID_DETAILS = "getPidDetails"
    const val SCAN_QR_CODE = "scanQrCode"
    const val ISSUE_DOCUMENT_OFFER = "issueDocumentOffer"
    const val RESOLVE_DOCUMENT_OFFER = "resolveDocumentOffer"
    const val GET_OFFER_CODE_DATA = "getOfferCodeData"
    const val SELECT_USER_SIGNATURES = "selectUserSignatures"
    const val GET_USER_SIGNATURE_OPTIONS = "getUserSignatureOptions"
    const val LAUNCH_SEB_ACTIVITY = "launchSEB"

    object SCREENS {
        const val PID_SUCCESS = "pid-success"
        const val DOCUMENT_SUCCESS = "document-success"
        const val DOCUMENT_DEFERRED_SUCCESS = "document-deferred-success"
    }
}

object SIGN {
    const val BRIDGE_NAME = "sign"

    const val PICK_FILES = "pickFiles"
    const val GET_SIGNING_METHODS = "getSigningMethods"
    const val SIGN_DOCUMENT = "signDocument"
    const val ESEAL_DOCUMENT = "eSealDocument"
    const val DOWNLOAD_DOCUMENT = "downloadSignedDocument"
    const val SHARE_DOCUMENT = "shareSignedDocument"
    const val CLOSE_SESSION = "closeSession"
    const val GET_SHARED_FILE = "getSharedFile"
    const val OPEN_FILE = "openFile"
}

object ONBOARDING {
    const val BRIDGE_NAME = "onboarding"

    const val INITIATE_EPARAKSTS = "initiateEParaksts"
    const val SUBMIT_EMAIL = "submitEmail"
    const val SUBMIT_SMS = "submitSms"
    const val SUBMIT_EMAIL_OTP = "verifyEmailOTP"
    const val SUBMIT_SMS_OTP = "verifySmsOTP"
    const val INITIALISE_WALLET = "initialiseWallet"

    object SCREENS {
        const val ACTIVATE = "activation"
        const val ACTIVATE_SUCCESS = "activation/2"
        const val LOADING = "loading"
        const val EMAIL = "onboarding/3"
        const val SMS = "onboarding/1"
        const val AddPid = "dashboard" // TODO:
    }
}

object DASHBOARD {
    const val BRIDGE_NAME = "dashboard"

    const val GET_DOCUMENTS = "getDocuments"
    const val GET_DOCUMENT_DETAILS = "getDocumentDetails"
    const val DELETE_DOCUMENT = "deleteDocument"
    const val SET_DOCUMENT_FAVORITE = "setDocumentFavorite"

    object SCREENS {
        const val MAIN = "dashboard"
    }
}

object PRESENTATION {
    const val BRIDGE_NAME = "presentation"

    const val GET_REQUEST_DOCUMENTS = "getRequestDocuments"
    const val UPDATE_FIELD = "updateField"
    const val CONFIRM_REQUEST = "confirmRequest"
    const val START_PRESENTATION = "scanQrCode"
    const val CANCEL_REQUEST = "presentationCanceled"

    object SCREENS {
        const val PRESENTATION_LOADING = "presentation-loading"
        const val PRESENTATION_SUCCESS = "presentation-success"
    }
}

object TRANSACTIONS {
    const val BRIDGE_NAME = "transactions"

    const val GET_TRANSACTIONS = "getTransactions"

    object SCREENS {
        const val MAIN = "transactionsHistory"
    }
}