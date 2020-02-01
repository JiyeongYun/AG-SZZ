package hgu.csee.isel.alinew.szz;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import hgu.csee.isel.alinew.szz.exception.UnknownHunkTypeException;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphBuilder;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphModel;
import hgu.csee.isel.alinew.szz.model.PathRevision;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AgSZZ {
	private final String GIT_DIR = "/Users/kimseokjin/git/DataForSZZ";
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
			List<RevCommit> commits = Utils.getCommits(git);
			
			// Phase 1 : Build the annotation graph
			AnnotationGraphBuilder agb = new AnnotationGraphBuilder(repo, commits);
			AnnotationGraphModel agm = agb.buildAnnotationGraph();
			
			//TEST
//			Iterator<PathRevision> pathRevisions = agm.keySet().iterator();
//			int cnt = 0;
//			while(pathRevisions.hasNext()) {
//				
//				System.out.println("iter : " + cnt);
//				System.out.println("key : " + pathRevisions.next().getCommit().getName());
//				
//				cnt++;
//			}
				
			
			
			// TODO Phase 2 : Trace and collect BIC candidates
			// TODO Phase 3 : Filter out format changes, comments, etc among BIC candidates
			
		} catch (IOException | GitAPIException | UnknownHunkTypeException e) {
			
			e.printStackTrace();
		} 
	}
}
