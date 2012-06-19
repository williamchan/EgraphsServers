#!/bin/bash
#
# Runs the egraphs server. When you're done then quit the terminal window
# that opened this server.
#
# Requires that you have already sourced the profile file in this same
# folder.

# Require the sourced profile
if [ -z "$EGRAPHS_SERVER_HOME" ]; then
   printf "EGRAPHS_SERVER_HOME variable was not set. Make sure you have sourced"
   printf " .egraphs_server.profile. Talk to Erem if this didn't make any"
   printf " sense to you.\n"

   exit 1
fi

# Perform our shenanigans.
cd $EGRAPHS_SERVER_HOME

play test