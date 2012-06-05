#!/bin/sh
# package the Eve library in a zip file

version="0.1"
bin="../bin"
package="${bin}/eve-nodejs-${version}"
files="eve.js server.js README agent node_modules"

# create package
echo "creating package ${package}..."
zip -q -r ${package} ${files}

echo "done"
