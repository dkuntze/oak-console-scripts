import org.apache.jackrabbit.oak.spi.state.*;
import org.apache.jackrabbit.oak.api.*;
import java.util.*;
import org.apache.jackrabbit.util.*;

class UnprocessedAssetsFinder {
	def NodeStore nodeStore;

	def checkedAssetCount = 0 as long;
	def unprocessedAssets = 0 as long;
	def processingAssets = 0 as long;
	def errorStatusAssets = 0 as long;
	def scene7MissingAssets = 0 as long;
	def scene7UnpublishedAssets = 0 as long;
	
	def check(){
		def timeStarted = new Date().getTime();
		
		// Walk the tree and check
		traverseDAM(nodeStore.getRoot().getChildNode("content").getChildNode("dam"), "/content/dam", false);

		def timeTaken = new Date().getTime() - timeStarted;
		
		println("Checked $checkedAssetCount assets in ${timeTaken}ms");
		println("Found $unprocessedAssets 'unprocessed' assets");
		println("Found $processingAssets 'processing' assets");
		println("Found $errorStatusAssets 'errorStatusAssets' assets");
		println("Found $scene7MissingAssets assets missing from Scene7");
		println("Found $scene7UnpublishedAssets assets unpublished in Scene7");
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
			boolean unprocessed = false;
			boolean processing = false;
			boolean errorStatus = false;
			boolean scene7Missing = false;
			boolean scene7Unpublished = false;
			
			def createdDate = ns.getProperty("jcr:created").getValue(org.apache.jackrabbit.oak.api.Type.DATE);
			
			if(ns.hasChildNode("jcr:content")){
				def contentNode = ns.getChildNode("jcr:content");
				def status = contentNode.getString("status");
				def assetState = contentNode.getString("dam:assetState");
				
				if(!"processed".equals(assetState)){
					unprocessed = true;
					++unprocessedAssets;
				}
				
				if("Error".equals(status)){
					errorStatus = true;
					++errorStatusAssets;
				}
				
				if("processing".equals(assetState)){
					processing = true;
					++processingAssets;
				}
				
				if(scene7Required && contentNode.hasChildNode("metadata")){
					def metadataNode = contentNode.getChildNode("metadata");
					
					if(!metadataNode.hasProperty("dam:scene7File")){
						scene7Missing = true;
						++scene7MissingAssets;
					} else if(!"PublishComplete".equals(metadataNode.getString("dam:scene7FileStatus"))){
						scene7Unpublished = true;
						++scene7UnpublishedAssets;
					} 	
				}
				
				if(unprocessed || processing || errorStatus || scene7Missing || scene7Unpublished){
					println("$basePath,$createdDate,$unprocessed,$processing,$errorStatus,$scene7Missing,$scene7Unpublished");
				}
			
			} 
			
			++checkedAssetCount;
		} else if(nodeType.equals("sling:OrderedFolder") || nodeType.equals("sling:Folder")){
		
			def localS7Required = getLocalS7Required(ns);
			if(scene7Required && ("exclude".equals(localS7Required))){
				scene7Required = false;
			} else if ("include".equals(localS7Required)){
				scene7Required = true;
			}
			
			// Check child nodes
			ns.getChildNodeEntries().each { cne ->
				if(!"archive".equals(cne.getName()) && !"uncategorized".equals(cne.getName()) && !"manual-upload".equals(cne.getName())){
					traverseDAM(cne.getNodeState(),basePath+"/"+cne.getName(), scene7Required);	
				}
			}

		}
		
	}
	
}
	
new UnprocessedAssetsFinder(nodeStore: session.store).check()

