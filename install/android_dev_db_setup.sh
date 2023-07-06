# This script is designed to be run on the development system
# running Android Studio.
# 
# Run android_dev_data_dir.sh, then reboot system.
# Then run this script. When complete the path expected by Android
# is linked to ~/robot/db. The database must not exist.
#
DBNAME=BertSpeak.db
ANDROID_LINK=~/robot/data
ANDROID_PATH=user/0/chuckcoughlin.bertspeak/databases
LINUX_PATH=~/robot/db

mkdir -p $ANDROID_LINK
cd $ANDROID_LINK
mkdir -p $ANDROID_PATH
cd $ANDROID_PATH
touch $DBNAME
mkdir -p $LINUX_PATH
cd $LINUX_PATH
rm -f $DBNAME
ln -s $ANDROID_LINK/$ANDROID_PATH/$DBNAME $DBNAME

