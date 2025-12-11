package com.wisecoders.jdbc.salesforce

import com.sforce.soap.partner.PartnerConnection
import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.salesforce.io.TransferReader
import com.wisecoders.jdbc.salesforce.schema.Schema
import com.wisecoders.jdbc.salesforce.schema.ShowTables
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.SQLClientInfoException
import java.sql.SQLException
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Savepoint
import java.sql.Statement
import java.sql.Struct
import java.util.Properties
import java.util.concurrent.Executor
import java.util.regex.Pattern
import org.h2.Driver
import org.h2.jdbc.JdbcConnection

/**
 * When you open a connection we load internally the list of tables.
 * When you execute a query, we check which table names can be found in the query and we transfer them in an internal H2 database stored in user.home/.DbSchema
 * We also create a proxy on Statement and intercept certain commands we implement in the driver.
 * The driver can be improved, we are happy for contributions.
 *
 *
 * Copyright Wise Coders GmbH https://wisecoders.com
 * Driver is used in the DbSchema Database Designer https://dbschema.com
 * Free to be used by everyone.
 * Code modifications allowed only to GitHub repository https://github.com/wise-coders/salesforce-jdbc-driver
 */
class SalesforceConnection internal constructor(
    private val databaseName: String,
    val partnerConnection: PartnerConnection,
    parameters: Map<String, String>,
) : Connection {

    val h2Connection: JdbcConnection
    private val reader: TransferReader

    init {
        val LOGGER = slf4jLogger()

        val h2DatabasePath = getH2DatabasePath(databaseName)
        val h2JdbcUrl = "jdbc:h2:$h2DatabasePath;database_to_upper=false"
        LOGGER.atInfo().setMessage(
            "Create H2 database '$h2JdbcUrl'"
        ).log()
        this.h2Connection = (Driver().connect(h2JdbcUrl, Properties())) as JdbcConnection

        var showTables = ShowTables.ALL
        if (parameters.containsKey("tables") && "custom".equals(parameters["tables"], ignoreCase = true)) {
            showTables = ShowTables.CUSTOM
        }
        if (!schemes.containsKey(databaseName)) {
            schemes[databaseName] =
                Schema(showTables)
        }
        this.reader = TransferReader(this)
    }

    private fun getH2DatabasePath(path: String): String {
        val h2File = File(INTERNAL_H2_LOCATION)
        if (!h2File.exists()) {
            h2File.mkdirs()
        }
        return INTERNAL_H2_LOCATION + path
    }


    val schemaDef: Schema?
        get() = schemes[databaseName]

    @Throws(SQLException::class)
    fun ensureTablesAreLoaded() {
        schemaDef!!.ensureTablesAreLoaded(partnerConnection)
    }

    @Throws(SQLException::class)
    fun ensureColumnsAreLoaded() {
        schemaDef!!.ensureColumnsAreLoaded(partnerConnection)
    }

    @Throws(SQLException::class)
    private fun transferDataForTablesFromQuery(query: String?) {
        if (query != null && !query.isEmpty()) {
            ensureTablesAreLoaded()
            for (table in schemaDef!!.tables) {
                if (!table.isLoaded && table.findNamePattern.matcher(query).find()) {
                    reader.transferData(table)
                    table.isLoaded = true
                }
            }
        }
    }

    @Throws(SQLException::class)
    override fun createStatement(): Statement {
        val statement = h2Connection.createStatement()
        return StatementProxy(statement).proxyStatement
    }

    @Throws(SQLException::class)
    override fun createStatement(
        resultSetType: Int,
        resultSetConcurrency: Int,
    ): Statement {
        val statement = h2Connection.createStatement(resultSetType, resultSetConcurrency)
        return StatementProxy(statement).proxyStatement
    }

    @Throws(SQLException::class)
    override fun createStatement(
        resultSetType: Int,
        resultSetConcurrency: Int,
        resultSetHoldability: Int,
    ): Statement {
        val statement = h2Connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability)
        return StatementProxy(statement).proxyStatement
    }


    private inner class StatementProxy(private val target: Any) : InvocationHandler {
        val proxyStatement: Statement = Proxy.newProxyInstance(
            Statement::class.java.classLoader,
            arrayOf<Class<*>>(Statement::class.java),
            this
        ) as Statement

        @Throws(Throwable::class)
        override fun invoke(
            proxy: Any,
            method: Method,
            args: Array<Any>?,
        ): Any {
            if (!args.isNullOrEmpty()) {
                val firstArgument = args[0].toString()
                if (RELOAD_SCHEMA.matcher(firstArgument).matches()) {
                    schemaDef?.refreshTables(partnerConnection)
                    schemaDef?.refreshColumns(partnerConnection)
                } else if (CACHE_ALL.matcher(firstArgument).matches()) {
                    TransferReader(this@SalesforceConnection).transferAllData()
                }
                val _schemaDef = schemaDef
                if (CLEAN_CACHES.matcher(firstArgument).matches() && _schemaDef != null) {
                    for (table in _schemaDef.tables) {
                        table.isLoaded = false
                    }
                } else {
                    transferDataForTablesFromQuery(firstArgument)
                }
            }

            return if (args == null) {
                method.invoke(target)
            } else {
                method.invoke(target, *args)
            }
        }
    }

    @Throws(SQLException::class)
    override fun prepareStatement(sql: String): PreparedStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareStatement(sql)
    }

    @Throws(SQLException::class)
    override fun prepareCall(sql: String): CallableStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareCall(sql)
    }

    @Throws(SQLException::class)
    override fun nativeSQL(sql: String): String {
        transferDataForTablesFromQuery(sql)
        return h2Connection.nativeSQL(sql)
    }

    @Throws(SQLException::class)
    override fun setAutoCommit(autoCommit: Boolean) {
        h2Connection.autoCommit = autoCommit
    }

    @Throws(SQLException::class)
    override fun getAutoCommit(): Boolean {
        return h2Connection.autoCommit
    }

    @Throws(SQLException::class)
    override fun commit() {
        h2Connection.commit()
    }

    @Throws(SQLException::class)
    override fun rollback() {
        h2Connection.rollback()
    }

    @Throws(SQLException::class)
    override fun close() {
        h2Connection.close()
    }

    @Throws(SQLException::class)
    override fun isClosed(): Boolean {
        return h2Connection.isClosed
    }

    override fun getMetaData(): DatabaseMetaData {
        return SalesforceMetaData(this)
    }

    @Throws(SQLException::class)
    override fun setReadOnly(readOnly: Boolean) {
        h2Connection.isReadOnly = readOnly
    }

    @Throws(SQLException::class)
    override fun isReadOnly(): Boolean {
        return h2Connection.isReadOnly
    }

    @Throws(SQLException::class)
    override fun setCatalog(catalog: String) {
        h2Connection.catalog = catalog
    }

    @Throws(SQLException::class)
    override fun getCatalog(): String {
        return h2Connection.catalog
    }

    @Throws(SQLException::class)
    override fun setTransactionIsolation(level: Int) {
        h2Connection.transactionIsolation = level
    }

    @Throws(SQLException::class)
    override fun getTransactionIsolation(): Int {
        return h2Connection.transactionIsolation
    }

    @Throws(SQLException::class)
    override fun getWarnings(): SQLWarning {
        return h2Connection.warnings
    }

    @Throws(SQLException::class)
    override fun clearWarnings() {
        h2Connection.clearWarnings()
    }


    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
    ): PreparedStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareStatement(sql, resultSetType, resultSetConcurrency)
    }

    @Throws(SQLException::class)
    override fun prepareCall(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
    ): CallableStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareCall(sql, resultSetType, resultSetConcurrency)
    }

    @Throws(SQLException::class)
    override fun getTypeMap(): Map<String, Class<*>> {
        return h2Connection.typeMap
    }

    @Throws(SQLException::class)
    override fun setTypeMap(map: Map<String?, Class<*>?>?) {
        h2Connection.typeMap = map
    }

    @Throws(SQLException::class)
    override fun setHoldability(holdability: Int) {
        h2Connection.holdability = holdability
    }

    @Throws(SQLException::class)
    override fun getHoldability(): Int {
        return h2Connection.holdability
    }

    @Throws(SQLException::class)
    override fun setSavepoint(): Savepoint {
        return h2Connection.setSavepoint()
    }

    @Throws(SQLException::class)
    override fun setSavepoint(name: String): Savepoint {
        return h2Connection.setSavepoint(name)
    }

    @Throws(SQLException::class)
    override fun rollback(savepoint: Savepoint) {
        h2Connection.rollback(savepoint)
    }

    @Throws(SQLException::class)
    override fun releaseSavepoint(savepoint: Savepoint) {
        h2Connection.releaseSavepoint(savepoint)
    }


    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
        resultSetHoldability: Int,
    ): PreparedStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability)
    }

    @Throws(SQLException::class)
    override fun prepareCall(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
        resultSetHoldability: Int,
    ): CallableStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability)
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        autoGeneratedKeys: Int,
    ): PreparedStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareStatement(sql, autoGeneratedKeys)
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        columnIndexes: IntArray,
    ): PreparedStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareStatement(sql, columnIndexes)
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        columnNames: Array<String>,
    ): PreparedStatement {
        transferDataForTablesFromQuery(sql)
        return h2Connection.prepareStatement(sql, columnNames)
    }

    @Throws(SQLException::class)
    override fun createClob(): Clob {
        return h2Connection.createClob()
    }

    @Throws(SQLException::class)
    override fun createBlob(): Blob {
        return h2Connection.createBlob()
    }

    @Throws(SQLException::class)
    override fun createNClob(): NClob {
        return h2Connection.createNClob()
    }

    @Throws(SQLException::class)
    override fun createSQLXML(): SQLXML {
        return h2Connection.createSQLXML()
    }

    @Throws(SQLException::class)
    override fun isValid(timeout: Int): Boolean {
        return h2Connection.isValid(timeout)
    }

    @Throws(SQLClientInfoException::class)
    override fun setClientInfo(
        name: String,
        value: String,
    ) {
        h2Connection.setClientInfo(name, value)
    }

    @Throws(SQLClientInfoException::class)
    override fun setClientInfo(properties: Properties) {
        h2Connection.clientInfo = properties
    }

    @Throws(SQLException::class)
    override fun getClientInfo(name: String): String {
        return h2Connection.getClientInfo(name)
    }

    @Throws(SQLException::class)
    override fun getClientInfo(): Properties {
        return h2Connection.clientInfo
    }

    @Throws(SQLException::class)
    override fun createArrayOf(
        typeName: String,
        elements: Array<Any>,
    ): java.sql.Array {
        return h2Connection.createArrayOf(typeName, elements)
    }

    @Throws(SQLException::class)
    override fun createStruct(
        typeName: String,
        attributes: Array<Any>,
    ): Struct {
        return h2Connection.createStruct(typeName, attributes)
    }

    @Throws(SQLException::class)
    override fun setSchema(schema: String) {
        h2Connection.schema = schema
    }

    @Throws(SQLException::class)
    override fun getSchema(): String {
        return h2Connection.schema
    }

    @Throws(SQLException::class)
    override fun abort(executor: Executor) {
        h2Connection.abort(executor)
    }

    @Throws(SQLException::class)
    override fun setNetworkTimeout(
        executor: Executor,
        milliseconds: Int,
    ) {
        h2Connection.setNetworkTimeout(executor, milliseconds)
    }

    @Throws(SQLException::class)
    override fun getNetworkTimeout(): Int {
        return h2Connection.networkTimeout
    }

    @Throws(SQLException::class)
    override fun <T> unwrap(iface: Class<T>): T {
        return h2Connection.unwrap(iface)
    }

    @Throws(SQLException::class)
    override fun isWrapperFor(iface: Class<*>?): Boolean {
        return h2Connection.isWrapperFor(iface)
    }

    companion object {
        private val INTERNAL_H2_LOCATION = System.getProperty("user.home") + "/.DbSchema/data/jdbc-salesforce-cache/"
        private val CLEAN_CACHES: Pattern = Pattern.compile("(\\s*)clean(\\s+)caches(\\s+)", Pattern.CASE_INSENSITIVE)
        private val CACHE_ALL: Pattern = Pattern.compile("(\\s*)cache(\\s+)all(\\s+)", Pattern.CASE_INSENSITIVE)
        private val RELOAD_SCHEMA: Pattern = Pattern.compile("(\\s*)reload(\\s+)schema(\\s+)", Pattern.CASE_INSENSITIVE)

        private val schemes = HashMap<String, Schema>()


        fun getSchema(schemaName: String): Schema? {
            return schemes[schemaName]
        }
    }
}
