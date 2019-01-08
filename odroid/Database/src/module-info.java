/**
 * 
 */
module bert.sql {
	requires java.sql;
	requires sqlite.jdbc;
	requires bert.share;
	exports bert.sql.db;
}