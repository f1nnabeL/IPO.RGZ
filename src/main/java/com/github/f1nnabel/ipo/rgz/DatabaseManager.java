package com.github.f1nnabel.ipo.rgz;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:simulation.db";

    public DatabaseManager() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                createTables(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String createSettingsTable = "CREATE TABLE IF NOT EXISTS settings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "payloadMass REAL," +
                "stageMasses TEXT," +
                "fuelMasses TEXT," +
                "thrustPerKgFuel REAL," +
                "cycleDelay INTEGER," +
                "fuelConsumptionPerCycle REAL" +
                ");";

        String createStatisticsTable = "CREATE TABLE IF NOT EXISTS statistics (" +
                "id INTEGER," +
                "settings_id INTEGER," +
                "currentMass REAL," +
                "speed REAL," +
                "altitude REAL," +
                "horizontalDistance REAL," +
                "remainingStages INTEGER," +
                "fuelMasses TEXT," +
                "timeStamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(settings_id) REFERENCES settings(id)" +
                ");";

        Statement stmt = conn.createStatement();
        stmt.execute(createSettingsTable);
        stmt.execute(createStatisticsTable);
    }

    public void saveSettings(RocketModel model) {
        String insertSettings = "INSERT INTO settings (payloadMass, stageMasses, fuelMasses, thrustPerKgFuel, cycleDelay, fuelConsumptionPerCycle) VALUES (?, ?, ?, ?, ?, ?);";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSettings)) {

            pstmt.setDouble(1, model.getPayloadMass());
            pstmt.setString(2, arrayToString(model.getStageMasses()));
            pstmt.setString(3, arrayToString(model.getFuelMasses()));
            pstmt.setDouble(4, model.getThrustPerKgFuel());
            pstmt.setInt(5, model.getCycleDelay());
            pstmt.setDouble(6, model.getFuelConsumptionPerCycle());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveStatistics(RocketModel model) {
        String insertStatistics = "INSERT INTO statistics (id, settings_id, currentMass, speed, altitude, horizontalDistance, remainingStages, fuelMasses) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            int lastSettingsId = getLastSettingsId(conn);

            try (PreparedStatement pstmt = conn.prepareStatement(insertStatistics)) {
                pstmt.setInt(1, (int) Math.round((model.getTotalTime()*10)));
                pstmt.setInt(2, lastSettingsId);
                pstmt.setDouble(3, model.getCurrentMass());
                pstmt.setDouble(4, model.getSpeed());
                pstmt.setDouble(5, model.getAltitude());
                pstmt.setDouble(6, model.getHorizontalDistance());
                pstmt.setInt(7, model.getRemainingStages());
                pstmt.setString(8, arrayToString(model.getFuelMasses()));

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getLastSettingsId(Connection conn) throws SQLException {
        String query = "SELECT id FROM settings ORDER BY id DESC LIMIT 1;";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                throw new SQLException("No settings found in the database.");
            }
        }
    }

    private String arrayToString(double[] array) {
        StringBuilder sb = new StringBuilder();
        for (double d : array) {
            sb.append(d).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public void loadLastSettings(RocketModel model) {
        String selectSettings = "SELECT * FROM settings ORDER BY id DESC LIMIT 1;";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSettings)) {

            if (rs.next()) {
                model.setPayloadMass(rs.getDouble("payloadMass"));
                model.setStageMasses(stringToArray(rs.getString("stageMasses")));
                model.setFuelMasses(stringToArray(rs.getString("fuelMasses")));
                model.setThrustPerKgFuel(rs.getDouble("thrustPerKgFuel"));
                model.setCycleDelay(rs.getInt("cycleDelay"));
                model.setFuelConsumptionPerCycle(rs.getDouble("fuelConsumptionPerCycle"));

                model.resetSimulationVariables();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double[] stringToArray(String str) {
        if (str == null || str.isEmpty()) {
            return new double[0];
        }
        String[] parts = str.split(",");
        double[] array = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                array[i] = Double.parseDouble(parts[i]);
            } catch (NumberFormatException e) {
                array[i] = 0;
            }
        }
        return array;
    }
}
