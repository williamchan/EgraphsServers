# Generates a .tags and .tags_sorted_by_file for use by Sublime

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $SCRIPT_DIR/..

echo "Generating .tags and .tags_sorted_by_file for repo"
$SCRIPT_DIR/source_directories.sh | xargs ctags -R -f .tags
$SCRIPT_DIR/source_directories.sh | xargs ctags --sort=yes -R -f .tags_sorted_by_file