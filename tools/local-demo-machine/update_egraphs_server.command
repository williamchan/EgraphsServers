#!/bin/bash
#
# Updates the egraphs server source code repository on the demo machine and
# checks out the most recent demo branch.
#
# Requires internet access.
#
# Requires that you have already sourced the profile file in this same
# folder.
#

# Require the sourced profile
if [ -z "$EGRAPHS_SERVER_HOME" ]; then
   printf "EGRAPHS_SERVER_HOME variable was not set. Make sure you have sourced"
   printf " .egraphs_server.profile. Talk to Erem if this didn't make any"
   printf " sense to you.\n"

   exit 1
fi

# Move into the project home to perform our shenanigan
cd $EGRAPHS_SERVER_HOME

# Jump to master so that git will allow us to pull
git checkout master
git pull

# Checkout the latest demo tag
tools/checkout-latest-local-demo-tag

# Update dependencies by clearing old ones then updating new ones.
play dependencies --sync
play dependencies