
#  Build a database for installation on the robot. It contains
#  the current repertoire of poses and actions.
#
# The current directory is the build project.
#
#!/bin/sh
#set -x
DB=bert.db
BUILD=`pwd`
CONFIG=${BUILD}/../Configuration
CSV=${CONFIG}/csv
ETC=${CONFIG}/etc
SQL=${CONFIG}/sql

cd ${ETC}
rm -f $DB
sqlite3 $DB < ${SQL}/createTables.sql

# Change to CSV mode and load pose and action tables
cd ${CSV}
cat Pose.csv | tail -n 1|sed -e 's/	/,/g' >/tmp/poses

cd ${ETC}
sqlite3 $DB << EOF
.mode csv
.import /tmp/poses Pose
EOF

cp ${DB} ${BERT_HOME}/etc/${DB}
echo "${DB} creation compete."