// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.uilogic.components

class InfoTextWithNameAndValueData private constructor(
    val identifier: String,
    val title: String,
    val infoValues: List<String>?,
) {
    override fun equals(other: Any?): Boolean {
        return if (other !is InfoTextWithNameAndValueData) {
            false
        } else {
            (this.title == other.title) &&
                    (this.infoValues?.equals(other.infoValues) == true)
        }
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (infoValues?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return buildString {
            append("\nTitle = $title,\n")
            append("InfoValues = $infoValues")
        }
    }

    companion object {
        fun create(
            identifier: String,
            title: String,
            vararg infoValues: String
        ): InfoTextWithNameAndValueData {
            return InfoTextWithNameAndValueData(
                identifier = identifier,
                title = title,
                infoValues = infoValues.toList()
            )
        }
    }
}

data class InfoTextWithNameAndImageData(
    val identifier: String,
    val title: String,
    val base64Image: String
)