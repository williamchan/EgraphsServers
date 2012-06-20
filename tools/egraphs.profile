#
# Common variables to easily access the different egraphs server
# resources, sub-projects, and servers from the command line.
#
# To use it, put the following line into the file ~/.profile
#   source <path to this file>
#
# Afterwards, you can do things like this:
#
# > cd $egw # Goes to the website home
# > cd $egf # Goes to the front-end module home
# > cd $egc # Goes to the front-end test app catalog
#
# Project variables:
# EG_HOME (eg) -- Git project root
# EG_APP_WEBSITE (egw) -- Main website app root
# EG_MOD_FRONTEND (egf) -- Front-end resources module root
# EG_APP_CATALOG (egc) -- Front-end catalog app root
# 
# Server variables:
# eg_monitoring -- The server that monitors our cloudbees logs for errors
#   then emails developers
# egdb_staging -- The postgres database used by our 'staging' servers.
# egdb_beta -- The postgres database used by our 'beta' servers.
# egdb_live -- The postgres database used by our 'live' servers.
#


# Full expansions
export EG_HOME="$( cd "$( dirname `dirname "${BASH_SOURCE[0]}"` )" && pwd )"

export EG_APPS=$EG_HOME/apps
export EG_APP_WEBSITE=$EG_APPS/website
export EG_APP_CATALOG=$EG_APPS/front-end-catalog

export EG_MODULES=$EG_HOME/modules
export EG_MOD_FRONTEND=$EG_MODULES/frontend

# Shortened convenience variables
export eg=$EG_HOME
export egw=$EG_APP_WEBSITE
export egf=$EG_MOD_FRONTEND
export egc=$EG_APP_CATALOG

# Servers
export eg_monitoring=ec2-50-16-114-74.compute-1.amazonaws.com
export egdb_staging=ec2-107-21-229-179.compute-1.amazonaws.com
export egdb_beta=ec2-50-19-82-23.compute-1.amazonaws.com
export egdb_live=ec2-23-21-137-254.compute-1.amazonaws.com