/*
 * Copyright (c) 2024 mahendra.chavan@synclite.io, all rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package com.synclite.jobmonitor.web;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Owns {@code synclite_jobmonitor_metadata.db} — the job-monitor's per-device
 * configuration SQLite file. Replaces the legacy {@code synclite_job_schedules.json}.
 *
 * <p>Tables:
 * <ul>
 *   <li>{@code metadata(key TEXT PRIMARY KEY, value TEXT)} — holds the
 *       {@link #SYNCLITE_METADATA_VERSION_KEY} marker so a future job-monitor
 *       release can detect an older on-disk store and run a migration routine.</li>
 *   <li>{@code job_schedules(schedule_id INTEGER PRIMARY KEY, ...)} — one row per
 *       configured schedule. Column set mirrors the legacy JSON schema.</li>
 * </ul>
 *
 * <p>This file is created up-front by {@link ValidateDeviceDirectory} when the
 * device directory is selected, so every downstream servlet/JSP can assume both
 * tables exist.
 */
public final class MetadataManager {

	/** Bump when on-disk layout/semantics of {@code synclite_jobmonitor_metadata.db}
	 *  change in a non-back-compatible way. Stored in the {@code metadata} table under
	 *  {@link #SYNCLITE_METADATA_VERSION_KEY}. */
	public static final long SYNCLITE_METADATA_VERSION = 1L;
	public static final String SYNCLITE_METADATA_VERSION_KEY = "synclite_metadata_version";

	public static final String METADATA_FILE_NAME = "synclite_jobmonitor_metadata.db";

	private MetadataManager() {
	}

	public static Path metadataFile(Path deviceDir) {
		return deviceDir.resolve(METADATA_FILE_NAME);
	}

	/** Idempotent — safe to call on every device-dir load. Creates the metadata
	 *  file (if absent), the {@code metadata} and {@code job_schedules} tables,
	 *  and seeds the version row. */
	public static void ensureMetadata(Path deviceDir) throws SQLException {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new SQLException("Failed to load sqlite JDBC driver : " + e.getMessage(), e);
		}
		Path metadataPath = metadataFile(deviceDir);
		String url = "jdbc:sqlite:" + metadataPath;
		try (Connection conn = DriverManager.getConnection(url)) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE IF NOT EXISTS metadata(key TEXT PRIMARY KEY, value TEXT)");
				stmt.execute("CREATE TABLE IF NOT EXISTS job_schedules("
						+ "schedule_id INTEGER PRIMARY KEY, "
						+ "job_name TEXT NOT NULL, "
						+ "job_type TEXT NOT NULL, "
						+ "job_sub_type TEXT NOT NULL, "
						+ "schedule_start_hour INTEGER NOT NULL, "
						+ "schedule_start_minute INTEGER NOT NULL, "
						+ "schedule_end_hour INTEGER NOT NULL, "
						+ "schedule_end_minute INTEGER NOT NULL, "
						+ "job_run_duration INTEGER NOT NULL, "
						+ "job_run_duration_unit TEXT NOT NULL, "
						+ "job_run_interval INTEGER NOT NULL, "
						+ "job_run_interval_unit TEXT NOT NULL)");
			}
			seedMetadataVersionIfAbsent(conn);
		}
	}

	public static JSONArray loadAllSchedules(Path deviceDir) throws SQLException {
		JSONArray out = new JSONArray();
		String url = "jdbc:sqlite:" + metadataFile(deviceDir);
		try (Connection conn = DriverManager.getConnection(url);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(
						"SELECT schedule_id, job_name, job_type, job_sub_type, "
						+ "schedule_start_hour, schedule_start_minute, "
						+ "schedule_end_hour, schedule_end_minute, "
						+ "job_run_duration, job_run_duration_unit, "
						+ "job_run_interval, job_run_interval_unit "
						+ "FROM job_schedules ORDER BY schedule_id")) {
			while (rs.next()) {
				out.put(rowToJson(rs));
			}
		}
		return out;
	}

	/** Atomic replace — clears {@code job_schedules} and bulk-inserts the new set in one txn. */
	public static void replaceAllSchedules(Path deviceDir, JSONArray schedules) throws SQLException {
		String url = "jdbc:sqlite:" + metadataFile(deviceDir);
		try (Connection conn = DriverManager.getConnection(url)) {
			conn.setAutoCommit(false);
			try {
				try (Statement stmt = conn.createStatement()) {
					stmt.execute("DELETE FROM job_schedules");
				}
				insertAll(conn, schedules);
				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			} finally {
				conn.setAutoCommit(true);
			}
		}
	}

	public static void deleteSchedulesByIds(Path deviceDir, Collection<Long> scheduleIds) throws SQLException {
		if (scheduleIds == null || scheduleIds.isEmpty()) {
			return;
		}
		String url = "jdbc:sqlite:" + metadataFile(deviceDir);
		try (Connection conn = DriverManager.getConnection(url);
				PreparedStatement del = conn.prepareStatement("DELETE FROM job_schedules WHERE schedule_id = ?")) {
			conn.setAutoCommit(false);
			try {
				for (Long id : scheduleIds) {
					del.setLong(1, id);
					del.addBatch();
				}
				del.executeBatch();
				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			} finally {
				conn.setAutoCommit(true);
			}
		}
	}

	private static void seedMetadataVersionIfAbsent(Connection conn) throws SQLException {
		try (PreparedStatement sel = conn.prepareStatement("SELECT 1 FROM metadata WHERE key = ?")) {
			sel.setString(1, SYNCLITE_METADATA_VERSION_KEY);
			try (ResultSet rs = sel.executeQuery()) {
				if (rs.next()) {
					return;
				}
			}
		}
		try (PreparedStatement ins = conn.prepareStatement("INSERT INTO metadata(key, value) VALUES(?, ?)")) {
			ins.setString(1, SYNCLITE_METADATA_VERSION_KEY);
			ins.setString(2, Long.toString(SYNCLITE_METADATA_VERSION));
			ins.executeUpdate();
		}
	}

	private static void insertAll(Connection conn, JSONArray schedules) throws SQLException {
		if (schedules == null || schedules.isEmpty()) {
			return;
		}
		try (PreparedStatement ins = conn.prepareStatement(
				"INSERT INTO job_schedules("
				+ "schedule_id, job_name, job_type, job_sub_type, "
				+ "schedule_start_hour, schedule_start_minute, "
				+ "schedule_end_hour, schedule_end_minute, "
				+ "job_run_duration, job_run_duration_unit, "
				+ "job_run_interval, job_run_interval_unit"
				+ ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			for (int i = 0; i < schedules.length(); i++) {
				JSONObject o = schedules.getJSONObject(i);
				ins.setLong(1, o.getLong("scheduleID"));
				ins.setString(2, o.getString("jobName"));
				ins.setString(3, o.getString("jobType"));
				ins.setString(4, o.getString("jobSubType"));
				ins.setInt(5, o.getInt("scheduleStartHour"));
				ins.setInt(6, o.getInt("scheduleStartMinute"));
				ins.setInt(7, o.getInt("scheduleEndHour"));
				ins.setInt(8, o.getInt("scheduleEndMinute"));
				ins.setInt(9, o.getInt("jobRunDuration"));
				ins.setString(10, o.getString("jobRunDurationUnit"));
				ins.setInt(11, o.getInt("jobRunInterval"));
				ins.setString(12, o.getString("jobRunIntervalUnit"));
				ins.addBatch();
			}
			ins.executeBatch();
		}
	}

	private static JSONObject rowToJson(ResultSet rs) throws SQLException {
		JSONObject o = new JSONObject();
		o.put("scheduleID", rs.getLong("schedule_id"));
		o.put("jobName", rs.getString("job_name"));
		o.put("jobType", rs.getString("job_type"));
		o.put("jobSubType", rs.getString("job_sub_type"));
		o.put("scheduleStartHour", rs.getInt("schedule_start_hour"));
		o.put("scheduleStartMinute", rs.getInt("schedule_start_minute"));
		o.put("scheduleEndHour", rs.getInt("schedule_end_hour"));
		o.put("scheduleEndMinute", rs.getInt("schedule_end_minute"));
		o.put("jobRunDuration", rs.getInt("job_run_duration"));
		o.put("jobRunDurationUnit", rs.getString("job_run_duration_unit"));
		o.put("jobRunInterval", rs.getInt("job_run_interval"));
		o.put("jobRunIntervalUnit", rs.getString("job_run_interval_unit"));
		return o;
	}
}
