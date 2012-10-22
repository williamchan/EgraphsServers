# Prints all source files in this repository to STDOUT

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $SCRIPT_DIR/..

find -E . -regex '.*(app|/main|/test)' \
  | grep -v 'target' \
  | egrep -v '(website/|frontend/|redis/|\.git|tmp|svgweb|jasmine|less)' 
