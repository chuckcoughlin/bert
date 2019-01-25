/**
 * 
 */
module bert.sql {
	requires java.sql;
	requires sqlite.jdbc;
	requires transitive bert.share;
	exports bert.sql.db;
}