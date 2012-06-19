#
# Runs the front-end test server on the port you specify.
#

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Get port (default is 9000)
DEFAULT_PORT=9000
read -e \
     -p "Run on which port? [$DEFAULT_PORT] " \
     PORT
PORT=${PORT:-$DEFAULT_PORT}

# Run play
echo "Launching the front-end test server. Hold tight..."
cd $SCRIPT_DIR

play dependencies
play test --http.port=$PORT