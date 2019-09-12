# oak-console-scripts
Oak Run Console Scripts 

## Namespaces Dump

This script will export all namespaces from the Oak repository in CND format - see [1] / [2]
These can then be imported into the target environment using the CRX DE 'Tools > Import Node Type' panel.

After running (as shown below) there will be a file 'namespaces.cnd' in the current directory.

```
$ java -Djline.terminal=jline.UnsupportedTerminal -jar oak-run*.jar console /path/to/segmentstore ":load https://raw.githubusercontent.com/blackfor/oak-console-scripts/master/src/main/groovy/oak/namespaces/namespaceDump.groovy" 
```

[1] https://jackrabbit.apache.org/jcr/node-type-notation.html
[2] <'slingevent'='http://sling.apache.org/jcr/event/1.0'>
