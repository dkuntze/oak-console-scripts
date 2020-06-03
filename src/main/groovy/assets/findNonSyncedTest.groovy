import org.apache.jackrabbit.oak.spi.state.*;
import org.apache.jackrabbit.oak.api.*;
import java.util.*;
import org.apache.jackrabbit.util.*;

class NonSyncedAssetsFinder {
	def NodeStore nodeStore;

	def checkedAssetCount = 0 as long;
	def nonSyncedAssetCount = 0 as long;
	
	def check(){
		def timeStarted = new Date().getTime();
		
		// Walk the tree and check
		// /content/dam/projects/marketing/seasonal/fw20/fw20-global-run-charged-seasonal-bronze
		traverseDAM(nodeStore.getRoot().getChildNode("content").getChildNode("dam").getChildNode("projects").getChildNode("marketing").getChildNode("seasonal").getChildNode("fw20").getChildNode("fw20-global-run-charged-seasonal-bronze"), "/content/dam/projects/marketing/seasonal/fw20/fw20-global-run-charged-seasonal-bronze", false);

		def timeTaken = new Date().getTime() - timeStarted;
		
		println("Checked $checkedAssetCount assets in ${timeTaken}ms");
		println("Found $nonSyncedAssetCount erroneously synced assets");
	}
	
	def getLocalS7Required(ns){
		def answer = null;
		
		if(ns.hasProperty("cq:conf")){
			answer = "include";
		} else if ("include".equals(ns.getString("s7-sync-mode"))){
			answer = "include";
		} else if ("exclude".equals(ns.getString("s7-sync-mode"))){
			answer = "exclude";
		} else if (ns.hasChildNode("jcr:content")){
			answer = getLocalS7Required(ns.getChildNode("jcr:content"));
		}
	
		return answer;
	}

	def traverseDAM(ns, basePath, scene7Required){
		
		def nodeType = ns.getProperty("jcr:primaryType").getValue(org.apache.jackrabbit.oak.api.Type.NAME);

		if(nodeType.equals("dam:Asset")){
			println("Checking asset $basePath ($scene7Required)");
			if(ns.hasChildNode("jcr:content")){
				def contentNode = ns.getChildNode("jcr:content");
				def status = contentNode.getString("status");
				def assetState = contentNode.getString("dam:assetState");

				if(!scene7Required && contentNode.hasChildNode("metadata")){
					def metadataNode = contentNode.getChildNode("metadata");
					
					if(metadataNode.hasProperty("dam:scene7File")){
						println("CANDIDATE ASSET -> $basePath");
						++nonSyncedAssetCount;
					} 
				}
			} 
			
			++checkedAssetCount;
			if(checkedAssetCount%100==0){
				println("Checked $checkedAssetCount assets");
			}
			
		} else if(nodeType.equals("sling:OrderedFolder") || nodeType.equals("sling:Folder")){
		
			def localS7Required = getLocalS7Required(ns);
			if(scene7Required && ("exclude".equals(localS7Required))){
				scene7Required = false;
			} else if ("include".equals(localS7Required)){
				scene7Required = true;
			}
			
			println("In folder $basePath ($scene7Required)");
			
			// Check child nodes
			ns.getChildNodeEntries().each { cne ->
				traverseDAM(cne.getNodeState(),basePath+"/"+cne.getName(), scene7Required);		
			}
		}	
	}
	
}
	
new NonSyncedAssetsFinder(nodeStore: session.store).check()
