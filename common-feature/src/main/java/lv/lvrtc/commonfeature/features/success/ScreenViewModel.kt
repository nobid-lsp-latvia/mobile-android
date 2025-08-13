// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.success

import android.net.Uri
import androidx.core.net.toUri
import lv.lvrtc.uilogic.config.ConfigNavigation
import lv.lvrtc.uilogic.config.NavigationType
import lv.lvrtc.uilogic.mvi.MviViewModel
import lv.lvrtc.uilogic.mvi.ViewEvent
import lv.lvrtc.uilogic.mvi.ViewSideEffect
import lv.lvrtc.uilogic.mvi.ViewState
import lv.lvrtc.uilogic.navigation.generateComposableArguments
import lv.lvrtc.uilogic.navigation.generateComposableNavigationLink
import lv.lvrtc.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class State(
    val successConfig: SuccessUIConfig
) : ViewState

sealed class Event : ViewEvent {
    data class ButtonClicked(val config: SuccessUIConfig.ButtonConfig) : Event()
    data object BackPressed : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String
        ) : Navigation()

        data class PopBackStackUpTo(
            val screenRoute: String,
            val inclusive: Boolean
        ) : Navigation()

        data object Pop : Navigation()

        data class DeepLink(
            val link: Uri,
            val routeToPop: String?
        ) : Navigation()
    }
}

@KoinViewModel
class SuccessViewModel(
    private val uiSerializer: UiSerializer,
    @InjectedParam private val successConfig: String
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State =
        State(
            successConfig = uiSerializer.fromBase64(
                successConfig,
                SuccessUIConfig::class.java,
                SuccessUIConfig.Parser
            ) ?: throw RuntimeException("SuccessUIConfig:: is Missing or invalid")
        )

    override fun handleEvents(event: Event) {
        when (event) {

            is Event.ButtonClicked -> {
                doNavigation(event.config.navigation)
            }

            is Event.BackPressed -> {
                doNavigation(
                    viewState.value.successConfig.onBackScreenToNavigate
                )
            }
        }
    }

    private fun doNavigation(navigation: ConfigNavigation) {

        val navigationEffect: Effect.Navigation = when (val nav = navigation.navigationType) {
            is NavigationType.PopTo -> {
                Effect.Navigation.PopBackStackUpTo(
                    screenRoute = nav.screen.screenRoute,
                    inclusive = false
                )
            }

            is NavigationType.PushScreen -> {
                Effect.Navigation.SwitchScreen(
                    generateComposableNavigationLink(
                        screen = nav.screen,
                        arguments = generateComposableArguments(nav.arguments),
                    )
                )
            }

            is NavigationType.Deeplink -> Effect.Navigation.DeepLink(
                nav.link.toUri(),
                nav.routeToPop
            )

            is NavigationType.Pop, NavigationType.Finish -> Effect.Navigation.Pop

            is NavigationType.PushRoute -> Effect.Navigation.SwitchScreen(nav.route)
        }

        setEffect {
            navigationEffect
        }
    }
}