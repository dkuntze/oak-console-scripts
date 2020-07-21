import org.apache.jackrabbit.oak.spi.state.*;
import org.apache.jackrabbit.oak.api.*;
import java.util.*;
import org.apache.jackrabbit.util.*;

class UnprocessedAssetsFinder {
	def NodeStore nodeStore;
	def checkedAssetCount = 0;
	
	def check(){
		traverseDAM(nodeStore.getRoot().getChildNode("content").getChildNode("dam"), "/content/dam");
		
		println("DONE - checked $checkedAssetCount assets");
	}

	def traverseDAM(ns, basePath){
		
		def nodeType = ns.getProperty("jcr:primaryType").getValue(org.apache.jackrabbit.oak.api.Type.NAME);

		if(nodeType.equals("dam:Asset")){

			def createdDate = ns.getProperty("jcr:created").getValue(org.apache.jackrabbit.oak.api.Type.DATE);
			
			if(ns.hasChildNode("jcr:content")){
				def contentNode = ns.getChildNode("jcr:content");
				
				if(contentNode.hasChildNode("renditions")){
					def renditionsNode = contentNode.getChildNode("renditions");
					
					if(!renditionsNode.hasChildNode("original")){
						println("Asset missing original : $basePath");
					}
				}
			
			} 
			
			if(checkedAssetCount % 1000 ==0){
						println("Checked $checkedAssetCount assets");
			}
			++checkedAssetCount;
		} else if(nodeType.equals("sling:OrderedFolder") || nodeType.equals("sling:Folder")){
		
			ns.getChildNodeEntries().each { cne ->
				traverseDAM(cne.getNodeState(),basePath+"/"+cne.getName());	
			}

		}
		
	}	
}
	
new UnprocessedAssetsFinder(nodeStore: session.store).check()