import org.apache.jackrabbit.oak.spi.state.*
import org.apache.jackrabbit.oak.api.*

/**
 * This script will export all namespaces from the Oak repository in CND format - see [1] / [2]
 * These can then be imported into the target environment using the CRX DE 'Tools > Import Node Type' panel.
 *
 * [1] https://jackrabbit.apache.org/jcr/node-type-notation.html
 * [2] <'slingevent'='http://sling.apache.org/jcr/event/1.0'>
 */

class NamespaceDump {
	NodeStore nodeStore;
	
	private File dumpFile = new File("namespaces.cnd")
	
	def dump(){
		def ns = nodeStore.getRoot().getChildNode("jcr:system").getChildNode("rep:namespaces");
		
		dumpFile.withPrintWriter { pw ->
			ns.getProperties().each { prop ->
				if(prop.getName().find(':')<0){
					pw.println("<'"+prop.getName()+"'='"+prop.getValue(Type.STRING)+"'>");
				}
			}
		}
	}
}

new NamespaceDump(nodeStore: session.store).dump()
