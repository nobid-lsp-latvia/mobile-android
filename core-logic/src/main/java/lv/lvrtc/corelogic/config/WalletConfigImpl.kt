// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.config

import android.content.Context
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import lv.lvrtc.businesslogic.config.EnvironmentConfig
import lv.lvrtc.corelogic.BuildConfig
import lv.lvrtc.resourceslogic.R

internal class WalletCoreConfigImpl(
    private val context: Context,
    private val environmentConfig: EnvironmentConfig
) : WalletConfig {

    private companion object {
        const val VCI_CLIENT_ID = "edim-local"
    }

    private var _config: EudiWalletConfig? = null

    override val config: EudiWalletConfig
        get() {
            if (_config == null) {
                _config = EudiWalletConfig {
                    configureOpenId4Vci {
                        withIssuerUrl(environmentConfig.getVciIssuerUrl())
                        withClientId(VCI_CLIENT_ID)
                        withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                        withUseDPoPIfSupported(true)
                        withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                    }
                    .configureLogging(
                        level = Logger.LEVEL_DEBUG
                    )
                    .configureOpenId4Vp {
                        withEncryptionAlgorithms(
                            EncryptionAlgorithm.ECDH_ES
                        )
                        withEncryptionMethods(
                            EncryptionMethod.A128CBC_HS256,
                            EncryptionMethod.A256GCM
                        )
                        withClientIdSchemes(
                            ClientIdScheme.X509SanDns,
                        )
                        withSchemes(
                            listOf(
                                BuildConfig.OPENID4VP_SCHEME,
                                BuildConfig.EUDI_OPENID4VP_SCHEME,
                                BuildConfig.MDOC_OPENID4VP_SCHEME
                            )
                        )
                        withFormats(
                            Format.MsoMdoc, Format.SdJwtVc.ES256
                        )
                    }
                    .configureDocumentKeyCreation(
                        userAuthenticationRequired = true,
                        useStrongBoxForKeys = true,
                        userAuthenticationTimeout = 30_000L
                    )
                    .configureReaderTrustStore(
                        context,
                        R.raw.eudi,
                        R.raw.cert,
                        R.raw.pidissuerca02_eu,
                        R.raw.pidissuerca02_ut,
                        R.raw.verifier
                    )
                }
            }
            return _config!!
        }
}