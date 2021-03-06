#!/bin/sh
#
# dcm4chee Control Script
#
# chkconfig: 3 80 20
# description: dcm4chee PACS
#
# To use this script
# run it as root - it will switch to the specified user
# It loses all console output - use the log.
#
# Here is a little (and extremely primitive)
# startup/shutdown script for RedHat systems. It assumes
# that dcm4chee lives in /usr/local/dcm4chee, it's run by user
# 'pacs' and JDK binaries are in /usr/local/jdk/bin. All
# this can be changed in the script itself.
# Bojan
#
# Either amend this script for your requirements
# or just ensure that the following variables are set correctly
# before calling the script

# [ #420297 ] dcm4chee startup/shutdown for SUSE
# modified By Paolo Marcheschi 22/12/2006 email paolo@ifc.cnr.it
# In order to adapt to Suse
#. /etc/rc.d/init.d/functions
### BEGIN INIT INFO
# Provides:       dcm4chee
# Required-Start:  $postgresql
# Required-Stop:
# Default-Start:  3 5
# Default-Stop:
# Description: Start the DCM4CHEE DICOM Image Manager
### END INIT INFO

#define where jboss is - this is the directory containing directories log, bin, conf etc
JBOSS_HOME=${JBOSS_HOME:-"/home/dicom/dcm4chee"}

#make java is on your path
JAVAPTH=${JAVAPTH:-"/usr/java/jdk1.5.0_10/bin"}

#define the classpath for the shutdown class
JBOSSCP=${JBOSSCP:-"$JBOSS_HOME/bin/shutdown.jar"}

#define the script to use to start jboss
JBOSSSH=${JBOSSSH:-"$JBOSS_HOME/bin/run.sh"}

if [ -n "$JBOSS_CONSOLE" -a ! -d "$JBOSS_CONSOLE" ]; then
 # ensure the file exists
 touch $JBOSS_CONSOLE
fi

if [ -n "$JBOSS_CONSOLE" -a ! -f "$JBOSS_CONSOLE" ]; then
 echo "WARNING: location for saving console log invalid: $JBOSS_CONSOLE"
 echo "WARNING: ignoring it and using /dev/null"
 JBOSS_CONSOLE="/dev/null"
fi

#define what will be done with the console log
JBOSS_CONSOLE=${JBOSS_CONSOLE:-"/dev/null"}

#define the user under which jboss will run, or use RUNASIS to run as the current user
JBOSSUS=${JBOSSUS:-"pacs"}

CMD_START="cd $JBOSS_HOME/bin; $JBOSSSH"
CMD_STOP="java -classpath $JBOSSCP org.jboss.Shutdown --shutdown -u admin -p admin"
# I Modified the command stop in order to use the password and the login user (Paolo)
if [ "$JBOSSUS" = "RUNASIS" ]; then
 SUBIT=""
else
 SUBIT="su - $JBOSSUS -c "
fi

if [ -z "`echo $PATH | grep $JAVAPTH`" ]; then
 export PATH=$PATH:$JAVAPTH
fi

if [ ! -d "$JBOSS_HOME" ]; then
 echo JBOSS_HOME does not exist as a valid directory : $JBOSS_HOME
 exit 1
fi


echo CMD_START = $CMD_START


case "$1" in
start)
   cd $JBOSS_HOME/bin
   if [ -z "$SUBIT" ]; then
       eval $CMD_START >${JBOSS_CONSOLE} 2>&1 &
   else
       $SUBIT "$CMD_START >${JBOSS_CONSOLE} 2>&1 &"
   fi
   ;;
stop)
   if [ -z "$SUBIT" ]; then
       $CMD_STOP
   else
       $SUBIT "$CMD_STOP"
   fi
   ;;
restart)
   $0 stop
   $0 start
   ;;
*)
   echo "usage: $0 (start|stop|restart|help)"
esac