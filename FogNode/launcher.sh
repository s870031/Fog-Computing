#!/bin/bash


# Change this to your netid
netid=$(whoami)

#
# Root directory of your project
PROJDIR=$(pwd)

#
# This assumes your config file is named "config.txt"
# and is located in your project directory
#
CONFIG=$PROJDIR/$1

#
# Directory your java classes are in
#
BINDIR=$PROJDIR/bin

#
# Your main project class
#
PROG=Main

nodeID=1 # NOTE: the node ID

#
# Read config file and launch Project on nodes listed
# on config file
#

cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read firstLine
	nodes=$( echo $firstLine | awk '{print $1 }')
    echo "TOTAL NODE# : "$nodes
    while read line 
    do
        host=$( echo $line | awk '{ print $2 }' )

		if (( $nodeID <= $nodes ))
		then
			# "StrictHost..no": set connect without y/n question
			ssh -o "StrictHostKeyChecking no" $netid@$host java -cp $BINDIR $PROG $nodeID $CONFIG&
		fi

        nodeID=$(( nodeID + 1 ))

    done
   
)


