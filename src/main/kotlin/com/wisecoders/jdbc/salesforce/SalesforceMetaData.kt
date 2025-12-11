package com.wisecoders.jdbc.salesforce

import com.wisecoders.common_jdbc.jvm.result_set.ArrayResultSet
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.RowIdLifetime
import java.sql.SQLException
import kotlin.math.max

/**
 * Copyright Wise Coders GmbH https://wisecoders.com
 * Driver is used in the DbSchema Database Designer https://dbschema.com
 * Free to be used by everyone.
 * Code modifications allowed only to GitHub repository https://github.com/wise-coders/salesforce-jdbc-driver
 */
class SalesforceMetaData(private val connection: SalesforceConnection) : DatabaseMetaData {
    @get:Throws(SQLException::class)
    private val h2Meta: DatabaseMetaData
        get() = connection.h2Connection.metaData


    @Throws(SQLException::class)
    override fun getTables(
        catalog: String?,
        schemaPattern: String,
        tableNamePattern: String?,
        types: Array<String>
    ): ResultSet {
        connection.ensureTablesAreLoaded()
        val resultSet = ArrayResultSet()
        resultSet.setColumnNames(
            listOf(
                "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME",
                "TABLE_TYPE", "REMARKS", "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME",
                "REF_GENERATION"
            )
        )
        for (table in connection.schemaDef!!.tables) {
            resultSet.addRow(listOf(null, DEFAULT_SCHEMA_NAME, table.name, "TABLE", table.comment, "", "", "", "", ""))
        }
        return resultSet
    }

    @Throws(SQLException::class)
    override fun getSchemas(): ResultSet {
        val resultSet = ArrayResultSet()
        resultSet.setColumnNames(listOf("TABLE_SCHEM", "TABLE_CATALOG"))
        resultSet.addRow(listOf(DEFAULT_SCHEMA_NAME, null))
        return resultSet
    }

    @Throws(SQLException::class)
    override fun getCatalogs(): ResultSet {
        val resultSet = ArrayResultSet()
        resultSet.setColumnNames(listOf("TABLE_CAALOG"))
        return resultSet
    }

    @Throws(SQLException::class)
    override fun getColumns(
        catalog: String?,
        schemaPattern: String,
        tableNamePattern: String?,
        columnNamePattern: String?
    ): ResultSet {
        connection.ensureColumnsAreLoaded()
        val result = ArrayResultSet()
        result.setColumnNames(
            listOf(
                "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
                "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
                "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH",
                "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATLOG", "SCOPE_SCHEMA", "SCOPE_TABLE",
                "SOURCE_DATA_TYPE", "IS_AUTOINCREMENT"
            )
        )
        for (table in connection.schemaDef!!.tables) {
            if (tableNamePattern == null || table.name.contains(tableNamePattern)) {
                for (column in table.columns) {
                    if (columnNamePattern == null || column.name.contains(columnNamePattern)) {
                        result.addRow(
                            listOf(
                                null,  // "TABLE_CAT",
                                DEFAULT_SCHEMA_NAME,  // "TABLE_SCHEMA",
                                table.name,  // "TABLE_NAME", (i.e. MongoDB Collection Name)
                                column.name,  // "COLUMN_NAME",
                                "" + column.javaType,  // "DATA_TYPE",
                                column.type,  // "TYPE_NAME",
                                "" + max(column.length, column.digits),  // "COLUMN_SIZE",
                                "0",  // "BUFFER_LENGTH", (not used)
                                "" + column.scale,  // "DECIMAL_DIGITS",
                                "10",  // "NUM_PREC_RADIX",
                                "" + (if (column.nullable) DatabaseMetaData.columnNullable else DatabaseMetaData.columnNoNulls),  // "NULLABLE",
                                column.comment,  // "REMARKS",
                                "",  // "COLUMN_DEF",
                                "0",  // "SQL_DATA_TYPE", (not used)
                                "0",  // "SQL_DATETIME_SUB", (not used)
                                "800",  // "CHAR_OCTET_LENGTH",
                                "1",  // "ORDINAL_POSITION",
                                "NO",  // "IS_NULLABLE",
                                null,  // "SCOPE_CATLOG", (not a REF type)
                                null,  // "SCOPE_SCHEMA", (not a REF type)
                                null,  // "SCOPE_TABLE", (not a REF type)
                                null,  // "SOURCE_DATA_TYPE", (not a DISTINCT or REF type)
                                if (column.autoIncrement) "YES" else "NO" // "IS_AUTOINCREMENT" (can be auto-generated, but can also be specified)
                            )
                        )
                    }
                }
            }
        }
        return result
    }


    @Throws(SQLException::class)
    override fun getPrimaryKeys(
        catalog: String?,
        schemaName: String,
        tableName: String
    ): ResultSet {
        connection.ensureColumnsAreLoaded()
        val result = ArrayResultSet()
        result.setColumnNames(
            listOf(
                "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
                "KEY_SEQ", "PK_NAME"
            )
        )
        for (table in connection.schemaDef!!.tables) {
            if (table.name == tableName) {
                for (column in table.columns) {
                    if ("Id" == column.name) {
                        result.addRow(
                            listOf(
                                null, DEFAULT_SCHEMA_NAME, table.name, column.name, "1",
                                "Pk_$tableName"
                            )
                        )
                    }
                }
            }
        }
        return result
    }


    @Throws(SQLException::class)
    override fun getImportedKeys(
        catalogName: String?,
        schemaName: String,
        tableNamePattern: String?
    ): ResultSet {
        connection.ensureColumnsAreLoaded()
        val result = ArrayResultSet()
        result.setColumnNames(
            listOf(
                "PKTABLE_CAT",
                "PKTABLE_SCHEM",
                "PKTABLE_NAME",
                "PKCOLUMN_NAME",
                "FKTABLE_CAT",
                "FKTABLE_SCHEM",
                "FKTABLE_NAME",
                "FKCOLUMN_NAME",
                "KEY_SEQ",
                "UPDATE_RULE",
                "DELETE_RULE",
                "FK_NAME",
                "PK_NAME",
                "DEFERRABILITY"
            )
        )

        for (table in connection.schemaDef!!.tables) {
            if (tableNamePattern == null || table.name.contains(tableNamePattern)) {
                for (reference in table.foreignKeys) {
                    result.addRow(
                        listOf(
                            null,  //PKTABLE_CAT
                            DEFAULT_SCHEMA_NAME,  //PKTABLE_SCHEMA
                            reference.targetTable.name,  //PKTABLE_NAME
                            "Id",  //PKCOLUMN_NAME
                            null,  //FKTABLE_CAT
                            DEFAULT_SCHEMA_NAME,  //FKTABLE_SCHEM
                            table.name,  //FKTABLE_NAME
                            reference.column.name,  //FKCOLUMN_NAME
                            "1",  //KEY_SEQ 1,2
                            "" + DatabaseMetaData.importedKeyNoAction,  //UPDATE_RULE
                            "" + DatabaseMetaData.importedKeyNoAction,  //DELETE_RULE
                            "Ref",  //FK_NAME
                            null,  //PK_NAME
                            "" + DatabaseMetaData.importedKeyInitiallyImmediate //DEFERRABILITY
                        )
                    )
                }
            }
        }
        return result
    }


    @Throws(SQLException::class)
    override fun getExportedKeys(
        catalog: String?,
        schema: String,
        table: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun allProceduresAreCallable(): Boolean {
        return h2Meta.allProceduresAreCallable()
    }

    @Throws(SQLException::class)
    override fun allTablesAreSelectable(): Boolean {
        return h2Meta.allTablesAreSelectable()
    }

    @Throws(SQLException::class)
    override fun getURL(): String {
        return h2Meta.url
    }

    @Throws(SQLException::class)
    override fun getUserName(): String {
        return h2Meta.userName
    }

    @Throws(SQLException::class)
    override fun isReadOnly(): Boolean {
        return h2Meta.isReadOnly
    }

    @Throws(SQLException::class)
    override fun nullsAreSortedHigh(): Boolean {
        return h2Meta.nullsAreSortedHigh()
    }

    @Throws(SQLException::class)
    override fun nullsAreSortedLow(): Boolean {
        return h2Meta.nullsAreSortedLow()
    }

    @Throws(SQLException::class)
    override fun nullsAreSortedAtStart(): Boolean {
        return h2Meta.nullsAreSortedAtStart()
    }

    @Throws(SQLException::class)
    override fun nullsAreSortedAtEnd(): Boolean {
        return h2Meta.nullsAreSortedAtEnd()
    }

    @Throws(SQLException::class)
    override fun getDatabaseProductName(): String {
        return h2Meta.databaseProductName
    }

    @Throws(SQLException::class)
    override fun getDatabaseProductVersion(): String {
        return h2Meta.databaseProductVersion
    }

    @Throws(SQLException::class)
    override fun getDriverName(): String {
        return h2Meta.driverName
    }

    @Throws(SQLException::class)
    override fun getDriverVersion(): String {
        return h2Meta.driverVersion
    }

    override fun getDriverMajorVersion(): Int {
        return 11
    }

    override fun getDriverMinorVersion(): Int {
        return 1
    }

    @Throws(SQLException::class)
    override fun usesLocalFiles(): Boolean {
        return h2Meta.usesLocalFiles()
    }

    @Throws(SQLException::class)
    override fun usesLocalFilePerTable(): Boolean {
        return h2Meta.usesLocalFilePerTable()
    }

    @Throws(SQLException::class)
    override fun supportsMixedCaseIdentifiers(): Boolean {
        return h2Meta.supportsMixedCaseIdentifiers()
    }

    @Throws(SQLException::class)
    override fun storesUpperCaseIdentifiers(): Boolean {
        return h2Meta.storesUpperCaseIdentifiers()
    }

    @Throws(SQLException::class)
    override fun storesLowerCaseIdentifiers(): Boolean {
        return h2Meta.storesLowerCaseIdentifiers()
    }

    @Throws(SQLException::class)
    override fun storesMixedCaseIdentifiers(): Boolean {
        return h2Meta.storesMixedCaseIdentifiers()
    }

    @Throws(SQLException::class)
    override fun supportsMixedCaseQuotedIdentifiers(): Boolean {
        return h2Meta.supportsMixedCaseIdentifiers()
    }

    @Throws(SQLException::class)
    override fun storesUpperCaseQuotedIdentifiers(): Boolean {
        return h2Meta.storesUpperCaseQuotedIdentifiers()
    }

    @Throws(SQLException::class)
    override fun storesLowerCaseQuotedIdentifiers(): Boolean {
        return h2Meta.storesLowerCaseQuotedIdentifiers()
    }

    @Throws(SQLException::class)
    override fun storesMixedCaseQuotedIdentifiers(): Boolean {
        return storesMixedCaseQuotedIdentifiers()
    }

    @Throws(SQLException::class)
    override fun getIdentifierQuoteString(): String {
        return h2Meta.identifierQuoteString
    }

    @Throws(SQLException::class)
    override fun getSQLKeywords(): String {
        return h2Meta.sqlKeywords
    }

    @Throws(SQLException::class)
    override fun getNumericFunctions(): String {
        return h2Meta.numericFunctions
    }

    @Throws(SQLException::class)
    override fun getStringFunctions(): String {
        return h2Meta.stringFunctions
    }

    @Throws(SQLException::class)
    override fun getSystemFunctions(): String {
        return h2Meta.systemFunctions
    }

    @Throws(SQLException::class)
    override fun getTimeDateFunctions(): String {
        return h2Meta.timeDateFunctions
    }

    @Throws(SQLException::class)
    override fun getSearchStringEscape(): String {
        return h2Meta.searchStringEscape
    }

    @Throws(SQLException::class)
    override fun getExtraNameCharacters(): String {
        return h2Meta.extraNameCharacters
    }

    @Throws(SQLException::class)
    override fun supportsAlterTableWithAddColumn(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsAlterTableWithDropColumn(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsColumnAliasing(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun nullPlusNonNullIsNull(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsConvert(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsConvert(
        fromType: Int,
        toType: Int
    ): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsTableCorrelationNames(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsDifferentTableCorrelationNames(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsExpressionsInOrderBy(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOrderByUnrelated(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsGroupBy(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsGroupByUnrelated(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsGroupByBeyondSelect(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsLikeEscapeClause(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsMultipleResultSets(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsMultipleTransactions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsNonNullableColumns(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsMinimumSQLGrammar(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCoreSQLGrammar(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsExtendedSQLGrammar(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsANSI92EntryLevelSQL(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsANSI92IntermediateSQL(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsANSI92FullSQL(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsIntegrityEnhancementFacility(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOuterJoins(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsFullOuterJoins(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsLimitedOuterJoins(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getSchemaTerm(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getProcedureTerm(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getCatalogTerm(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun isCatalogAtStart(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getCatalogSeparator(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun supportsSchemasInDataManipulation(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSchemasInProcedureCalls(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSchemasInTableDefinitions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSchemasInIndexDefinitions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSchemasInPrivilegeDefinitions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCatalogsInDataManipulation(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCatalogsInProcedureCalls(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCatalogsInTableDefinitions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCatalogsInIndexDefinitions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCatalogsInPrivilegeDefinitions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsPositionedDelete(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsPositionedUpdate(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSelectForUpdate(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsStoredProcedures(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSubqueriesInComparisons(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSubqueriesInExists(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSubqueriesInIns(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSubqueriesInQuantifieds(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCorrelatedSubqueries(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsUnion(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsUnionAll(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOpenCursorsAcrossCommit(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOpenCursorsAcrossRollback(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOpenStatementsAcrossCommit(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOpenStatementsAcrossRollback(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getMaxBinaryLiteralLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxCharLiteralLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInGroupBy(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInIndex(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInOrderBy(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInSelect(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInTable(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxConnections(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxCursorNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxIndexLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxSchemaNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxProcedureNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxCatalogNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxRowSize(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun doesMaxRowSizeIncludeBlobs(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getMaxStatementLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxStatements(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxTableNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxTablesInSelect(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxUserNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getDefaultTransactionIsolation(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun supportsTransactions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsTransactionIsolationLevel(level: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsDataDefinitionAndDataManipulationTransactions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsDataManipulationTransactionsOnly(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun dataDefinitionCausesTransactionCommit(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun dataDefinitionIgnoredInTransactions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getProcedures(
        catalog: String,
        schemaPattern: String,
        procedureNamePattern: String
    ): ResultSet {
        return h2Meta.getProcedures(catalog, schemaPattern, procedureNamePattern)
    }

    @Throws(SQLException::class)
    override fun getProcedureColumns(
        catalog: String,
        schemaPattern: String,
        procedureNamePattern: String,
        columnNamePattern: String
    ): ResultSet {
        return getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern)
    }


    @Throws(SQLException::class)
    override fun getTableTypes(): ResultSet? {
        return null
    }


    @Throws(SQLException::class)
    override fun getColumnPrivileges(
        catalog: String,
        schema: String,
        table: String,
        columnNamePattern: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getTablePrivileges(
        catalog: String,
        schemaPattern: String,
        tableNamePattern: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getBestRowIdentifier(
        catalog: String,
        schema: String,
        table: String,
        scope: Int,
        nullable: Boolean
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getVersionColumns(
        catalog: String,
        schema: String,
        table: String
    ): ResultSet? {
        return null
    }


    @Throws(SQLException::class)
    override fun getCrossReference(
        parentCatalog: String,
        parentSchema: String,
        parentTable: String,
        foreignCatalog: String,
        foreignSchema: String,
        foreignTable: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getTypeInfo(): ResultSet {
        return h2Meta.typeInfo
    }

    @Throws(SQLException::class)
    override fun getIndexInfo(
        catalog: String,
        schema: String,
        table: String,
        unique: Boolean,
        approximate: Boolean
    ): ResultSet {
        return h2Meta.getIndexInfo(catalog, schema, table, unique, approximate)
    }

    @Throws(SQLException::class)
    override fun supportsResultSetType(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsResultSetConcurrency(
        type: Int,
        concurrency: Int
    ): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun ownUpdatesAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun ownDeletesAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun ownInsertsAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun othersUpdatesAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun othersDeletesAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun othersInsertsAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun updatesAreDetected(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun deletesAreDetected(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun insertsAreDetected(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsBatchUpdates(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getUDTs(
        catalog: String,
        schemaPattern: String,
        typeNamePattern: String,
        types: IntArray
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
        return h2Meta.connection
    }

    @Throws(SQLException::class)
    override fun supportsSavepoints(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsNamedParameters(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsMultipleOpenResults(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsGetGeneratedKeys(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getSuperTypes(
        catalog: String,
        schemaPattern: String,
        typeNamePattern: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getSuperTables(
        catalog: String,
        schemaPattern: String,
        tableNamePattern: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getAttributes(
        catalog: String,
        schemaPattern: String,
        typeNamePattern: String,
        attributeNamePattern: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun supportsResultSetHoldability(holdability: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getResultSetHoldability(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getDatabaseMajorVersion(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getDatabaseMinorVersion(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getJDBCMajorVersion(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getJDBCMinorVersion(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getSQLStateType(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun locatorsUpdateCopy(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsStatementPooling(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getRowIdLifetime(): RowIdLifetime? {
        return null
    }

    @Throws(SQLException::class)
    override fun getSchemas(
        catalog: String,
        schemaPattern: String
    ): ResultSet {
        return schemas
    }

    @Throws(SQLException::class)
    override fun supportsStoredFunctionsUsingCallSyntax(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun autoCommitFailureClosesAllResultSets(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getClientInfoProperties(): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getFunctions(
        catalog: String,
        schemaPattern: String,
        functionNamePattern: String
    ): ResultSet {
        return h2Meta.getFunctions(catalog, schemaPattern, functionNamePattern)
    }

    @Throws(SQLException::class)
    override fun getFunctionColumns(
        catalog: String,
        schemaPattern: String,
        functionNamePattern: String,
        columnNamePattern: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getPseudoColumns(
        catalog: String,
        schemaPattern: String,
        tableNamePattern: String,
        columnNamePattern: String
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun generatedKeyAlwaysReturned(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun <T> unwrap(iface: Class<T>): T? {
        return null
    }

    @Throws(SQLException::class)
    override fun isWrapperFor(iface: Class<*>?): Boolean {
        return false
    }

    companion object {
        private const val DEFAULT_SCHEMA_NAME = "Default"
    }
}
