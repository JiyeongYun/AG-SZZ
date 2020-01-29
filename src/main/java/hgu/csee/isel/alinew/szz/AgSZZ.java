package hgu.csee.isel.alinew.szz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import hgu.csee.isel.alinew.szz.model.RevsInPath;
import hgu.csee.isel.alinew.szz.model.PathRevision;

public class AgSZZ {
	private final String GIT_DIR = "/Users/yoon/git/DataForSZZ";
	//private final String FIX_COMMIT = "768b0df07b2722db926e99a8f917deeb5b55d628";
	
	private static Git git;
	private Repository repo;
	private List<PathRevision> paths = new ArrayList<>();
	
	
	public static void main(String[] args) {
		new AgSZZ().run();	
	}
	
	private void run() {
		try {
			git = Git.open(new File(GIT_DIR));
			repo = git.getRepository();
			
			List<RevCommit> commits = getCommits(git);
			paths = configurePathRevisionList(repo, commits);
			RevsInPath revsInPath = configureRevsInPath(repo, commits);
			
			
			Iterator<String> keys = revsInPath.keySet().iterator();
			while( keys.hasNext() ){
				String path = keys.next();
//				System.out.println("\tkey : " + key);
				List<RevCommit> lst = revsInPath.get(path);
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
						String prevFileSource = fetchBlob(repo, parent, oldPath);
						String fileSource = fetchBlob(repo, child, newPath);
						
						System.out.println("prev: "+prevFileSource);
						System.out.println("current: "+fileSource);
						
						// get line indices that are related to BI lines.
						EditList editList = getEditListFromDiff(prevFileSource, fileSource);
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
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	static public String fetchBlob(Repository repo, RevCommit commit, String path) throws IncorrectObjectTypeException, IOException{

		// Makes it simpler to release the allocated resources in one go
		ObjectReader reader = repo.newObjectReader();

		// Get the revision's file tree
		RevTree tree = commit.getTree();
		// .. and narrow it down to the single file's path
		TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

		if (treewalk != null) {
			// use the blob id to read the file's data
			byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
			reader.close();
			return new String(data, "utf-8");
		} else {
			return "";
		}

	}
	
	static public DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS);
	static public RawTextComparator diffComparator = RawTextComparator.WS_IGNORE_ALL;
	static public EditList getEditListFromDiff(String file1, String file2) {
		RawText rt1 = new RawText(file1.getBytes());
		RawText rt2 = new RawText(file2.getBytes());
		EditList diffList = new EditList();

		diffList.addAll(diffAlgorithm
				.diff(diffComparator, rt1, rt2));
		return diffList;
	}
	
	private List<PathRevision> configurePathRevisionList(Repository repo, List<RevCommit> commits) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException{
		for(RevCommit commit : commits) {
			RevTree tree = commit.getTree();
			
			TreeWalk treeWalk = new TreeWalk(repo);
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			treeWalk.setFilter(PathSuffixFilter.create(".java"));
			
			//TEST
//			System.out.println("\nRev : " + commit.getName() + "\n");
			
			while(treeWalk.next()) {
				String path = treeWalk.getPathString();
				
				//TEST
//				System.out.println("found: " + path);
				
				paths.add(new PathRevision(path, commit));			
			}
		
		}
		
		return paths;
	}
	
	private RevsInPath configureRevsInPath(Repository repo, List<RevCommit> commits) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		RevsInPath revsInPath = new RevsInPath();
		
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
	
	
	private List<RevCommit> getCommits(Git git) {
		List<RevCommit> commits = new ArrayList<>();
	
		Iterable<RevCommit> logs;
		
		try {
			logs = git.log().call();
			
			for(RevCommit rev:logs) {
				commits.add(rev);
			}
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return commits;
	}

}
