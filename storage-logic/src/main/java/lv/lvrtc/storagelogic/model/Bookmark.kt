// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.storagelogic.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import lv.lvrtc.storagelogic.model.type.StoredObject

internal class RealmBookmark : RealmObject {
    @PrimaryKey
    var identifier: String = ""
}

data class Bookmark(
    val identifier: String
) : StoredObject

internal fun Bookmark.toRealm() = RealmBookmark().apply {
    identifier = this@toRealm.identifier
}

internal fun RealmBookmark?.toBookmark() = this?.let {
    Bookmark(
        it.identifier
    )
}

internal fun List<RealmBookmark>.toBookmarks() = this.map {
    Bookmark(
        it.identifier
    )
}