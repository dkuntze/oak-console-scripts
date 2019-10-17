import org.apache.jackrabbit.oak.spi.commit.EmptyHook
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.apache.jackrabbit.oak.commons.PathUtils
import org.apache.jackrabbit.oak.spi.commit.CommitInfo
import com.google.common.collect.Lists
import java.util.List


class TextRenditionMimeTypeFixer {
	def NodeStore nodeStore;
    
    def validRenditionCount = 0 as long;
    def fixedRenditionCount = 0 as long;
    def checkedNodeCount = 0 as long;
    
    def traverse(ns, nb, path, name, rnb){
	if(name=='cqdam.text.txt'){
	    
	    if(nb.hasProperty('jcr:mimeType')){
		nb.removeProperty('jcr:mimeType');
	    }
		
		
	    ns = ns.getChildNode('jcr:content');
	    nb = nb.getChildNode('jcr:content');
	    if(ns.getString('jcr:mimeType')==null){
		nb.setProperty('jcr:mimeType','text/plain');
		println("Updated mimetype at "+path);
		++fixedRenditionCount;
	    } else {
		++validRenditionCount;   
	    }
	}

	++checkedNodeCount;
	if(checkedNodeCount % 1000 == 0){
	    println("Checked $checkedNodeCount");	
	}

	if(fixedRenditionCount > 0 &&  fixedRenditionCount % 1000 == 0){
	   println("Saving 1000 fixed renditions");	
	   nodeStore.merge(rnb, EmptyHook.INSTANCE, CommitInfo.EMPTY);
	   println("Saved");	
	}

	// Check child nodes
	ns.getChildNodeEntries().each { cne ->
	    traverse(cne.getNodeState(), nb.getChildNode(cne.getName()), path+'/'+cne.getName(), cne.getName(), rnb);	
	}
    }
    
    
    
    def fixMimeTypes(){
        println("Fixing mimetypes");
        def timeStarted = new Date().getTime();
        
	def rootNodeBuilder = nodeStore.getRoot().builder();
	def nodeState = nodeStore.getRoot().getChildNode("content").getChildNode("dam").getChildNode("marketing");
	def nodeBuilder = rootNodeBuilder.getChildNode("content").getChildNode("dam").getChildNode("marketing");
	    
        traverse(nodeState, nodeBuilder, "/content/dam/marketing", "marketing", rootNodeBuilder);
        nodeStore.merge(rootNodeBuilder, EmptyHook.INSTANCE, CommitInfo.EMPTY);
        def timeTaken = new Date().getTime() - timeStarted;
        
        println("Checked $checkedNodeCount nodes in ${timeTaken}ms, found ${validRenditionCount} valid text renditions and fixed ${fixedRenditionCount}");

        println("Done")
    }
}

new TextRenditionMimeTypeFixer(nodeStore: session.store).fixMimeTypes();
