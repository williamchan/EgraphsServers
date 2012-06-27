#!/bin/bash
#
# Triggers a play build by curling against localhost
#
# $1 the port against which to curl (defaults to 9000)

# Select the port
port=$1
if [ -z "$port" ]; then
  port=9000
fi

echo "Building scala resources on play app at localhost:$port..."

# First curl for scala builfd
build_results=`curl localhost:$port/build 2>/dev/null`
curl_return_value=$?

if [ "$curl_return_value" == 0 ]; then
  build_successful_text=`echo $build_results | grep -v 'Compilation error' | grep -i 'build successful'`
  success_search_result=$?  

  if [ "$success_search_result" == 0 ]; then
    echo "Scala build successful."
    echo "Building less-css resources..."
    
    less_results=`curl localhost:9000/public/stylesheets/main.less 2>/dev/null | sed 's/.*\(\[LESS ERROR.*\)"; }/\1/g' | grep 'LESS ERROR'`    
    grep_return_value=$?

    if [ "$grep_return_value" == 1 ]; then
      echo "$build_successful_text"
    else
      echo $less_results
    fi
  else
    echo "Build Failed. See server terminal for build errors."
  fi
else
  printf "No result from the server. Are you sure it's running at localhost:$port?\n"
fi

