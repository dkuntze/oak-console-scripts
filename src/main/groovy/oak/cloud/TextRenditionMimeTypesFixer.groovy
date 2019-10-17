import org.apache.jackrabbit.oak.spi.commit.CommitInfo
import org.apache.jackrabbit.oak.spi.commit.EmptyHook
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.apache.jackrabbit.oak.commons.PathUtils
import com.google.common.collect.Lists
import java.util.List


class TextRenditionMimeTypeFixer {
	def NodeStore nodeStore;
    
    def validRenditionCount = 0 as long;
    def fixedRenditionCount = 0 as long;
    def checkedNodeCount = 0 as long;
    
    def traverse(ns, path){
	if(ns.getName()=='cqdam.text.txt'){
	    ns = ns.getChildNode('jcr:content');

	    if(ns.getString('jcr:mimeType')==null){
		ns.builder.setProperty('jcr:mimeType','text/plain');
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

	if(fixedRenditionCount % 1000 == 0){
	   println("Saving 1000 fixed renditions");	
	   ns.merge(rnb, EmptyHook.INSTANCE, CommitInfo.EMPTY);
	   println("Saved");	
	}

	// Check child nodes
	ns.getChildNodeEntries().each { cne ->
	    traverse(cne.getNodeState(), path+'/'+cne.getName());	
	}
    }
    
    
    
    def fixMimeTypes(){
        println("Fixing mimetypes");
        def timeStarted = new Date().getTime();
        
        traverse(nodeStore.getRoot().getChildNode("content").getChildNode("dam"), "/content/dam");
        
        def timeTaken = new Date().getTime() - timeStarted;
        
        println("Checked $checkedNodeCount nodes in ${timeTaken}ms, found ${validRenditionCount} valid text renditions and fixed ${fixedRenditionCount}");

        println("Done")
    }
}

new TextRenditionMimeTypeFixer(nodeStore: session.store).fixMimeTypes();

