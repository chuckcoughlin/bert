
#  Build a database for migration of diagnostics applications. Copy into the migration
#  directory of the project tree.
#
#  -- must be run in the same directory as the insert script.
#
#!/bin/sh
#set -x
DB=conversion.db
BIN=${GIT_REPO}/sfc/migration/mdb
TARGETDIR=${GIT_REPO}/sfc/migration/mdb
mkdir -p ${TARGETDIR}

cd ${TARGETDIR}
rm -f $DB
sqlite3 $DB < ${BIN}/createTables.sql
sqlite3 $DB < ${BIN}/insertClassMap.sql
sqlite3 $DB < ${BIN}/insertSymbols.sql
sqlite3 $DB < ${BIN}/insertPreferences.sql
sqlite3 $DB < ${BIN}/insertProperties.sql
sqlite3 $DB < ${BIN}/insertTags.sql
sqlite3 $DB < ${BIN}/insertOutputTags.sql

# BLT Tables
sqlite3 $DB < ${BIN}/insertBltAnchorMap.sql
sqlite3 $DB < ${BIN}/insertBltProperties.sql
sqlite3 $DB < ${BIN}/insertBltPythonBlocks.sql
# G2-Python
sqlite3 $DB < ${BIN}/insertG2PyArguments.sql
# SFC Tables
sqlite3 $DB < ${BIN}/insertSfcClassMap.sql
sqlite3 $DB < ${BIN}/insertSfcProperties.sql
sqlite3 $DB < ${BIN}/insertSfcPropertyValues.sql

# Must follow insertPropertyValues...
sqlite3 $DB < ${BIN}/insertProcedures.sql
