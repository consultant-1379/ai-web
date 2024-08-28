#!/bin/sh
#
# This procedure looks in a base directory after a defined file structure and
# removes any rpms that it finds according to certain rules.
#
# One arguments are given:
# $1 - The base directory of this software-bundle
#
# Rules:
# 1) RPM is packaged in the software-bundle toplevel directory
# 2) RPM located in software-bundle toplevel directory will be removed from all nodes
# 3) RPMs that should be removed from a sub-set of nodes should be located in a
#    special directory. Two special directories are managed: payload and control
# 4) Any *.rpm files located under 'rpms/control' will be removed from nodes
#    with TIPC address 1 and 2
# 5) Any *.rpm files located under 'rpms/payload' will be removed from nodes
#    with TIPC address 3 and larger.
#
die () {
  echo "ERROR: $@"
  exit 1
}

remove_rpms ()
{
  local rpmdir=$1
  local host=`hostname`
  local nodeid=`cmwea tipcaddress-get | sed 's/.*,.*,//'`
  local rpmname
	
	for file in `ls ${rpmdir}/*.rpm 2>/dev/null`
	do
		rpmfile=`basename $file`
		rpmname=` echo ${rpmfile} | sed "s/.rpm//" `
		test -n "${rpmname}" || continue
		cmwea rpm-config-delete $rpmname $host || die "Failed to remove $rpmname on $host"
		echo "HUZH remove 1 RPM $file"
	done
	if [ $nodeid -le 2 ]
	then
		for file in `ls ${rpmdir}/control/*.rpm 2>/dev/null`
		do
			rpmfile=`basename $file`
			rpmname=`grep $rpmfile ${rpmdir}/rpm.list | sed "s/${rpmfile}://"`
			test -n "${rpmname}" || continue
			cmwea rpm-config-delete $rpmname $host || die "Failed to remove $rpmname on $host"
			echo "HUZH remove 2 RPM $file"
		done
	else
		for file in `ls ${rpmdir}/payload/*.rpm 2>/dev/null`
		do
			rpmfile=`basename $file`
			rpmname=`grep $rpmfile ${rpmdir}/rpm.list | sed "s/${rpmfile}://"`
			test -n "${rpmname}" || continue
			cmwea rpm-config-delete $rpmname $host || die "Failed to remove $rpmname on $host"
			echo "HUZH remove 3 RPM $file"
		done
	fi
}

remove_rpms `dirname $0`

