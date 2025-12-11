package com.wisecoders.jdbc.salesforce

import com.sforce.soap.partner.Connector
import com.sforce.ws.ConnectionException
import com.sforce.ws.ConnectorConfig
import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.salesforce.io.H2Trigger
import com.wisecoders.jdbc.salesforce.io.Util.md5Java
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.sql.SQLException
import java.util.Properties
import java.util.logging.Logger


/**
 * When you open a connection we load internally the list of tables.
 * When you execute a query, we check which table names can be found in the query and we transfer them in an internal H2 database stored in user.home/.DbSchema
 * We also create a proxy on Statement and intercept certain commands we implement in the driver.
 * The driver can be improved, we are happy for contributions.
 *
 */
class JdbcDriver : Driver {

    @Throws(SQLException::class)
    override fun connect(
        url: String,
        info: Properties?,
    ): Connection {
        require(acceptsURL(JDBC_PREFIX)) {
            "Incorrect URL. Expected jdbc:dbschema:salesforce://https://login|OTHER.salesforce.com/services/Soap/u/APIVERSION?<parameters>"
        }

        LOGGER.atInfo().setMessage(("Connect URL '$url'.")).log()

        val parameters = parseProperties(info)
        val hostRef = extractHost(url.removePrefix(JDBC_PREFIX), parameters)

        val userName = parameters["user"]
        val password = parameters["password"]
        val sessionId = parameters["sessionid"]

        val config = ConnectorConfig().apply { authEndpoint = hostRef }

        LOGGER.atInfo().setMessage(("Connect to endpoint '$hostRef' using ${if (sessionId != null) "sessionid" else "user/password"}")).log()

        if (sessionId == null) {
            requireNotNull(userName) { "Missing username. Please add it to URL as user=<value>" }
            requireNotNull(password) { "Missing password. Please add it to URL as password=<value>" }

            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(userName, password.toCharArray())
            })

            config.username = userName
            config.password = password
        } else {
            config.sessionId = sessionId
        }

        return try {
            val partnerConnection = Connector.newConnection(config)
            val h2DbName = md5Java(userName ?: sessionId!!)
            H2Trigger.partnerConnection = partnerConnection

            SalesforceConnection(h2DbName, partnerConnection, parameters)
        } catch (ex: ConnectionException) {
            LOGGER.atError().setMessage("PartnerConnection error: $ex").setCause(ex).log()
            throw SQLException(buildString {
                append(ex)
                ex.cause?.let { append("\n").append(it.message) }
            }, ex)
        }
    }

    private fun parseProperties(info: Properties?): MutableMap<String, String> =
        mutableMapOf<String, String>().apply { info?.forEach { k, v -> this[k.toString()] = v.toString() } }

    private fun extractHost(
        url: String,
        parameters: MutableMap<String, String>,
    ): String {
        val delimiters = listOf("?", ";")
        val idx = delimiters.map { url.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: -1
        var hostRef = if (idx >= 0) url.substring(0, idx) else url

        if (idx >= 0) {
            val paramStr = url.substring(idx + 1)
            paramStr.split("&").forEach { pair ->
                pair.split("=").takeIf { it.size == 2 }?.let { (key, value) ->
                    parameters[key.lowercase()] = value
                }
            }
        }

        if (hostRef.isEmpty()) hostRef = "https://login.salesforce.com/services/Soap/u/55.0"
        if ("localhost" in hostRef) hostRef = hostRef.replace("localhost", "login.salesforce.com")

        return hostRef
    }


    // https://login.salesforce.com/services/Soap/u/51.0
    override fun acceptsURL(url: String): Boolean {
        return url.startsWith(JDBC_PREFIX) || url.startsWith("https://")
    }

    internal class ExtendedDriverPropertyInfo(
        name: String?,
        value: String?,
        choices: Array<String>?,
        description: String?,
    ) :
        DriverPropertyInfo(name, value) {
        init {
            this.description = description
            this.choices = choices
        }
    }

    override fun getPropertyInfo(
        url: String,
        info: Properties,
    ): Array<DriverPropertyInfo> {
        val result = arrayOf<DriverPropertyInfo>(ExtendedDriverPropertyInfo("log", "true", arrayOf("true", "false"), "Activate driver INFO logging"))
        return result
    }

    override fun getMajorVersion(): Int {
        return 1
    }

    override fun getMinorVersion(): Int {
        return 0
    }

    override fun jdbcCompliant(): Boolean {
        return true
    }

    override fun getParentLogger(): Logger? {
        return null
    }


    companion object {
        private const val JDBC_PREFIX = "jdbc:dbschema:salesforce://"
        private val LOGGER: org.slf4j.Logger = slf4jLogger()


        init {
            DriverManager.registerDriver(JdbcDriver())
        }
    }
}
