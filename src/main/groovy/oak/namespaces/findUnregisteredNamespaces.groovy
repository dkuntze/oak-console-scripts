import org.apache.jackrabbit.oak.spi.state.*;
import org.apache.jackrabbit.oak.api.*;
import java.util.*;
import org.apache.jackrabbit.util.*;

class UnregisteredNamespaceChecker {
	def NodeStore nodeStore;
	def nsCache = [:];
	def missingPrefixes = [] as Set;
	def checkedNodeCount = 0 as long;
	
	def check(){
		def timeStarted = new Date().getTime();
		
		def ns = nodeStore.getRoot().getChildNode("jcr:system").getChildNode("rep:namespaces");
		
		// Populate the namespace cache
		ns.getProperties().each { prop ->
			if(prop.getName().indexOf(':')<0){
				nsCache[prop.getName()] = prop.getValue(Type.STRING);
			}
		}
		
		// println("Current namespaces "+nsCache);
		
		// Walk the tree and check
		checkNode(nodeStore.getRoot().getChildNode("content"));

		def timeTaken = new Date().getTime() - timeStarted;
		
		println("Checked $checkedNodeCount nodes in ${timeTaken}ms, found ${missingPrefixes.size()} unregistered prefixes: ${missingPrefixes}");
	}

	def checkNode(ns){
	
		// Check local properties
		ns.getProperties().each { prop ->
			if(prop.getName().indexOf(':')>2){
				def prefix = Text.getNamespacePrefix(prop.getName());

				if(nsCache[prefix]==null && !missingPrefixes.contains(prefix)){
					missingPrefixes.add(prefix);
					println("Found missing prefix at "+prop.getName());
				}
			}	
		}
		++checkedNodeCount;
		if(checkedNodeCount % 1000 == 0){
			println("Checked $checkedNodeCount");	
		}
		
		// Check child nodes
		ns.getChildNodeEntries().each { cne ->
			checkNode(cne.getNodeState());	
		}
	}
	
}
	
new UnregisteredNamespaceChecker(nodeStore: session.store).check()
