package hgu.csee.isel.alinew.szz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.Edit.Type;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import hgu.csee.isel.alinew.szz.exception.EmptyHunkTypeException;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphBuilder;
import hgu.csee.isel.alinew.szz.graph.AnnotationGraphModel;
import hgu.csee.isel.alinew.szz.model.Line;
import hgu.csee.isel.alinew.szz.model.LineType;
import hgu.csee.isel.alinew.szz.trace.Tracer;
import hgu.csee.isel.alinew.szz.util.Utils;

public class AgSZZ {
	private final String GIT_DIR = "/Users/kimseokjin/git/DataForSZZ";
	private final String FIX_COMMIT = "768b0df07b2722db926e99a8f917deeb5b55d628";
	private List<String> BFCList = new ArrayList<>();
	
	private static Git git;
	private Repository repo;
	
	
	public static void main(String[] args) {
		new AgSZZ().run();	
	}
	
	private void run() {
		try {
			git = Git.open(new File(GIT_DIR));
			repo = git.getRepository();
			List<RevCommit> revs = Utils.getRevs(git);
			BFCList.add(FIX_COMMIT);
			
			// Phase 1 : Build the annotation graph
			AnnotationGraphBuilder agb = new AnnotationGraphBuilder(repo, revs);
			AnnotationGraphModel agm = agb.buildAnnotationGraph();
			
			// TEST
//			Iterator<RevCommit> iter = agm.keySet().iterator();
//			
//			int revCnt = 1;
//			while(iter.hasNext()) {
//				
//				RevCommit rev = iter.next();
//				
//				System.out.println("\nRev Count : " + revCnt + "=========================\n");
//				
//				System.out.println("rev : " + rev.getName());
//				HashMap<String, ArrayList<Line>> map = agm.get(rev);
//				Iterator<String> iter2 = map.keySet().iterator();
//				
//				while(iter2.hasNext()) {
//					String path = iter2.next();
//					System.out.println("path : " + path);
//					ArrayList<Line> lines = map.get(path);
//					
//					for(Line l : lines) {
//						System.out.println("\trev : " + l.getRev());
//						System.out.println("\tpath : " + l.getPath() + "\n\n");
//						
//						System.out.println("content : " + l.getContent());
//						System.out.println("index : " + l.getIdx());
//						System.out.println("type : " + l.getLineType() + "\n") ;
//					}
//					
//				}
//				
//				revCnt++;
//			}
			
			// TODO Phase 2 : Trace and collect BIC candidates
			Tracer tracer = new Tracer();
			List<Line> BILines = tracer.collectBILines(repo, revs, agm, BFCList);
			
			//TEST
			for(Line line : BILines) {
				System.out.println("BIC: "+line.getIdx());
				System.out.println("Path: "+line.getPath());
				System.out.println("Revision: "+line.getRev());
				System.out.println("Content: "+line.getContent() +"\n");
			}
			
			// TODO Phase 3 : Filter out format changes, comments, etc among BIC candidates
			
			
			
			
		} catch (IOException | GitAPIException | EmptyHunkTypeException e) {
			e.printStackTrace();
		} 
	}
}
