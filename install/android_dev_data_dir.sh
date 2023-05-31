# This script configures an Android Studio host machine for database access.
# After running, the host must be restarted.
#
# On Android, database access is to a fixed path which, unfortunately,
# coincides with a read-only path on MacOSX. This script creates 
# etc/synthetic.conf which gets around this restriction by creating a
# link to /data.
#
ROOT_FOLDER=data
LINK_FOLDER=/Users/chuckc/robot/data
SYNTHETIC=/etc/synthetic.conf

sudo rm -f $SYNTHETIC
sudo touch $SYNTHETIC
sudo chmod 0666 $SYNTHETIC
sudo echo "$ROOT_FOLDER	$LINK_FOLDER" >> $SYNTHETIC
sudo chmod 0644 $SYNTHETIC
sudo chown root:wheel $SYNTHETIC
