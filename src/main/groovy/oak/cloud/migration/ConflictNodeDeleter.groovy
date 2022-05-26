import org.apache.jackrabbit.oak.spi.commit.EmptyHook
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.apache.jackrabbit.oak.commons.PathUtils
import org.apache.jackrabbit.oak.spi.commit.CommitInfo
import com.google.common.collect.Lists
import java.util.List

/**
 * A brute force conflict node removal utility. This class is meant to be run via the oak-run console.
 */
class ConflictNodeDeleter {
	def NodeStore nodeStore;

	def validRenditionCount = 0 as long;
	def fixedRenditionCount = 0 as long;
	def checkedNodeCount = 0 as long;
	def needsSave = false;

	def traverse(ns, nb, path, name, rnb){

		if (name==':conflict') {
			nb.remove();
			println("Removed node at " + path);
			++fixedRenditionCount;
		} else {
			++validRenditionCount;
		}


		++checkedNodeCount;
		if(checkedNodeCount % 1000 == 0){
			println("Checked $checkedNodeCount");
		}

		if(needsSave &&  fixedRenditionCount % 1000 == 0){
			println("Saving 1000 fixed renditions");
			nodeStore.merge(rnb, EmptyHook.INSTANCE, CommitInfo.EMPTY);
			println("Saved");
			needsSave=false;
		}
		// Check child nodes
		ns.getChildNodeEntries().each { cne ->
			traverse(cne.getNodeState(), nb.getChildNode(cne.getName()), path+'/'+cne.getName(), cne.getName(), rnb);
		}
	}



	def fixIt() {
		println("Finding/Fixing conflict nodes...");
		def timeStarted = new Date().getTime();

		def rootNodeBuilder = nodeStore.getRoot().builder();
		def nodeState = nodeStore.getRoot().getChildNode("content").getChildNode("dam");
		def nodeBuilder = rootNodeBuilder.getChildNode("content").getChildNode("dam");

		traverse(nodeState, nodeBuilder, "/content/dam", "dam", rootNodeBuilder);
		if(needsSave){
			nodeStore.merge(rootNodeBuilder, EmptyHook.INSTANCE, CommitInfo.EMPTY);
		}
		def timeTaken = new Date().getTime() - timeStarted;

		println("Checked $checkedNodeCount nodes in ${timeTaken}ms, found ${validRenditionCount} valid nodes and fixed ${fixedRenditionCount}");

		println("Done")
	}
}

new ConflictNodeDeleter(nodeStore: session.store).fixIt();