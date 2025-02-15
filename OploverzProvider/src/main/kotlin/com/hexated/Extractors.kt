package com.hexated

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

class Krakenfiles : ExtractorApi() {
    override val name = "Krakenfiles"
    override val mainUrl = "https://krakenfiles.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val id = Regex("(?://|\\.)(krakenfiles\\.com)/(?:view|embed-video)?/([\\da-zA-Z]+)").find(url)?.groupValues?.get(2)
        val doc = app.get("$mainUrl/embed-video/$id").document
        val link = doc.selectFirst("source")?.attr("src")

        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                httpsify(link ?: return),
                "",
                Qualities.Unknown.value
            )
        )

    }

    data class Source(
        @JsonProperty("url") val url: String? = null,
    )

}

class Gofile : ExtractorApi() {
    override val name = "Gofile"
    override val mainUrl = "https://gofile.io"
    override val requiresReferer = false
    private val mainApi = "https://api.gofile.io"

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val id =
            Regex("(?://|\\.)(gofile\\.io)/(?:\\?c=|d/)([\\da-zA-Z]+)").find(url)?.groupValues?.get(
                2
            )
        val token = app.get("$mainApi/createAccount").parsedSafe<Account>()?.data?.get("token")
        app.get("$mainApi/getContent?contentId=$id&token=$token&websiteToken=12345")
            .parsedSafe<Source>()?.data?.contents?.forEach {
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name,
                        it.value["link"] ?: return,
                        "",
                        getQuality(it.value["name"]),
                        headers = mapOf(
                            "Cookie" to "accountToken=$token"
                        )
                    )
                )
            }

    }

    private fun getQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }

    data class Account(
        @JsonProperty("data") val data: HashMap<String, String>? = null,
    )

    data class Data(
        @JsonProperty("contents") val contents: HashMap<String, HashMap<String, String>>? = null,
    )

    data class Source(
        @JsonProperty("data") val data: Data? = null,
    )

}