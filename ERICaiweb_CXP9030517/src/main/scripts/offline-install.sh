#!/bin/sh
#
# This procedure looks in a base directory after a defined file structure and
# installs any rpms that it finds according to certain rules.
#
# One arguments are given:
# $1 - The base directory of this software-bundle
#
# Rules:
# 1) RPM is packaged in the toplevel software-bundle directory
# 2) Any RPM files located directly in this directory will be installed on all nodes
# 3) RPMs that should be installed on a sub-set of nodes should be located in a
#        special directory. Two special directories are managed: payload and control
# 4) Any *.rpm files located under 'rpms/control' will be installed on nodes
#        with TIPC address 1 and 2
# 5) Any *.rpm files located under 'rpms/payload' will be installed on nodes
#        with TIPC address 3 and larger.
#
die () {
  echo "ERROR: $@"
  exit 1
 }

install_rpms ()
 {
   local rpmdir=$1
   local host=`hostname`
   local nodeid=`cmwea tipcaddress-get | sed 's/.*,.*,//'`
 
   for file in `ls ${rpmdir}/*.rpm 2>/dev/null`
   do
        cmwea rpm-config-add $file $host || die "Failed to add $file on $host"
   done
 }
 install_rpms `dirname $0`