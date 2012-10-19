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
printf "\e[0;32mThe Play! Server will be accessible on port $PORT\e[0;00m\n"
echo ""

# Get whether or not to allow Play to perform less recompilation
DEFAULT_COMPILE_LESS_EXTERNALLY=" "
read -e \
     -p "Perform Less CSS compilation outside of Play? [y/n, default n]" \
     COMPILE_LESS_EXTERNALLY

COMPILE_LESS_EXTERNALLY=${COMPILE_LESS_EXTERNALLY:-$DEFAULT_COMPILE_LESS_EXTERNALLY}

printf "\e[0;32m"
if [ "$COMPILE_LESS_EXTERNALLY" == "y" ]; then
  printf "The Play! server will not be compiling less files. Make your external tool compile to:\e[0;00m\n"
  echo "  $SCRIPT_DIR/../../modules/frontend-2/target/scala-2.9.1/resource_managed/main/public/stylesheets/main.min.css"
  LESS_COMPILE_SETTING='set (lessEntryPoints in "front-end") := Seq()'
else
  echo "The Play! server will be compiling less files. Be prepared for some wait times..."
  LESS_COMPILE_SETTING=''
fi
printf "\e[0;00m"
echo ""
echo "Launching the front-end test server. Hold tight..."

# Move to the build root
cd $SCRIPT_DIR/../..

play2 "project front-end-catalog" "$LESS_COMPILE_SETTING" "run $PORT"