package hgu.csee.isel.alinew.szz.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import hgu.csee.isel.alinew.szz.model.PathRevision;
import hgu.csee.isel.alinew.szz.model.RevsWithPath;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AnnotationGraphBuilder {
	private Repository repo;
	private List<RevCommit> commits;

	public AnnotationGraphBuilder(Repository repo, List<RevCommit> commits) {
		super();
		this.repo = repo;
		this.commits = commits;
	}

	public Repository getRepo() {
		return repo;
	}

	public void setRepo(Repository repo) {
		this.repo = repo;
	}
	
	public AnnotationGraphModel buildAnnotationGraph() throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		List<PathRevision> paths = configurePathRevisionList(repo, commits);
		RevsWithPath revsWithPath = configureRevsWithPath(paths);
		
		Iterator<String> keys = revsWithPath.keySet().iterator();
		while( keys.hasNext() ){
			String path = keys.next();
//			System.out.println("\tkey : " + key);
			List<RevCommit> lst = revsWithPath.get(path);
			for(RevCommit child : lst) {
				if(lst.indexOf(child) == lst.size()-1) break;
				
				RevCommit parent = lst.get(lst.indexOf(child)+1);
				
				// Diff
				DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
				df.setRepository(repo);
				df.setPathFilter(PathFilter.create(path));
				List<DiffEntry> diffs = df.scan(parent.getTree(), child.getTree());
				
				
				for(DiffEntry diff : diffs) {
					
					String oldPath = diff.getOldPath();
					String newPath = diff.getNewPath();

					// get preFixSource and fixSource without comments
					String prevFileSource = Utils.fetchBlob(repo, parent, oldPath);
					String fileSource = Utils.fetchBlob(repo, child, newPath);
					
					System.out.println("prev: "+prevFileSource);
					System.out.println("current: "+fileSource);
					
					// get line indices that are related to BI lines.
					EditList editList = Utils.getEditListFromDiff(prevFileSource, fileSource);
					for (Edit edit : editList) {

						int beginA = edit.getBeginA();
						int endA = edit.getEndA();
						int beginB = edit.getBeginB();
						int endB = edit.getEndB();

						// TEST
						System.out.println("Type : " + edit.getType());
						System.out.println("beginA : " + beginA);
						System.out.println("endA : " + endA);
						System.out.println("beginB : " + beginB);
						System.out.println("endB : " + endB);
						System.out.println("");

						
					}
				}
			}
		}
		
		return null;
	}
	
	private List<PathRevision> configurePathRevisionList(Repository repo, List<RevCommit> commits) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		List<PathRevision> paths = new ArrayList<>();
		
		for(RevCommit commit : commits) {
			RevTree tree = commit.getTree();
			
			TreeWalk treeWalk = new TreeWalk(repo);
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			treeWalk.setFilter(PathSuffixFilter.create(".java"));
			
			while(treeWalk.next()) {
				String path = treeWalk.getPathString();
				
				paths.add(new PathRevision(path, commit));			
			}
		}
		
		return paths;
	}
	
	private RevsWithPath configureRevsWithPath(List<PathRevision> paths) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		RevsWithPath revsInPath = new RevsWithPath();
		
		for(PathRevision pr : paths) {
			if(revsInPath.containsKey(pr.getPath())) {
				List<RevCommit> lst = revsInPath.get(pr.getPath());
				lst.add(pr.getCommit());
				revsInPath.replace(pr.getPath(), lst);
			}else{
				List<RevCommit> lst = new ArrayList<>();
				lst.add(pr.getCommit());
				revsInPath.put(pr.getPath(), lst);
			}
		}
		
		return revsInPath;
	}

}
