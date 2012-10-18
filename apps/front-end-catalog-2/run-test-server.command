#
# Runs the front-end test server on the port you specify.
# Assumes that this file is in the project root.
#

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Get port (default is 9000)
DEFAULT_PORT=9000
read -e \
     -p "Run on which port? [$DEFAULT_PORT] " \
     PORT
PORT=${PORT:-$DEFAULT_PORT}

echo "Launching the front-end test server. Hold tight..."

# Move to the build root
cd $SCRIPT_DIR/../..

play2 "project front-end-catalog" "run $PORT"