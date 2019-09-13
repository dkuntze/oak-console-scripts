import org.apache.jackrabbit.oak.spi.state.*;
import org.apache.jackrabbit.oak.api.*;
import java.util.*;
import org.apache.jackrabbit.util.*;

class NamespacesChecker {
	def NodeStore nodeStore;
	def localNamespaces = [:];
	def remoteNamespaces = [:];

	
	def check(){
		def ns = nodeStore.getRoot().getChildNode("jcr:system").getChildNode("rep:namespaces");
		
		// Cache the local namespaces
		ns.getProperties().each { prop ->
			if(prop.getName().indexOf(':')<0){
				localNamespaces[prop.getValue(Type.STRING)] = prop.getName();
			}
		}
		
		println("Local system has ${localNamespaces.size()} namespaces");
		
		// Cache the remote namespaces
		new File("./namespaces.cnd").eachLine { line ->
			def tokens = line.split('\'');
			remoteNamespaces.put(tokens[3], tokens[1]);
		}
		
		println("Remote system has ${remoteNamespaces.size()} namespaces");
		
		//
		localNamespaces.keySet().each { uri ->
			def localPrefix = localNamespaces[uri];
			def remotePrefix = remoteNamespaces[uri];
			
			if(remotePrefix==null){
				println("ADDITIONAL : Namespace '$localPrefix = $uri' not registered in remote system");
			} else if (!remotePrefix.equals(localPrefix)) {
				println("CONFLICT : Namespace $uri has prefix '$localPrefix' locally and '$remotePrefix' remotely");
			}
		}
		
		remoteNamespaces.keySet().each { uri ->
			def localPrefix = localNamespaces[uri];
			def remotePrefix = remoteNamespaces[uri];
			
			if(localPrefix==null){
				println("MISSING : Namespace '$remotePrefix = $uri' not registered in locals system");
			} 
		}
	}
	
}
	
new NamespacesChecker(nodeStore: session.store).check()
