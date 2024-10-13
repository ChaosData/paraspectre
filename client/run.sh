#!/bin/sh

SCRIPT=$0

cd `dirname $SCRIPT`
SCRIPT=`basename $SCRIPT`

while [ -L "$SCRIPT" ]
do
    SCRIPT=`readlink $SCRIPT`
    cd `dirname $SCRIPT`
    SCRIPT=`basename $SCRIPT`
done

SCRIPTDIR=`pwd -P`

tmux new-session -s paraspectre node "$SCRIPTDIR/prox.js" 127.0.0.1:4442 127.0.0.1:4443 127.0.0.1:4444
