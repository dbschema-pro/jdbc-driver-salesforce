package com.wisecoders.jdbc.salesforce

import java.io.FileInputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.util.Properties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Requires a valid Salesforce connection URL in gradle.properties")
class TestConnectivity {

    private lateinit var connection: Connection
    
    @BeforeEach 
    fun testDriver() {
        JdbcDriver()
        
        val input = FileInputStream("gradle.properties")
        val prop = Properties()
        prop.load(input)
        // IN gradle.properties EDIT salesforceURL=jdbc:dbschema:salesforce://user=...&password=...+token
        val url = prop.getProperty("salesforceURL")
        connection = DriverManager.getConnection(url)
    }

    @Test
    fun test() {
        val st: Statement = connection.createStatement()
        if (st.execute("select * from UserRole")) {
            val rs = st.resultSet
            while (rs.next()) {
                for (i in 0..<rs.metaData.columnCount) {
                    print(rs.getString(i + 1) + ",")
                }
                println()
            }
        }
        //st.execute("save dbf to out/testExport");
    }
}
