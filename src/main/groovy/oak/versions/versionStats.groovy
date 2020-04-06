import com.google.common.base.Stopwatch
import groovy.transform.CompileStatic

import org.apache.jackrabbit.oak.api.PropertyState
import org.apache.jackrabbit.oak.api.Tree
import org.apache.jackrabbit.oak.api.Type
import org.apache.jackrabbit.oak.api.Root
import org.apache.jackrabbit.oak.commons.PathUtils
import org.apache.jackrabbit.oak.core.ImmutableRoot
import org.apache.jackrabbit.oak.plugins.tree.impl.TreeProviderService
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.apache.jackrabbit.oak.plugins.tree.TreeUtil

@CompileStatic
class VersionStats {

    NodeStore nodeStore;

    String version = "0.3"
    String token = version + "-" + System.currentTimeMillis()

    //
    // DEBUG
    //
    boolean debug = Boolean.getBoolean("stats.debug")

	String currentPath = ""
	Map<String, Tuple2<String, Integer>> versionsByUUID = [:]

    
    //
    // VERSION STORE STATS
    //
    String versionStorePath = "/jcr:system/jcr:versionStorage"

    def doStats() {
        println "Version Setup Stats Collector (v$version) " + new Date()
        Stopwatch watch = Stopwatch.createStarted()

        //Tree root = TreeUtil.createReadOnlyTree(nodeStore.root)
        Tree root = new TreeProviderService().createReadOnlyTree(nodeStore.root)
        collectVersions(TreeUtil.getTree(root, versionStorePath))

        // print metrics
        reportStats()
        
        println "Total time taken : $watch"
    }

    def collectVersions(Tree t) {
    
       switch (TreeUtil.getPrimaryTypeName(t)) {
            case 'rep:versionStorage' :
            	t.children.each {Tree c -> collectVersions(c)}
                break
            case 'nt:versionHistory' :
            	currentPath = TreeUtil.getString(t, "crx.default")
            	t.children.each {Tree c -> collectVersions(c)}
                break
            case 'nt:version' :
            	t.children.each {Tree c -> collectVersions(c)}
                break
            case 'nt:frozenNode' :
            	String uuid = TreeUtil.getString(t, "jcr:frozenUuid")
            	
            	Tuple2 uuidResult = versionsByUUID[uuid]
            	if(uuidResult==null){
            		Integer count = new Integer(0);
            		uuidResult = new Tuple2(currentPath,count)
            	} 
            	
            	Integer count = uuidResult.getSecond()
            	++count;
            	uuidResult = new Tuple2(currentPath,count)
            	
            	versionsByUUID[uuid] = uuidResult;

                break
            case 'nt:versionLabels' :
                break
        }
    }
    
    def reportStats() {
 		File file = new File("./versionReport.csv")
 		if(file.exists()){
 			file.delete();
 		}
		file.write "\"UUID\",\"Path\",\"Version Count\"\n"

        for (v in versionsByUUID) {
            file << "$v.key,\"$v.value.first\",$v.value.second\n"

        }
        
        println file.text

    }
    
}
new VersionStats(nodeStore: session.store).doStats();
