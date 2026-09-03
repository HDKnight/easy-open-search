package org.dromara.easyos.jdbc;

import org.dromara.easyos.exception.EasyOsException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SqlProbe {
    private SqlProbe() {
    }

    public static void verify(Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw EasyOsException.sqlPluginMissing();
            }
        } catch (EasyOsException e) {
            throw e;
        } catch (SQLException e) {
            throw EasyOsException.sqlPluginMissing();
        }
    }
}
