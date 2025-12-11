package com.wisecoders.jdbc.salesforce.io

import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
object Util {
    @JvmStatic
    fun md5Java(message: String): String {
        var digest: String? = null
        try {
            val md = MessageDigest.getInstance("MD5")
            val hash = md.digest(message.toByteArray(StandardCharsets.UTF_8))

            //converting byte array to Hexadecimal String
            val sb = StringBuilder(2 * hash.size)
            for (b in hash) {
                sb.append(String.format("%02x", b.toInt() and 0xff))
            }

            digest = sb.toString()
        } catch (ex: NoSuchAlgorithmException) {
            LOGGER.atError().setCause(ex).log()
        }
        return digest ?: ""
    }

    val LOGGER = slf4jLogger()
}
