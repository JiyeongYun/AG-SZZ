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
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import hgu.csee.isel.alinew.szz.model.RevsInPath;
import hgu.csee.isel.alinew.szz.model.PathRevision;

public class AgSZZ {
	private final String GIT_DIR = "/Users/yoon/git/DataForSZZ";
	//private final String FIX_COMMIT = "768b0df07b2722db926e99a8f917deeb5b55d628";
	
	private static Git git;
	private Repository repo;
	
	
	public static void main(String[] args) {
		new AgSZZ().run();	
	}
	
	private void run() {
		try {
			git = Git.open(new File(GIT_DIR));
			repo = git.getRepository();
			
			List<RevCommit> commits = getCommits(git);
			
			// TODO make RevsInPath
			RevsInPath revsInPath = configureRevsInPath(repo, commits);

			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	private RevsInPath configureRevsInPath(Repository repo, List<RevCommit> commits) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		RevsInPath revsInPath = new RevsInPath();
		List<PathRevision> paths = new ArrayList<>();
		
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
		
		//TEST
//		Iterator<String> keys = revsInPath.keySet().iterator();
//		while( keys.hasNext() ){
//			String key = keys.next();
//			System.out.println("\tkey : " + key);
//			List<RevCommit> lst = revsInPath.get(key);
//			for(RevCommit rc : lst) {
//				String sha1 = rc.getName();
//				System.out.println( String.format("val : %s\n", sha1 ));
//			}
//		}
		
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
