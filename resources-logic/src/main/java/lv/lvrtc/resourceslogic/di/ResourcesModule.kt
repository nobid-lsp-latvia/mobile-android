// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.resourceslogic.di

import android.content.Context
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.resourceslogic.provider.ResourceProviderImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.resourceslogic")
class ResourcesModule

@Single
fun provideResourceProvider(context: Context): ResourceProvider {
    return ResourceProviderImpl(context)
}