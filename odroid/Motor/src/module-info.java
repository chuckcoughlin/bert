/**
 * 
 */
module bert.motor {
	requires transitive jssc;
	requires transitive bert.control;
	requires transitive bert.share;
	requires transitive bert.sql;
	exports bert.motor.main;
	exports bert.motor.dynamixel;
}