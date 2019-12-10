import org.apache.jackrabbit.oak.spi.state.*;
import org.apache.jackrabbit.oak.api.*;
import java.util.*;
import org.apache.jackrabbit.util.*;

class UnprocessedAssetsFinder {
	def NodeStore nodeStore;

	def checkedAssetCount = 0 as long;
	def unprocessedAssets = 0 as long;
	
	def check(){
		def timeStarted = new Date().getTime();
		
		
		// Walk the tree and check
		traverseDAM(nodeStore.getRoot().getChildNode("content").getChildNode("dam"), "/content/dam");

		def timeTaken = new Date().getTime() - timeStarted;
		
		println("Checked $checkedAssetCount assets in ${timeTaken}ms");
		println("Found $unprocessedAssets 'processing' assets");
	}

	def traverseDAM(ns, basePath){
		
		def nodeType = ns.getProperty("jcr:primaryType").getValue(org.apache.jackrabbit.oak.api.Type.NAME);

		if(nodeType.equals("dam:Asset")){
			
			if(ns.hasChildNode("jcr:content")){
				def assetState = ns.getChildNode("jcr:content").getString("dam:assetState");
				if("processing".equals(assetState)){
					println("Found asset : $basePath");
					++unprocessedAssets;
				}
			
			} else {
				println("Seen asset without content node $basePath");
			}
			
			++checkedAssetCount;
			if(checkedAssetCount % 1000 == 0){
				println("Checked $checkedAssetCount assets");	
			}
			
		} else if(nodeType.equals("sling:OrderedFolder") || nodeType.equals("sling:Folder")){

			// Check child nodes
			ns.getChildNodeEntries().each { cne ->
				traverseDAM(cne.getNodeState(),basePath+"/"+cne.getName());	
			}

		}
		
	}
	
}
	
new UnprocessedAssetsFinder(nodeStore: session.store).check()
